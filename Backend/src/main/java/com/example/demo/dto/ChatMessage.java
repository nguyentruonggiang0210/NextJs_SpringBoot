package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload transferred over WebSocket STOMP for chat messages.
 * Handles both public (broadcast) and private (direct) messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** Message type: CHAT for normal messages, JOIN/LEAVE for presence events. */
    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    private MessageType type;

    /** Text content of the message. */
    private String content;

    /** ID of the user who sent the message (set by server from Principal). */
    private Long senderId;

    /** Display name of the sender. */
    private String senderName;

    /**
     * Target user ID for private messages; null means a public broadcast.
     */
    private Long receiverId;

    /** ISO-8601 timestamp set by server when the message is processed. */
    private String timestamp;
}
