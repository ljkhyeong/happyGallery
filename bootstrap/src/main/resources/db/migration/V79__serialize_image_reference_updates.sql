CREATE TABLE image_media_reference_lock
(
    id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT chk_image_media_reference_lock_singleton CHECK (id = 1)
);

INSERT INTO image_media_reference_lock (id) VALUES (1);
