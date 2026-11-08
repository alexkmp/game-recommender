# Game Recommender

[![Java 21](https://img.shields.io/badge/Java-21-E58235?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1.1](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI 2.0.1](https://img.shields.io/badge/Spring%20AI-2.0.1-4F97D6?logo=spring&logoColor=white)](https://docs.spring.io/spring-ai/reference/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-%23336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![pgvector](https://img.shields.io/badge/pgvector-latest-%23336791)](https://github.com/pgvector/pgvector)
[![Ollama](https://img.shields.io/badge/Ollama-qwen2.5%3A7b-%23000000?logo=ollama&logoColor=white)](https://ollama.com/)
[![Docker](https://img.shields.io/badge/Docker-24.0+-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Game Recommender** is a local web-based chatbot that recommends PC video games based on user preferences using **Retrieval-Augmented Generation (RAG)**. Built on **Spring AI 2.0.1** and **Spring Boot 4.1.1**, this project demonstrates practical implementation of LLM integration with vector databases for offline, local AI applications.

---

## ⭐ Features

- **🤖 Local LLM Processing**: Runs entirely offline using Ollama with `qwen2.5:7b` for chat and `mxbai-embed-large` for embeddings
- **📊 RAG Architecture**: Combines vector search with traditional BM25 reranking for accurate retrieval
- **💬 Real-time Streaming**: Server-Sent Events (SSE) for incremental response delivery
- **🎮 Comprehensive Game Database**: 27+ years of game data (2000-2026) from Metacritic and ProtonDB
- **🗃️ Vector Database**: PostgreSQL with pgvector extension for efficient similarity search
- **💾 Chat History**: Persistent conversation storage with PostgreSQL
- **🌐 Web Interface**: Thymeleaf-based UI with responsive design
- **🔌 Docker Support**: Easy deployment with docker-compose

---

## 🏗️ Architecture

### Data Flow: Knowledge Indexing (Offline)

```
Text Files (knowledgebase/*.txt)
    ↓
TikaDocumentReader → TokenTextSplitter (chunk size: 200)
    ↓
Embedding Generation (mxbai-embed-large via Ollama)
    ↓
PostgreSQL + pgvector (vector storage + metadata)
    ↓
LoadedDocument Tracking
```

### Data Flow: User Query Processing (Online, Streaming)

```
User Request (Web UI)
    ↓
Spring AI Pipeline:
├─ System Prompt (Context & Rules)
├─ ExpansionQueryAdvisor (Query Expansion)
├─ MessageChatMemoryAdvisor (Chat History)
├─ RagAdvisor + BM25RerankEngine (Retrieval + Reranking)
└─ ChatClient (Streaming to qwen2.5:7b via Ollama)
    ↓
SseEmitter → Browser (Incremental Token Streaming)
```

---

## 🛠️ Tech Stack

| **Component** | **Technology** | **Purpose** |
|---------------|---------------|-------------|
| **Language** | Java 21 | Core application logic |
| **Framework** | Spring Boot 4.1.1 | Web application framework |
| **AI Framework** | Spring AI 2.0.1 | LLM integration & RAG pipeline |
| **LLM** | qwen2.5:7b (Ollama) | Language model for chat responses |
| **Embeddings** | mxbai-embed-large (Ollama) | Vector embeddings for semantic search |
| **Vector DB** | PostgreSQL 15+ + pgvector | Vector storage with HNSW indexing |
| **Frontend** | Thymeleaf + JavaScript + CSS | Web interface |
| **Chat Transport** | SSE (Server-Sent Events) | Real-time response streaming |
| **Containerization** | Docker + docker-compose | Easy deployment |
| **Build Tool** | Gradle 8.12 | Dependency management & build |
| **Logging** | Logback | Application logging |
| **Language Detection** | Lingua 1.2.2 | Multi-language support (EN/RU) |
| **Tokenization** | Lucene 10.2.2 | Text processing for BM25 |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** (JDK 21+)
- **Docker** & **docker-compose** (for PostgreSQL)
- **Ollama** (can be run via Docker or installed natively)

### Installation

#### 1. Clone the repository

```bash
git clone https://github.com/your-username/game-recommender.git
cd game-recommender
```

#### 2. Start the infrastructure

```bash
# Start PostgreSQL with pgvector
docker-compose up -d postgres
```

#### 3. Pull required Ollama models

```bash
# Option 1: Using the provided script (requires Ollama running locally, better performance)
./ollama-pull-models.sh

# Option 2: Manual pull
ollama pull qwen2.5:7b
ollama pull mxbai-embed-large
```

**Note**: If using Docker for Ollama, uncomment the ollama service in `docker-compose.yml` and run:
```bash
docker-compose up -d ollama
```

#### 4. Run the application

```bash
# Build and run
./gradlew bootRun
```

#### 5. Access the application

- **Web Interface**: Open [http://localhost:8081](http://localhost:8081) in your browser
- **API Endpoint**: `GET /chat-stream/{chatId}?userPrompt={your_query}` for streaming responses

---

## 📥 Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```properties
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DATABASE=ragdb
```

### Application Properties

Main configuration in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8081

spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b
      embedding:
        model: mxbai-embed-large
  datasource:
    url: jdbc:postgresql://localhost:5431/ragdb
    username: postgres
    password: postgres
```

### Database Configuration

- **Port**: PostgreSQL runs on port `5431` (to avoid conflicts with local PostgreSQL)
- **Extension**: `pgvector` is automatically installed via `init.sql`
- **Tables**: Created automatically on first run

---

## 📚 Data Format

### Knowledge Base Structure

Game data is stored in text files under `src/main/resources/knowledgebase/`:
- One file per year (e.g., `2000.txt`, `2001.txt`, ..., `2026.txt`)
- Each line represents one game

### Game Entry Format

```
[Game Title] - genres: [Genre] - age: [Year] - rating: [Metacritic Score] - steamdeck rating: [Rating] ([ProtonDB Level])
```

**Example:**
```
Baldur's Gate II: Shadows of Amn - genres: CRPG - age: 2000 - rating: 95 - steamdeck rating: 5 (platinum)
```

### Data Sources

- **Metacritic**: Game ratings for PC (2000-2026)
- **ProtonDB**: Steam Deck compatibility levels (Platinum, Gold, Silver, Bronze, Borked)

### Steam Deck Rating Decoding

| **Rating** | **Level** | **Description** |
|------------|-----------|-----------------|
| 5 | Platinum | Works perfectly on Steam Deck |
| 4 | Gold | Works well |
| 3 | Silver | Satisfactory, may require tweaking |
| 2 | Bronze | Poor performance |
| 1 | Borked | Does not work |

---

## 🎯 Usage Examples

### Web Interface

1. Open [http://localhost:8081](http://localhost:8081)
2. Click "Create new chat"
3. Enter your request, for example:
   - "Recommend top 5 CRPG games with Steam Deck support"
   - "Best Action RPG games released after 2020"
   - "Games with Metacritic score above 95"
   - "What are the best games for Steam Deck?"

### API Usage

```bash
# Start a new chat
curl -X POST -d "title=My Game Search" http://localhost:8081/chat/new

# Get streaming response
curl http://localhost:8081/chat-stream/1?userPrompt="Top 5 RPG games"
```

---

## 🔧 Project Structure

```
game-recommender/
├── src/
│   ├── main/
│   │   ├── java/com/alex/recommender/
│   │   │   ├── advisor/                     # Spring AI advisors
│   │   │   │   ├── rag/                      # RAG-specific advisors
│   │   │   │   │   ├── RagAdvisor.java        # RAG pipeline advisor
│   │   │   │   │   └── BM25RerankEngine.java  # BM25 reranking implementation
│   │   │   │   └── ExpansionQueryAdvisor.java # Query expansion
│   │   │   ├── controller/                  # Web controllers
│   │   │   │   ├── ChatController.java       # Chat management
│   │   │   │   └── StreamingChatController.java # SSE streaming
│   │   │   ├── model/                        # JPA entities
│   │   │   │   ├── Chat.java                  # Chat entity
│   │   │   │   ├── ChatEntry.java             # Chat message entity
│   │   │   │   ├── LoadedDocument.java        # Document tracking
│   │   │   │   └── Role.java                  # Message roles
│   │   │   ├── repository/                  # Spring Data JPA repositories
│   │   │   │   ├── ChatRepository.java
│   │   │   │   └── LoadedDocumentRepository.java
│   │   │   ├── service/                     # Business services
│   │   │   │   ├── ChatService.java           # Chat service
│   │   │   │   ├── DocumentLoaderService.java # Document loading
│   │   │   │   └── PostgresChatMemory.java    # Chat memory implementation
│   │   │   └── GameRecommenderApplication.java # Main application
│   │   └── resources/
│   │       ├── knowledgebase/               # Game data files
│   │       │   ├── 2000.txt
│   │       │   ├── 2001.txt
│   │       │   └── ...
│   │       ├── prompts/                     # LLM prompts
│   │       │   ├── system-prompt.st         # System instructions
│   │       │   └── expansion-prompt.st      # Query expansion instructions
│   │       ├── templates/                   # Thymeleaf templates
│   │       │   └── chat.html
│   │       ├── static/                      # Static assets
│   │       │   ├── css/
│   │       │   │   └── chat.css
│   │       │   ├── js/
│   │       │   │   └── chat.js
│   │       │   └── images/
│   │       ├── application.yaml             # Spring configuration
│   │       └── logback.xml                  # Logging configuration
│   └── test/                               # Tests
│       └── java/.../GameRecommenderApplicationTests.java
├── postgres/
│   └── init-scripts/
│       └── init.sql                         # Database initialization
├── docker-compose.yml                       # Docker configuration
├── ollama-pull-models.sh                    # Model download script
├── build.gradle                              # Gradle build file
├── settings.gradle
├── .env                                       # Environment variables
├── .gitignore
└── README.md
```

---

## 🎨 Key Components

### Advisors (Spring AI)

#### ExpansionQueryAdvisor
- **Purpose**: Expands user queries to improve retrieval accuracy
- **Features**:
  - Abbreviation expansion (GTA → Grand Theft Auto, BG3 → Baldur's Gate 3)
  - Synonym generation
  - Constraint preservation (genre, year, platform)
  - Multi-language support (English, Russian)
- **Model**: Uses `qwen2.5:7b` with `temperature=0.0` for deterministic output

#### RagAdvisor
- **Purpose**: Retrieves relevant documents from vector store
- **Features**:
  - Vector similarity search with `topK=10`, `similarityThreshold=0.6`
  - BM25 reranking for improved relevance
  - Context window management
- **Integration**: Uses `mxbai-embed-large` embeddings stored in PostgreSQL

#### BM25RerankEngine
- **Purpose**: Reranks retrieval results using BM25 algorithm
- **Features**:
  - Language detection (English/Russian) via Lingua
  - Lucene analyzers for tokenization
  - Configurable parameters: `K=1.2`, `B=0.75`
  - IDF and TF calculation for term weighting

### Services

#### DocumentLoaderService
- **Purpose**: Loads and processes knowledge base files
- **Trigger**: Runs on application startup (`CommandLineRunner`)
- **Process**:
  1. Scans `knowledgebase/` directory for text files
  2. Parses files using TikaDocumentReader
  3. Splits text into chunks (200 tokens each)
  4. Generates embeddings using `mxbai-embed-large`
  5. Stores vectors in PostgreSQL with pgvector
  6. Tracks loaded documents to avoid duplicates

#### ChatService
- **Purpose**: Manages chat sessions and message processing
- **Features**:
  - Chat CRUD operations
  - Message history management
  - Streaming response handling via SseEmitter
  - Integration with Spring AI ChatClient

#### PostgresChatMemory
- **Purpose**: Persistent chat memory storage
- **Implementation**: Spring AI `ChatMemory` interface
- **Features**:
  - Stores conversation history in PostgreSQL
  - Configurable message limit (`maxMessages=2`)
  - Automatic conversion between Spring AI `Message` and `ChatEntry` entities

---

## ⚠️ Known Limitations

| **Issue** | **Description** | **Root Cause** | **Workaround/Planned Fix** |
|-----------|----------------|----------------|---------------------------|
| Genre confusion | Model occasionally misclassifies game genres (e.g., shooter as RPG) | Imperfect embeddings, insufficient structural filtering | Add hybrid search: vectors + metadata filters (genre, year, rating) |
| Unstable rating sort | Model doesn't always maintain correct Metacritic score order | RAG context lacks explicit sorting rules, LLM rearranges results | Add structural filtering layer (SQL query on metadata) before LLM call |
| Limited context | Model may miss relevant games due to context size constraints | LLM context window limitations | Improve retrieval and reranking mechanisms |

---

## 🗺️ Roadmap

- [ ] **Hybrid Retrieval**: Implement vector + structural metadata filtering (genre, year, rating, Steam Deck rating)
- [ ] **Structural Sorting**: Add dedicated tool/function for numeric sorting and filtering
- [ ] **UI Enhancements**: Dialogue history, game cards with metadata, direct links to Metacritic/ProtonDB
- [ ] **Performance Optimization**: Improve embedding generation and search speed
- [ ] **Multi-model Support**: Add support for additional LLM and embedding models
- [ ] **Advanced Filtering**: Implement complex query filters (AND/OR conditions, ranges)