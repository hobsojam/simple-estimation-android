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

## Suggested Android Architecture

Start with a single Android application using Kotlin and Jetpack Compose.
Kotlin Multiplatform is not required for the initial Android-only client.

Suggested layers:

```text
app/
  UI navigation and dependency wiring

data/
  HTTP configuration client
  WebSocket connection manager
  Protocol DTOs and JSON parsing
  Session-scoped preferences

domain/
  Room-state models
  Connection and compatibility state
  Participant actions

feature/
  Server configuration
  Room join
  Planning Poker
  Bucket Estimation
  Relative Estimation
```

Keep protocol DTOs separate from UI models. Unknown JSON fields should not break
parsing so compatible server additions remain safe.

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

## Delivery Plan

1. Create the Android Gradle project and CI build
2. Add server URL configuration and protocol-version checks
3. Add active-room discovery and refresh
4. Implement room-list, room-link, and room-ID joining
5. Add session-scoped display name and participant ID handling
6. Implement WebSocket reconnect and error handling
7. Build the Planning Poker participant screen
8. Build Bucket and Relative Estimation participant screens
9. Add integration tests against a local server
10. Verify a deployed HTTPS and WSS instance on a physical Android device

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

Repository scaffold only. Android project setup is the next step.
