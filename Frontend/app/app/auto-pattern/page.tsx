"use client";

import { useEffect, useRef, useState } from "react";
import { Client, IMessage } from "@stomp/stompjs";
import { API_BASE } from "../../lib/config";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface TriggerRequest {
  patternType: string;
  payload: string;
  userId: number;
  priority?: number;
  metadata?: string;
}

interface TriggerResponse {
  jobId: string;
  status: string;
  message: string;
}

interface JobResult {
  id: number;
  jobId: string;
  patternType: string;
  payload: string;
  userId: number;
  status: string;
  result?: string;
  errorMessage?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
}

interface UserInfo {
  id: number;
  name: string;
  email: string;
  phone: string;
  permissionName: string | null;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function AutoPatternPage() {
  const [me, setMe] = useState<UserInfo | null>(null);
  const [accessToken, setAccessToken] = useState<string>("");

  // Form state
  const [patternType, setPatternType] = useState("DATA_ANALYSIS");
  const [payload, setPayload] = useState('{"data": "sample"}');
  const [priority, setPriority] = useState(1);
  const [metadata, setMetadata] = useState("");

  // Job results
  const [jobs, setJobs] = useState<JobResult[]>([]);
  const [currentJobId, setCurrentJobId] = useState<string | null>(null);

  // UI state
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);

  const clientRef = useRef<Client | null>(null);

  // ---------------------------------------------------------------------------
  // Auth check (optional for guest access)
  // ---------------------------------------------------------------------------
  useEffect(() => {
    const raw = localStorage.getItem("user");
    const token = localStorage.getItem("accessToken") ?? "";
    
    if (raw && token) {
      const user: UserInfo = JSON.parse(raw);
      setMe(user);
      setAccessToken(token);
    }
    // Allow access without authentication
  }, []);

