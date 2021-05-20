package de.rapha.Chat.controller;

import de.rapha.Chat.message.Message;
import de.rapha.Chat.message.MessageService;
import de.rapha.Chat.notification.Notification;
import de.rapha.Chat.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ChatController {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private MessageService messageService;
    @Autowired private RoomService roomService;

    @MessageMapping("/chat")
    public void processMessage(@Payload Message message) {
        Optional<String> chatId = roomService
                .getChatId(message.getSenderId(), message.getRecipientId(), true);
        message.setChatId(chatId.get());

        Message saved = messageService.save(message);
        messagingTemplate.convertAndSendToUser(
                message.getRecipientId(),"/queue/messages",
                new Notification(
                        saved.getId(),
                        saved.getSenderId(),
                        saved.getSenderName()));
    }

    @GetMapping("/messages/{senderId}/{recipientId}/count")
    public ResponseEntity<Long> countNewMessages(
            @PathVariable String senderId,
            @PathVariable String recipientId) {

        return ResponseEntity
                .ok(messageService.countNewMessages(senderId, recipientId));
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<?> findChatMessages ( @PathVariable String senderId,
                                                @PathVariable String recipientId) {
        return ResponseEntity
                .ok(messageService.findChatMessages(senderId, recipientId));
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<?> findMessage ( @PathVariable String id) {
        return ResponseEntity
                .ok(messageService.findById(id));
    }
}
