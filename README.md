# simple-estimation-android

Native Android participation client for
[simple-estimation](https://github.com/hobsojam/simple-estimation), a lightweight
self-hosted tool for collaborative agile estimation.

The Android app is intended for participants joining an existing estimation
session from a phone or tablet. Room creation, facilitation, administration, and
data export remain in the web application.

## Product Scope

### Supported Workflows

- Configure the URL of a deployed Simple Estimation server
- Browse and refresh the server's active-room list
- Open a room link or enter an existing room ID
- Select an active room from the list
- Join a room with a display name
- Supply an access PIN when a room requires one
- Reconnect after transient connectivity failures
- Resume the same participant identity during the current app session
- Display server and protocol compatibility errors clearly

### Planning Poker

- View the active backlog item selected by the facilitator
- Choose a Planning Poker card: `1`, `2`, `3`, `5`, `8`, `13`, `21`, `?`, `∞`,
  or `☕`
- See participant voting progress while votes remain hidden
- View revealed votes, outliers, and the accepted estimate
- View facilitator-controlled countdown timers
- Follow backlog progress as the facilitator selects and finalises items

### Bucket Estimation

- View unsized items and the `XS`, `S`, `M`, `L`, and `XL` buckets
- Move items with a touch-friendly select-and-place interaction
- See other participants' changes in real time

### Relative Estimation

- View unplaced items and the Fibonacci scale: `1`, `2`, `3`, `5`, `8`, `13`,
  and `21`
- Move items with a touch-friendly select-and-place interaction
- See other participants' changes in real time

Drag-and-drop may be added later as an optional enhancement. The primary mobile
interaction should remain reliable for touch input and accessible navigation.

## Explicit Non-Goals

The initial Android client will not:

- Create or delete rooms
- Claim the facilitator role
- Expose facilitator controls
- Add, select, finalise, or remove backlog items
- Reveal votes, reset rounds, or control timers
- Export CSV files
- Persist access PINs or facilitator PINs
- Replace the web application for session administration

## Server Contract

The Android client uses the same HTTP and WebSocket contract as the web client:

- [API documentation](https://github.com/hobsojam/simple-estimation/blob/main/docs/api.md)
- [Server repository](https://github.com/hobsojam/simple-estimation)

The server is the source of truth for room state. After connecting, the client
should render the latest sanitized `state.room` payload rather than attempt to
reconstruct state from local commands.

### Protocol Compatibility

Before joining a room, call:

```text
GET /api/config
```

The client must reject protocol versions it does not support and show a useful
upgrade message. The initial app should support protocol version `1`.

### Connectivity

Production servers should use:

```text
https://example.com
wss://example.com/ws
```

Local Android emulator development commonly uses:

```text
http://10.0.2.2:3000
ws://10.0.2.2:3000/ws
```

Cleartext HTTP and WebSocket traffic should be permitted only in debug builds.

### Session Identity

Generate a UUID `participantId` for the current app session and include it when
opening the WebSocket connection:

```text
/ws?roomId=<room-id>&participantId=<participant-id>
```

Reuse that UUID across reconnects so the server preserves participant identity.
Remember the display name for the current app session so it can be prefilled on
subsequent join screens. Do not persist PINs.

## Android Project Setup

The repository now contains a single-module Android application scaffold in the
`app` module. It uses Kotlin, Jetpack Compose, and the package namespace
`com.hobsojam.simpleestimation`. Kotlin Multiplatform is not required for the
initial Android-only client.

### Prerequisites

- JDK 17 through 25
- Android SDK with API 35 installed
- Android Studio or command-line Android SDK tools

Use the checked-in Gradle wrapper for all local build commands. On Windows, run
commands with `gradlew.bat`; on macOS or Linux, run commands with `./gradlew`.
CI validates the build on JDK 25 while the Android app continues to compile to
Java 17 bytecode for device compatibility.

```powershell
.\gradlew.bat ktlintCheck
.\gradlew.bat detekt
.\gradlew.bat test
.\gradlew.bat koverXmlReportDebug
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

The same checks run in GitHub Actions for pushes to `main` and pull requests.
Kover writes the debug unit-test coverage report for SonarQube to
`app/build/reports/kover/reportDebug.xml`.
Gradle verifies downloaded dependency and plugin artifacts against the trusted
SHA-256 checksums in `gradle/verification-metadata.xml`. When intentionally
updating dependencies, regenerate the metadata with the affected build tasks
and review the checksum changes before committing them:

```powershell
.\gradlew.bat --write-verification-metadata sha256 ktlintCheck detekt koverXmlReportDebug lint assembleDebug
```

The detailed architecture guide is in `docs/architecture.md`. It owns package
boundaries, dependency direction, protocol mapping, state management,
persistence, and networking decisions.

## Security and Privacy

- Do not persist access PINs or facilitator PINs
- Do not log PINs, room payloads, or participant-entered backlog content
- Use HTTPS and WSS in production
- Restrict cleartext traffic to debug builds
- Treat display names and project items as potentially sensitive data
- Surface the server's demo-mode warning when `GET /api/config` reports it

## Testing Strategy

The initial test suite should include:

- Protocol JSON parsing tests using fixtures from the server contract
- Compatibility tests for supported and unsupported protocol versions
- Reducer or state-holder tests for room updates and reconnect transitions
- WebSocket error-code mapping tests
- Compose UI tests for joining, voting, timer display, and touch-friendly item
  movement
- A small integration suite against a locally running server

The server repository remains responsible for contract tests that verify the
HTTP and WebSocket implementation itself.

## Work Tracking

Use Linear for product planning and backlog tracking, and GitHub for code
review and delivery.

Linear should track:

- Product backlog items
- Feature work
- Bugs
- Accessibility and security follow-up
- Delivery-plan phases
- Sprint or cycle planning, if used

GitHub should remain the source of truth for:

- Branches
- Pull requests
- CI results
- Code review
- Dependabot and security alerts
- Release tags

Suggested Linear setup:

- Team: Hobsojam (`HOB`)
- Projects aligned to delivery-plan phases or major feature areas
- Labels: `feature`, `bug`, `docs`, `test`, `accessibility`, `security`,
  `protocol`, `tech-debt`

Do not duplicate every pull request as a manual Linear issue. Prefer linking
GitHub pull requests to the relevant Linear issue when implementation work is
tracked there. Keep protocol contracts, setup instructions, architecture
decisions, and project conventions in the repository docs rather than in
Linear.

## Status

Initial Android Gradle scaffold is present with ktlint, Detekt, unit-test, Android lint, and debug assembly checks wired into CI.
