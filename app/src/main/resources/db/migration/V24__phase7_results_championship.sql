-- V24: Phase 7 — Results & Championship schema additions.
-- car_number on race_entries: assigned by RoundGeneratorService on qualifying creation,
--   re-numbered by BumpUpSeedingService on finals seeding.
-- show_car_tags_in_results on club_profiles: global admin toggle for RESULT-04.

ALTER TABLE race_entries
    ADD COLUMN car_number int;
-- Nullable: car_number is null until the round generator assigns it.
-- Existing race entries keep car_number = NULL; PrintResultsPage renders null as '—'.

COMMENT ON COLUMN race_entries.car_number IS
    'Assigned by RoundGeneratorService on qualifying creation (1-N in entry/grid_position order). '
    'Re-numbered by BumpUpSeedingService on finals seeding (1-N from qualifying standing position). '
    'Consistent within a phase; changes at qualifying-to-finals boundary. '
    'Historical snapshots in result_snapshots.positions_json retain carNumber=null (accepted gap).';

ALTER TABLE club_profiles
    ADD COLUMN show_car_tags_in_results boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN club_profiles.show_car_tags_in_results IS
    'When true, car tag key/value pairs are displayed beneath the driver name in printed results (RESULT-04, D-07, D-08).';
