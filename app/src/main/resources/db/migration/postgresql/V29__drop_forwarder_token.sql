-- V29: remove the forwarder API token. The gRPC forwarder is gone (L2): the app reads the
-- decoder directly, so nothing authenticates with a forwarder token any more.
-- Supersedes V21 (create) and V26 (plaintext column).
DROP TABLE IF EXISTS forwarder_token;
