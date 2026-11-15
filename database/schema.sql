-- ============================================================
-- Agriculture Crop Management and Yield Analysis System
-- Database schema (SQLite)
-- ============================================================
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS Farmer (
    farmer_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT    NOT NULL,
    contact_number TEXT    NOT NULL UNIQUE CHECK (length(contact_number) = 10),
    address        TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS Farm (
    farm_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    farmer_id  INTEGER NOT NULL,
    location   TEXT    NOT NULL,
    total_area REAL    NOT NULL CHECK (total_area > 0),          -- acres
    FOREIGN KEY (farmer_id) REFERENCES Farmer(farmer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Crop (
    crop_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    crop_name     TEXT    NOT NULL UNIQUE,
    crop_category TEXT    NOT NULL
                  CHECK (crop_category IN ('Cereal','Pulse','Oilseed','Cash Crop','Vegetable','Millet')),
    duration_days INTEGER NOT NULL CHECK (duration_days > 0)
);

CREATE TABLE IF NOT EXISTS Cultivation (
    cultivation_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    farm_id          INTEGER NOT NULL,
    crop_id          INTEGER NOT NULL,
    cultivation_date TEXT    NOT NULL,                            -- YYYY-MM-DD
    cultivated_area  REAL    NOT NULL CHECK (cultivated_area > 0), -- acres
    season           TEXT    NOT NULL CHECK (season IN ('Kharif','Rabi','Summer')),
    FOREIGN KEY (farm_id) REFERENCES Farm(farm_id) ON DELETE CASCADE,
    FOREIGN KEY (crop_id) REFERENCES Crop(crop_id)
);

CREATE TABLE IF NOT EXISTS FertilizerInput (
    input_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    cultivation_id   INTEGER NOT NULL,
    fertilizer_type  TEXT    NOT NULL,
    quantity_kg      REAL    NOT NULL CHECK (quantity_kg > 0),
    input_cost       REAL    NOT NULL CHECK (input_cost >= 0),    -- INR
    application_date TEXT    NOT NULL,
    FOREIGN KEY (cultivation_id) REFERENCES Cultivation(cultivation_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Irrigation (
    irrigation_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    cultivation_id    INTEGER NOT NULL,
    irrigation_date   TEXT    NOT NULL,
    water_quantity_l  REAL    NOT NULL CHECK (water_quantity_l > 0), -- litres
    irrigation_method TEXT    NOT NULL
                      CHECK (irrigation_method IN ('Drip','Sprinkler','Flood','Furrow')),
    FOREIGN KEY (cultivation_id) REFERENCES Cultivation(cultivation_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Harvest (
    harvest_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    cultivation_id  INTEGER NOT NULL UNIQUE,                      -- one harvest per cultivation
    harvest_date    TEXT    NOT NULL,
    quantity_kg     REAL    NOT NULL CHECK (quantity_kg > 0),
    quality         TEXT    NOT NULL CHECK (quality IN ('A','B','C')),
    selling_price   REAL    NOT NULL CHECK (selling_price > 0),   -- INR per kg
    FOREIGN KEY (cultivation_id) REFERENCES Cultivation(cultivation_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_farm_farmer        ON Farm(farmer_id);
CREATE INDEX IF NOT EXISTS idx_cult_farm          ON Cultivation(farm_id);
CREATE INDEX IF NOT EXISTS idx_cult_crop          ON Cultivation(crop_id);
CREATE INDEX IF NOT EXISTS idx_input_cult         ON FertilizerInput(cultivation_id);
CREATE INDEX IF NOT EXISTS idx_irrigation_cult    ON Irrigation(cultivation_id);

-- ------------------------------------------------------------
-- View: one row per cultivation with all derived measures.
-- yield_per_acre = quantity harvested / cultivated area
-- revenue        = quantity harvested * selling price
-- profit         = revenue - total fertilizer input cost
-- Cultivations that are not harvested yet have NULL harvest fields.
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_cultivation_summary AS
SELECT t.*,
       ROUND(t.revenue - t.total_input_cost, 2) AS profit
FROM (
    SELECT c.cultivation_id,
           fr.farmer_id,
           fr.name              AS farmer_name,
           c.farm_id,
           f.location,
           cr.crop_id,
           cr.crop_name,
           cr.crop_category,
           c.season,
           c.cultivation_date,
           c.cultivated_area,
           COALESCE((SELECT SUM(fi.input_cost)       FROM FertilizerInput fi WHERE fi.cultivation_id = c.cultivation_id), 0) AS total_input_cost,
           COALESCE((SELECT SUM(ir.water_quantity_l) FROM Irrigation ir      WHERE ir.cultivation_id = c.cultivation_id), 0) AS total_water_l,
           h.harvest_date,
           h.quantity_kg,
           h.quality,
           h.selling_price,
           ROUND(h.quantity_kg / c.cultivated_area, 2)  AS yield_per_acre,
           ROUND(h.quantity_kg * h.selling_price, 2)    AS revenue
    FROM Cultivation c
    JOIN Farm   f  ON f.farm_id    = c.farm_id
    JOIN Farmer fr ON fr.farmer_id = f.farmer_id
    JOIN Crop   cr ON cr.crop_id   = c.crop_id
    LEFT JOIN Harvest h ON h.cultivation_id = c.cultivation_id
) AS t;
