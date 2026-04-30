CREATE TABLE system_settings (
    id              BIGINT PRIMARY KEY DEFAULT 1,
    require_images_admin   BOOLEAN NOT NULL DEFAULT FALSE,
    require_images_dealer  BOOLEAN NOT NULL DEFAULT FALSE,
    require_images_public  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT single_row CHECK (id = 1)
);

INSERT INTO system_settings (id, require_images_admin, require_images_dealer, require_images_public)
VALUES (1, FALSE, FALSE, FALSE);
