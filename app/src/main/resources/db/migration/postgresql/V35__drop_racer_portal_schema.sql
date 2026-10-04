-- V35: Drop the racer portal schema (L10, #18).
-- Officials are the only accounts that sign in. Racer self-service (profile, cars,
-- transponders, car tags, class ratings, governing-body memberships, password reset)
-- has no code left, so its tables and columns go. Entries keep their transponder
-- snapshots and competitor; the car and transponder links they pointed at go with them.

-- Entries: links to racer-owned cars and transponders, and the racer membership override
alter table entries
    drop column car_id,
    drop column transponder_id,
    drop column membership_override,
    drop column membership_override_by,
    drop column membership_override_at,
    drop column membership_override_note;

alter table event_classes
    drop column required_governing_body_code;

-- Racer-owned tables (car_tag_values first: it references cars and car_tag_categories)
drop table car_tag_values;
drop table car_tag_categories;
drop table cars;
drop table transponders;
drop table user_class_ratings;
drop table user_governing_body_memberships;
drop table password_reset_tokens;

-- Racer profile fields
alter table users
    drop column phone_number,
    drop column emergency_contact_name,
    drop column emergency_contact_phone,
    drop column phonetic_name,
    drop column preferred_voice_id;

alter table club_profiles
    drop column show_car_tags_in_results;

-- The RACER role is gone. Former racer accounts stay (entries and audit rows may point
-- at them) but hold no role, so they cannot sign in; revoke any session they still hold.
delete from user_roles where role = 'RACER';

update refresh_tokens rt
set revoked = true
where not rt.revoked
  and not exists (select 1 from user_roles ur where ur.user_id = rt.user_id);
