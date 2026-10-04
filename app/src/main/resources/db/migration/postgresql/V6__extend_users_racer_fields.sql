-- V6: Extend users table with racer contact fields (Phase 2, RACER-01, RACER-02, AUDIO-12)

alter table users
    add column phone_number              varchar(30),
    add column emergency_contact_name    varchar(100),
    add column emergency_contact_phone   varchar(30),
    add column phonetic_name             varchar(255);
-- phonetic_name included early per AUDIO-12 (Phase 6 will use it; no harm adding now)
