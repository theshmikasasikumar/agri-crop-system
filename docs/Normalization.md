# Relational Model and Normalization

## 1. Relational model

Primary keys are underlined in the usual notation; here they are marked **PK**, foreign keys **FK**.

```
Farmer      (farmer_id PK, name, contact_number UNIQUE, address)
Farm        (farm_id PK, farmer_id FK -> Farmer, location, total_area)
Crop        (crop_id PK, crop_name UNIQUE, crop_category, duration_days)
Cultivation (cultivation_id PK, farm_id FK -> Farm, crop_id FK -> Crop,
             cultivation_date, cultivated_area, season)
FertilizerInput (input_id PK, cultivation_id FK -> Cultivation, fertilizer_type,
                 quantity_kg, input_cost, application_date)
Irrigation  (irrigation_id PK, cultivation_id FK -> Cultivation, irrigation_date,
             water_quantity_l, irrigation_method)
Harvest     (harvest_id PK, cultivation_id FK UNIQUE -> Cultivation, harvest_date,
             quantity_kg, quality, selling_price)
```

| Relationship | Cardinality | Implemented by |
|---|---|---|
| Farmer - Farm | 1 : many | `Farm.farmer_id` |
| Farm - Cultivation | 1 : many | `Cultivation.farm_id` |
| Crop - Cultivation | 1 : many | `Cultivation.crop_id` |
| Cultivation - FertilizerInput | 1 : many | `FertilizerInput.cultivation_id` |
| Cultivation - Irrigation | 1 : many | `Irrigation.cultivation_id` |
| Cultivation - Harvest | 1 : 0..1 | `Harvest.cultivation_id UNIQUE` |

Integrity rules: all foreign keys are enforced (`PRAGMA foreign_keys = ON`); deleting a farmer cascades to their farms and everything recorded on them; CHECK constraints enforce positive values and allowed codes (season, method, quality, category); `contact_number` must be 10 characters and unique.

The ER diagram is in `diagrams/er_diagram.png` (source `er_diagram.dot`).

---

## 2. Normalization

### 2.1 Starting point: one flat record (unnormalized, UNF)

If all data were kept in a single sheet, one row would look like this:

```
FARM_RECORD (farmer_id, farmer_name, contact_number, farmer_address,
             farm_id, farm_location, farm_total_area,
             crop_id, crop_name, crop_category, crop_duration,
             cultivation_id, cultivation_date, cultivated_area, season,
             {fertilizer_type, quantity_kg, input_cost, application_date},   <- repeating group
             {irrigation_date, water_quantity, method},                      <- repeating group
             harvest_date, quantity_harvested, quality, selling_price)
```

### 2.2 First Normal Form (1NF) - remove repeating groups
Each cell must hold one atomic value, and there must be no repeating groups. A cultivation has many fertilizer applications and many irrigations, so these are moved to their own tables:

- `FertilizerInput(input_id, cultivation_id, fertilizer_type, quantity_kg, input_cost, application_date)`
- `Irrigation(irrigation_id, cultivation_id, irrigation_date, water_quantity_l, irrigation_method)`

Every table now has a primary key and atomic columns.

### 2.3 Second Normal Form (2NF) - remove partial dependencies
2NF removes attributes that depend on only part of a composite key. In the flat design the key of a row is (cultivation_id, input_id / irrigation_id). Attributes of farm, farmer and crop (location, name, category...) depend only on `farm_id`, `farmer_id` or `crop_id`, not on the whole key. They are moved out:

| Functional dependency | New table |
|---|---|
| farmer_id -> name, contact_number, address | Farmer |
| farm_id -> farmer_id, location, total_area | Farm |
| crop_id -> crop_name, crop_category, duration_days | Crop |
| cultivation_id -> farm_id, crop_id, cultivation_date, cultivated_area, season | Cultivation |

All of our tables use a **single-column surrogate primary key**, so partial dependencies cannot occur: every table is in 2NF.

### 2.4 Third Normal Form (3NF) - remove transitive dependencies
3NF removes non-key attributes that depend on other non-key attributes. Example in the flat design:

```
cultivation_id -> farm_id -> farm_location        (farm_location is transitive)
cultivation_id -> farm_id -> farmer_id -> farmer_name
cultivation_id -> crop_id -> crop_name
```
Moving `location`, `farmer_name`, `crop_name` etc. into Farm, Farmer and Crop removes these dependencies.

Check of the final tables (every non-key attribute depends on the key, the whole key, and nothing but the key):

| Table | Key | Functional dependencies | Normal form |
|---|---|---|---|
| Farmer | farmer_id | farmer_id -> name, contact_number, address | 3NF |
| Farm | farm_id | farm_id -> farmer_id, location, total_area | 3NF |
| Crop | crop_id | crop_id -> crop_name, crop_category, duration_days | 3NF |
| Cultivation | cultivation_id | cultivation_id -> farm_id, crop_id, cultivation_date, cultivated_area, season | 3NF |
| FertilizerInput | input_id | input_id -> cultivation_id, fertilizer_type, quantity_kg, input_cost, application_date | 3NF |
| Irrigation | irrigation_id | irrigation_id -> cultivation_id, irrigation_date, water_quantity_l, irrigation_method | 3NF |
| Harvest | harvest_id (and cultivation_id, candidate key) | harvest_id -> cultivation_id, harvest_date, quantity_kg, quality, selling_price | 3NF |

### 2.5 BCNF
In every table the determinant of each functional dependency is a candidate key (`farmer_id`, `contact_number` is also a candidate key of Farmer, `crop_name` of Crop, `cultivation_id` of Harvest), so the schema is also in **Boyce-Codd Normal Form**.

### 2.6 Derived values are not stored
Yield per acre, total input cost, revenue and profit are calculated (by Java `Calculator` and by the view `v_cultivation_summary`) rather than stored. Storing them would create update anomalies, because they depend on other columns.

### 2.7 Anomalies prevented

| Anomaly | In the flat sheet | In the normalized design |
|---|---|---|
| Insertion | A new crop cannot be stored until it is cultivated | `Crop` row can exist alone |
| Update | Changing a farmer's phone number means editing every row | One row in `Farmer` |
| Deletion | Deleting the last cultivation of a farm loses the farm's details | Farm survives (only the cultivation rows are removed) |
