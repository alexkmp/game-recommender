package com.alex.recommender.service;

import com.alex.recommender.model.Chat;
import com.alex.recommender.model.ChatEntry;
import com.alex.recommender.repository.ChatRepository;
import lombok.Builder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public class PostgresChatMemory implements ChatMemory {

    private ChatRepository chatMemoryRepository;
    private int maxMessages;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        messages.forEach(m -> {
            chat.addChatEntry(ChatEntry.toChatEntry(m));
        });
        chatMemoryRepository.save(chat);
    }

    @Override
    public List<Message> get(String conversationId) {
        Chat chat = chatMemoryRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        long messagesToSkip = Math.max(0, chat.getHistory().size() - maxMessages);
        return chat.getHistory().stream()
                .skip(messagesToSkip)
                .map(ChatEntry::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        // not implemented
    }
}
