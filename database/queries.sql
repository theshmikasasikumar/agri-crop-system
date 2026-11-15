-- ============================================================
-- Reporting and analysis queries (SQLite)
-- ============================================================

-- Q1. Crop-wise yield (average yield per acre, harvested cultivations only)
SELECT crop_name,
       COUNT(*)                          AS harvests,
       ROUND(SUM(quantity_kg), 1)        AS total_kg,
       ROUND(SUM(quantity_kg) / SUM(cultivated_area), 2) AS yield_kg_per_acre
FROM v_cultivation_summary
WHERE quantity_kg IS NOT NULL
GROUP BY crop_name
ORDER BY yield_kg_per_acre DESC;

-- Q2. Farm-wise production
SELECT farm_id, location, farmer_name,
       COUNT(*)                   AS harvests,
       ROUND(SUM(quantity_kg), 1) AS total_production_kg,
       ROUND(SUM(revenue), 2)     AS total_revenue
FROM v_cultivation_summary
WHERE quantity_kg IS NOT NULL
GROUP BY farm_id, location, farmer_name
ORDER BY total_production_kg DESC;

-- Q3. Agricultural input cost by crop and fertilizer type
SELECT cr.crop_name,
       fi.fertilizer_type,
       ROUND(SUM(fi.quantity_kg), 1) AS total_quantity_kg,
       ROUND(SUM(fi.input_cost), 2)  AS total_cost
FROM FertilizerInput fi
JOIN Cultivation c ON c.cultivation_id = fi.cultivation_id
JOIN Crop cr       ON cr.crop_id       = c.crop_id
GROUP BY cr.crop_name, fi.fertilizer_type
ORDER BY cr.crop_name, total_cost DESC;

-- Q4. Seasonal harvest summary
SELECT season,
       COUNT(*)                   AS harvests,
       ROUND(SUM(quantity_kg), 1) AS total_harvest_kg,
       ROUND(SUM(revenue), 2)     AS revenue,
       ROUND(SUM(total_input_cost), 2) AS input_cost,
       ROUND(SUM(profit), 2)      AS profit
FROM v_cultivation_summary
WHERE quantity_kg IS NOT NULL
GROUP BY season
ORDER BY season;

-- Q5. Profit per cultivation (yield, cost, revenue, profit)
SELECT cultivation_id, farmer_name, location, crop_name, season,
       cultivated_area, yield_per_acre, total_input_cost, revenue, profit
FROM v_cultivation_summary
WHERE quantity_kg IS NOT NULL
ORDER BY profit DESC;

-- Q6. Top 3 most profitable crops (profit per acre)
SELECT crop_name,
       ROUND(SUM(profit) / SUM(cultivated_area), 2) AS profit_per_acre
FROM v_cultivation_summary
WHERE quantity_kg IS NOT NULL
GROUP BY crop_name
ORDER BY profit_per_acre DESC
LIMIT 3;

-- Q7. Total irrigation water and number of irrigations per crop
SELECT cr.crop_name,
       COUNT(ir.irrigation_id)            AS irrigations,
       ROUND(SUM(ir.water_quantity_l), 0) AS total_water_litres
FROM Irrigation ir
JOIN Cultivation c ON c.cultivation_id = ir.cultivation_id
JOIN Crop cr       ON cr.crop_id       = c.crop_id
GROUP BY cr.crop_name
ORDER BY total_water_litres DESC;

-- Q8. Harvest quality distribution by crop category
SELECT crop_category, quality, COUNT(*) AS harvests
FROM v_cultivation_summary
WHERE quality IS NOT NULL
GROUP BY crop_category, quality
ORDER BY crop_category, quality;

-- Q9. Cultivations still waiting for harvest
SELECT c.cultivation_id, cr.crop_name, c.cultivation_date, c.season
FROM Cultivation c
JOIN Crop cr ON cr.crop_id = c.crop_id
LEFT JOIN Harvest h ON h.cultivation_id = c.cultivation_id
WHERE h.harvest_id IS NULL;

-- Q10. Farmers with total cultivated area above 15 acres (HAVING example)
SELECT fr.name, ROUND(SUM(c.cultivated_area), 1) AS total_cultivated_acres
FROM Farmer fr
JOIN Farm f        ON f.farmer_id = fr.farmer_id
JOIN Cultivation c ON c.farm_id   = f.farm_id
GROUP BY fr.farmer_id, fr.name
HAVING SUM(c.cultivated_area) > 15
ORDER BY total_cultivated_acres DESC;
