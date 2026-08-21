"""
Results figures for the IEEE paper, built directly from the stored
rolling-origin CSV/JSON outputs (outputs/rolling_origin_pooled_v5.csv and
outputs/rolling_origin_per_origin_v5.csv) so nothing in the figures can
disagree with Table III or the reported text.

Produces:
  fig1_spearman_ranking.png   - horizontal bar, pooled Spearman rho +- std,
                                 sorted descending, XGBRanker highlighted.
  fig2_top1_top3.png          - grouped bar, pooled Top-1 hit rate vs
                                 Top-3 overlap, all 9 models.
  fig3_rolling_stability.png  - line chart, per-origin Spearman rho for
                                 every model across the four rolling
                                 origins (raw per-origin values, not pooled
                                 averages).

A fourth figure (actual-vs-predicted scatter for the XGBoost regressor) was
requested but is NOT produced here: the stored artifacts
(rolling_origin_per_origin_v5.csv, rolling_origin_per_product_v5.csv)
contain only aggregated metrics (RMSE, MAE, R^2, rho, top-k), not the raw
per-row actual/predicted export values needed for a scatter plot. Building
that figure would require re-running rolling_origin_eval.py with an added
step that dumps df_labeled[TARGET] and predictions to disk. Per the
instruction not to fabricate figures from unavailable data, that figure is
omitted and flagged in make_results_figures_v6.py's docstring rather than
approximated.
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

SRC = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.dirname(SRC)
OUTPUTS = os.path.join(DATA, "outputs")
PAPER_DIR = os.path.dirname(DATA)

pooled = pd.read_csv(os.path.join(OUTPUTS, "rolling_origin_pooled_v5.csv"))
per_origin = pd.read_csv(os.path.join(OUTPUTS, "rolling_origin_per_origin_v5.csv"))

RK = "XGBRanker (pairwise)"

plt.rcParams.update({
    "font.family": "serif",
    "font.serif": ["Times New Roman", "DejaVu Serif"],
    "font.size": 8,
    "axes.linewidth": 0.6,
    "mathtext.fontset": "dejavuserif",
})

# ---------------------------------------------------------------------------
# Figure 1: pooled Spearman rho, sorted descending, with std error bars
# ---------------------------------------------------------------------------
p1 = pooled.sort_values("rho_origin_mean", ascending=True).reset_index(drop=True)
colors = ["#c0392b" if m == RK else "#4a6fa5" for m in p1["model"]]

fig1, ax1 = plt.subplots(figsize=(3.45, 3.0))
y = np.arange(len(p1))
ax1.barh(y, p1["rho_origin_mean"], xerr=p1["rho_origin_std"],
         color=colors, height=0.62, capsize=2.2,
         error_kw={"linewidth": 0.8, "ecolor": "0.25"})
ax1.set_yticks(y)
ax1.set_yticklabels(p1["model"], fontsize=6.8)
ax1.set_xlabel(r"Mean Spearman $\rho_s$ across 4 rolling origins", fontsize=7.4)
ax1.set_xlim(0, 1.0)
ax1.grid(axis="x", linestyle=":", linewidth=0.45, alpha=0.6)
ax1.set_axisbelow(True)
for s in ("top", "right"):
    ax1.spines[s].set_visible(False)
for yi, (rho, std) in enumerate(zip(p1["rho_origin_mean"], p1["rho_origin_std"])):
    ax1.text(rho + std + 0.015, yi, f"{rho:.4f}", va="center", fontsize=6.0)
fig1.tight_layout(pad=0.4)
fig1.savefig(os.path.join(PAPER_DIR, "fig1_spearman_ranking.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
print("wrote fig1_spearman_ranking.png")

# ---------------------------------------------------------------------------
# Figure 2: Top-1 hit rate vs Top-3 overlap, grouped bars
# ---------------------------------------------------------------------------
p2 = pooled.sort_values("rho_origin_mean", ascending=False).reset_index(drop=True)
fig2, ax2 = plt.subplots(figsize=(3.45, 2.6))
xpos = np.arange(len(p2))
w = 0.36
ax2.bar(xpos - w / 2, p2["top1_origin_mean"], width=w, label="Top-1 hit rate",
        color="#4a6fa5")
ax2.bar(xpos + w / 2, p2["top3_origin_mean"], width=w, label="Top-3 overlap",
        color="#e0a83a")
ax2.set_xticks(xpos)
ax2.set_xticklabels(p2["model"], rotation=42, ha="right", fontsize=5.8)
ax2.set_ylabel("Pooled mean value", fontsize=7.4)
ax2.set_ylim(0, 1.0)
ax2.grid(axis="y", linestyle=":", linewidth=0.45, alpha=0.6)
ax2.set_axisbelow(True)
for s in ("top", "right"):
    ax2.spines[s].set_visible(False)
ax2.legend(fontsize=6.2, frameon=False, loc="upper right")
fig2.tight_layout(pad=0.4)
fig2.savefig(os.path.join(PAPER_DIR, "fig2_top1_top3.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
print("wrote fig2_top1_top3.png")

# ---------------------------------------------------------------------------
# Figure 3: per-origin Spearman rho for every model (rolling stability)
# ---------------------------------------------------------------------------
ORIGINS = sorted(per_origin["origin"].unique())
models_order = list(pooled.sort_values("rho_origin_mean", ascending=False)["model"])

markers = ["o", "s", "^", "D", "v", "P", "*", "X", "h"]
# Taller figure with explicit height ratios: plot on top, legend panel below.
# This keeps the legend from ever overlapping the x-axis label or ticks,
# regardless of how many legend columns/rows are needed.
fig3 = plt.figure(figsize=(3.6, 4.3))
gs = fig3.add_gridspec(2, 1, height_ratios=[3.0, 1.0], hspace=0.55)
ax3 = fig3.add_subplot(gs[0])

for i, m in enumerate(models_order):
    vals = [float(per_origin[(per_origin.model == m) & (per_origin.origin == T)]
                  ["spearman_rho"].iloc[0]) for T in ORIGINS]
    is_rk = (m == RK)
    ax3.plot(ORIGINS, vals, marker=markers[i % len(markers)],
              linestyle="-" if is_rk else "--",
              linewidth=1.8 if is_rk else 1.0,
              markersize=4.2 if is_rk else 3.2,
              color="#c0392b" if is_rk else None,
              label=m, zorder=5 if is_rk else 3)
ax3.set_xlabel("Test origin " r"($T$: features in $T$ $\rightarrow$ exports in $T{+}1$)",
                fontsize=6.8, labelpad=4.0)
ax3.set_ylabel(r"Spearman $\rho_s$", fontsize=7.4)
ax3.set_xticks(ORIGINS)
ax3.set_xticklabels([str(int(t)) for t in ORIGINS])
ax3.grid(axis="both", linestyle=":", linewidth=0.45, alpha=0.6)
ax3.set_axisbelow(True)
for s in ("top", "right"):
    ax3.spines[s].set_visible(False)

# Dedicated legend axes below the plot -- guarantees no overlap with the
# x-axis label since it occupies its own row in the grid.
handles, labels = ax3.get_legend_handles_labels()
ax_leg = fig3.add_subplot(gs[1])
ax_leg.axis("off")
ax_leg.legend(handles, labels, loc="center", ncol=2, fontsize=6.0,
              frameon=False, handlelength=1.8, columnspacing=1.1,
              labelspacing=0.55)

fig3.savefig(os.path.join(PAPER_DIR, "fig3_rolling_stability.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
print("wrote fig3_rolling_stability.png")

print("\nFigure 4 (actual-vs-predicted regression scatter) NOT generated: "
      "raw per-row predictions are not present in the stored rolling-origin "
      "output files; only aggregated RMSE/MAE/R^2 are available.")
