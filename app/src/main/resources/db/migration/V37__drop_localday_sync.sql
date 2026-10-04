-- V37: L13 (#21) retires the separate offline race-day app. Its cloud-side sync tables
-- (V27 day lock, sync generation, day-scoped credentials and instance secrets; V28 snapshot
-- ingest and device-loss audit) have no remaining users.

DROP TABLE IF EXISTS device_loss_audit;
DROP TABLE IF EXISTS event_snapshot_state;
DROP TABLE IF EXISTS localday_snapshots;
DROP TABLE IF EXISTS localday_instance_secrets;
DROP TABLE IF EXISTS localday_credentials;
DROP TABLE IF EXISTS event_sync_generations;
DROP TABLE IF EXISTS event_offline_locks;
