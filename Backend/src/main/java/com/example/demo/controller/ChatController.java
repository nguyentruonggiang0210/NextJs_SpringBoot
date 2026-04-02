package com.example.demo.controller;

import com.example.demo.dto.ChatMessage;
import com.example.demo.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles STOMP WebSocket messages for real-time chat.
 * Supports both public broadcast and private direct messages.
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JwtService jwtService;

    public ChatController(SimpMessagingTemplate messagingTemplate, JwtService jwtService) {
        this.messagingTemplate = messagingTemplate;
        this.jwtService = jwtService;
    }

    /**
     * Handles a public chat message and broadcasts it to all subscribers of /topic/public.
     *
     * @param message   the incoming chat payload
     * @param principal the authenticated WebSocket user (userId as name)
     * @return the enriched ChatMessage sent to /topic/public
     */
    @MessageMapping("/chat.sendPublic")
    @SendTo("/topic/public")
    public ChatMessage sendPublicMessage(@Payload ChatMessage message, Principal principal) {
        if (principal != null) {
            message.setSenderId(Long.parseLong(principal.getName()));
        }
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return message;
    }

    /**
     * Handles a private chat message and routes it to the target user's queue.
     * The message is echoed back to the sender as well.
     *
     * @param message   the incoming chat payload; receiverId must be set
     * @param principal the authenticated WebSocket user
     */
    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessage message, Principal principal) {
        if (principal == null || message.getReceiverId() == null) {
            return;
        }

        message.setSenderId(Long.parseLong(principal.getName()));
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String receiverIdStr = String.valueOf(message.getReceiverId());
        String senderIdStr = principal.getName();

        // Deliver to receiver
        messagingTemplate.convertAndSendToUser(receiverIdStr, "/queue/messages", message);

        // Echo back to sender (unless messaging themselves)
        if (!senderIdStr.equals(receiverIdStr)) {
            messagingTemplate.convertAndSendToUser(senderIdStr, "/queue/messages", message);
        }
    }

    /**
     * Handles a JOIN notification and broadcasts it to /topic/public so all
     * clients know a new user has connected to the chat.
     *
     * @param message   join message with senderName populated
     * @param principal the authenticated WebSocket user
     * @return the enriched JOIN message
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatMessage joinChat(@Payload ChatMessage message, Principal principal) {
        if (principal != null) {
            message.setSenderId(Long.parseLong(principal.getName()));
        }
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return message;
    }
}
