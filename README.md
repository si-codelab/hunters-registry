# Hunters Registry

A real time fantasy management game where you command monster hunters, gather intelligence, and capture supernatural threats.

Built as a learning and portfolio project using Kotlin, Spring Boot, React, Server Sent Events, and WebSockets.

## Repository structure

- `backend/` Kotlin Spring Boot application
- `frontend/` React application bootstrapped with Vite

## Prerequisites

- Java 21
- Maven
- Node.js

## Run backend

```bash
cd backend
mvn spring-boot:run
````

Backend will start on [http://localhost:8080](http://localhost:8080)

### API

- GET /api/health
- GET /api/state
- GET /api/state/stream (SSE)

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will start on [http://localhost:5173](http://localhost:5173)