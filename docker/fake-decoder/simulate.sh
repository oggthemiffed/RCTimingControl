#!/bin/sh
set -e
JAR_COUNT=$(ls /app/lib/*.jar 2>/dev/null | wc -l)
if [ "$JAR_COUNT" -eq 0 ]; then
    echo "ERROR: No jars found in /app/lib/ — build may have failed" >&2
    exit 1
fi
exec java -cp "/app/lib/*" \
  dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"
