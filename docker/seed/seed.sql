-- Wyvern RC Club demo seed data
-- Idempotent: skips all inserts if club already exists.
-- Run order: after Flyway migrations have completed (schema must exist).
-- FK-safe insertion order matches V1..V26 migration dependency chain.

DO $$
DECLARE
    -- users
    v_admin_id       bigint;
    v_dave_id        bigint;
    v_sam_id         bigint;
    v_jo_id          bigint;
    v_pat_id         bigint;
    v_kim_id         bigint;
    v_lee_id         bigint;
    v_max_id         bigint;
    v_nina_id        bigint;

    -- tracks
    v_rivermead_id   bigint;
    v_parklands_id   bigint;

    -- racing classes
    v_touring_id     bigint;
    v_buggy_id       bigint;
    v_f1_id          bigint;

    -- race format templates
    v_timed5_id      bigint;
    v_bumpup_id      bigint;
    v_points_id      bigint;

    -- events
    v_round4_id      bigint;
    v_round3_id      bigint;

    -- event classes
    v_ec_round4_id   bigint;
    v_ec_round3_id   bigint;

    -- cars
    v_dave_car_id    bigint;
    v_sam_car_id     bigint;
    v_jo_car_id      bigint;
    v_pat_car_id     bigint;
    v_kim_car_id     bigint;
    v_lee_car_id     bigint;
    v_max_car_id     bigint;
    v_nina_car_id    bigint;

    -- entries (round 4 open event)
    v_entry_dave_r4  bigint;
    v_entry_sam_r4   bigint;
    v_entry_jo_r4    bigint;
    v_entry_pat_r4   bigint;
    v_entry_kim_r4   bigint;
    v_entry_lee_r4   bigint;
    v_entry_max_r4   bigint;
    v_entry_nina_r4  bigint;

    -- entries (round 3 completed event)
    v_entry_dave_r3  bigint;
    v_entry_sam_r3   bigint;
    v_entry_jo_r3    bigint;
    v_entry_pat_r3   bigint;
    v_entry_kim_r3   bigint;
    v_entry_lee_r3   bigint;
    v_entry_max_r3   bigint;
    v_entry_nina_r3  bigint;

    -- championship
    v_champ_id       bigint;

    -- rounds / races
    v_round_id       bigint;
    v_race_id        bigint;

    -- BCrypt hash for password 'trial123' (cost 10, verified with Python bcrypt)
    v_pw_hash        text := '$2b$10$O1DvFrcjL3XLlNnXWdaa1.vynY1S5eZ2eLwvJ9NuA2jbxLcLO4y52';

