-- Rename stage values: SEEDING → EARLY, NEXT_ROUNDS → OTHER
UPDATE card         SET stage = 'EARLY' WHERE stage = 'SEEDING';
UPDATE card         SET stage = 'OTHER' WHERE stage = 'NEXT_ROUNDS';
UPDATE card_history SET stage = 'EARLY' WHERE stage = 'SEEDING';
UPDATE card_history SET stage = 'OTHER' WHERE stage = 'NEXT_ROUNDS';
