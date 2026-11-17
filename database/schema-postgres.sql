
-- Agriculture Crop Management and Yield Analysis System
-- PostgreSQL schema for Neon

CREATE TABLE IF NOT EXISTS Farmer (
    farmer_id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    contact_number TEXT NOT NULL UNIQUE
        CHECK (length(contact_number) = 10),
    address TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS Farm (
    farm_id SERIAL PRIMARY KEY,
    farmer_id INTEGER NOT NULL,
    location TEXT NOT NULL,
    total_area DOUBLE PRECISION NOT NULL CHECK (total_area > 0),
    FOREIGN KEY (farmer_id) REFERENCES Farmer(farmer_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Crop (
    crop_id SERIAL PRIMARY KEY,
    crop_name TEXT NOT NULL UNIQUE,
    crop_category TEXT NOT NULL
        CHECK (crop_category IN (
            'Cereal', 'Pulse', 'Oilseed',
            'Cash Crop', 'Vegetable', 'Millet'
        )),
    duration_days INTEGER NOT NULL CHECK (duration_days > 0)
);

CREATE TABLE IF NOT EXISTS Cultivation (
    cultivation_id SERIAL PRIMARY KEY,
    farm_id INTEGER NOT NULL,
    crop_id INTEGER NOT NULL,
    cultivation_date TEXT NOT NULL,
    cultivated_area DOUBLE PRECISION NOT NULL
        CHECK (cultivated_area > 0),
    season TEXT NOT NULL
        CHECK (season IN ('Kharif', 'Rabi', 'Summer')),
    FOREIGN KEY (farm_id) REFERENCES Farm(farm_id)
        ON DELETE CASCADE,
    FOREIGN KEY (crop_id) REFERENCES Crop(crop_id)
);

CREATE TABLE IF NOT EXISTS FertilizerInput (
    input_id SERIAL PRIMARY KEY,
    cultivation_id INTEGER NOT NULL,
    fertilizer_type TEXT NOT NULL,
    quantity_kg DOUBLE PRECISION NOT NULL
        CHECK (quantity_kg > 0),
    input_cost DOUBLE PRECISION NOT NULL
        CHECK (input_cost >= 0),
    application_date TEXT NOT NULL,
    FOREIGN KEY (cultivation_id)
        REFERENCES Cultivation(cultivation_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Irrigation (
    irrigation_id SERIAL PRIMARY KEY,
    cultivation_id INTEGER NOT NULL,
    irrigation_date TEXT NOT NULL,
    water_quantity_l DOUBLE PRECISION NOT NULL
        CHECK (water_quantity_l > 0),
    irrigation_method TEXT NOT NULL
        CHECK (irrigation_method IN (
            'Drip', 'Sprinkler', 'Flood', 'Furrow'
        )),
    FOREIGN KEY (cultivation_id)
        REFERENCES Cultivation(cultivation_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Harvest (
    harvest_id SERIAL PRIMARY KEY,
    cultivation_id INTEGER NOT NULL UNIQUE,
    harvest_date TEXT NOT NULL,
    quantity_kg DOUBLE PRECISION NOT NULL
        CHECK (quantity_kg > 0),
    quality TEXT NOT NULL CHECK (quality IN ('A', 'B', 'C')),
    selling_price DOUBLE PRECISION NOT NULL
        CHECK (selling_price > 0),
    FOREIGN KEY (cultivation_id)
        REFERENCES Cultivation(cultivation_id)
        ON DELETE CASCADE
);

-- Indexes

CREATE INDEX IF NOT EXISTS idx_farm_farmer
    ON Farm(farmer_id);

CREATE INDEX IF NOT EXISTS idx_cult_farm
    ON Cultivation(farm_id);

CREATE INDEX IF NOT EXISTS idx_cult_crop
    ON Cultivation(crop_id);

CREATE INDEX IF NOT EXISTS idx_input_cult
    ON FertilizerInput(cultivation_id);

CREATE INDEX IF NOT EXISTS idx_irrigation_cult
    ON Irrigation(cultivation_id);

-- Cultivation summary view

CREATE OR REPLACE VIEW v_cultivation_summary AS
SELECT
    t.*,
    ROUND((t.revenue - t.total_input_cost)::numeric, 2) AS profit
FROM (
    SELECT
        c.cultivation_id,
        fr.farmer_id,
        fr.name AS farmer_name,
        c.farm_id,
        f.location,
        cr.crop_id,
        cr.crop_name,
        cr.crop_category,
        c.season,
        c.cultivation_date,
        c.cultivated_area,

        COALESCE((
            SELECT SUM(fi.input_cost)
            FROM FertilizerInput fi
            WHERE fi.cultivation_id = c.cultivation_id
        ), 0) AS total_input_cost,

        COALESCE((
            SELECT SUM(ir.water_quantity_l)
            FROM Irrigation ir
            WHERE ir.cultivation_id = c.cultivation_id
        ), 0) AS total_water_l,

        h.harvest_date,
        h.quantity_kg,
        h.quality,
        h.selling_price,

        ROUND(
            (h.quantity_kg / c.cultivated_area)::numeric, 2
        ) AS yield_per_acre,

        ROUND(
            (h.quantity_kg * h.selling_price)::numeric, 2
        ) AS revenue

    FROM Cultivation c
    JOIN Farm f ON f.farm_id = c.farm_id
    JOIN Farmer fr ON fr.farmer_id = f.farmer_id
    JOIN Crop cr ON cr.crop_id = c.crop_id
    LEFT JOIN Harvest h ON h.cultivation_id = c.cultivation_id
) AS t;