  // ---------------------------------------------------------------------------
  // WebSocket connection
  // ---------------------------------------------------------------------------
  useEffect(() => {
    if (!accessToken || !me) return;

    const stompClient = new Client({
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

        // Subscribe to job results for this user
        stompClient.subscribe(`/topic/job-results/${me.id}`, (msg: IMessage) => {
          const jobResult: JobResult = JSON.parse(msg.body);
          setJobs((prev) => {
            const existing = prev.findIndex((j) => j.jobId === jobResult.jobId);
            if (existing >= 0) {
              const updated = [...prev];
              updated[existing] = jobResult;
              return updated;
            }
            return [jobResult, ...prev];
          });

          // Update current job if matches
          if (currentJobId === jobResult.jobId) {
            setCurrentJobId(jobResult.jobId);
          }
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
  }, [accessToken, me, currentJobId]);

  // ---------------------------------------------------------------------------
  // Trigger job
  // ---------------------------------------------------------------------------
  const handleTrigger = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!me || loading) {
      if (!me) {
        alert("Vui lòng đăng nhập để sử dụng tính năng này");
        window.location.href = "/login";
      }
      return;
    }

    setLoading(true);

    try {
      const request: TriggerRequest = {
        patternType,
        payload,
        userId: me.id,
        priority,
        metadata: metadata || undefined,
      };

      const response = await fetch(`${API_BASE}/api/auto-pattern/trigger`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error("Failed to trigger job");
      }

      const data: TriggerResponse = await response.json();
      setCurrentJobId(data.jobId);

      // Poll for initial job status
      const statusResponse = await fetch(`${API_BASE}/api/auto-pattern/status/${data.jobId}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });

      if (statusResponse.ok) {
        const jobResult: JobResult = await statusResponse.json();
        setJobs((prev) => [jobResult, ...prev]);
      }
    } catch (error) {
      console.error("Error triggering job:", error);
      alert("Failed to trigger job");
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  // Allow rendering without authentication for guest access

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-black p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
          Auto Pattern Trigger
        </h1>
        <p className="text-zinc-600 dark:text-zinc-400 mb-6">
          Gửi message để xử lý và nhận kết quả qua WebSocket
        </p>

        {/* Connection status */}
        <div className="mb-6">
          <span
            className={`inline-block text-sm font-medium px-3 py-1 rounded-full ${
              connected
                ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                : "bg-zinc-200 text-zinc-500 dark:bg-zinc-700 dark:text-zinc-400"
            }`}
          >
            {connected ? "Đã kết nối WebSocket" : "Đang kết nối..."}
          </span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Trigger Form */}
          <div className="bg-white dark:bg-zinc-800 rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
              Gửi Message
            </h2>
            <form onSubmit={handleTrigger} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                  Pattern Type
                </label>
                <select
                  value={patternType}
                  onChange={(e) => setPatternType(e.target.value)}
                  className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-zinc-50 dark:bg-zinc-700 px-4 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="DATA_ANALYSIS">DATA_ANALYSIS</option>
                  <option value="REPORT_GENERATION">REPORT_GENERATION</option>
                  <option value="NOTIFICATION">NOTIFICATION</option>
                  <option value="FILE_CONVERTER">FILE_CONVERTER</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                  Payload (JSON)
                </label>
                <textarea
                  value={payload}
                  onChange={(e) => setPayload(e.target.value)}
                  rows={4}
                  className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-zinc-50 dark:bg-zinc-700 px-4 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                  Priority
                </label>
                <input
                  type="number"
                  value={priority}
                  onChange={(e) => setPriority(parseInt(e.target.value))}
                  min={1}
                  max={10}
                  className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-zinc-50 dark:bg-zinc-700 px-4 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                  Metadata (optional)
                </label>
                <input
                  type="text"
                  value={metadata}
                  onChange={(e) => setMetadata(e.target.value)}
                  className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-zinc-50 dark:bg-zinc-700 px-4 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <button
                type="submit"
                disabled={loading || !connected}
                className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-zinc-300 dark:disabled:bg-zinc-600 text-white text-sm font-medium rounded-lg transition-colors"
              >
                {loading ? "Đang gửi..." : "Gửi Message"}
              </button>
            </form>
          </div>

          {/* Job Results */}
          <div className="bg-white dark:bg-zinc-800 rounded-xl shadow-sm p-6">
            <h2 className="text-xl font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
              Kết quả Jobs
            </h2>
            <div className="space-y-3 max-h-[500px] overflow-y-auto">
              {jobs.length === 0 && (
                <p className="text-sm text-zinc-400 text-center py-8">
                  Chưa có job nào được gửi
                </p>
              )}

              {jobs.map((job) => (
                <div
                  key={job.jobId}
                  className={`p-4 rounded-lg border ${
                    job.status === "COMPLETED"
                      ? "border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-900/20"
                      : job.status === "FAILED"
                      ? "border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20"
                      : job.status === "PROCESSING"
                      ? "border-blue-200 bg-blue-50 dark:border-blue-800 dark:bg-blue-900/20"
                      : "border-zinc-200 bg-zinc-50 dark:border-zinc-700 dark:bg-zinc-700"
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-mono text-zinc-500 dark:text-zinc-400">
                      {job.jobId}
                    </span>
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                        job.status === "COMPLETED"
                          ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                          : job.status === "FAILED"
                          ? "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
                          : job.status === "PROCESSING"
                          ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                          : "bg-zinc-200 text-zinc-600 dark:bg-zinc-700 dark:text-zinc-400"
                      }`}
                    >
                      {job.status}
                    </span>
                  </div>

                  <p className="text-sm text-zinc-700 dark:text-zinc-300 mb-1">
                    <strong>Type:</strong> {job.patternType}
                  </p>

                  {job.result && (
                    <div className="mt-2">
                      <p className="text-xs text-zinc-500 dark:text-zinc-400 mb-1">
                        Result:
                      </p>
                      <p className="text-sm text-zinc-800 dark:text-zinc-100 bg-white dark:bg-zinc-900 p-2 rounded border border-zinc-200 dark:border-zinc-600">
                        {job.result}
                      </p>
                    </div>
                  )}

                  {job.errorMessage && (
                    <div className="mt-2">
                      <p className="text-xs text-red-500 dark:text-red-400 mb-1">
                        Error:
                      </p>
                      <p className="text-sm text-red-700 dark:text-red-300">
                        {job.errorMessage}
                      </p>
                    </div>
                  )}

                  <div className="mt-2 text-xs text-zinc-400 dark:text-zinc-500">
                    {job.completedAt
                      ? `Hoàn thành: ${new Date(job.completedAt).toLocaleString()}`
                      : job.startedAt
                      ? `Bắt đầu: ${new Date(job.startedAt).toLocaleString()}`
                      : `Tạo: ${new Date(job.createdAt).toLocaleString()}`}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
