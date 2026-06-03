# Android Architecture Decisions

This document records implementation decisions for the Android client. The
README owns product scope. Linear owns delivery status and sequencing.

## Decision Summary

| Area | Decision |
|---|---|
| Platform | Native Android only |
| Language | Kotlin |
| UI | Jetpack Compose |
| Initial module shape | Single `app` Gradle module |
| Package namespace | `com.hobsojam.simpleestimation` |
| Build system | Gradle wrapper with Android Gradle Plugin, Kotlin, Compose compiler plugin, ktlint, and Detekt |
| Presentation pattern | ViewModel/state-holder per feature with immutable UI state |
| State flow | Unidirectional events to state |
| Protocol model | Server DTOs mapped into domain models |
| Persistence | Session-first; only non-sensitive configuration may persist |
| Networking | HTTP for configuration/discovery, WebSocket for room state/actions |

## Module Strategy

Start with one Android application module. Use packages for boundaries rather
than separate Gradle modules until the codebase proves that module separation
would reduce complexity.

Do not introduce these in the MVP (Minimum Viable Product):

- Kotlin Multiplatform
- Dynamic feature modules
- A shared JVM library
- A dependency injection framework solely for structure
- Offline-first persistence

## Package Boundaries

Use this package shape inside the `app` module:

```text
app
  MainActivity
  navigation
  dependency wiring

core
  shared domain primitives
  shared Compose UI primitives
  small platform utilities

data
  HTTP clients
  WebSocket client
  protocol DTOs
  DTO mappers
  session storage

domain
  room models
  connection models
  compatibility decisions
  participant actions

feature
  server configuration
  joining
  planning poker
  bucket estimation
  relative estimation
```

Keep package dependencies directional:

```text
feature -> domain
data -> domain
app -> feature + data wiring
```

Avoid dependencies in the opposite direction. Domain code must not depend on
Compose, Android framework UI types, HTTP clients, WebSocket clients, or JSON
libraries.

## Protocol Boundary

Protocol DTOs are not UI models and are not the app's domain model.

Use this flow:

```text
server JSON -> protocol DTO -> validation/mapping -> domain model -> UI state
```

The mapper is where the app handles:

- unknown JSON fields
- missing required fields
- unsupported protocol versions
- unknown room or message types
- malformed room state
- user-safe error categories

Do not let composables or feature screens inspect raw JSON or protocol DTOs.

## State Model

Represent major state machines explicitly. Prefer sealed classes or equivalent
closed models for mutually exclusive states.

Required state groups:

- server configuration
- protocol compatibility
- room discovery
- joining
- WebSocket connection
- reconnect
- active room state
- user-facing errors

Avoid independent Boolean flags such as `isLoading`, `isConnected`,
`hasError`, and `isCompatible` when combinations can become invalid. Model the
state instead.

## Presentation Pattern

Each feature should expose:

- immutable UI state
- event functions or an event sink
- no direct networking from composables
- no direct persistence from composables
- no server DTOs in composable parameters

The expected flow is:

```text
Composable event -> ViewModel/state holder -> domain/data operation ->
state update -> Compose recomposition
```

Use Android `ViewModel` where lifecycle ownership matters. A plain state holder
is acceptable for small stateless or preview-only components.

## Session and Persistence

Session-scoped:

- `participantId`
- remembered display name
- active room connection state

May persist:

- validated server base URL, if the user chooses to save it
- non-sensitive UI preferences added later

Must not persist:

- access PINs
- facilitator PINs
- room contents
- backlog item labels
- revealed votes
- raw WebSocket payloads

## Networking Decisions

The app derives WebSocket URLs from the configured server base URL. It does not
accept a separate arbitrary WebSocket URL.

Release builds require:

```text
https://
wss://
```

Debug builds may allow local cleartext development:

```text
http://10.0.2.2:3000
ws://10.0.2.2:3000/ws
```

Connection handling must define:

- timeout behavior
- reconnect backoff
- cancellation behavior
- close-code mapping
- malformed-message handling

## Testing Implications

The architecture should make these tests straightforward:

- protocol DTO parsing and mapping tests
- compatibility decision tests
- state-holder tests for connection and reconnect transitions
- WebSocket error mapping tests
- Compose UI tests using fake state holders or fake repositories

Unit and Compose UI tests should not need a real server. Local-server tests
belong in a small integration suite.

## Open Decisions

Resolve these after the Gradle scaffold work:

- HTTP/WebSocket client libraries
- JSON serialization library
- Whether dependency wiring stays manual or uses a lightweight framework
- Whether server URL persistence uses DataStore or a simpler MVP mechanism

## Build and Quality Gates

The scaffold uses the checked-in Gradle wrapper so contributors and CI run the
same Gradle distribution. The root version catalog owns Android Gradle Plugin,
Kotlin, Compose, ktlint, Detekt, and AndroidX versions.

Static analysis is intentionally pragmatic at the scaffold stage:

- ktlint enforces the shared `.editorconfig` and Android Kotlin formatting.
- Detekt builds on the default rule set with a small project config in
  `config/detekt/detekt.yml`.
- GitHub Actions runs `ktlintCheck`, `detekt`, unit tests, Android lint, and
  `assembleDebug` for pull requests and pushes to `main`.

No production networking, persistence, dependency-injection, JSON, HTTP, or
WebSocket libraries are included yet. Those choices remain tied to the first
protocol implementation work and must follow the server contract.
