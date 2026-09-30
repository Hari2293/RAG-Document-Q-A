package com.example.rag.service;

import com.example.rag.repository.DocumentVectorRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final DocumentVectorRepository repository;

    public RagService(ChatClient chatClient,
                      EmbeddingModel embeddingModel,
                      DocumentVectorRepository repository) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.repository = repository;
    }

    public String answerQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty.");
        }

        repository.initializeSchema();
        List<DocumentVectorRepository.StoredChunk> chunks = repository.findAll();

        if (chunks.isEmpty()) {
            return "No documents have been uploaded yet.";
        }

        float[] questionEmbedding = embeddingModel.embed(question);

        String context = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        cosineSimilarity(questionEmbedding, chunk.embedding())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(5)
                .filter(item -> item.score() > 0.15)
                .map(item -> item.chunk().content())
                .collect(Collectors.joining("\n\n---\n\n"));

        if (context.isBlank()) {
            return "I could not find relevant information in the uploaded documents.";
        }

        return chatClient.prompt()
                .system("You answer questions using only the supplied document context. "
                        + "If the answer is not present in the context, say that it was not found "
                        + "in the uploaded documents. Do not invent facts.")
                .user("Document context:\n\n" + context + "\n\nQuestion:\n" + question)
                .call()
                .content();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record ScoredChunk(DocumentVectorRepository.StoredChunk chunk, double score) {
    }
}
