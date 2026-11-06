# Huddle

[![CI](https://github.com/CormacM22/huddle/actions/workflows/ci.yml/badge.svg)](https://github.com/CormacM22/huddle/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![Line coverage 98%](https://img.shields.io/badge/line%20coverage-98%25-brightgreen)

A minimal real-time team chat with AI meeting summaries, in the spirit of Webex Messaging.
People join rooms and chat live over WebSockets. One click summarises the conversation and pulls out
action items using Claude.

The point of the project is **engineering quality over feature count**: a layered object-oriented design,
test-driven development throughout, integration tests over real sockets, and a CI pipeline that builds
on Linux, Windows and macOS.

## Features

- **Live rooms**: STOMP over WebSocket; messages are broadcast to every subscriber of a room
- **History**: `GET /api/rooms/{id}/messages` returns recent messages, so late joiners catch up
- **AI summaries**: `POST /api/rooms/{id}/summary` returns a summary plus action items with owners
- **Safe by default**: input validated in the domain, XSS-safe client, prompt-injection-aware AI prompt,
  no secrets in code, runs fully without an API key

## Architecture

```mermaid
flowchart LR
    subgraph Client["Browser (static HTML/JS)"]
        UI[index.html + app.js]
    end

    subgraph Transport["web / config (transport)"]
        WS[ChatSocketController<br/>@MessageMapping]
        REST[RoomController<br/>REST + ProblemDetail]
        BROKER[(Simple STOMP broker<br/>/topic, /user/queue)]
    end

    subgraph Service["service (use cases)"]
        CS[ChatService]
        SS[SummaryService]
    end

    subgraph Domain["domain (plain Java, no frameworks)"]
        MSG[ChatMessage, RoomId,<br/>MeetingSummary]
        REPO{{MessageRepository}}
        SUM{{Summariser}}
    end

    subgraph Infra["infrastructure (adapters)"]
        MEM[InMemoryMessageRepository]
        CLAUDE[ClaudeSummariser]
        OFF[DisabledSummariser]
    end

    UI -- "SEND /app/rooms/{id}/messages" --> WS
    BROKER -- "MESSAGE /topic/rooms/{id}" --> UI
    UI -- "GET history / POST summary" --> REST
    WS --> CS
    WS -- "@SendTo" --> BROKER
    REST --> CS
    REST --> SS
    CS --> REPO
    SS --> REPO
    SS --> SUM
    MEM -. implements .-> REPO
    CLAUDE -. implements .-> SUM
    OFF -. implements .-> SUM
    CLAUDE -- "Messages API<br/>(structured output)" --> API[(Claude API)]
```

Dependencies point **inwards**: transport → service → domain ← infrastructure. The domain defines the
`MessageRepository` and `Summariser` interfaces (ports), and infrastructure implements them (adapters).
Nothing in `domain` imports Spring, Jackson or the Anthropic SDK.

| Package | Responsibility |
|---|---|
| `domain` | Immutable records with invariants (`RoomId`, `ChatMessage`, `MeetingSummary`) and the two ports |
| `service` | Use cases: post a message, read history, summarise a room |
| `infrastructure` | Thread-safe bounded in-memory store; Claude adapter; null-object summariser |
| `web` | STOMP and REST controllers, DTOs, error mapping to RFC 9457 `ProblemDetail` |
| `config` | Spring wiring: WebSocket broker, AI implementation choice, clock and id beans |

The reasoning behind each choice is in [docs/decisions.md](docs/decisions.md).

## Testing

Built test-first: every production class started with a failing test. Commit history shows the order.

| Level | What it proves | Examples |
|---|---|---|
| Unit (JUnit 5, AssertJ) | Domain invariants and edge cases | `RoomIdTest`, `ChatMessageTest` |
| Unit with mocks (Mockito) | Services in isolation, with fixed `Clock` and ids | `ChatServiceTest`, `SummaryServiceTest` |
| Contract test | Behaviour *any* repository must satisfy, plus an 8-thread concurrency test | `MessageRepositoryContract` |
| Web slice (`@WebMvcTest`) | HTTP mapping, JSON shape, 400/503 problem responses | `RoomControllerTest` |
| Adapter test against a stub server | The real Anthropic SDK request/response, refusals, upstream errors | `ClaudeSummariserTest` |
| Integration over a real socket | Live broadcast, room isolation, private error delivery | `ChatWebSocketIntegrationTest` |

**60 tests, 98% line coverage** (169/172 lines). JaCoCo fails the build below 80%, and CI publishes the
report as an artifact. No test touches the network or needs an API key.

Two QA details worth calling out:
- **No sleeps in WebSocket tests.** Spring's simple broker ignores STOMP `RECEIPT` requests, so a test
  can't know when its subscription is live. `SubscriptionReceiptInterceptor` sends the receipt once the
  subscription is really registered, and the tests wait for it. The suite was run repeatedly to check for flakiness.
- **The AI adapter is tested with the real SDK** pointed at a JDK `HttpServer` stub, so serialisation,
  headers and error mapping are covered, not mocked away.

```bash
./mvnw verify                      # tests + coverage gate
open target/site/jacoco/index.html # coverage report (use `start` on Windows)
```

## Running it

**Requirements:** Java 21 (or just Docker). The Maven wrapper downloads Maven for you.

```bash
./mvnw spring-boot:run             # http://localhost:8080
```

With Docker Compose:

```bash
cp .env.example .env               # optional: add ANTHROPIC_API_KEY to enable summaries
docker compose up --build
```

Without `ANTHROPIC_API_KEY` everything works except summaries, which return a clear `503`.
The model defaults to `claude-opus-5-5` and can be changed with `HUDDLE_AI_MODEL`.

## API

| Method | Path | Description |
|---|---|---|
| WS | `/ws` | STOMP endpoint |
| SEND | `/app/rooms/{roomId}/messages` | Body `{"author": "...", "content": "..."}` |
| SUBSCRIBE | `/topic/rooms/{roomId}` | Receives each new message |
| SUBSCRIBE | `/user/queue/errors` | Receives validation errors for *your* messages only |
| GET | `/api/rooms/{roomId}/messages?limit=50` | Recent history, oldest first (max 200) |
| POST | `/api/rooms/{roomId}/summary` | `{"summary": "...", "actionItems": [{"owner": "...", "task": "..."}]}` |
| GET | `/actuator/health` | Health check |

Room ids are 1–32 lowercase letters, digits and inner hyphens. Messages are up to 2000 characters.

## CI/CD

GitHub Actions ([ci.yml](.github/workflows/ci.yml)):
1. **Test matrix**: `./mvnw verify` on `ubuntu-latest`, `windows-latest` and `macos-latest`
2. **Docker**: builds the multi-stage image, starts it, waits for `/actuator/health`, and smoke-tests the API

Cross-platform details handled: LF line endings enforced via `.gitattributes`, the executable bit set on
`mvnw`, and no OS-specific paths in tests.

## What I'd do next

- Persist messages (H2 or Postgres) by adding a new `MessageRepository` adapter. The contract test already exists for it.
- Authentication (Spring Security + OAuth2) so authors are verified rather than self-declared
- Swap the simple broker for a RabbitMQ relay to run more than one instance
- Stream summaries token-by-token to the client
- A Playwright end-to-end test of the browser flow, and deployment to Cloud Run
