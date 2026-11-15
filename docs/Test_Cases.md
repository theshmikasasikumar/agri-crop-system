# Testing Document

## 1. Testing techniques used

| Technique | Where it is applied |
|---|---|
| **Black-box: equivalence partitioning** | Valid / invalid classes for phone numbers, dates, numbers, choices (season, method, quality) |
| **Black-box: boundary value analysis** | Cultivated area vs farm area (equal / just above), zero values, cost = 0 |
| **Error guessing** | Duplicate phone, second harvest for the same cultivation, text where a number is expected |
| **White-box: statement and branch coverage** | `Calculator` (every formula and the zero-area branch), `InputHelper` (valid and invalid path of every read method) |
| **Database constraint testing** | PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK and ON DELETE CASCADE rules in `schema.sql` |
| **Unit testing (JUnit 5)** | `CalculatorTest`, `InputHelperTest` |
| **Integration testing (JUnit 5)** | `ServiceIntegrationTest` - services + DAOs + SQLite on a temporary database |
| **System testing** | Full menu run with `sample-output/sample_input.txt`; Python script end to end |

## 2. How the tests were run

* **Calculator, input validation, database constraints, report SQL, Python script** - executed; the "Actual result" column is the observed output.
* **Rows marked PENDING** need the Java application and the SQLite JDBC driver (downloaded by Maven). They are covered by the JUnit tests named in the last column and by the menu run in `sample-output/`. Run them on your machine, then change PENDING to PASS (or FAIL) and paste the actual result:

```
cd java-app
mvn test                      # JUnit results
mvn -q compile exec:java      # interactive menu run for the PENDING menu cases
```

## 3. Test cases

