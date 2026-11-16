BEGIN;

ALTER TABLE recipes
    ADD COLUMN IF NOT EXISTS drink_size VARCHAR(1) DEFAULT 'M';

UPDATE recipes
SET drink_size = 'M'
WHERE drink_size IS NULL;

ALTER TABLE recipes
    ALTER COLUMN drink_size SET DEFAULT 'M',
    ALTER COLUMN drink_size SET NOT NULL;

COMMIT;
