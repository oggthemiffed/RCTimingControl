-- V32: Transponders per event, with a primary and a secondary (L6, #14).
-- transponder_number stays the primary. A driver may run a second car or a spare transponder,
-- so laps from either number count for the entry. Numbers are unique only within a race,
-- and that is checked when laps arrive, not by a constraint.

alter table entries add column secondary_transponder_number varchar(20);
