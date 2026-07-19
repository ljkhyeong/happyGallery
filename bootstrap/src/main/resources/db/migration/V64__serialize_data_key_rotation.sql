CREATE TABLE data_key_rotation_lock
(
    id TINYINT NOT NULL PRIMARY KEY,
    CONSTRAINT chk_data_key_rotation_lock_singleton CHECK (id = 1)
);

INSERT INTO data_key_rotation_lock (id) VALUES (1);
