package com.example.rag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DocumentVectorRepository {
    private final JdbcTemplate jdbcTemplate;

    public DocumentVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initializeSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS document_chunks (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                content LONGTEXT NOT NULL,
                embedding LONGTEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    public void save(String content, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO document_chunks (content, embedding) VALUES (?, ?)",
                content, toText(embedding));
    }

    public List<StoredChunk> findAll() {
        return jdbcTemplate.query(
                "SELECT id, content, embedding FROM document_chunks",
                (rs, rowNum) -> new StoredChunk(
                        rs.getLong("id"),
                        rs.getString("content"),
                        fromText(rs.getString("embedding"))));
    }

    private String toText(float[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(values[i]);
        }
        return result.toString();
    }

    private float[] fromText(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }

        String[] parts = text.split(",");
        List<Float> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            values.add(Float.parseFloat(part));
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public record StoredChunk(long id, String content, float[] embedding) {
    }
}
