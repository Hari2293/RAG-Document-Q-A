# RAG Document Q&A - Java + Spring Boot + Spring AI + MySQL

This project uses **MySQL only**. PostgreSQL and PGVector are not required.

## Stack
- Java 17
- Spring Boot 4.0.1
- Spring AI 2.0.1
- OpenAI Chat + Embeddings
- MySQL
- Spring JDBC
- PDF reader
- REST APIs

## 1. Create the database

In MySQL Workbench:

```sql
CREATE DATABASE ragdb;
```

The application creates the `document_chunks` table automatically.

## 2. Configure MySQL

Open:

`src/main/resources/application.properties`

Change:

```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

to your actual MySQL password.

## 3. Configure OpenAI

Windows CMD:

```cmd
set OPENAI_API_KEY=your_key_here
```

PowerShell:

```powershell
$env:OPENAI_API_KEY="your_key_here"
```

For Eclipse, you can also add `OPENAI_API_KEY` in the Run Configuration environment variables.

## 4. Run in Eclipse

Right-click the project -> Maven -> Update Project.

Then open:

`RagDocumentQaApplication.java`

Right-click -> Run As -> Java Application.

## 5. Upload PDF

POST:

`http://localhost:8080/api/documents/upload`

Postman -> Body -> form-data:

- Key: `file`
- Type: File
- Select a PDF

## 6. Ask a question

GET:

`http://localhost:8080/api/rag/ask?question=What is this document about?`

## RAG flow

PDF -> pages -> chunks -> OpenAI embeddings -> MySQL

Question -> OpenAI embedding -> cosine similarity in Java -> top 5 chunks -> ChatClient -> answer

## Important

Embeddings are stored as text in MySQL and cosine similarity is calculated in Java. This is intentionally simple for learning. It does not require PostgreSQL or PGVector.
