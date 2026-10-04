-- V20: Link transponder to car (RACER domain — transponder is physically installed in the car)
-- Nullable so existing cars are not broken; UI enforces presence before entry submission.

alter table cars
    add column transponder_id bigint references transponders(id) on delete set null;

create index idx_cars_transponder_id on cars(transponder_id);
