# Design decisions

Short records of the choices that shaped Huddle: what was decided, why, and what it costs.

## 1. Layered design with ports and adapters

**Decision:** Four layers (domain, service, infrastructure, web). The domain owns the
`MessageRepository` and `Summariser` interfaces; infrastructure implements them.

**Why:** Dependency inversion. Business rules don't depend on storage or on an AI vendor, so either can
be swapped without touching callers, and services can be tested with Mockito instead of a database
or the network.

**Trade-off:** More files than a single controller would need. Worth it because each class has one
reason to change.

## 2. Validation lives in the domain (records with compact constructors)

**Decision:** `RoomId` and `ChatMessage` validate themselves on construction. Controllers don't
re-validate with Bean Validation annotations.

**Why:** An invalid `ChatMessage` can't exist, whichever path created it (REST, STOMP, tests). There's
one source of truth for the rules. Controllers translate `IllegalArgumentException` into a 400
`ProblemDetail` (REST) or a private `/user/queue/errors` message (WebSocket).

## 3. In-memory, bounded, thread-safe storage

**Decision:** `ConcurrentHashMap<RoomId, Deque>` with a lock per room, capped at 500 messages per room.

**Why:** The brief ruled out a database. A cap means memory can't grow without bound. Locking per room
(not one global lock) means busy rooms don't block each other. A shared contract test means a future
H2/JPA implementation must behave identically.

**Trade-off:** Data is lost on restart, and it only works on a single instance.

## 4. STOMP over WebSocket with Spring's simple broker

**Decision:** STOMP destinations (`/app`, `/topic`, `/user/queue`) instead of a raw WebSocket handler.

**Why:** STOMP gives pub/sub routing, per-session private queues and annotation-based controllers for
free, and the client library handles reconnects. The simple broker can later be replaced by a
RabbitMQ relay for horizontal scaling, with a config change and no controller changes.

**Ordering:** `setPreserveReceiveOrder` and `setPreservePublishOrder` are enabled, because Spring
otherwise processes a client's frames on a thread pool and two quick messages can be reordered.

## 5. Sending STOMP receipts for subscriptions ourselves

**Decision:** `SubscriptionReceiptInterceptor` replies to `SUBSCRIBE` frames that request a receipt,
after the broker (or user-destination resolver) has registered the subscription.

**Why:** The simple broker ignores receipts, so tests would have to `sleep()` and hope. Sleeps make
tests slow and flaky. Receipts make the WebSocket integration tests deterministic.

## 6. AI behind an interface, with a null object when unconfigured

**Decision:** `Summariser` interface; `ClaudeSummariser` when `ANTHROPIC_API_KEY` is set, otherwise
`DisabledSummariser`, which throws a clear `SummaryUnavailableException` (mapped to HTTP 503).

**Why:** The app, the tests and CI all run with no secrets. Summaries degrade gracefully instead of
breaking chat. The key is read only from the environment (`.env` is gitignored, `.env.example` is committed).

## 7. Official Anthropic SDK with structured outputs

**Decision:** Use `anthropic-java` and its structured outputs: the JSON schema is derived from the
`ClaudeSummaryResponse` record, then mapped to domain types.

**Why:** The model's reply is guaranteed to match the schema, so there's no regex or JSON-repair code.
The SDK provides retries with backoff, timeouts and typed errors. The response DTO stays in
`infrastructure`, so schema annotations don't leak into the domain.

**Settings:** model `claude-opus-5-5` (configurable). `effort: low`, because summarising chat is simple
and low effort reduces latency and cost. Server-side refusal `fallbacks` are enabled. Transcript capped at
200 messages. Empty rooms never call the API.

## 8. Treating chat content as untrusted input to the model

**Decision:** The transcript goes inside `<transcript>` tags; the system prompt says to treat it purely
as data, and any `</transcript` typed by a user is neutralised.

**Why:** Chat text is user input. Without this, someone could type "ignore previous instructions…"
and steer the summary. It's one layer of defence; the output is also schema-constrained and rendered
as plain text.

## 9. Testing the AI adapter against a stub HTTP server, not mocks

**Decision:** `ClaudeSummariserTest` points the real SDK at a JDK `HttpServer` on a random port.

**Why:** Mocking a fluent SDK means mocking chains of generic types, which ends up testing the mocks. A
stub server tests what matters: the request we send (model, beta header, effort, transcript framing)
and how we handle real response shapes (success, refusal, HTTP 500).

## 10. Plain HTML/JS client instead of React

**Decision:** One static page served by Spring Boot, plus a pinned STOMP client library.

**Why:** The brief was backend-heavy, and React would add a second build system and toolchain for three
UI elements. User text is always rendered with `textContent` (never `innerHTML`) to prevent XSS. The
client subscribes before loading history and de-duplicates by message id, so no message is lost or
shown twice when a user switches rooms.

## 11. Cross-platform CI

**Decision:** The test matrix runs on Ubuntu, Windows and macOS, with `shell: bash` everywhere;
`.gitattributes` forces LF; `mvnw` is committed with its executable bit.

**Why:** Line endings and file permissions are the classic "works on my machine" failures, and both
showed up while building this on Windows. The Docker job then checks that the packaged image actually
starts and serves requests.
