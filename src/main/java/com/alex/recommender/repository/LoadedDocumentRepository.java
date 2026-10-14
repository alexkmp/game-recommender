package com.alex.recommender.repository;

import com.alex.recommender.model.LoadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoadedDocumentRepository extends JpaRepository<LoadedDocument, Long> {
    boolean existsByFilenameAndContentHash(String filename, String contentHash);
}
