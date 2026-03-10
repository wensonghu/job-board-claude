-- Drop all check constraints on card status/stage columns.
-- These constraints block inserting new enum values without a migration.
-- No restriction is needed — any string value is valid (validated at the app layer).

ALTER TABLE card DROP CONSTRAINT IF EXISTS card_status_check;
ALTER TABLE card DROP CONSTRAINT IF EXISTS card_stage_check;
ALTER TABLE card_history DROP CONSTRAINT IF EXISTS card_history_status_check;
ALTER TABLE card_history DROP CONSTRAINT IF EXISTS card_history_stage_check;
