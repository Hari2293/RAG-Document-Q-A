package com.example.rag.service;

import com.example.rag.repository.DocumentVectorRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentService {
    private final DocumentVectorRepository repository;
    private final EmbeddingModel embeddingModel;

    public DocumentService(DocumentVectorRepository repository, EmbeddingModel embeddingModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
    }

    public int processPdf(MultipartFile file) {
        validatePdf(file);
        Path temporaryFile = null;

        try {
            repository.initializeSchema();
            temporaryFile = Files.createTempFile("rag-document-", ".pdf");
            file.transferTo(temporaryFile);

            PagePdfDocumentReader reader =
                    new PagePdfDocumentReader(temporaryFile.toUri().toString());

            List<Document> documents = reader.read();

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(800)
                    .withMinChunkSizeChars(350)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .build();

            List<Document> chunks = splitter.split(documents);
            int stored = 0;

            for (Document chunk : chunks) {
                String text = chunk.getText();
                if (text != null && !text.isBlank()) {
                    float[] embedding = embeddingModel.embed(text);
                    repository.save(text, embedding);
                    stored++;
                }
            }

            return stored;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process PDF document.", e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a PDF file.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }
    }
}
