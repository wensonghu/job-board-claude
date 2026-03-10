-- Add OFFER_PENDING to the card status check constraint.
-- The constraint was created without this value, causing a 500 when
-- the auto-offer card is inserted.

ALTER TABLE card DROP CONSTRAINT IF EXISTS card_status_check;
ALTER TABLE card ADD CONSTRAINT card_status_check
    CHECK (status IN (
        'IN_PROGRESS',
        'INTERVIEW_SCHEDULE_PENDING',
        'INTERVIEW_DATE_CONFIRMED',
        'INTERVIEW_COMPLETED',
        'OFFER_PENDING',
        'REJECTED'
    ));

-- Also update card_history in case it has the same constraint.
ALTER TABLE card_history DROP CONSTRAINT IF EXISTS card_history_status_check;
ALTER TABLE card_history ADD CONSTRAINT card_history_status_check
    CHECK (status IN (
        'IN_PROGRESS',
        'INTERVIEW_SCHEDULE_PENDING',
        'INTERVIEW_DATE_CONFIRMED',
        'INTERVIEW_COMPLETED',
        'OFFER_PENDING',
        'REJECTED'
    ));
