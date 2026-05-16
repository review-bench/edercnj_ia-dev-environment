---
name: performance-socket
description: "Socket/WebSocket performance testing playbook: custom harness with k6 ws extension, load scenarios, and container reference."
requires-capabilities: [quality.performance.socket]
---

# Socket / WebSocket Performance Testing Playbook

## Default Tool: k6 with WebSocket extension

k6 supports WebSocket natively via `import ws from 'k6/ws'`. TCP-custom sockets use
the k6 `socket` extension or a custom harness depending on protocol specifics.

## Minimum Version

`grafana/k6:0.52` — container image with Alpine base.

## WebSocket Command Reference

```bash
docker run --rm --platform=linux/amd64 \
  -v "$PWD":/workspace \
  grafana/k6:0.52 run \
    --out json=/workspace/.perf-results.json \
    /workspace/performance/ws-scenario.js
```

## k6 WebSocket Scenario (`performance/ws-scenario.js`)

```javascript
import ws from 'k6/ws';
import { check } from 'k6';

export const options = {
  vus: 50,
  duration: '30s',
};

export default function () {
  const url = 'ws://host.docker.internal:8080/ws';
  const res = ws.connect(url, {}, function (socket) {
    socket.on('open', () => { socket.send('ping'); });
    socket.on('message', (data) => {
      check(data, { 'received pong': (d) => d === 'pong' });
      socket.close();
    });
    socket.on('error', (e) => { console.error(e.error()); });
  });
  check(res, { 'status was 101': (r) => r && r.status === 101 });
}
```

## TCP-Custom Harness (non-WebSocket)

For raw TCP sockets, use a custom Python/Go harness that measures round-trip latency
and writes the result JSON to `.perf-results.json` following the schema below.
Place the harness at `performance/tcp-harness.py` or `performance/tcp-harness.go`.

## Result Format (k6 JSON output — summary)

```json
{
  "metrics": {
    "ws_session_duration": {
      "values": { "avg": 42, "min": 5, "med": 40, "max": 200, "p(95)": 95, "p(99)": 180 }
    },
    "ws_msgs_sent": { "values": { "count": 1500 } }
  }
}
```

`x-test-performance` reads `p(50)` → p50Ms, `p(95)` → p95Ms, `p(99)` → p99Ms.

## Load Scenario Reference

| Scenario | VUs | Duration |
|----------|-----|----------|
| Baseline | 1 | 30s |
| Sustained | 50 VUs | 30s |
| Spike | 200 VUs | 10s |

## Baseline JSON Schema (Socket)

```json
{
  "socket": {
    "/ws (ping-pong)": {
      "p50Ms": 40, "p95Ms": 95, "p99Ms": 180,
      "throughputRps": 47.8,
      "measuredAt": "2026-05-01T00:00:00Z",
      "gitSha": "abc123..."
    }
  }
}
```

## Tooling Pinning

| Tool | Minimum | Container |
|------|---------|-----------|
| k6 | 0.52.0 | `grafana/k6:0.52` |

## Integration Notes

- WebSocket URL: `ws://` or `wss://` — add TLS cert volume for `wss://`
- Auth: inject tokens via k6 environment: `--env TOKEN=xxx`
- Binary messages: use `socket.sendBinary(buffer)` for non-text protocols
- Custom TCP harness output must match the baseline schema above
- Cold-start Docker pull: ~20s on first run; subsequent runs use layer cache
