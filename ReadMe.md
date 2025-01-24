
# Distributed Text Search and Web Search System

## Overview

This project combines a **Distributed Text Search System** and a **Web Search Application** to provide a robust, scalable, and user-friendly platform for querying distributed text documents. The system includes:

1. **Distributed Text Search System**: Implements a distributed architecture with a **Coordinator**, multiple **Worker** nodes, and a **Service Registry** for querying text documents using TF-IDF scoring.
2. **Web Search Application**: A Spring Boot application that provides a user-friendly interface for submitting search queries and displaying results.

---

## Features

### Distributed Text Search System
- **Query Distribution**: Efficiently distributes search queries across multiple worker nodes.
- **Dynamic Worker Registration**: Workers dynamically register and deregister with the Service Registry.
- **TF-IDF Scoring**: Ranks documents based on query relevance.

### Web Search Application
- **User Interface**: A simple web-based interface for submitting queries.
- **Leader Communication**: Communicates with the leader node of the distributed system to process queries.
- **Real-time Results**: Displays aggregated results from the distributed system.

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 11 or higher.
- **Maven**: For building the Spring Boot application.
- **Apache ZooKeeper**: For coordination and leader election.
- **Spring Boot**: For the web application.

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── Registration_Discovery/   # Distributed Text Search System
│   │   │   ├── Coordinator.java          # Distributes queries, aggregates results
│   │   │   ├── Worker.java               # Processes text documents
│   │   │   ├── ServiceRegistry.java      # Manages worker registration
│   │   │   ├── DocumentTermsInfo.java    # Stores term frequency data
│   │   │   ├── LeaderElection.java       # Handles leader election
│   │   │   ├── OnElectionAction.java     # Actions triggered on election
│   │   │   ├── OnElectionCallback.java   # Leader election callback
│   │   └── websearch/                # Web Search Application
│   │       ├── WebsearchApplication.java # Spring Boot main class
│   │       ├── WebSearchResult.java      # Stores search results
│   │       ├── SearchService.java        # Handles queries and communicates with the leader
│   │       ├── SearchResponse.java       # Encapsulates search responses
│   │       ├── SearchController.java     # Handles web routes and query processing
│   │       └── resources/
│   │           ├── templates/            # HTML templates for web interface
│   │           │   └── index.html        # Main UI for query submission
│   │           └── static/               # Static assets (CSS, JS)
│   └── resources/
│       └── application.properties        # Spring Boot configuration
```

---

## Setup Instructions

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd Distributed_text_search_system
```

### Step 2: Compile and Run the Distributed Text Search System
1. **Compile the code**:
   ```bash
   javac -d out src/main/java/Registration_Discovery/*.java
   ```
2. **Prepare documents**:
   - Create directories for each worker under `resources/documents`.

3. **Start the Coordinator**:
   ```
  
   ```
4. **Start Worker Nodes**:
   ```
   ```

### Step 3: Run the Web Search Application
1. **Navigate to the websearch directory**:
   ```bash
   cd src/main/java/websearch
   ```
2. **Start the Spring Boot application**:
   ```bash
   mvn spring-boot:run
   ```

### Step 4: Access the Web Interface
1. Open a web browser and navigate to:
   ```
   http://localhost:8081
   ```
2. Submit a query and view the results.

---

## Code Highlights

### Distributed Text Search System
- **Leader Election**: Ensures fault-tolerant coordination.
- **Dynamic Scaling**: Workers can join or leave without disrupting the system.
- **TF-IDF Calculation**: Efficiently computes document relevance scores.

### Web Search Application
- **Spring Boot Framework**: Provides a powerful backend with minimal configuration.
- **ZooKeeper Integration**: Manages leader discovery and communication.
- **Responsive Design**: Offers a simple and intuitive interface for end-users.

---
