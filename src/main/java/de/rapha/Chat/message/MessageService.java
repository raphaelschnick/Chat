package de.rapha.Chat.message;

import de.rapha.Chat.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {
    @Autowired private MessageRepository repository;
    @Autowired private RoomService roomService;
    @Autowired private MongoOperations mongoOperations;

    public Message save(Message message) {
        message.setStatus(Status.RECEIVED);
        repository.save(message);
        return message;
    }

    public long countNewMessages(String senderId, String recipientId) {
        return repository.countBySenderIdAndRecipientIdAndStatus(
                senderId, recipientId, Status.RECEIVED);
    }

    public List<Message> findChatMessages(String senderId, String recipientId) {
        Optional<String> chatId = roomService.getChatId(senderId, recipientId, false);

        List<Message> messages =
                chatId.map(cId -> repository.findByChatId(cId)).orElse(new ArrayList<>());

        if(messages.size() > 0) {
            updateStatuses(senderId, recipientId, Status.DELIVERED);
        }

        return messages;
    }

    public Message findById(String id) {
        return repository
                .findById(id)
                .map(chatMessage -> {
                    chatMessage.setStatus(Status.DELIVERED);
                    return repository.save(chatMessage);
                })
                .orElseThrow(() ->
                        new MessageNotFoundException("can't find message (" + id + ")"));
    }

    public void updateStatuses(String senderId, String recipientId, Status status) {
        Query query = new Query(
                Criteria
                        .where("senderId").is(senderId)
                        .and("recipientId").is(recipientId));
        Update update = Update.update("status", status);
        mongoOperations.updateMulti(query, update, Message.class);
    }
}
