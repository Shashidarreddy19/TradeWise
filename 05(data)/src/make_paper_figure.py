"""
Build the results figure for the IEEE paper directly from the rolling-origin
result files, so the figure can never disagree with the tables.

(a) Spearman rho by test origin for the competitive cluster, with naive
    persistence drawn as a heavy dashed line: every model tracks the baseline.
(b) Delta rho vs persistence by origin. This is the paper's central result:
    the ranker's advantage is nearly flat across origins while the regression
    models swing through zero.
(c) Top-1 hit rate by origin: the ranker sits at the persistence level while
    random forest is consistently best, at every origin.
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

SRC = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.dirname(SRC)
OUTPUTS = os.path.join(DATA, "outputs")
PAPER_DIR = os.path.dirname(DATA)
FIG_PATH = os.path.join(PAPER_DIR, "results_overview.png")

po = pd.read_csv(os.path.join(OUTPUTS, "rolling_origin_per_origin_v5.csv"))
st = pd.read_csv(os.path.join(OUTPUTS, "rolling_origin_delta_stability_v5.csv"))

RK, NV, RF = "XGBRanker (pairwise)", "Naive persistence", "Random Forest"
ORIGINS = sorted(po["origin"].unique())

# series drawn in panels (a) and (b): the competitive cluster only.
# Ridge and the heuristic sit far below and would flatten the scale; they are
# reported in the tables and named in the caption.
SERIES = [
    (RK,                  "o", "-",  "0.05", 1.9, 4.2),
    (RF,                  "s", "--", "0.40", 1.1, 3.4),
    ("Two-Stage Hurdle",  "^", "--", "0.52", 1.1, 3.4),
    ("XGBoost Regressor", "D", "--", "0.62", 1.1, 3.0),
    ("LightGBM",          "v", ":",  "0.70", 1.0, 3.0),
    ("Gradient Boosting", "P", ":",  "0.76", 1.0, 3.0),
]

plt.rcParams.update({
    "font.family": "serif",
    "font.serif": ["Times New Roman", "DejaVu Serif"],
    "font.size": 7,
    "axes.linewidth": 0.6,
    "xtick.major.width": 0.6,
    "ytick.major.width": 0.6,
    "mathtext.fontset": "dejavuserif",
})

fig, (ax1, ax2, ax3) = plt.subplots(
    3, 1, figsize=(3.45, 5.95), sharex=True,
    gridspec_kw={"height_ratios": [1.0, 1.0, 0.86]})


def get(model, col):
    return [float(po[(po.model == model) & (po.origin == T)][col].iloc[0]) for T in ORIGINS]


def finish(ax, ylab, title):
    ax.set_ylabel(ylab, fontsize=6.8)
    ax.grid(axis="both", linestyle=":", linewidth=0.45, alpha=0.6)
    ax.set_axisbelow(True)
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    ax.tick_params(labelsize=6.3)
    ax.set_title(title, fontsize=7.0, pad=3.5)
    ax.set_xlim(ORIGINS[0] - 0.28, ORIGINS[-1] + 0.28)


# ---------------- (a) rho by origin ----------------
for m, mk, ls, c, lw, ms in SERIES:
    ax1.plot(ORIGINS, get(m, "spearman_rho"), marker=mk, linestyle=ls, color=c,
             linewidth=lw, markersize=ms, label=m,
             zorder=5 if m == RK else 3)
ax1.plot(ORIGINS, get(NV, "spearman_rho"), marker="*", linestyle="--",
         color="black", linewidth=1.5, markersize=6.5, label=NV, zorder=4)
finish(ax1, r"Spearman $\rho$", "(a) Ranking quality by test origin")
# headroom at the bottom so the legend never sits on the data
ax1.set_ylim(0.796, 0.900)
ax1.legend(fontsize=5.4, ncol=2, frameon=False, loc="lower right",
           handlelength=2.0, columnspacing=1.0, labelspacing=0.25,
           borderaxespad=0.15)

# ---------------- (b) delta rho vs persistence ----------------
ax2.axhline(0.0, color="black", linewidth=1.1, zorder=2)
for m, mk, ls, c, lw, ms in SERIES:
    d = [r - n for r, n in zip(get(m, "spearman_rho"), get(NV, "spearman_rho"))]
    ax2.plot(ORIGINS, d, marker=mk, linestyle=ls, color=c, linewidth=lw,
             markersize=ms, zorder=5 if m == RK else 3)
rkd = st[st.model == RK].iloc[0]
rfd = st[st.model == RF].iloc[0]
ax2.text(ORIGINS[0] - 0.16, 0.0575,
         f"ranker  $\\Delta\\rho={rkd.delta_mean:.4f}\\pm{rkd.delta_std:.4f}$\n"
         f"forest   $\\Delta\\rho={rfd.delta_mean:.4f}\\pm{rfd.delta_std:.4f}$",
         fontsize=5.8, va="top", ha="left", linespacing=1.3)
ax2.text(ORIGINS[-1] + 0.20, 0.0008, "persistence", fontsize=5.6, style="italic",
         va="bottom", ha="right")
finish(ax2, r"$\Delta\rho$ vs. persistence",
       "(b) The ranker's gain is stable; the others swing")
# extra headroom above the highest point (~0.040) for the annotation block
ax2.set_ylim(-0.017, 0.060)

# ---------------- (c) top-1 by origin ----------------
for m, mk, ls, c, lw, ms in [(RK, "o", "-", "0.05", 1.9, 4.2),
                             (RF, "s", "--", "0.40", 1.3, 3.6)]:
    ax3.plot(ORIGINS, get(m, "top1_hit_rate"), marker=mk, linestyle=ls, color=c,
             linewidth=lw, markersize=ms, label=m, zorder=5)
ax3.plot(ORIGINS, get(NV, "top1_hit_rate"), marker="*", linestyle="--",
         color="black", linewidth=1.5, markersize=6.5, label=NV, zorder=4)
finish(ax3, "Top-1 hit rate", "(c) On Top-1 the ranker only matches persistence")
ax3.legend(fontsize=5.6, frameon=False, loc="lower left", handlelength=2.0,
           labelspacing=0.25, borderaxespad=0.15)
ax3.set_xlabel("test origin  (features in year $T$ $\\rightarrow$ exports in $T{+}1$)",
               fontsize=6.6, labelpad=2.0)
ax3.set_xticks(ORIGINS)
ax3.set_xticklabels([str(int(t)) for t in ORIGINS])

fig.tight_layout(pad=0.4, h_pad=0.85)
fig.savefig(FIG_PATH, dpi=600, bbox_inches="tight", facecolor="white")

print(f"wrote {FIG_PATH}")
print(f"  origins: {[int(o) for o in ORIGINS]}")
print(f"  ranker delta {rkd.delta_mean:.4f} +/- {rkd.delta_std:.4f} "
      f"(min {rkd.delta_min:.4f}, {int(rkd.n_origins_positive)}/4 positive)")
print(f"  forest delta {rfd.delta_mean:.4f} +/- {rfd.delta_std:.4f} "
      f"(min {rfd.delta_min:.4f}, {int(rfd.n_origins_positive)}/4 positive)")
print(f"  stability ratio: ranker {rkd.stability_ratio:.2f} vs forest {rfd.stability_ratio:.2f}")