BEGIN
    -- Guard 1: schema not yet migrated — fail loudly so compose retries.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'club_profiles'
    ) THEN
        RAISE EXCEPTION 'Schema not migrated yet — club_profiles missing. Retry after app starts.';
    END IF;

    -- Guard 2: idempotency — skip if Wyvern club already present.
    IF EXISTS (SELECT 1 FROM club_profiles WHERE name = 'Wyvern RC Club') THEN
        RAISE NOTICE 'Seed data already present — skipping';
        RETURN;
    END IF;

    -- =========================================================================
    -- 1. governing_body_affiliations
    -- =========================================================================
    INSERT INTO governing_body_affiliations (code, display_name, membership_required)
    VALUES ('BRCA', 'British Radio Car Association', false);

    -- =========================================================================
    -- 2. users
    -- =========================================================================
    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('admin@example.com', v_pw_hash, 'Alice', 'Admin')
    RETURNING id INTO v_admin_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('dave.racer@example.com', v_pw_hash, 'Dave', 'Quick')
    RETURNING id INTO v_dave_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('sam.speed@example.com', v_pw_hash, 'Sam', 'Speed')
    RETURNING id INTO v_sam_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('jo.turner@example.com', v_pw_hash, 'Jo', 'Turner')
    RETURNING id INTO v_jo_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('pat.drift@example.com', v_pw_hash, 'Pat', 'Drift')
    RETURNING id INTO v_pat_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('kim.apex@example.com', v_pw_hash, 'Kim', 'Apex')
    RETURNING id INTO v_kim_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('lee.grid@example.com', v_pw_hash, 'Lee', 'Grid')
    RETURNING id INTO v_lee_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('max.lap@example.com', v_pw_hash, 'Max', 'Lap')
    RETURNING id INTO v_max_id;

    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('nina.pole@example.com', v_pw_hash, 'Nina', 'Pole')
    RETURNING id INTO v_nina_id;

    -- user_roles
    INSERT INTO user_roles (user_id, role) VALUES (v_admin_id, 'ADMIN');
    INSERT INTO user_roles (user_id, role) VALUES (v_admin_id, 'RACE_DIRECTOR');
    INSERT INTO user_roles (user_id, role) VALUES (v_admin_id, 'REFEREE');

    INSERT INTO user_roles (user_id, role) VALUES (v_dave_id,  'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_sam_id,   'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_jo_id,    'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_pat_id,   'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_kim_id,   'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_lee_id,   'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_max_id,   'RACER');
    INSERT INTO user_roles (user_id, role) VALUES (v_nina_id,  'RACER');

    -- =========================================================================
    -- 3. club_profiles
    -- =========================================================================
    INSERT INTO club_profiles (name, email, timezone, decoder_host, decoder_port, decoder_protocol,
                               show_car_tags_in_results)
    VALUES ('Wyvern RC Club', 'info@example.com', 'Europe/London',
            'fake-decoder', 5100, 'RC4', false);

    -- =========================================================================
    -- 4. tracks + decoder_loops
    -- =========================================================================
    INSERT INTO tracks (name, track_length)
    VALUES ('Rivermead Circuit', 220)
    RETURNING id INTO v_rivermead_id;

    INSERT INTO decoder_loops (track_id, loop_id, display_name, loop_type, is_scoring_loop)
    VALUES (v_rivermead_id, 'L1', 'Start/Finish', 'FINISH_LINE', true);

    INSERT INTO tracks (name, track_length)
    VALUES ('Parklands Arena', 185)
    RETURNING id INTO v_parklands_id;

    INSERT INTO decoder_loops (track_id, loop_id, display_name, loop_type, is_scoring_loop)
    VALUES (v_parklands_id, 'L1', 'Start/Finish', 'FINISH_LINE', true);

    -- =========================================================================
    -- 5. racing_classes
    -- =========================================================================
    INSERT INTO racing_classes (name, description)
    VALUES ('13.5 Touring', 'Spec 13.5T brushless touring car class')
    RETURNING id INTO v_touring_id;

    INSERT INTO racing_classes (name, description)
    VALUES ('Stock Buggy', 'Stock-spec 2WD/4WD buggy class')
    RETURNING id INTO v_buggy_id;

    INSERT INTO racing_classes (name, description)
    VALUES ('F1 Open', 'Open formula 1/10 scale F1 class')
    RETURNING id INTO v_f1_id;

    -- =========================================================================
    -- 6. race_format_templates
    -- =========================================================================
    INSERT INTO race_format_templates (name, config)
    VALUES ('Timed 5-min', '{"type":"TIMED","durationMinutes":5}'::jsonb)
    RETURNING id INTO v_timed5_id;

    INSERT INTO race_format_templates (name, config)
    VALUES ('Bump-up Finals', '{"type":"BUMP_UP","heatSize":8,"bumpCount":2}'::jsonb)
    RETURNING id INTO v_bumpup_id;

    INSERT INTO race_format_templates (name, config)
    VALUES ('Points Finals', '{"type":"POINTS_FINALS","finalsCount":3}'::jsonb)
    RETURNING id INTO v_points_id;

    -- =========================================================================
    -- 7. events
    -- =========================================================================
    -- Upcoming open event (Round 4)
    INSERT INTO events (name, event_date, status, track_id)
    VALUES ('Wyvern Winter Series Round 4', '2026-06-20', 'OPEN', v_rivermead_id)
    RETURNING id INTO v_round4_id;

    -- Completed historical event (Round 3)
    INSERT INTO events (name, event_date, status, track_id)
    VALUES ('Wyvern Winter Series Round 3', '2026-05-09', 'COMPLETED', v_parklands_id)
    RETURNING id INTO v_round3_id;

    -- =========================================================================
    -- 8. event_classes
    -- =========================================================================
    -- Round 4 (OPEN) event class
    INSERT INTO event_classes (config_snapshot, template_id, event_id, racing_class_id,
                               finals_count, cars_per_final, bump_count)
    VALUES ('{"type":"TIMED","durationMinutes":5}'::jsonb,
            v_timed5_id, v_round4_id, v_touring_id, 1, 8, 0)
    RETURNING id INTO v_ec_round4_id;

    -- Round 3 (COMPLETED) event class
    INSERT INTO event_classes (config_snapshot, template_id, event_id, racing_class_id,
                               finals_count, cars_per_final, bump_count)
    VALUES ('{"type":"TIMED","durationMinutes":5}'::jsonb,
            v_timed5_id, v_round3_id, v_touring_id, 1, 8, 0)
    RETURNING id INTO v_ec_round3_id;

    -- =========================================================================
    -- 9. cars (one per racer, 13.5 Touring)
    -- =========================================================================
    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_dave_id, 'Dave''s TC', v_touring_id)
    RETURNING id INTO v_dave_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_sam_id, 'Sam''s TC', v_touring_id)
    RETURNING id INTO v_sam_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_jo_id, 'Jo''s TC', v_touring_id)
    RETURNING id INTO v_jo_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_pat_id, 'Pat''s TC', v_touring_id)
    RETURNING id INTO v_pat_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_kim_id, 'Kim''s TC', v_touring_id)
    RETURNING id INTO v_kim_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_lee_id, 'Lee''s TC', v_touring_id)
    RETURNING id INTO v_lee_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_max_id, 'Max''s TC', v_touring_id)
    RETURNING id INTO v_max_car_id;

    INSERT INTO cars (user_id, name, primary_class_id)
    VALUES (v_nina_id, 'Nina''s TC', v_touring_id)
    RETURNING id INTO v_nina_car_id;

    -- =========================================================================
    -- 10. transponders (one per racer, IDs 101-108)
    -- =========================================================================
    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_dave_id, '101', 'Dave #101');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_sam_id, '102', 'Sam #102');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_jo_id, '103', 'Jo #103');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_pat_id, '104', 'Pat #104');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_kim_id, '105', 'Kim #105');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_lee_id, '106', 'Lee #106');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_max_id, '107', 'Max #107');

    INSERT INTO transponders (user_id, transponder_number, label)
    VALUES (v_nina_id, '108', 'Nina #108');

    -- =========================================================================
    -- 11. entries — Round 4 (OPEN) event
    -- =========================================================================
    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_dave_id, v_round4_id, v_ec_round4_id, '101', 'Dave #101', 'CONFIRMED')
    RETURNING id INTO v_entry_dave_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_sam_id, v_round4_id, v_ec_round4_id, '102', 'Sam #102', 'CONFIRMED')
    RETURNING id INTO v_entry_sam_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_jo_id, v_round4_id, v_ec_round4_id, '103', 'Jo #103', 'CONFIRMED')
    RETURNING id INTO v_entry_jo_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_pat_id, v_round4_id, v_ec_round4_id, '104', 'Pat #104', 'CONFIRMED')
    RETURNING id INTO v_entry_pat_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_kim_id, v_round4_id, v_ec_round4_id, '105', 'Kim #105', 'CONFIRMED')
    RETURNING id INTO v_entry_kim_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_lee_id, v_round4_id, v_ec_round4_id, '106', 'Lee #106', 'CONFIRMED')
    RETURNING id INTO v_entry_lee_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_max_id, v_round4_id, v_ec_round4_id, '107', 'Max #107', 'CONFIRMED')
    RETURNING id INTO v_entry_max_r4;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_nina_id, v_round4_id, v_ec_round4_id, '108', 'Nina #108', 'CONFIRMED')
    RETURNING id INTO v_entry_nina_r4;

    -- =========================================================================
    -- 12. championships
    -- =========================================================================
    INSERT INTO championships (name, best_x_from_y_x, best_x_from_y_y, scoring_source,
                               tq_bonus_points, afinal_winner_bonus_points)
    VALUES ('2026 Wyvern Winter Series', 4, 6, 'FINALS', 2, 3)
    RETURNING id INTO v_champ_id;

    -- =========================================================================
    -- 13. championship_classes
    -- =========================================================================
    INSERT INTO championship_classes (championship_id, racing_class_id)
    VALUES (v_champ_id, v_touring_id);

    -- =========================================================================
    -- 14. championship_event_links
    -- =========================================================================
    INSERT INTO championship_event_links (championship_id, event_id, round_number)
    VALUES (v_champ_id, v_round3_id, 3);

    INSERT INTO championship_event_links (championship_id, event_id, round_number)
    VALUES (v_champ_id, v_round4_id, 4);

    -- =========================================================================
    -- 15. championship_points_scale (positions 1-10)
    -- =========================================================================
    INSERT INTO championship_points_scale (championship_id, position, points)
    VALUES
        (v_champ_id, 1,  15),
        (v_champ_id, 2,  12),
        (v_champ_id, 3,  10),
        (v_champ_id, 4,   8),
        (v_champ_id, 5,   6),
        (v_champ_id, 6,   5),
        (v_champ_id, 7,   4),
        (v_champ_id, 8,   3),
        (v_champ_id, 9,   2),
        (v_champ_id, 10,  1);

    -- =========================================================================
    -- 16. rounds + races + race_entries + result_snapshots for COMPLETED event
    -- =========================================================================

    -- Entries for Round 3 (completed event)
    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_dave_id, v_round3_id, v_ec_round3_id, '101', 'Dave #101', 'CONFIRMED')
    RETURNING id INTO v_entry_dave_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_sam_id, v_round3_id, v_ec_round3_id, '102', 'Sam #102', 'CONFIRMED')
    RETURNING id INTO v_entry_sam_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_jo_id, v_round3_id, v_ec_round3_id, '103', 'Jo #103', 'CONFIRMED')
    RETURNING id INTO v_entry_jo_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_pat_id, v_round3_id, v_ec_round3_id, '104', 'Pat #104', 'CONFIRMED')
    RETURNING id INTO v_entry_pat_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_kim_id, v_round3_id, v_ec_round3_id, '105', 'Kim #105', 'CONFIRMED')
    RETURNING id INTO v_entry_kim_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_lee_id, v_round3_id, v_ec_round3_id, '106', 'Lee #106', 'CONFIRMED')
    RETURNING id INTO v_entry_lee_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_max_id, v_round3_id, v_ec_round3_id, '107', 'Max #107', 'CONFIRMED')
    RETURNING id INTO v_entry_max_r3;

    INSERT INTO entries (user_id, event_id, event_class_id, transponder_number,
                         transponder_label, status)
    VALUES (v_nina_id, v_round3_id, v_ec_round3_id, '108', 'Nina #108', 'CONFIRMED')
    RETURNING id INTO v_entry_nina_r3;

    -- Round record
    INSERT INTO rounds (event_id, type, round_number, sequence_in_event, status)
    VALUES (v_round3_id, 'FINAL', 1, 1, 'COMPLETED')
    RETURNING id INTO v_round_id;

    -- Race record
    INSERT INTO races (round_id, event_class_id, heat_number, sequence_in_round,
                       final_letter, start_type, format_id, status,
                       started_at, finished_at)
    VALUES (v_round_id, v_ec_round3_id, 1, 1, 'A', 'GRID', v_timed5_id, 'FINISHED',
            '2026-05-09 13:00:00+00', '2026-05-09 13:05:30+00')
    RETURNING id INTO v_race_id;

    -- race_entries (grid positions 1-8, car numbers 1-8)
    INSERT INTO race_entries (race_id, entry_id, grid_position, car_number)
    VALUES
        (v_race_id, v_entry_dave_r3,  1, 1),
        (v_race_id, v_entry_sam_r3,   2, 2),
        (v_race_id, v_entry_jo_r3,    3, 3),
        (v_race_id, v_entry_pat_r3,   4, 4),
        (v_race_id, v_entry_kim_r3,   5, 5),
        (v_race_id, v_entry_lee_r3,   6, 6),
        (v_race_id, v_entry_max_r3,   7, 7),
        (v_race_id, v_entry_nina_r3,  8, 8);

    -- result_snapshots
    INSERT INTO result_snapshots (race_id, finished_at, positions_json, lap_history_json)
    VALUES (
        v_race_id,
        '2026-05-09 13:05:30+00',
        '[
          {"position":1,"driverName":"Sam Speed",  "laps":14,"transponderNumber":"102"},
          {"position":2,"driverName":"Nina Pole",  "laps":14,"transponderNumber":"108"},
          {"position":3,"driverName":"Dave Quick", "laps":13,"transponderNumber":"101"},
          {"position":4,"driverName":"Kim Apex",   "laps":13,"transponderNumber":"105"},
          {"position":5,"driverName":"Jo Turner",  "laps":12,"transponderNumber":"103"},
          {"position":6,"driverName":"Pat Drift",  "laps":12,"transponderNumber":"104"},
          {"position":7,"driverName":"Max Lap",    "laps":11,"transponderNumber":"107"},
          {"position":8,"driverName":"Lee Grid",   "laps":11,"transponderNumber":"106"}
        ]'::jsonb,
        '{}'::jsonb
    );

    -- =========================================================================
    -- 17. forwarder_token — demo token matching FORWARDER_API_TOKEN in .env.example
    -- =========================================================================
    INSERT INTO forwarder_token (token_hash, token_value, status, generated_at)
    VALUES ('demo-not-used-for-validation',
            'DEMO-FORWARDER-TOKEN-CHANGE-BEFORE-PRODUCTION',
            'ACTIVE',
            NOW());

    RAISE NOTICE 'Wyvern RC Club seed complete';
END $$;
