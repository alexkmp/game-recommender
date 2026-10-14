package com.alex.recommender.service;

import com.alex.recommender.model.Chat;
import com.alex.recommender.model.ChatEntry;
import com.alex.recommender.model.Role;
import com.alex.recommender.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static com.alex.recommender.model.Role.ASSISTANT;
import static com.alex.recommender.model.Role.USER;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatClient chatClient;

    @Autowired
    private ChatService chatServiceProxy;

    public List<Chat> getAllChats() {
        return chatRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Chat createNewChat(String title) {
        Chat chat = Chat.builder().title(title).build();
        chatRepository.save(chat);
        return chat;
    }

    public Chat getChat(Long chatId) {
        return chatRepository.findById(chatId).orElseThrow();
    }
    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }


    @Transactional
    public void addChatEntry(Long chatId, String prompt, Role role) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        chat.addChatEntry(ChatEntry.builder().content(prompt).role(role).build());
    }

    @Transactional
    public void proceedInteraction(Long chatId, String prompt) {
        chatServiceProxy.addChatEntry(chatId, prompt, USER);
        String answer = chatClient.prompt().user(prompt).call().content();
        chatServiceProxy.addChatEntry(chatId, answer, ASSISTANT);
    }

    public SseEmitter proceedInteractionWithStreaming(Long chatId, String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        final StringBuilder answer = new StringBuilder();

        chatClient
                .prompt(userPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response) -> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete);
        return sseEmitter;
    }

    @SneakyThrows
    private static void processToken(ChatResponse response, SseEmitter emitter, StringBuilder answer) {
        var token = response.getResult().getOutput();
        emitter.send(token);
        answer.append(token.getText());
    }
}
