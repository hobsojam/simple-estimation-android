# simple-estimation-android - Project Context

## Purpose

This repository contains the native Android participation client for
[simple-estimation](https://github.com/hobsojam/simple-estimation), a
self-hosted real-time agile estimation tool.

The Android app is for participants joining an existing room from a phone or
tablet. Room creation, facilitation, administration, and data export remain in
the web application.

## Ways of Working

- Work contract-first. Check `../simple-estimation/docs/api.md` before
  implementing HTTP or WebSocket behavior. If the protocol needs to change,
  update the server contract first rather than adding Android-only behavior.
- Use test-driven development for behavior changes. Add or update the focused
  failing test first, implement the smallest passing change, then refactor and
  run broader checks.
- Keep changes narrow and reviewable. Do not mix unrelated refactors,
  formatting churn, dependency changes, and feature work in the same change.
- Prefer simple, explicit Android architecture: protocol DTOs in data, business
  state in domain, and Compose UI driven by immutable state plus event
  callbacks.
- Treat security, privacy, and accessibility as design constraints, not cleanup
  tasks. Do not persist PINs or sensitive room data, and keep participant flows
  usable with TalkBack, keyboard navigation, touch, and font scaling.
- Run the relevant Gradle checks before pushing once the project is scaffolded:
  `ktlintCheck`, unit tests, Android lint, and `assembleDebug`.
- Update documentation in the same change when setup, supported workflows,
  architecture, protocol assumptions, or delivery status changes.
- Ask before adding production dependencies, analytics, crash reporting,
  external services, signing material, or any behavior outside the documented
  participant-client scope.
- Do not leave generated build output, secrets, local machine config, or
  signing material in git.

## Product Scope

Supported participant workflows:

- Configure the URL of a deployed Simple Estimation server.
- Browse and refresh the active-room list.
- Open a room link, enter an existing room ID, or choose an active room.
- Join with a display name and provide an access PIN when required.
- Reconnect after transient failures while preserving the participant identity
  for the current app session.
- Show protocol compatibility, server, and connection errors clearly.
- Participate in Planning Poker, Bucket Estimation, and Relative Estimation.

Explicit non-goals:

- Do not create or delete rooms.
- Do not claim the facilitator role or expose facilitator controls.
- Do not add, select, finalise, or remove backlog items.
- Do not reveal votes, reset rounds, control timers, or export CSV files.
- Do not persist access PINs or facilitator PINs.
- Do not replace the web client for administration.

## Server Contract

The sibling `../simple-estimation` repository owns the HTTP and WebSocket
contract. Its `docs/api.md` file is the source of truth. Check it before
implementing protocol behavior and update the server repository first when a
contract change is required. Do not invent Android-only protocol extensions.

Before joining a room, call:

```text
GET /api/config
```

Reject unsupported protocol versions with a useful upgrade message. The
initial Android client supports protocol version `1`.

The server is the source of truth for room state. Render the latest sanitized
`state.room` payload after connecting; do not reconstruct authoritative state
from local commands.

Open WebSocket connections using a session-scoped UUID:

```text
/ws?roomId=<room-id>&participantId=<participant-id>
```

Reuse that UUID across reconnects during the current app session so the server
can preserve participant identity. Remember the display name only for the
current app session. Never persist PINs.

Production servers should use HTTPS and WSS. Cleartext HTTP and WebSocket
traffic is allowed only in debug builds for local development, typically via:

```text
http://10.0.2.2:3000
ws://10.0.2.2:3000/ws
```

## Android Architecture

Use Kotlin and Jetpack Compose for the initial Android-only client. Kotlin
Multiplatform is not required.

Keep protocol DTOs separate from UI and domain models. JSON parsing must ignore
unknown fields so compatible server additions do not break older clients.

Prefer these boundaries as the project is scaffolded:

```text
app/
  UI navigation and dependency wiring

data/
  HTTP configuration client
  WebSocket connection manager
  Protocol DTOs and JSON parsing
  Session-scoped state

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

## Security and Privacy

- Never persist or log access PINs, facilitator PINs, room payloads,
  participant names, or participant-entered backlog content.
- Treat display names and backlog content as potentially sensitive data.
- Do not add analytics, crash-reporting, or external cloud services without
  explicit user approval.
- Use HTTPS and WSS in production.
- Restrict cleartext traffic to debug builds.
- Surface the demo-mode warning when `GET /api/config` reports it.
- Validate server payloads defensively. Handle malformed data without crashing
  or exposing internal details.
- Keep secrets, signing keys, `local.properties`, and generated build output
  out of git.
- Do not add dependencies without a clear feature requirement. Review their
  maintenance status and known security issues first.

## Accessibility

The Android client must remain usable with TalkBack, keyboard navigation, and
touch input.

- Give every actionable Compose element a meaningful semantic role and label.
- Do not use color as the only way to communicate state. Pair it with text,
  icons, or content descriptions.
- Announce dynamic errors, connection-state changes, vote progress, timers,
  and item moves appropriately.
- Preserve visible keyboard focus and logical focus order.
- Use touch targets of at least `48.dp`.
- Support font scaling without clipping essential content or controls.
- Keep select-and-place as the primary accessible interaction for moving items.
  Drag-and-drop may be added only as an optional enhancement.

For UI work, test the affected flow with TalkBack or keyboard navigation in
addition to automated Compose tests.

## Testing

Build coverage at the appropriate layer:

| Layer | What to cover |
|---|---|
| Unit | Protocol JSON parsing, compatibility checks, reducers/state holders, reconnect transitions, WebSocket error mapping |
| Compose UI | Joining, voting, timers, connection errors, and touch-friendly select-and-place movement |
| Integration | A small set of client flows against a locally running `../simple-estimation` server |

When the Gradle project exists, run the relevant checks before pushing. Prefer
the repository's Gradle wrapper:

```powershell
.\gradlew.bat ktlintCheck
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Run instrumentation or emulator-backed tests when a change affects Android UI,
navigation, or platform behavior.

### Test-Driven Development

Use test-driven development for behavior changes:

1. Add or update a focused failing test that describes the required behavior.
2. Implement the smallest production change that makes the test pass.
3. Refactor only after the test passes, then run the relevant broader checks.

Bug fixes must include a regression test when the behavior can be exercised
automatically. For Android UI and platform behavior that cannot be covered
practically by a local automated test, document the manual verification
performed.

Do not add tests that merely mirror implementation details. Test observable
behavior at the lowest appropriate layer and keep emulator-backed tests for
flows that require Android platform integration.

## Development Conventions

- Use Kotlin and Jetpack Compose unless the existing scaffold establishes a
  more specific local pattern.
- Keep changes scoped. Avoid unrelated refactors while implementing features.
- Keep network and protocol handling out of composables.
- Model connection and compatibility states explicitly; do not bury them in
  ad hoc UI flags.
- Prefer immutable state and one-way data flow.
- Use session-scoped storage for participant identity and display name.
- Do not commit generated build output.
- Update `README.md` when architecture, supported workflows, setup steps, or
  delivery status change.

### Code Format

- All Kotlin source must follow the
  [Google Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
  Treat it as the authoritative baseline for formatting, naming, file
  structure, and Kotlin coding conventions.
- Use 4 spaces for indentation. Do not use tabs.
- Keep source files focused: one primary public type per file unless small
  related types are clearer when colocated.
- Match filenames to their primary public type.
- Use `UpperCamelCase` for types and composable functions, `lowerCamelCase` for
  functions and properties, and `UPPER_SNAKE_CASE` for constants.
- Keep composables small. Hoist state when it is shared, persisted, or needed
  for testing. Pass immutable UI state and event callbacks where practical.
- Prefer named arguments when a call has multiple parameters of the same type
  or when they make intent clearer.
- Add comments only when they explain a non-obvious reason or constraint.
- Remove unused imports and keep import ordering consistent with the formatter.
- Add `ktlint` during the initial Gradle scaffold and configure it to enforce
  the automatable subset of Google Android Kotlin Style.
- Commit the shared `.editorconfig` used by `ktlint`; do not rely on individual
  Android Studio settings.
- Run `.\gradlew.bat ktlintCheck` before pushing and in CI. Use
  `.\gradlew.bat ktlintFormat` to apply automatic formatting fixes.
- The Google Android Kotlin Style Guide remains authoritative where `ktlint`
  does not enforce a rule. If tool defaults conflict with the guide, update the
  tool configuration rather than adopting the conflicting defaults.

## Delivery Plan

| Phase | Scope | Status |
|---|---|---|
| 1 | Create the Android Gradle project, `ktlint` configuration, and CI build | Planned |
| 2 | Add server URL configuration and protocol-version checks | Planned |
| 3 | Add active-room discovery and refresh | Planned |
| 4 | Implement room-list, room-link, and room-ID joining | Planned |
| 5 | Add session-scoped display name and participant ID handling | Planned |
| 6 | Implement WebSocket reconnect and error handling | Planned |
| 7 | Build the Planning Poker participant screen | Planned |
| 8 | Build Bucket and Relative Estimation participant screens | Planned |
| 9 | Add integration tests against a local server | Planned |
| 10 | Verify a deployed HTTPS and WSS instance on a physical device | Planned |

Update this table and `README.md` when a phase is completed.

## Git Workflow

- Develop each feature on its own branch from an up-to-date `main`.
- Use branch names such as `feat/<short-description>`.
- Open a PR targeting `main` for feature work.
- Never commit feature work directly to `main`.
- Never force-push to `main`.
- Before switching branches, commit and push finished work. If local changes
  are incomplete, ask before stashing or discarding them.
- Never commit secrets, credentials, signing material, or local machine config.
- Do not amend published shared-branch commits without user confirmation.
- Include this trailer in commits created by Codex:

  ```text
  Co-Authored-By: Codex <noreply@anthropic.com>
  ```

## Local Windows Sandbox Notes

In the Codex desktop environment, PowerShell and Git commands may
intermittently fail with:

```text
windows sandbox: runner error: CreateProcessAsUserW failed: 1312
```

This is usually a sandbox process-launch issue, not a repository problem. If a
read-only or otherwise appropriate command is needed, retry the same command
with escalation rather than modifying the repository or adding workaround
logic.

Do not emit Codex desktop clickable action directives or action suggestions on
this Windows installation, including `::git-stage`, `::git-commit`,
`::git-create-branch`, `::git-push`, and `::git-create-pr`. Report completed
actions using normal text.

Do not emit Markdown links for local files, web URLs, or other resources in
responses on this Windows installation. Use plain-text paths and plain-text
URLs instead. Clicking rendered links is routed through the Codex desktop
`codex:type=action...` handler, which currently launches Electron with an
invalid `C:\Program Files\WindowsApps\...` module path.
