import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch
import shutil

def create_focused_class_diagram():
    # Dimensions optimized for Half-A4 Project Report (Landscape, 18.5 x 11.5 inches at 300 DPI)
    fig, ax = plt.subplots(figsize=(18.5, 11.5), dpi=300)
    ax.set_xlim(0, 185)
    ax.set_ylim(0, 115)
    ax.axis('off')

    # Background color
    fig.patch.set_facecolor('#FFFFFF')
    ax.set_facecolor('#FFFFFF')

    # Color Tokens
    C_BLUE_HDR = '#1D4ED8'     # Domain Entity Blue
    C_PURPLE_HDR = '#7E22CE'   # Regulatory Purple
    C_TEAL_HDR = '#0F766E'     # Service / Engine Teal
    C_ORANGE_HDR = '#C2410C'   # Controller Orange
    C_BOX_BG = '#F8FAFC'       # Box content background
    C_BOX_BORDER = '#334155'   # Crisp dark border

    # Header Title
    title_box = FancyBboxPatch((5, 104), 175, 8.5, boxstyle="round,pad=0.3,rounding_size=0.8",
                               facecolor='#0F172A', edgecolor='#2563EB', linewidth=1.8, zorder=2)
    ax.add_patch(title_box)
    ax.text(92.5, 109.5, "TradeWise — Core System Class Diagram",
            fontsize=15, fontweight='bold', color='#FFFFFF', ha='center', va='center', zorder=3)
    ax.text(92.5, 106.2, "Essential Domain Entities, Regulatory Engine, AI/ML Services & Calculation Pipeline",
            fontsize=9.5, color='#94A3B8', ha='center', va='center', zorder=3)

    # Helper: Draw Class Box with large readable text
    def draw_class(x, y, w, h, stereotype, name, attrs, methods, hdr_color):
        # Card body
        card = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.1,rounding_size=0.6",
                              facecolor=C_BOX_BG, edgecolor=C_BOX_BORDER, linewidth=1.6, zorder=3)
        ax.add_patch(card)
        
        # Header banner
        hdr_h = 3.6 if stereotype else 2.8
        hdr = FancyBboxPatch((x, y + h - hdr_h), w, hdr_h, boxstyle="round,pad=0.0,rounding_size=0.6",
                             facecolor=hdr_color, edgecolor=C_BOX_BORDER, linewidth=1.4, zorder=4)
        ax.add_patch(hdr)
        
        # Header texts (Large font)
        if stereotype:
            ax.text(x + w/2, y + h - 1.1, f"«{stereotype}»", fontsize=7.5, color='#E2E8F0', ha='center', va='center', style='italic', zorder=5)
            ax.text(x + w/2, y + h - 2.5, name, fontsize=10.5, fontweight='bold', color='#FFFFFF', ha='center', va='center', zorder=5)
        else:
            ax.text(x + w/2, y + h - 1.4, name, fontsize=11.0, fontweight='bold', color='#FFFFFF', ha='center', va='center', zorder=5)

        # Attributes (Large monospace)
        curr_y = y + h - hdr_h - 1.4
        for attr in attrs:
            ax.text(x + 1.2, curr_y, attr, fontsize=8.2, color='#0F172A', fontfamily='monospace', fontweight='medium', va='center', zorder=5)
            curr_y -= 1.35

        # Divider
        if methods:
            ax.plot([x, x + w], [curr_y + 0.4, curr_y + 0.4], color='#94A3B8', linewidth=1.0, zorder=4)
            curr_y -= 1.1
            for m in methods:
                ax.text(x + 1.2, curr_y, m, fontsize=8.2, color='#1E3A8A', fontfamily='monospace', fontweight='semibold', va='center', zorder=5)
                curr_y -= 1.35

    # Helper: Draw Associations
    def draw_arrow(x1, y1, x2, y2, label='', card1='', card2='', style='-', color='#1E293B', lw=1.6):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->', color=color, lw=lw, ls=style), zorder=6)
        if label:
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            ax.text(mx, my, label, fontsize=8.0, fontweight='bold', color=color,
                    ha='center', va='center', bbox=dict(boxstyle='round,pad=0.2', facecolor='white', edgecolor='#CBD5E1', lw=0.8), zorder=7)
        if card1:
            ax.text(x1 + (0.8 if x2 >= x1 else -1.2), y1 + 0.8, card1, fontsize=8.2, fontweight='bold', color='#0F172A', zorder=7)
        if card2:
            ax.text(x2 + (-1.2 if x2 >= x1 else 0.8), y2 + 0.8, card2, fontsize=8.2, fontweight='bold', color='#0F172A', zorder=7)

    # ═══════════════════════════════════════════════════════════════════
    # ROW 1: CORE DOMAIN ENTITIES (TOP)
    # ═══════════════════════════════════════════════════════════════════
    # 1. User
    draw_class(5, 73, 38, 26, 'Entity', 'User',
               ['+ Long id', '+ String email', '+ String password', '+ String fullName', '+ Role role [EXPORTER|LOGISTICS]', '+ String iecNumber, gstNumber'],
               ['+ getAuthorities()', '+ isVerifiedExporter()'], C_BLUE_HDR)

    # 2. Product
    draw_class(50, 73, 38, 26, 'Entity', 'Product',
               ['+ Long id', '+ User exporter', '+ String name', '+ String hsCode (6-10 digits)', '+ ProductCategory category', '+ BigDecimal unitPrice, unitCost', '+ BigDecimal unitWeightKg'],
               ['+ calculateMargin()', '+ getTariffChapter()'], C_BLUE_HDR)

    # 3. Order
    draw_class(95, 73, 40, 26, 'Entity', 'Order',
               ['+ Long id', '+ User exporter', '+ Product product', '+ Country destinationCountry', '+ Integer quantity', '+ BigDecimal totalInvoiceValue', '+ OrderStatus status'],
               ['+ assignPartner(User partner)', '+ updateStatus(OrderStatus)'], C_BLUE_HDR)

    # 4. Shipment
    draw_class(142, 73, 38, 26, 'Entity', 'Shipment',
               ['+ Long id', '+ Order order', '+ User logisticsPartner', '+ ShipmentStatus status (7 states)', '+ String trackingNumber', '+ String originPort, destPort', '+ LocalDate pickupDate, etaDate'],
               ['+ updateMilestone(status)', '+ calculateTransitTime()'], C_BLUE_HDR)

    # ═══════════════════════════════════════════════════════════════════
    # ROW 2: REGULATORY KNOWLEDGE & ML INTELLIGENCE (MIDDLE)
    # ═══════════════════════════════════════════════════════════════════
    # 5. HsMasterEntity
    draw_class(5, 39, 40, 26, 'Entity (TradeData)', 'HsMasterEntity',
               ['+ Long id', '+ String country (India/Destination)', '+ String nationalCode (e.g. 10063090)', '+ String hs6, hs4, hs2', '+ String officialDescription', '+ Boolean isCurrent'],
               ['+ isFoodCommodity()'], C_PURPLE_HDR)

    # 6. RegulationMasterEntity
    draw_class(50, 39, 40, 26, 'Entity (TradeData)', 'RegulationMasterEntity',
               ['+ Long id', '+ String destinationCountry', '+ String authority (SFDA/ZATCA/DGFT)', '+ String regulationType', '+ String title, legalSummary', '+ LocalDateTime effectiveDate'],
               ['+ isMandatoryForImport()'], C_PURPLE_HDR)

    # 7. RegulatoryRetrievalService
    draw_class(95, 39, 42, 26, 'Core Service', 'RegulatoryRetrievalService',
               ['+ HsMasterRepository hsRepo', '+ RegulationMappingRepo mapRepo', '+ RegulatoryKnowledgeService kbService'],
               ['+ getRegulations(country, hsCode)', '+ calculateCompliance(RegResult)', '+ verifyOriginDestinationRules()'], C_TEAL_HDR)

    # 8. MlExportRankingService
    draw_class(142, 39, 38, 26, 'AI/ML Service', 'MlExportRankingService',
               ['+ XGBRankerModel winning_v4_model', '+ PredictionMatrix 550_corridors', '+ NvidiaAiService ragAssistant'],
               ['+ predictBlock(hsCode, destination)', '+ rankOpportunity(hsCode, countries)', '+ getReliabilityTier()'], C_TEAL_HDR)

    # ═══════════════════════════════════════════════════════════════════
    # ROW 3: CALCULATION ENGINE & REST API CONTROLLER (BOTTOM)
    # ═══════════════════════════════════════════════════════════════════
    # 9. CostEngine (Client Calculation Engine)
    draw_class(5, 6, 83, 25, 'Calculation Engine', 'costEngine.js  (calculateTradeEconomics)',
               ['+ INCOTERMS: EXW, FOB, CIF, DAP, DDP  (10 terms supported)',
                '+ FX Normalization: SAR, USD, EUR, GBP, AED -> INR calculation base',
                '+ Segregation: Product Cost | Seller CIF Cost | Invoice Value | Customs Value | Landed Cost | Profit',
                '+ Duty & VAT: Verified ZATCA 0% MFN + 15% Standard Import VAT base'],
               ['+ calculateTradeEconomics(transaction, fxRates, dutyData, taxData) : TradeEconomicsResult',
                '+ runScenarioEngine() | runSensitivityMatrix() | runQuantityEconomics()'], C_TEAL_HDR)

    # 10. ExportAnalysisController (REST Gateway)
    draw_class(97, 6, 83, 25, 'REST Controller', 'ExportAnalysisController  (/api/v1/export)',
               ['+ RegulatoryRetrievalService regulatoryService',
                '+ RegulatoryKnowledgeService knowledgeService',
                '+ MlExportRankingService mlRankingService',
                '+ CostEstimationService costEstimationService'],
               ['+ POST /api/v1/export/analyze (product, origin, destination, hsCode) : ExportAnalysisResponse',
                '+ GET  /api/v1/regulations/{country}/{hsCode} : RegulatoryProfileResponse',
                '+ POST /api/v1/intelligence/rank-market-opportunity : CountryRankingsResponse'], C_ORANGE_HDR)

    # ═══════════════════════════════════════════════════════════════════
    # ARROWS & RELATIONSHIPS
    # ═══════════════════════════════════════════════════════════════════
    # User -> Product
    draw_arrow(43, 86, 50, 86, label='exports', card1='1', card2='0..*')

    # Product -> Order
    draw_arrow(88, 86, 95, 86, label='ordered in', card1='1', card2='0..*')

    # Order -> Shipment
    draw_arrow(135, 86, 142, 86, label='tracked by', card1='1', card2='0..1')

    # Product -> HsMasterEntity
    draw_arrow(69, 73, 25, 65, label='classified by hsCode', style='--', color='#7E22CE')

    # HsMasterEntity -> RegulationMasterEntity
    draw_arrow(45, 52, 50, 52, label='maps to', card1='1..*', card2='1..*', color='#7E22CE')

    # RegulationMasterEntity -> RegulatoryRetrievalService
    draw_arrow(90, 52, 95, 52, label='queried by', color='#0F766E')

    # RegulatoryRetrievalService -> MlExportRankingService
    draw_arrow(137, 52, 142, 52, label='feeds features', color='#0F766E')

    # Services -> ExportAnalysisController
    draw_arrow(116, 39, 116, 31, label='delegates', style='--', color='#C2410C')
    draw_arrow(161, 39, 145, 31, label='ML scores', style='--', color='#C2410C')

    # CostEngine -> ExportAnalysisController
    draw_arrow(88, 18, 97, 18, label='', style='--', color='#0F766E')
    ax.text(92.5, 20.2, "integrates\neconomics", fontsize=7.2, fontweight='bold', color='#0F766E', ha='center', va='center',
            bbox=dict(boxstyle='round,pad=0.2', facecolor='white', edgecolor='#CBD5E1', lw=0.8), zorder=7)

    # Save outputs
    output_png = r'd:\MAJOR PROJECT\TradeWise_Class_Diagram.png'
    output_svg = r'd:\MAJOR PROJECT\TradeWise_Class_Diagram.svg'
    artifact_png = r'C:\Users\DELL\.gemini\antigravity-ide\brain\fad105f1-54ee-4c47-a006-82c36481779e\TradeWise_Class_Diagram.png'

    plt.tight_layout()
    plt.savefig(output_png, format='png', bbox_inches='tight', dpi=300, facecolor='#FFFFFF')
    plt.savefig(output_svg, format='svg', bbox_inches='tight', facecolor='#FFFFFF')
    shutil.copyfile(output_png, artifact_png)
    print("SUCCESS: Focused Class Diagram saved to:", output_png)

if __name__ == '__main__':
    create_focused_class_diagram()
