CREATE TABLE public_holiday_snapshot (
    holiday_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    synced_at DATETIME(6) NOT NULL,
    PRIMARY KEY (holiday_date)
);
