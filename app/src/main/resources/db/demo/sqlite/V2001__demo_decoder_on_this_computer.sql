-- The demo club's decoder was the trial stack's fake-decoder container. The trial now runs the
-- simulator on the same computer as the app (`RCTimingControl simulate`, #24), so point it there.
UPDATE club_profiles SET decoder_host = 'localhost' WHERE decoder_host = 'fake-decoder';
