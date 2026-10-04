-- V25: Phase 8 — First-run setup wizard
-- Adds decoder configuration columns to club_profiles. All nullable.

ALTER TABLE club_profiles
    ADD COLUMN decoder_host     VARCHAR(255),
    ADD COLUMN decoder_port     INTEGER,
    ADD COLUMN decoder_protocol VARCHAR(10);

COMMENT ON COLUMN club_profiles.decoder_host IS
    'Hostname or IP address of the AMB decoder. Null until configured via setup wizard.';
COMMENT ON COLUMN club_profiles.decoder_port IS
    'TCP port. RC-4 -> 5100, P3 -> 5403. Null until configured.';
COMMENT ON COLUMN club_profiles.decoder_protocol IS
    'Decoder protocol: RC4 (text, firmware <4.5) or P3 (binary, firmware >=4.5). Null until configured.';
