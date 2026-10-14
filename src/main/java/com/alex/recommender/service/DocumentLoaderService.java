package com.alex.recommender.service;

import com.alex.recommender.model.LoadedDocument;
import com.alex.recommender.repository.LoadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentLoaderService implements CommandLineRunner {

    private final LoadedDocumentRepository documentRepository;

    private final ResourcePatternResolver resolver;

    private final VectorStore vectorStore;

    private Logger log = LoggerFactory.getLogger(DocumentLoaderService.class);

    @SneakyThrows
    public void loadDocument() {
        List<Resource> resources = Arrays.stream(resolver.getResources("classpath:/knowledgebase/**/*.txt")).toList();
        resources.stream()
                .map((resource -> Pair.of(resource, calcContentHash(resource))))
                .filter(pair -> !documentRepository.existsByFilenameAndContentHash(
                        pair.getFirst().getFilename(), pair.getSecond()
                )
        ).forEach(pair -> {
            long start = System.currentTimeMillis();
            log.info("Start processing document: {}", pair.getFirst().getFilename());
            Resource resource = pair.getFirst();
            List<Document> documents = new TikaDocumentReader(resource).get();
            // TODO add overlap in TokenTextSplitter after it became available
            TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                    .withChunkSize(200)
                    .withKeepSeparator(true)
                    .build();
            List<Document> chunks = textSplitter.apply(documents);

            vectorStore.accept(chunks);

            LoadedDocument document = LoadedDocument.builder()
                    .documentType("txt")
                    .chunkCount(chunks.size())
                    .filename(pair.getFirst().getFilename())
                    .contentHash(pair.getSecond())
                    .build();

            documentRepository.save(document);
            log.info("Finished processing document in {}ms", System.currentTimeMillis() - start);
        });
    }

    @SneakyThrows
    private String calcContentHash(Resource resource) {
        return DigestUtils.md5DigestAsHex(resource.getInputStream());
    }

    @Override
    public void run(String... args) {
        loadDocument();
    }
}
