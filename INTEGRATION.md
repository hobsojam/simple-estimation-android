# Integration Testing

This document describes how to run the integration test suite and perform the
physical device smoke test for HOB-14.

## Prerequisites

- The `../simple-estimation` server must be running locally on port 3000.
- The test device or emulator must be able to reach the server.

To start the server:

```bash
cd ../simple-estimation
npm install   # first time only
npm start
```

The server listens on `http://localhost:3000` by default.

## Running the Integration Suite

Integration tests are excluded from the normal unit test task. They connect to
a real server and are not run in CI.

### From an Android emulator (AVD)

The emulator routes `10.0.2.2` to the host machine's loopback interface:

```powershell
.\gradlew.bat testDebugUnitTest -PrunIntegrationTests -PintegrationServerUrl=http://10.0.2.2:3000
```

### From a physical device (USB debugging or Wi-Fi)

Replace `<host-lan-ip>` with your development machine's LAN IP address:

```powershell
.\gradlew.bat testDebugUnitTest -PrunIntegrationTests -PintegrationServerUrl=http://<host-lan-ip>:3000
```

### Against the deployed HTTPS instance

```powershell
.\gradlew.bat testDebugUnitTest -PrunIntegrationTests -PintegrationServerUrl=https://your-server.example.com
```

Note: the Android network security configuration permits cleartext (`http://`)
only in debug builds. The deployed production server must use `https://`.

## Test Coverage

| Test class | What it verifies |
|---|---|
| `ServerConfigIntegrationTest` | `GET /health` returns 200 and `{"status":"ok"}`; `GET /api/config` returns `protocolVersion` and `demoMode` |
| `ActiveRoomsIntegrationTest` | `GET /api/rooms` returns a parseable list |
| `RoomSessionIntegrationTest` | WebSocket join receives a state message; voting updates participant voted status; an invalid vote returns a server error |

## Protocol Fixtures

Checked-in JSON fixtures in `app/src/test/resources/fixtures/` document the
key HTTP and WebSocket payload shapes:

| Fixture | Payload |
|---|---|
| `server_config.json` | `GET /api/config` — normal mode |
| `server_config_demo_mode.json` | `GET /api/config` — demo mode |
| `active_rooms.json` | `GET /api/rooms` — three room types |
| `ws_state_planning_poker.json` | WebSocket `state` — Planning Poker room |
| `ws_state_bucket.json` | WebSocket `state` — Bucket Estimation room |
| `ws_state_relative.json` | WebSocket `state` — Relative Estimation room |
| `ws_state_access_protected.json` | WebSocket `state` — access-protected (redacted) room |
| `ws_error_invalid_vote.json` | WebSocket `error` — invalid vote |
| `ws_error_join_before_voting.json` | WebSocket `error` — join required before voting |

The `FixtureParserTest` unit test verifies that each fixture parses correctly
using the production parser classes, so fixture drift is caught in CI.

## Physical Device Smoke Test

The following flows must be verified manually on a physical Android device:

### Setup

1. Enable USB debugging on the device.
2. Install the debug APK: `.\gradlew.bat installDebug`
3. Ensure the device can reach the server (USB tethering or shared Wi-Fi).

### Flows to verify

- [ ] **Server URL entry**: Enter the server URL; the app fetches config and
      shows the active rooms list.
- [ ] **Demo mode banner**: If the server is in demo mode, the banner appears.
- [ ] **Room join (Planning Poker)**: Select a room or enter a room ID and join.
      The Planning Poker screen appears with the participant's name.
- [ ] **Voting**: Tap a vote chip; it becomes selected. The vote is reflected in
      the participants list after the server echoes state.
- [ ] **Server error feedback**: Attempt an invalid action; the error banner
      appears and the selected chip is cleared.
- [ ] **Leave**: Tap Leave from the active room; the app returns to discovery.
- [ ] **Reconnect**: With the room open, toggle airplane mode off and on. The
      app reconnects and shows the current room state.
- [ ] **Room join (Bucket Estimation)**: Join a bucket room; the placement
      screen appears with XS/S/M/L/XL columns.
- [ ] **Item placement**: Select an item and tap Place in a bucket; the item
      moves to that column after the server echoes state.
- [ ] **Room join (Relative Estimation)**: Join a relative room; the placement
      screen appears with 1/2/3/5/8/13/21 columns.
- [ ] **TalkBack**: Enable TalkBack and navigate the join and voting flows;
      all interactive elements have meaningful labels.
- [ ] **Font scaling**: Set font size to largest in Accessibility settings;
      confirm no content is clipped.

### Recording results

Document the device model, OS version, and the date of the test run in a
comment on the HOB-14 Linear issue. Note any platform-specific behavior that
automated tests cannot cover.
