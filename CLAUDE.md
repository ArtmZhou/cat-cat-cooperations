# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cat Agent Platform (猫猫多Agent协同系统) is a CLI Agent collaboration platform supporting external CLI tools (Claude Code, OpenCode, etc.) with real-time communication and multi-agent group chat.

## Build & Development Commands

### Quick Start

```bash
# Build and run (no external dependencies)
mvn clean package -pl cat-standalone -am -DskipTests
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar

# Or run directly with Maven
mvn spring-boot:run -pl cat-standalone

# Or use the startup scripts
run-standalone.bat  # Windows
./run-standalone.sh # Linux/Mac
```

**Features:**
- JSON file storage (no database required)
- Embedded Redis server (optional, for caching)
- No authentication required (direct access)
- Single process, easy local development

**Access:**
- Frontend: http://localhost:3000
- API: http://localhost:8080/api/v1
- Data Directory: ./data/

### Backend (Java/Spring Boot)

```bash
# Build
mvn clean package -pl cat-standalone -am -DskipTests

# Run tests
mvn test -pl cat-standalone

# Run development server
mvn spring-boot:run -pl cat-standalone
```

### Frontend (Vue 3/TypeScript)

```bash
cd cat-web

# Install dependencies
npm install

# Development server (port 3000)
npm run dev

# Build for production
npm run build

# Lint code
npm run lint
```

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| cat-standalone | 8080 | Backend API |
| cat-web | 3000 | Frontend dev server |
| Embedded Redis | 6380 | Optional caching |

## Architecture

### Module Structure

```
cat-cat-cooperations/
├── cat-standalone/    # Standalone module - all backend code
│   └── src/main/java/com/cat/
│       ├── CatApplication.java        # Entry point
│       ├── controller/                # REST controllers
│       │   ├── CliAgentController.java
│       │   ├── ChatGroupController.java
│       │   ├── CliAgentMonitorController.java
│       │   ├── CliAgentTemplateController.java
│       │   └── CliAgentCapabilityController.java
│       ├── cliagent/                  # CLI Agent services + DTOs
│       │   ├── CliAgentService.java
│       │   ├── CliProcessService.java
│       │   ├── CliSessionService.java
│       │   ├── CliTaskExecutionService.java
│       │   ├── CliOutputPushService.java
│       │   ├── TokenUsageService.java
│       │   ├── CliAgentMonitorService.java
│       │   ├── CliAgentTemplateService.java
│       │   ├── CliAgentCapabilityService.java
│       │   └── dto/
│       ├── chatgroup/                 # Chat group service + entities
│       │   ├── ChatGroupService.java
│       │   └── entity/
│       ├── config/                    # Spring configuration
│       │   ├── WebConfig.java
│       │   ├── WebSocketConfig.java
│       │   ├── StoreConfig.java
│       │   └── GlobalExceptionHandler.java
│       ├── store/                     # JSON file storage layer
│       │   ├── JsonFileStore.java
│       │   └── entity/
│       ├── common/                    # Shared models
│       │   ├── ApiResponse.java
│       │   ├── PageResult.java
│       │   └── BusinessException.java
│       └── dashboard/                 # Dashboard (if separate)
└── cat-web/           # Vue 3 frontend SPA
    └── src/
        ├── api/              # API client modules (TypeScript)
        │   ├── cliAgent.ts
        │   ├── chatGroup.ts
        │   └── request.ts
        ├── assets/styles/    # SCSS design system (dark tech theme)
        │   ├── _variables.scss  # Design tokens (colors, spacing, etc.)
        │   └── main.scss        # Global styles + Element Plus overrides
        ├── components/       # Shared components
        │   ├── CatIcons.ts       # 12 SVG icon components (functional)
        │   └── layout/           # AppLayout (dark sidebar + glass header)
        ├── composables/      # Reusable composables
        │   ├── useSpinner.ts
        │   └── useAgentPolling.ts
        ├── types/            # Shared TypeScript types
        │   └── models.ts
        ├── views/            # Page components
        │   ├── dashboard/    # DashboardView
        │   ├── cliAgent/     # CliAgentListView, CliAgentDetailView + dialogs
        │   └── groupChat/    # GroupChatView + sub-components
        ├── router/           # Vue Router config (4 routes)
        └── utils/            # Utilities (request, websocket)
```

### Backend Layer Architecture

