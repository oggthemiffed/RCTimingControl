#!/bin/sh
# Writes forwarder.properties from env vars, then launches the forwarder.
set -e
cat > /tmp/forwarder.properties <<PROPS
forwarder.api-token=${FORWARDER_API_TOKEN}
forwarder.decoder.host=${FORWARDER_DECODER_HOST:-fake-decoder}
forwarder.decoder.port=${FORWARDER_DECODER_PORT:-5100}
forwarder.grpc.host=${FORWARDER_GRPC_HOST:-app}
forwarder.grpc.port=${FORWARDER_GRPC_PORT:-9090}
forwarder.grpc.plaintext=${FORWARDER_GRPC_PLAINTEXT:-true}
PROPS
exec /app/bin/forwarder --config-file=/tmp/forwarder.properties
