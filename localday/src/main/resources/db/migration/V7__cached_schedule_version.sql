-- V7: optimistic-lock version column on cached_schedule (U9 code-review follow-up).
--
-- Without this, two concurrent race-control requests for the same race (e.g. a double-submitted
-- "Finish" click, or a client retry after a slow response over venue WiFi) could both read the
-- same in-memory status, both pass the state-machine's transition check, and both persist —
-- for a FINISHED transition, this meant duplicate race_results rows per entry. JPA's standard
-- @Version optimistic-locking column makes the loser's save() throw
-- ObjectOptimisticLockingFailureException instead of silently racing ahead.
--
-- All DDL below is plain, transactionally-safe DDL (no CREATE INDEX CONCURRENTLY, no
-- non-transactional ALTER TYPE ... ADD VALUE) per the constraint established in V1.

ALTER TABLE cached_schedule ADD COLUMN version bigint NOT NULL DEFAULT 0;
