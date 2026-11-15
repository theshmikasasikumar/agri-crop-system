"""
Agriculture Crop Management and Yield Analysis - data retrieval, preprocessing and visualisation.

Steps
  1. Retrieve  : read cultivation / harvest data from the SQLite database (view v_cultivation_summary)
  2. Preprocess: remove duplicates, fix types and dates, separate harvested from ongoing cultivations,
                 derive per-acre measures and per-crop "index" values (1.0 = that crop's average)
  3. Visualise : save charts to python-analysis/charts/ and the cleaned data to python-analysis/output/

Run from the repository root:
    python database/setup_db.py          (once, builds the database)
    python python-analysis/analysis.py
"""
import sqlite3
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")                      # draw to files, no window needed
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parent
DB_PATH = ROOT.parent / "database" / "agri.db"
CHART_DIR = ROOT / "charts"
OUT_DIR = ROOT / "output"

# ---- chart styling: light surface, thin marks, recessive grid, text in neutral ink ----
SURFACE, INK, INK2, GRID = "#fcfcfb", "#0b0b0b", "#52514e", "#e6e5e1"
BLUE, ORANGE = "#2a78d6", "#eb6834"
QUALITY_COLORS = {"C": "#bcd6f5", "B": "#6fa4e8", "A": "#2a78d6"}   # one hue, light -> dark = low -> high
plt.rcParams.update({
    "figure.facecolor": SURFACE, "axes.facecolor": SURFACE, "savefig.facecolor": SURFACE,
    "font.family": "DejaVu Sans", "font.size": 10, "text.color": INK,
    "axes.labelcolor": INK2, "xtick.color": INK2, "ytick.color": INK2,
    "axes.spines.top": False, "axes.spines.right": False, "axes.spines.left": False,
    "axes.edgecolor": GRID, "axes.grid": False,
})


# =============================================================== 1. RETRIEVE
def load_data():
    if not DB_PATH.exists():
        sys.exit(f"Database not found: {DB_PATH}\nRun  python database/setup_db.py  first.")
    with sqlite3.connect(DB_PATH) as conn:
        cultivation = pd.read_sql("SELECT * FROM v_cultivation_summary", conn)
        fertilizer = pd.read_sql(
            "SELECT cr.crop_name, fi.fertilizer_type, fi.quantity_kg, fi.input_cost "
            "FROM FertilizerInput fi "
            "JOIN Cultivation c ON c.cultivation_id = fi.cultivation_id "
            "JOIN Crop cr ON cr.crop_id = c.crop_id", conn)
    return cultivation, fertilizer


# ============================================================ 2. PREPROCESS
def preprocess(raw):
    log = {"rows loaded": len(raw)}

    df = raw.drop_duplicates(subset="cultivation_id").copy()
    log["duplicate rows removed"] = len(raw) - len(df)

    # clean text columns
    for col in ["farmer_name", "location", "crop_name", "crop_category", "season", "quality"]:
        df[col] = df[col].astype("string").str.strip()
    df["crop_name"] = df["crop_name"].str.title()

    # types and dates
    for col in ["cultivation_date", "harvest_date"]:
        df[col] = pd.to_datetime(df[col], errors="coerce")
    for col in ["cultivated_area", "total_input_cost", "total_water_l", "quantity_kg", "selling_price",
                "yield_per_acre", "revenue", "profit"]:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    # drop impossible records
    bad = (df["cultivated_area"] <= 0) | (df["quantity_kg"] <= 0) | (df["selling_price"] <= 0)
    log["invalid rows removed"] = int(bad.sum())
    df = df[~bad]

    # cultivations without a harvest are kept aside (they have no yield / revenue yet)
    ongoing = df[df["quantity_kg"].isna() | df["selling_price"].isna()]
    harvested = df.dropna(subset=["quantity_kg", "selling_price", "harvest_date"]).copy()
    log["ongoing (not harvested) excluded"] = len(ongoing)
    log["harvested rows analysed"] = len(harvested)

    # derived measures
    area = harvested["cultivated_area"]
    harvested["input_cost_per_acre"] = harvested["total_input_cost"] / area
    harvested["revenue_per_acre"] = harvested["revenue"] / area
    harvested["profit_per_acre"] = harvested["profit"] / area
    harvested["water_kl_per_acre"] = harvested["total_water_l"] / 1000 / area
    harvested["days_to_harvest"] = (harvested["harvest_date"] - harvested["cultivation_date"]).dt.days

    # crops differ hugely in scale (sugarcane vs cotton), so compare each crop with its own average
    for measure, name in [("yield_per_acre", "yield_index"), ("input_cost_per_acre", "cost_index"),
                          ("water_kl_per_acre", "water_index")]:
        harvested[name] = harvested[measure] / harvested.groupby("crop_name")[measure].transform("mean")

    # outlier check (IQR rule) on the yield index - reported, not removed
    q1, q3 = harvested["yield_index"].quantile([0.25, 0.75])
    iqr = q3 - q1
    outliers = harvested[(harvested["yield_index"] < q1 - 1.5 * iqr) | (harvested["yield_index"] > q3 + 1.5 * iqr)]
    log["yield outliers flagged (kept)"] = len(outliers)

    harvested["season"] = pd.Categorical(harvested["season"], ["Kharif", "Rabi", "Summer"], ordered=True)
    return harvested.reset_index(drop=True), log


