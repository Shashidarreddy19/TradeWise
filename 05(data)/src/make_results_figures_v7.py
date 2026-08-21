"""
Final IEEE-conference-quality Results figures, built strictly from stored
rolling-origin outputs (outputs/rolling_origin_pooled_v5.csv and
outputs/rolling_origin_per_origin_v5.csv). No value in these figures is
estimated, interpolated, or fabricated.

Canonical model names (must match Table III / paper text exactly):
    XGBRanker (Pairwise Ranking)
    Two-Stage Hurdle
    Random Forest
    XGBoost Regressor (Ranking via Prediction)
    Gradient Boosting
    LightGBM
    Naive Historical Growth
    Ridge Regression
    MCDM-Style Heuristic

Metric-definition note (audited against rolling_origin_eval.py):
    top_k_overlap(actual, pred, k=3) returns
        |top3(actual) INTERSECT top3(pred)| / k          (k fixed at 3)
    This is an OVERLAP COEFFICIENT with a fixed denominator, not a Jaccard
    index (Jaccard divides by |union|, which is >= 3 and only equals 3 when
    the two sets are identical). The figures and captions therefore label
    this metric "Top-3 overlap", not "Top-3 Jaccard overlap", to avoid
    misrepresenting the underlying computation.

Outputs (written to the paper root directory):
    fig1_spearman_ranking_v7.png
    fig2_top1_top3_v7.png
    fig3_rolling_stability_v7.png
Figure 4 (actual-vs-predicted regression scatter) is NOT generated: raw
per-row predictions are not present in any stored output file, only
aggregated RMSE/MAE/R^2. Fabricating scatter points from those aggregates
would violate the no-fabrication requirement, so it is omitted.
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

# Canonical name mapping: raw model key in the CSVs -> paper-exact label.
NAME_MAP = {
    "XGBRanker (pairwise)": "XGBRanker (Pairwise Ranking)",
    "Two-Stage Hurdle": "Two-Stage Hurdle",
    "Random Forest": "Random Forest",
    "XGBoost Regressor": "XGBoost Regressor (Ranking via Prediction)",
    "Gradient Boosting": "Gradient Boosting",
    "LightGBM": "LightGBM",
    "Naive persistence": "Naive Historical Growth",
    "Ridge Regression": "Ridge Regression",
    "Heuristic weighted": "MCDM-Style Heuristic",
}
pooled["model"] = pooled["model"].map(NAME_MAP)
per_origin["model"] = per_origin["model"].map(NAME_MAP)

RK = "XGBRanker (Pairwise Ranking)"
NEUTRAL = "#5b6b79"      # slate grey-blue for all non-proposed models
HIGHLIGHT = "#b5352a"    # muted brick red for XGBRanker only
GRID = "0.85"

plt.rcParams.update({
    "font.family": "serif",
    "font.serif": ["Times New Roman", "DejaVu Serif"],
    "font.size": 8,
    "axes.linewidth": 0.7,
    "xtick.major.width": 0.7,
    "ytick.major.width": 0.7,
    "mathtext.fontset": "dejavuserif",
    "axes.edgecolor": "0.25",
    "text.color": "0.1",
    "axes.labelcolor": "0.1",
    "xtick.color": "0.15",
    "ytick.color": "0.15",
})

# ===========================================================================
# Figure 1: pooled Spearman rho, sorted descending, horizontal bars + std
# ===========================================================================
p1 = pooled.sort_values("rho_origin_mean", ascending=True).reset_index(drop=True)
colors = [HIGHLIGHT if m == RK else NEUTRAL for m in p1["model"]]

fig1, ax1 = plt.subplots(figsize=(3.45, 2.7))
y = np.arange(len(p1))
ax1.barh(y, p1["rho_origin_mean"], xerr=p1["rho_origin_std"],
         color=colors, height=0.60, capsize=2.0, edgecolor="none",
         error_kw={"linewidth": 0.9, "ecolor": "0.15", "capthick": 0.9})
ax1.set_yticks(y)
ax1.set_yticklabels(p1["model"], fontsize=6.4)
ax1.set_xlabel(r"Mean Spearman rank correlation ($\rho_s$)", fontsize=7.2)
ax1.set_xlim(0, 1.0)
ax1.axvline(0, color="0.25", linewidth=0.8)
ax1.grid(False)
for s in ("top", "right", "left"):
    ax1.spines[s].set_visible(False)
ax1.tick_params(axis="y", length=0)
for yi, (rho, std) in enumerate(zip(p1["rho_origin_mean"], p1["rho_origin_std"])):
    ax1.text(rho + std + 0.018, yi, f"{rho:.4f}", va="center", fontsize=6.0,
              color="0.1")
fig1.tight_layout(pad=0.35)
fig1.savefig(os.path.join(PAPER_DIR, "fig1_spearman_ranking_v7.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
fig1.savefig(os.path.join(PAPER_DIR, "fig1_spearman_ranking_v7.pdf"),
             bbox_inches="tight", facecolor="white")
print("wrote fig1_spearman_ranking_v7.{png,pdf}")

# ===========================================================================
# Figure 2: Top-1 hit rate vs Top-3 overlap, grouped horizontal bars
# ===========================================================================
p2 = pooled.sort_values("rho_origin_mean", ascending=True).reset_index(drop=True)
# Dedicated legend row below the plot (as in Figure 3) so the legend can
# never overlap a bar or its value label, regardless of bar length.
fig2 = plt.figure(figsize=(3.45, 3.35))
gs2 = fig2.add_gridspec(2, 1, height_ratios=[3.0, 0.42], hspace=0.55)
ax2 = fig2.add_subplot(gs2[0])
ypos = np.arange(len(p2))
h = 0.36
ax2.barh(ypos + h / 2, p2["top1_origin_mean"] * 100, height=h,
         label="Top-1 hit rate", color="#2f5d8a", edgecolor="none")
ax2.barh(ypos - h / 2, p2["top3_origin_mean"] * 100, height=h,
         label="Top-3 overlap", color="#c98a2d", edgecolor="none")
ax2.set_yticks(ypos)
ax2.set_yticklabels(p2["model"], fontsize=6.4)
ax2.set_xlabel("Pooled mean performance (%)", fontsize=7.2)
ax2.set_xlim(0, 108)
ax2.axvline(0, color="0.25", linewidth=0.8)
for s in ("top", "right", "left"):
    ax2.spines[s].set_visible(False)
ax2.tick_params(axis="y", length=0)
for yi, (t1, t3) in enumerate(zip(p2["top1_origin_mean"], p2["top3_origin_mean"])):
    ax2.text(t1 * 100 + 1.5, yi + h / 2, f"{t1*100:.1f}", va="center", fontsize=5.5)
    ax2.text(t3 * 100 + 1.5, yi - h / 2, f"{t3*100:.1f}", va="center", fontsize=5.5)

handles2, labels2 = ax2.get_legend_handles_labels()
ax2_leg = fig2.add_subplot(gs2[1])
ax2_leg.axis("off")
ax2_leg.legend(handles2, labels2, loc="center", ncol=2, fontsize=6.4,
               frameon=False, handlelength=1.6, columnspacing=1.4)
fig2.savefig(os.path.join(PAPER_DIR, "fig2_top1_top3_v7.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
fig2.savefig(os.path.join(PAPER_DIR, "fig2_top1_top3_v7.pdf"),
             bbox_inches="tight", facecolor="white")
print("wrote fig2_top1_top3_v7.{png,pdf}")

# ===========================================================================
# Figure 3: per-origin Spearman rho, all 9 models, actual origin-level values
# ===========================================================================
ORIGINS = sorted(per_origin["origin"].unique())
models_order = list(pooled.sort_values("rho_origin_mean", ascending=False)["model"])

# Restrained, distinct styles; solid line reserved for XGBRanker only.
STYLE = {
    "XGBRanker (Pairwise Ranking)":              dict(color=HIGHLIGHT, marker="o", ls="-",  lw=1.8, ms=4.4, zorder=6),
    "Two-Stage Hurdle":                          dict(color="#2f5d8a", marker="s", ls="--", lw=1.0, ms=3.2, zorder=4),
    "Random Forest":                             dict(color="#3f7f3f", marker="^", ls="--", lw=1.0, ms=3.2, zorder=4),
    "XGBoost Regressor (Ranking via Prediction)":dict(color="#7a4fa3", marker="D", ls="-.", lw=1.0, ms=3.0, zorder=4),
    "Gradient Boosting":                         dict(color="#c98a2d", marker="v", ls="--", lw=1.0, ms=3.2, zorder=3),
    "LightGBM":                                  dict(color="#3aa0a0", marker="P", ls=":",  lw=1.0, ms=3.4, zorder=3),
    "Naive Historical Growth":                   dict(color="0.2",     marker="*", ls="--", lw=1.3, ms=5.0, zorder=5),
    "Ridge Regression":                          dict(color="#c25b8e", marker="X", ls=":",  lw=1.0, ms=3.4, zorder=2),
    "MCDM-Style Heuristic":                      dict(color="0.55",    marker="h", ls=":",  lw=1.0, ms=3.4, zorder=2),
}

fig3 = plt.figure(figsize=(3.6, 4.5))
gs = fig3.add_gridspec(2, 1, height_ratios=[3.1, 1.0], hspace=0.55)
ax3 = fig3.add_subplot(gs[0])

for m in models_order:
    rows = per_origin[per_origin.model == m]
    vals = [float(rows[rows.origin == T]["spearman_rho"].iloc[0]) for T in ORIGINS]
    st = STYLE[m]
    ax3.plot(ORIGINS, vals, label=m, **st)

ax3.set_xlabel("Rolling-origin evaluation window\n"
               r"($T$: features observed in $T$ $\rightarrow$ exports realized in $T{+}1$)",
               fontsize=6.6, labelpad=4.0)
ax3.set_ylabel(r"Spearman rank correlation ($\rho_s$)", fontsize=7.2)
ax3.set_xticks(ORIGINS)
ax3.set_xticklabels([str(int(t)) for t in ORIGINS])
ax3.grid(axis="y", linestyle=":", linewidth=0.4, color=GRID)
ax3.set_axisbelow(True)
for s in ("top", "right"):
    ax3.spines[s].set_visible(False)

handles, labels = ax3.get_legend_handles_labels()
ax_leg = fig3.add_subplot(gs[1])
ax_leg.axis("off")
ax_leg.legend(handles, labels, loc="center", ncol=2, fontsize=5.6,
              frameon=False, handlelength=2.0, columnspacing=1.0,
              labelspacing=0.6)

fig3.savefig(os.path.join(PAPER_DIR, "fig3_rolling_stability_v7.png"),
             dpi=600, bbox_inches="tight", facecolor="white")
fig3.savefig(os.path.join(PAPER_DIR, "fig3_rolling_stability_v7.pdf"),
             bbox_inches="tight", facecolor="white")
print("wrote fig3_rolling_stability_v7.{png,pdf}")

print("\nFigure 4 (actual-vs-predicted regression scatter) NOT generated: "
      "no raw per-row prediction/target pairs exist in any stored output "
      "file; only aggregated RMSE/MAE/R^2 are available. Per the "
      "no-fabrication rule, this figure is omitted rather than approximated.")

# ---------------------------------------------------------------------------
# Console verification dump: every plotted number, traceable to source rows
# ---------------------------------------------------------------------------
print("\n--- Figure 1/2 source values (rolling_origin_pooled_v5.csv) ---")
print(pooled[["model", "rho_origin_mean", "rho_origin_std",
              "top3_origin_mean", "top1_origin_mean"]].to_string(index=False))

print("\n--- Figure 3 source values (rolling_origin_per_origin_v5.csv) ---")
piv = per_origin.pivot(index="model", columns="origin", values="spearman_rho")
print(piv.to_string())
