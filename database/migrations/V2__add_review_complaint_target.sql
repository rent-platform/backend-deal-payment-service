ALTER TABLE complaints DROP CONSTRAINT IF EXISTS complaints_target_type_check;

ALTER TABLE complaints ADD CONSTRAINT complaints_target_type_check
    CHECK (target_type IN ('USER', 'ITEM', 'REVIEW'));