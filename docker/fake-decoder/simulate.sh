#!/bin/sh
set -e
exec java -cp "/app/lib/*" \
  dev.monkeypatch.rctiming.forwarder.simulator.SimulatorMain "$@"
