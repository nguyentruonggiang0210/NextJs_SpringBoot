"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Client, IMessage } from "@stomp/stompjs";
import { API_BASE } from "../../lib/config";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface ChatMsg {
  type: "CHAT" | "JOIN" | "LEAVE";
  content: string;
  senderId: number;
  senderName: string;
  receiverId?: number;
  timestamp: string;
}

interface UserInfo {
  id: number;
  name: string;
  email: string;
  phone: string;
  permissionName: string | null;
}

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

/**
 * Formats an ISO timestamp string to a short HH:mm display.
 *
 * @param ts ISO-8601 timestamp
 * @returns formatted time string or empty string
 */
function formatTime(ts: string): string {
  if (!ts) return "";
  try {
    return new Date(ts).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return ts.slice(11, 16);
  }
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function ChatPage() {
  const router = useRouter();

  // Current logged-in user
  const [me, setMe] = useState<UserInfo | null>(null);
  const [accessToken, setAccessToken] = useState<string>("");

  // User list for private chat selection
  const [users, setUsers] = useState<UserInfo[]>([]);

  // Selected conversation: null = public chat, otherwise a userId
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  // All received messages
  const [publicMessages, setPublicMessages] = useState<ChatMsg[]>([]);
  const [privateMessages, setPrivateMessages] = useState<
    Record<number, ChatMsg[]>
  >({});

  // Draft input
  const [draft, setDraft] = useState("");

  // Connection status
  const [connected, setConnected] = useState(false);

  const clientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  // Stable ref so WebSocket callbacks always see the latest me without
  // needing me as a dependency (avoids reconnect on every me change).
  const meRef = useRef<UserInfo | null>(null);
  useEffect(() => { meRef.current = me; }, [me]);

  // ---------------------------------------------------------------------------
  // Auth check on mount — redirect to /login if not authenticated,
  // redirect to /forbidden if authenticated but lacks member/admin role.
  useEffect(() => {
    const raw = localStorage.getItem("user");
    const token = localStorage.getItem("accessToken") ?? "";
    if (!raw || !token) {
      router.push("/login");
      return;
    }
    const user: UserInfo = JSON.parse(raw);
    const role = user?.permissionName;
    if (role !== "member" && role !== "admin") {
      router.push("/forbidden");
      return;
    }
    setMe(user);
    setAccessToken(token);
  }, [router]);

  // ---------------------------------------------------------------------------
  // Fetch user list
  // ---------------------------------------------------------------------------
  useEffect(() => {
    if (!accessToken) return;
    fetch(`${API_BASE}/api/users`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
      .then((r) => r.json())
      .then((data: UserInfo[]) => setUsers(data))
      .catch(() => {});
  }, [accessToken]);

  // ---------------------------------------------------------------------------
  // WebSocket connection
  // ---------------------------------------------------------------------------
  const handlePublicMessage = useCallback((msg: IMessage) => {
    const data: ChatMsg = JSON.parse(msg.body);
    setPublicMessages((prev) => [...prev, data]);
  }, []);

  const handlePrivateMessage = useCallback((msg: IMessage) => {
    const data: ChatMsg = JSON.parse(msg.body);
    // Read me via ref — no need to have me in deps, avoids nested setState.
    const currentMe = meRef.current;
    const otherId =
      currentMe && data.senderId === currentMe.id
        ? data.receiverId!
        : data.senderId;
    setPrivateMessages((prev) => ({
      ...prev,
      [otherId]: [...(prev[otherId] ?? []), data],
    }));
  }, []);  // meRef is a stable ref, safe to omit from deps

  useEffect(() => {
    if (!accessToken || !me) return;

    const stompClient = new Client({
      /**
       * Factory that creates a SockJS connection to the backend WebSocket endpoint.
       * SockJS is imported dynamically here to prevent a server-side
       * "window is not defined" crash during Next.js pre-rendering.
       *
       * @returns SockJS instance
       */
      webSocketFactory: () => {
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const SockJS = require("sockjs-client") as new (url: string) => WebSocket;
        return new SockJS(`${API_BASE}/ws`);
      },
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);

        // Subscribe to public channel
        stompClient.subscribe("/topic/public", handlePublicMessage);

        // Subscribe to private messages for this user
        stompClient.subscribe("/user/queue/messages", handlePrivateMessage);

        // Announce presence
        stompClient.publish({
          destination: "/app/chat.join",
          body: JSON.stringify({
            type: "JOIN",
            content: "",
            senderName: me.name,
            senderId: me.id,
          }),
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    stompClient.activate();
    clientRef.current = stompClient;

    return () => {
      stompClient.deactivate();
    };
  }, [accessToken, me, handlePublicMessage, handlePrivateMessage]);

  // ---------------------------------------------------------------------------
  // Auto-scroll to latest message
  // ---------------------------------------------------------------------------
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [publicMessages, privateMessages, selectedUserId]);

  // ---------------------------------------------------------------------------
  // Send message
  // ---------------------------------------------------------------------------
  /**
   * Sends the current draft as either a public or private message via STOMP.
   */
  function sendMessage() {
    const text = draft.trim();
    if (!text || !clientRef.current?.connected || !me) return;

    if (selectedUserId === null) {
      // Public broadcast
      clientRef.current.publish({
        destination: "/app/chat.sendPublic",
        body: JSON.stringify({
          type: "CHAT",
          content: text,
          senderName: me.name,
          senderId: me.id,
        }),
      });
    } else {
      // Private direct message
      clientRef.current.publish({
        destination: "/app/chat.sendPrivate",
        body: JSON.stringify({
          type: "CHAT",
          content: text,
          senderName: me.name,
          senderId: me.id,
          receiverId: selectedUserId,
        }),
      });
    }

    setDraft("");
  }

  // ---------------------------------------------------------------------------
  // Derived data
  // ---------------------------------------------------------------------------
  const currentMessages =
    selectedUserId === null
      ? publicMessages
      : (privateMessages[selectedUserId] ?? []);

  const selectedUserName =
    users.find((u) => u.id === selectedUserId)?.name ?? "";

  // Count unread private messages per user (simplified: just count)
  const unreadCount = (userId: number) =>
    (privateMessages[userId] ?? []).length;

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  if (!me) return null;

  return (
    <div className="flex h-[calc(100vh-3.5rem)] bg-zinc-100 dark:bg-zinc-900">
      {/* ------------------------------------------------------------------ */}
      {/* Left sidebar — conversation list                                    */}
      {/* ------------------------------------------------------------------ */}
      <aside className="w-64 shrink-0 bg-white dark:bg-zinc-800 border-r border-zinc-200 dark:border-zinc-700 flex flex-col">
        {/* Header */}
        <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700">
          <p className="text-xs text-zinc-500 dark:text-zinc-400">
            Đã đăng nhập:
          </p>
          <p className="text-sm font-semibold text-zinc-800 dark:text-zinc-100 truncate">
            {me.name}
          </p>
          <span
            className={`inline-block mt-1 text-xs font-medium px-2 py-0.5 rounded-full ${
              connected
                ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                : "bg-zinc-200 text-zinc-500 dark:bg-zinc-700 dark:text-zinc-400"
            }`}
          >
            {connected ? "Đã kết nối" : "Đang kết nối..."}
          </span>
        </div>

        {/* Conversation items */}
        <nav className="flex-1 overflow-y-auto py-2">
          {/* Public chat */}
          <button
            onClick={() => setSelectedUserId(null)}
            className={`w-full text-left px-4 py-2.5 transition-colors ${
              selectedUserId === null
                ? "bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 font-semibold"
                : "text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-700"
            }`}
          >
            <span className="text-sm">💬 Trò chuyện chung</span>
          </button>

          <div className="mx-4 my-2 border-t border-zinc-200 dark:border-zinc-700" />
          <p className="px-4 text-xs text-zinc-400 uppercase tracking-wide mb-1">
            Nhắn tin riêng
          </p>

          {users
            .filter((u) => u.id !== me.id)
            .map((u) => {
              const count = unreadCount(u.id);
              return (
                <button
                  key={u.id}
                  onClick={() => setSelectedUserId(u.id)}
                  className={`w-full text-left px-4 py-2.5 flex items-center justify-between transition-colors ${
                    selectedUserId === u.id
                      ? "bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 font-semibold"
                      : "text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-700"
                  }`}
                >
                  <span className="text-sm truncate">{u.name}</span>
                  {count > 0 && (
                    <span className="ml-2 text-xs bg-blue-600 text-white rounded-full px-1.5 py-0.5 shrink-0">
                      {count}
                    </span>
                  )}
                </button>
              );
            })}
        </nav>
      </aside>

      {/* ------------------------------------------------------------------ */}
      {/* Main chat area                                                       */}
      {/* ------------------------------------------------------------------ */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Chat header */}
        <div className="h-12 px-5 flex items-center border-b border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 shrink-0">
          <h2 className="text-sm font-semibold text-zinc-800 dark:text-zinc-100">
            {selectedUserId === null
              ? "💬 Trò chuyện chung"
              : `✉️ ${selectedUserName}`}
          </h2>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
          {currentMessages.length === 0 && (
            <p className="text-sm text-zinc-400 text-center mt-10">
              {selectedUserId === null
                ? "Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!"
                : `Bắt đầu nhắn tin với ${selectedUserName}`}
            </p>
          )}

          {currentMessages.map((msg, idx) => {
            const isMe = msg.senderId === me.id;

            if (msg.type === "JOIN" || msg.type === "LEAVE") {
              return (
                <div key={idx} className="flex justify-center">
                  <span className="text-xs text-zinc-400 bg-zinc-200 dark:bg-zinc-700 px-3 py-1 rounded-full">
                    {msg.senderName}{" "}
                    {msg.type === "JOIN" ? "đã tham gia" : "đã rời khỏi"} chat
                  </span>
                </div>
              );
            }

            return (
              <div
                key={idx}
                className={`flex flex-col ${isMe ? "items-end" : "items-start"}`}
              >
                {!isMe && (
                  <span className="text-xs text-zinc-500 dark:text-zinc-400 mb-0.5 px-1">
                    {msg.senderName}
                  </span>
                )}
                <div
                  className={`max-w-xs lg:max-w-md px-4 py-2 rounded-2xl text-sm ${
                    isMe
                      ? "bg-blue-600 text-white rounded-br-sm"
                      : "bg-white dark:bg-zinc-700 text-zinc-800 dark:text-zinc-100 shadow-sm rounded-bl-sm"
                  }`}
                >
                  {msg.content}
                </div>
                <span className="text-xs text-zinc-400 mt-0.5 px-1">
                  {formatTime(msg.timestamp)}
                </span>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        {/* Input bar */}
        <div className="px-5 py-3 border-t border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 shrink-0">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              sendMessage();
            }}
            className="flex gap-2"
          >
            <input
              type="text"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={
                selectedUserId === null
                  ? "Nhắn tin tới mọi người..."
                  : `Nhắn tin tới ${selectedUserName}...`
              }
              disabled={!connected}
              className="flex-1 rounded-xl border border-zinc-300 dark:border-zinc-600 bg-zinc-50 dark:bg-zinc-700 px-4 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={!connected || !draft.trim()}
              className="px-5 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-zinc-300 dark:disabled:bg-zinc-600 text-white text-sm font-medium rounded-xl transition-colors"
            >
              Gửi
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