- Controller → Service → JsonFileStore (flat, no interface/implementation split)
- Entity classes in `store/entity/` and `chatgroup/entity/`, DTOs in `cliagent/dto/`
- Global exception handling via `GlobalExceptionHandler`
- Standardized API responses via `ApiResponse<T>`
- Spring component scan: `com.cat`

### Frontend Structure

- Vue 3 Composition API with `<script setup lang="ts">`
- **Dark tech theme**: Violet→Cyan gradient, glassmorphism, custom SVG icons
- Design tokens in `_variables.scss`, Element Plus dark overrides in `main.scss`
- API modules in `src/api/` (TypeScript)
- WebSocket via `@stomp/stompjs`
- No authentication store (direct access, no login required)

### CLI Agent Architecture

**Execution Mode:** Per-request (`--print` mode)
- New process spawned for each request
- Uses `--resume <sessionId>` for conversation continuity
- Output parsed from `stream-json` format

**Startup Behavior:**
- On backend restart, all RUNNING/EXECUTING agents are automatically reset to STOPPED
- This prevents stale status display since process context is lost on restart

**Key Services (all in `com.cat.cliagent`):**
- `CliAgentService` - Agent CRUD operations
- `CliSessionService` - Session & process management
- `CliProcessService` - Process lifecycle
- `CliTaskExecutionService` - Task execution
- `TokenUsageService` - Token usage tracking
- `CliOutputPushService` - WebSocket output push
- `CliAgentMonitorService` - System overview for dashboard
- `CliAgentTemplateService` - Template management
- `CliAgentCapabilityService` - Capability management

### Chat Group Architecture

**Key Service (in `com.cat.chatgroup`):**
- `ChatGroupService` - Group CRUD, messaging, @mentions, context-aware agent prompts, output routing

### Data Storage

**JSON Files (./data/ directory):**
- `cli_agents.json` - CLI Agent instances
- `cli_agent_templates.json` - CLI Agent templates
- `cli_agent_capabilities.json` - Agent capabilities
- `token_usage_logs.json` - Token usage records
- `cli_agent_output_logs.json` - CLI Agent output logs (max 100 per agent)
- `chat_groups.json` - Chat groups
- `chat_group_messages.json` - Chat group messages (max 200 per group)

**Format:** Jackson with Java 8 time support

## Tech Stack

| Layer | Technology | Notes |
|-------|------------|-------|
| Frontend | Vue 3.4+, TypeScript 5.0+, Vite 5.0+, Element Plus 2.5+ | |
| Backend | Spring Boot 3.2+, Java 17 | |
| Storage | JSON Files | ./data/ directory |
| Cache | Embedded Redis | Port 6380, optional |
| WebSocket | STOMP over SockJS | Real-time output |

## Key API Endpoints

**CLI Agent Management:**
- `GET/POST /api/v1/cli-agents` - Agent list/create
- `GET/PUT/DELETE /api/v1/cli-agents/{id}` - Agent operations
- `POST /api/v1/cli-agents/{id}/actions/start` - Start agent
- `POST /api/v1/cli-agents/{id}/actions/stop` - Stop agent
- `POST /api/v1/cli-agents/{id}/actions/restart` - Restart agent

**CLI Agent Communication:**
- `POST /api/v1/cli-agents/{id}/session/input` - Send input
- `GET /api/v1/cli-agents/{id}/logs` - Get output logs
- WebSocket `/ws` - Real-time output streaming

**CLI Agent Monitoring:**
- `GET /api/v1/cli-agents/monitor/overview` - System overview (used by dashboard)

**Chat Groups:**
- `GET/POST /api/v1/chat-groups` - List/create groups
- `GET/PUT/DELETE /api/v1/chat-groups/{id}` - Group operations
- `POST /api/v1/chat-groups/{id}/messages` - Send message (supports @mentions and broadcast)
- `GET /api/v1/chat-groups/{id}/messages` - Get message history
- `POST /api/v1/chat-groups/{id}/messages/clear` - Clear messages

**Templates & Capabilities:**
- `GET/POST /api/v1/cli-agent/templates` - Template management
- `GET /api/v1/cli-agents/capability-types` - Capability type listing

## Testing

```bash
# Run all backend tests (46 tests)
mvn test -pl cat-standalone

# Test files
cat-standalone/src/test/java/com/cat/cliagent/CliAgentServiceTest.java      # 19 tests
cat-standalone/src/test/java/com/cat/cliagent/CliProcessServiceTest.java     # 21 tests
cat-standalone/src/test/java/com/cat/cliagent/CliOutputPushServiceTest.java  # 6 tests
```
