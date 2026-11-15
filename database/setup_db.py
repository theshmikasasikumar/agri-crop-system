"""Creates database/agri.db from schema.sql and sample_data.sql (no SQLite install needed).

Usage (from the repository root):
    python database/setup_db.py
"""
import sqlite3
from pathlib import Path

HERE = Path(__file__).resolve().parent
DB = HERE / "agri.db"

if DB.exists():
    DB.unlink()                      # start fresh every time

conn = sqlite3.connect(DB)
conn.executescript((HERE / "schema.sql").read_text(encoding="utf-8"))
conn.executescript((HERE / "sample_data.sql").read_text(encoding="utf-8"))
conn.commit()

for table in ("Farmer", "Farm", "Crop", "Cultivation", "FertilizerInput", "Irrigation", "Harvest"):
    count = conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
    print(f"{table:<16} {count:>4} rows")
conn.close()
print(f"\nDatabase ready: {DB}")