# ============================================================= 3. VISUALISE
def style_axes(ax, title, subtitle):
    ax.set_title(title, loc="left", fontsize=13, fontweight="bold", color=INK, pad=26)
    ax.text(0, 1.03, subtitle, transform=ax.transAxes, fontsize=9, color=INK2, va="bottom")
    ax.tick_params(length=0)


def save(fig, name):
    fig.tight_layout()
    fig.savefig(CHART_DIR / name, dpi=150)
    plt.close(fig)
    print(f"  saved charts/{name}")


def hbar(series, title, subtitle, name, fmt, xlabel, color=BLUE):
    series = series.sort_values()
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.barh(series.index, series.values, height=0.6, color=color)
    pad = series.max() * 0.01
    for y, v in enumerate(series.values):
        ax.text(v + pad, y, fmt(v), va="center", fontsize=9, color=INK)
    ax.set_xlim(0, series.max() * 1.15)
    ax.xaxis.grid(True, color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_xlabel(xlabel)
    style_axes(ax, title, subtitle)
    save(fig, name)


def scatter(df, xcol, xlabel, name, title, subtitle):
    x, y = df[xcol], df["yield_index"]
    fig, ax = plt.subplots(figsize=(8, 5.5))
    ax.axhline(1, color=GRID, linewidth=1)
    ax.axvline(1, color=GRID, linewidth=1)
    ax.scatter(x, y, s=70, color=BLUE, edgecolor=SURFACE, linewidth=1.5, zorder=3)
    slope, intercept = np.polyfit(x, y, 1)
    xs = np.linspace(x.min(), x.max(), 50)
    ax.plot(xs, slope * xs + intercept, color=INK2, linewidth=1.5, linestyle="--", zorder=2)
    r = np.corrcoef(x, y)[0, 1]
    ax.text(0.98, 0.03, f"correlation r = {r:.2f}   (n = {len(df)} harvests)", transform=ax.transAxes,
            fontsize=9, color=INK2, va="bottom", ha="right")
    ax.set_xlabel(xlabel)
    ax.set_ylabel("Yield index (1.0 = crop's own average)")
    ax.yaxis.grid(True, color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    style_axes(ax, title, subtitle)
    save(fig, name)
    return r


def make_charts(h, fert):
    CHART_DIR.mkdir(exist_ok=True)

    # 1. yield per acre by crop (crop-wise yields)
    by_crop = h.groupby("crop_name").agg(qty=("quantity_kg", "sum"), area=("cultivated_area", "sum"))
    yield_t = (by_crop["qty"] / by_crop["area"]) / 1000
    hbar(yield_t, "Yield per acre by crop", "Tonnes harvested per cultivated acre (all harvests of the crop)",
         "1_yield_per_acre_by_crop.png", lambda v: f"{v:,.2f} t", "Tonnes per acre")

    # 2. input cost per acre by crop (cost comparison)
    cost = h.groupby("crop_name").apply(lambda g: g["total_input_cost"].sum() / g["cultivated_area"].sum(),
                                        include_groups=False)
    hbar(cost, "Fertilizer input cost per acre by crop", "INR spent on fertilizer per cultivated acre",
         "2_input_cost_per_acre_by_crop.png", lambda v: f"{v:,.0f}", "INR per acre", color=ORANGE)

    # 3. profit per acre by crop (productivity)
    profit = h.groupby("crop_name").apply(lambda g: g["profit"].sum() / g["cultivated_area"].sum(),
                                          include_groups=False)
    hbar(profit, "Estimated profit per acre by crop",
         "Revenue minus fertilizer input cost, INR per cultivated acre",
         "3_profit_per_acre_by_crop.png", lambda v: f"{v:,.0f}", "INR per acre")

    # 4. seasonal harvest summary
    season = h.groupby("season", observed=True).agg(harvests=("profit", "size"), profit=("profit", "sum"))
    fig, ax = plt.subplots(figsize=(8, 5))
    labels = [f"{s}\n({int(n)} harvests)" for s, n in zip(season.index, season["harvests"])]
    ax.bar(labels, season["profit"] / 1e5, width=0.5, color=BLUE)
    for i, v in enumerate(season["profit"] / 1e5):
        ax.text(i, v + season["profit"].max() / 1e5 * 0.015, f"{v:,.1f} lakh", ha="center", fontsize=9, color=INK)
    ax.set_ylim(0, season["profit"].max() / 1e5 * 1.15)
    ax.yaxis.grid(True, color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    ax.set_ylabel("Estimated profit (INR lakh)")
    style_axes(ax, "Seasonal harvest summary", "Total estimated profit of harvests by season (1 lakh = 100,000 INR)")
    save(fig, "4_seasonal_profit.png")

    # 5. farm-wise production value
    farm = h.groupby(["farm_id", "location"]).agg(profit=("profit", "sum")).reset_index()
    farm.index = [f"Farm {i} - {loc}" for i, loc in zip(farm["farm_id"], farm["location"])]
    hbar(farm["profit"] / 1e5, "Farm-wise estimated profit", "Total estimated profit per farm, INR lakh",
         "5_farm_wise_profit.png", lambda v: f"{v:,.1f}", "INR lakh")

    # 6 + 7. what is associated with higher yield?
    r_cost = scatter(h, "cost_index", "Fertilizer cost per acre (1.0 = crop's own average)",
                     "6_input_cost_vs_yield.png", "Do higher fertilizer inputs go with higher yield?",
                     "Each dot is one harvest; both axes are relative to the same crop's average")
    r_water = scatter(h, "water_index", "Water per acre (1.0 = crop's own average)",
                      "7_water_vs_yield.png", "Does more irrigation water go with higher yield?",
                      "Each dot is one harvest; both axes are relative to the same crop's average")

    # 8. harvest quality mix by crop category
    mix = h.groupby(["crop_category", "quality"]).size().unstack(fill_value=0).reindex(columns=["C", "B", "A"],
                                                                                         fill_value=0)
    mix = mix.loc[mix.sum(axis=1).sort_values().index]
    fig, ax = plt.subplots(figsize=(9, 5))
    left = np.zeros(len(mix))
    for q in ["C", "B", "A"]:
        ax.barh(mix.index, mix[q], left=left, height=0.6, color=QUALITY_COLORS[q],
                edgecolor=SURFACE, linewidth=2, label=f"Quality {q}")
        for y, (l, v) in enumerate(zip(left, mix[q])):
            if v > 0:
                ax.text(l + v / 2, y, str(v), ha="center", va="center", fontsize=9,
                        color=INK if q != "A" else "#ffffff")
        left += mix[q].values
    ax.legend(frameon=False, ncol=3, loc="lower right")
    ax.set_xlabel("Number of harvests")
    ax.xaxis.grid(True, color=GRID, linewidth=0.8)
    ax.set_axisbelow(True)
    style_axes(ax, "Harvest quality by crop category", "Number of harvests graded A (best), B or C")
    save(fig, "8_quality_by_category.png")

    return {"corr_cost_vs_yield": r_cost, "corr_water_vs_yield": r_water}


# ==================================================================== MAIN
def main():
    print("1. Retrieving data from", DB_PATH)
    raw, fert = load_data()
    print(f"   {len(raw)} cultivation rows, {len(fert)} fertilizer rows")

    print("2. Preprocessing")
    harvested, log = preprocess(raw)
    for key, value in log.items():
        print(f"   {key:<34} {value}")
    OUT_DIR.mkdir(exist_ok=True)
    harvested.to_csv(OUT_DIR / "cleaned_harvest_data.csv", index=False)

    print("\n   Crop-wise summary")
    summary = harvested.groupby("crop_name").agg(
        harvests=("cultivation_id", "count"),
        area_acres=("cultivated_area", "sum"),
        yield_kg_per_acre=("yield_per_acre", "mean"),
        input_cost_per_acre=("input_cost_per_acre", "mean"),
        profit_per_acre=("profit_per_acre", "mean"),
    ).round(2)
    print(summary.to_string())
    summary.to_csv(OUT_DIR / "crop_summary.csv")

    print("\n3. Creating charts")
    stats = make_charts(harvested, fert)

    print("\nKey findings")
    best = summary["profit_per_acre"].idxmax()
    print(f"   - Most profitable crop per acre : {best} (INR {summary.loc[best, 'profit_per_acre']:,.0f})")
    print(f"   - Highest yield per acre        : {summary['yield_kg_per_acre'].idxmax()}")
    print(f"   - Correlation fertilizer cost index vs yield index : {stats['corr_cost_vs_yield']:.2f}")
    print(f"   - Correlation water index vs yield index           : {stats['corr_water_vs_yield']:.2f}")
    print("\nDone. Charts are in python-analysis/charts/")


if __name__ == "__main__":
    main()
