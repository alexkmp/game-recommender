package com.alex.recommender;

import com.alex.recommender.advisor.ExpansionQueryAdvisor;
import com.alex.recommender.advisor.rag.RagAdvisor;
import com.alex.recommender.repository.ChatRepository;
import com.alex.recommender.service.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
public class GameRecommenderApplication {

	@Autowired
	public ChatRepository chatRepository;

	@Autowired
	public VectorStore vectorStore;

	@Autowired
	private ChatModel chatModel;

	private static final PromptTemplate SYSTEM_PROMPT = PromptTemplate.builder()
			.resource(new ClassPathResource("prompts/system-prompt.st"))
			.build();

	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultAdvisors(
				ExpansionQueryAdvisor.builder(chatModel).order(0).build(),
				getHistoryAdvisor(1),
				SimpleLoggerAdvisor.builder().order(2).build(),
				RagAdvisor.builder(vectorStore).order(3).build(),
				SimpleLoggerAdvisor.builder().order(4).build()
				)
				.defaultOptions(OllamaChatOptions.builder().temperature(0.3).topP(0.7).topK(20).repeatPenalty(1.1))
				.defaultSystem(SYSTEM_PROMPT.render())
				.build();
	}

	private Advisor getHistoryAdvisor(int order) {
		return MessageChatMemoryAdvisor.builder(getChatMemory()).order(order).build();
	}

    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
				.maxMessages(2)
				.chatMemoryRepository(chatRepository)
				.build();
    }

	public static void main(String[] args) {
		run(GameRecommenderApplication.class, args);
	}

}
