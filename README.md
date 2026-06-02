# simple-estimation-android

Native Android participation client for [simple-estimation](https://github.com/hobsojam/simple-estimation).

## Initial Scope

This app is a participant-focused client. It will support:

- Configuring a Simple Estimation server URL
- Checking the server protocol version
- Joining an existing room with a display name and optional access PIN
- Persisting a session-scoped display name and participant ID for reconnects
- Voting in Planning Poker rooms
- Viewing facilitator-driven backlog, reveal, result, and timer state
- Participating in Bucket and Relative Estimation rooms with touch-friendly move controls
- Reconnecting after transient network failures

The initial client will not create or delete rooms, expose facilitator controls, edit backlogs, export CSV files, or persist PINs.

## Protocol

The server contract is documented in the main repository:

- [API documentation](https://github.com/hobsojam/simple-estimation/blob/main/docs/api.md)

The client must check `GET /api/config` and reject protocol versions it does not support.

## Status

Repository scaffold only. Android project setup is the next step.