| ID | Technique | Description | Input | Expected result | Actual result | Status | Automated test |
|---|---|---|---|---|---|---|---|
| TC-01 | Black-box | Add a valid farmer | Name `Test Farmer`, phone `9000000001`, address `Demo Street` | Farmer saved, new id (9) displayed | - | PENDING | `ServiceIntegrationTest.validRecordsAreSavedAndGetNewIds` |
| TC-02 | Equivalence partition | Invalid phone formats | `12345`, `1234567890`, `98430abcde`, then `9843012345` | First three rejected with error, fourth accepted | Three rejected, `9843012345` returned | PASS | `InputHelperTest.readPhoneRequiresTenDigitsStartingWithSixToNine` |
| TC-03 | Error guessing | Duplicate contact number | Farmer with phone `9843012345` (already exists) | Rejected: "contact number already exists" | DB level: `UNIQUE constraint failed: Farmer.contact_number` (rejected). Service level not run | PASS (DB) / PENDING (service) | `ServiceIntegrationTest.duplicateContactNumberIsRejected` |
| TC-04 | Equivalence partition | Empty mandatory text | Blank lines, then `Hosur` | Blank rejected, `Hosur` accepted | Blank lines rejected, `Hosur` returned | PASS | `InputHelperTest.readNonEmptyRejectsBlankLines` |
| TC-05 | Black-box | Update farmer contact number | Farmer 2, new phone `9000000002` | Number updated | - | PENDING | menu option 11 |
| TC-06 | Black-box | Delete farmer cascades | Delete farmer 1 | Farmer, their farms and all farm records removed | DB level: farms of farmer 1 = 0; cultivations 27 -> 21 | PASS (DB) / PENDING (Java) | `ServiceIntegrationTest.deletingFarmerRemovesTheirFarms` |
| TC-07 | Black-box | Farm for unknown farmer | Farmer id 999 | Rejected | DB level: `FOREIGN KEY constraint failed` (rejected). Service level not run | PASS (DB) / PENDING (service) | menu option 2 |
| TC-08 | Equivalence partition | Invalid choice (category / season / method / quality) | `winter`, then `kharif` for season | `winter` rejected, `kharif` accepted as `Kharif` | `winter` rejected, `Kharif` returned. DB: invalid category and invalid method rejected by CHECK | PASS | `InputHelperTest.readChoiceIsCaseInsensitive` |
| TC-09 | Boundary value | Cultivated area equal to farm area | Farm 1 (12 acres), area 12 | Accepted | - | PENDING | menu option 4 |
| TC-10 | Boundary value | Cultivated area just above farm area | Farm 1 (12 acres), area 13 | Rejected: area exceeds farm area | - | PENDING | `ServiceIntegrationTest.cultivatedAreaCannotExceedFarmArea` |
| TC-11 | Equivalence partition | Invalid season stored | Season `Winter` | Rejected | DB level: `CHECK constraint failed: season IN ('Kharif','Rabi','Summer')` | PASS | schema constraint |
| TC-12 | Boundary value | Fertilizer applied before cultivation date | Cultivation 1 (2024-07-10), date 2024-07-01 | Rejected | - | PENDING | `ServiceIntegrationTest.inputBeforeCultivationDateIsRejected` |
| TC-13 | Equivalence partition | Invalid irrigation data | Method `Rain`; water 0 | Both rejected | DB: `CHECK constraint failed: irrigation_method IN (...)`; `CHECK constraint failed: water_quantity_l > 0` | PASS | schema constraint |
| TC-14 | Black-box | Valid harvest | Cultivation 26, 2027-02-01, 9000 kg, `A`, 27 | Accepted | DB level: ACCEPTED. Service level not run | PASS (DB) / PENDING (service) | `ServiceIntegrationTest.validRecordsAreSavedAndGetNewIds` |
| TC-15 | Error guessing | Second harvest for same cultivation | Cultivation 1 again | Rejected | DB level: `UNIQUE constraint failed: Harvest.cultivation_id` | PASS (DB) / PENDING (service) | `ServiceIntegrationTest.secondHarvestForSameCultivationIsRejected` |
| TC-16 | White-box | Yield per acre | 14408.6 kg, 8 acres | 1801.08 kg/acre | 1801.08 | PASS | `CalculatorTest.yieldPerAreaDividesQuantityByArea` |
| TC-17 | White-box | Revenue | 14408.6 kg x 19.5 INR | 280,967.70 | 280967.7 | PASS | `CalculatorTest.revenueIsQuantityTimesPrice` |
| TC-18 | White-box | Profit | Revenue 280,967.70, cost 12,744.69 | 268,223.01 | 268223.01 | PASS | `CalculatorTest.profitIsRevenueMinusCost` |
| TC-19 | Boundary value | Loss (negative profit) | Revenue 1000, cost 1500 | -500.00 | -500.0 | PASS | `CalculatorTest.profitCanBeNegative` |
| TC-20 | Boundary value | Zero area in yield formula | 100 kg, 0 acres | `IllegalArgumentException` (no divide by zero) | `IllegalArgumentException` thrown | PASS | `CalculatorTest.yieldPerAreaRejectsZeroArea` |
| TC-21 | Black-box | Crop-wise yield report | Menu 10 -> 1 | One row per harvested crop, highest yield first | SQL of the report returns 8 rows, first row Sugarcane 31,397.97 kg/acre | PASS (SQL) / PENDING (menu) | menu option 10 |
| TC-22 | Black-box | Farm-wise production | Menu 10 -> 2 | One row per farm with harvests | SQL returns 10 rows, first row farm 5 (Dharmapuri), 397,381.9 kg | PASS (SQL) / PENDING (menu) | menu option 10 |
| TC-23 | Black-box | Input cost report | Menu 10 -> 3 | Fertilizer quantity and cost per crop | SQL returns 8 rows, first row Maize 9,381.0 kg, INR 82,061.23 | PASS (SQL) / PENDING (menu) | menu option 10 |
| TC-24 | Black-box | Seasonal summary | Menu 10 -> 4 | One row each for Kharif, Rabi, Summer | SQL returns 3 rows, Kharif: 12 harvests, 488,362.8 kg | PASS (SQL) / PENDING (menu) | menu option 10 |
| TC-25 | Equivalence partition | Invalid menu choice | `abc`, `0`, `99`, then `5` (allowed range 1-10) | Invalid values rejected, valid accepted | `abc`, `0`, `99` rejected; `5` returned | PASS | `InputHelperTest.readIntSkipsTextAndOutOfRangeValues` |
| TC-26 | Equivalence partition | Invalid date format | `15-07-2025`, `2025-13-40`, then `2025-07-15` | First two rejected, third accepted | Same | PASS | `InputHelperTest.readDateRequiresIsoFormat` |
| TC-27 | Boundary value | Zero / negative / text numbers | `0`, `-3`, `xyz`, then `2.5` | First three rejected, `2.5` accepted | Same | PASS | `InputHelperTest.readPositiveDoubleRejectsZeroNegativeAndText` |
| TC-28 | Boundary value | Fertilizer cost = 0 and negative | Cost 0; cost -5 | 0 accepted, -5 rejected | DB: 0 ACCEPTED, -5 `CHECK constraint failed: input_cost >= 0` | PASS | schema constraint |
| TC-29 | Black-box | Productivity of a cultivation without harvest | Cultivation 26 | Message "harvest not recorded yet", no yield or profit | - | PENDING | `ServiceIntegrationTest.productivityOfCultivationWithoutHarvest` |
| TC-30 | System | Python script end to end | `python python-analysis/analysis.py` | Data loaded, cleaned, 8 charts saved, no error | 27 rows loaded, 25 harvested analysed, 2 ongoing excluded, 8 PNG files + 2 CSV files created | PASS | - |
| TC-31 | Error guessing | Python script without database | Run with no `agri.db` | Clear message, exit code 1, no traceback | "Database not found ... Run python database/setup_db.py first.", exit code 1 | PASS | - |
| TC-32 | White-box | Preprocessing rules | Database with 2 unharvested cultivations | Unharvested rows excluded from yield statistics; duplicates and invalid rows removed | `ongoing excluded 2`, `duplicate rows removed 0`, `invalid rows removed 0` | PASS | - |
| TC-33 | System | Database build script | `python database/setup_db.py` | 7 tables created, sample data loaded, no FK errors | Farmer 8, Farm 10, Crop 8, Cultivation 27, FertilizerInput 80, Irrigation 127, Harvest 25; `PRAGMA foreign_key_check` empty | PASS | - |
| TC-34 | System | Full menu run | `sample-output/sample_input.txt` | Records saved, calculations shown, reports printed, clean exit | - | PENDING | see README |

## 4. Summary

| | Count |
|---|---|
| Test cases | 34 |
| Executed and passed (PASS) | 18 |
| Partly executed: database / SQL part passed, Java service or menu part PENDING | 9 (TC-03, 06, 07, 14, 15, 21, 22, 23, 24) |
| Fully PENDING - need the Java application with the JDBC driver | 7 (TC-01, 05, 09, 10, 12, 29, 34) |
| Failed | 0 |

Update this table after running `mvn test` and the menu run.
