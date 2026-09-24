import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, BoxStyle
import shutil
import os

def create_class_diagram():
    # Set up large canvas with high resolution (width: 38 inches, height: 26 inches)
    fig, ax = plt.subplots(figsize=(38, 26), dpi=300)
    ax.set_xlim(0, 380)
    ax.set_ylim(0, 260)
    ax.axis('off')

    # Color Palette Definitions
    BG_MAIN = '#F8FAFC'
    fig.patch.set_facecolor(BG_MAIN)
    ax.set_facecolor(BG_MAIN)

    # Package container colors (fill, border, title_bg, text)
    PKG_DOMAIN = {'fill': '#EFF6FF', 'border': '#3B82F6', 'header': '#1E40AF', 'title': '1. DOMAIN ENTITIES (InternationalTrade DB)'}
    PKG_REGULATORY = {'fill': '#FAF5FF', 'border': '#A855F7', 'header': '#6B21A8', 'title': '2. REGULATORY & TARIFF ENTITIES (TradeData DB)'}
    PKG_SERVICES = {'fill': '#F0FDFA', 'border': '#14B8A6', 'header': '#0F766E', 'title': '3. BACKEND SERVICE LAYER (Spring Boot Services & ML)'}
    PKG_CONTROLLERS = {'fill': '#FFF7ED', 'border': '#F97316', 'header': '#C2410C', 'title': '4. REST API CONTROLLERS'}
    PKG_DTOS = {'fill': '#FEFCE8', 'border': '#EAB308', 'header': '#A16207', 'title': '5. DATA TRANSFER OBJECTS (DTOs)'}
    PKG_FRONTEND = {'fill': '#F0FDF4', 'border': '#22C55E', 'header': '#15803D', 'title': '6. FRONTEND ARCHITECTURE & CLIENT SERVICES (React)'}

    # Helper: Draw Package Boundary
    def draw_package(x, y, w, h, config):
        # Outer background box
        rect = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.5,rounding_size=1.2",
                              facecolor=config['fill'], edgecolor=config['border'], linewidth=2.0, linestyle='--', alpha=0.9, zorder=1)
        ax.add_patch(rect)
        # Header title pill
        hdr_rect = FancyBboxPatch((x + 1.5, y + h - 4.5), min(w - 3, len(config['title']) * 1.6 + 6), 3.8,
                                  boxstyle="round,pad=0.2,rounding_size=0.6",
                                  facecolor=config['header'], edgecolor='none', zorder=2)
        ax.add_patch(hdr_rect)
        ax.text(x + 3.0, y + h - 2.6, config['title'], fontsize=11, fontweight='bold', color='white', va='center', zorder=3)

    # Helper: Draw UML Class Box
    def draw_class(x, y, w, h, stereotype, class_name, attributes, methods, header_color='#1E293B'):
        # Main box
        box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.2,rounding_size=0.6",
                             facecolor='#FFFFFF', edgecolor='#64748B', linewidth=1.4, zorder=4)
        ax.add_patch(box)
        
        # Header banner
        hdr_h = 4.2 if stereotype else 3.2
        hdr_box = FancyBboxPatch((x, y + h - hdr_h), w, hdr_h, boxstyle="round,pad=0.0,rounding_size=0.6",
                                facecolor=header_color, edgecolor='#64748B', linewidth=1.2, zorder=5)
        ax.add_patch(hdr_box)
        
        # Header text
        if stereotype:
            ax.text(x + w/2, y + h - 1.4, f"«{stereotype}»", fontsize=8.0, color='#E2E8F0', ha='center', va='center', zorder=6, style='italic')
            ax.text(x + w/2, y + h - 3.0, class_name, fontsize=10.0, fontweight='bold', color='#FFFFFF', ha='center', va='center', zorder=6)
        else:
            ax.text(x + w/2, y + h - 1.8, class_name, fontsize=10.5, fontweight='bold', color='#FFFFFF', ha='center', va='center', zorder=6)

        # Attribute text
        curr_y = y + h - hdr_h - 1.5
        for attr in attributes:
            ax.text(x + 1.2, curr_y, attr, fontsize=7.6, color='#1E293B', fontfamily='monospace', va='center', zorder=6)
            curr_y -= 1.45

        # Divider line between attributes and methods
        if methods:
            ax.plot([x, x + w], [curr_y + 0.5, curr_y + 0.5], color='#CBD5E1', linewidth=1.0, zorder=5)
            curr_y -= 1.2
            for m in methods:
                ax.text(x + 1.2, curr_y, m, fontsize=7.6, color='#0F172A', fontfamily='monospace', va='center', zorder=6)
                curr_y -= 1.45

    # Helper: Draw Relationships / Arrows
    def draw_assoc(x1, y1, x2, y2, label='', card1='', card2='', style='-', color='#475569', is_directed=True):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle='->' if is_directed else '-', color=color, lw=1.5, ls=style), zorder=7)
        if label:
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            ax.text(mx, my + 0.8, label, fontsize=7.5, fontweight='bold', color=color,
                    ha='center', va='center', bbox=dict(boxstyle='round,pad=0.2', facecolor='white', edgecolor='none', alpha=0.85), zorder=8)
        if card1:
            ax.text(x1 + (0.8 if x2 >= x1 else -0.8), y1 + 1.0, card1, fontsize=7.5, fontweight='bold', color='#0F172A', zorder=8)
        if card2:
            ax.text(x2 + (-0.8 if x2 >= x1 else 0.8), y2 + 1.0, card2, fontsize=7.5, fontweight='bold', color='#0F172A', zorder=8)

    # ═══════════════════════════════════════════════════════════════════
    # TOP HEADER BANNER
    # ═══════════════════════════════════════════════════════════════════
    title_rect = FancyBboxPatch((8, 246), 364, 10.5, boxstyle="round,pad=0.4,rounding_size=1.0",
                                facecolor='#0F172A', edgecolor='#38BDF8', linewidth=2.0, zorder=3)
    ax.add_patch(title_rect)
    ax.text(190, 252.8, "TradeWise — AI-Driven Cross-Border Export Intelligence Platform",
            fontsize=18, fontweight='heavy', color='#FFFFFF', ha='center', va='center', zorder=4)
    ax.text(190, 248.5, "Complete Unified System Class Diagram  |  Domain Entities, Regulatory Schema, Spring Boot Services, REST APIs, DTOs & React Client",
            fontsize=10.5, color='#94A3B8', ha='center', va='center', zorder=4)

    # ═══════════════════════════════════════════════════════════════════
    # 1. DOMAIN ENTITIES (TOP LEFT: x=8, y=125, w=118, h=116)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(8, 125, 118, 116, PKG_DOMAIN)

    # User
    draw_class(12, 202, 28, 29, 'Entity', 'User',
               ['+ Long id', '+ String email', '+ String password', '+ String fullName', '+ String phone', '+ Role role', '+ Boolean active', '+ LocalDateTime createdAt'],
               ['+ getAuthorities()', '+ isAccountNonExpired()'], header_color='#1E40AF')

    # Role Enum
    draw_class(44, 212, 20, 19, 'Enumeration', 'Role',
               ['EXPORTER', 'LOGISTICS', 'LOGISTICS_PARTNER', 'IMPORTER', 'ADMIN'], [], header_color='#334155')

    # ExporterProfile
    draw_class(12, 168, 28, 26, 'Entity', 'ExporterProfile',
               ['+ Long id', '+ User user', '+ String companyName', '+ String iecNumber', '+ String gstNumber', '+ String city, state', '+ String primaryCategory'],
               ['+ getVerificationStatus()'], header_color='#1E40AF')

    # LogisticsProfile
    draw_class(44, 168, 28, 26, 'Entity', 'LogisticsProfile',
               ['+ Long id', '+ User user', '+ String companyName', '+ String fleetType', '+ Integer fleetSize', '+ String serviceRegions', '+ String licenseNumber'],
               ['+ getVerificationStatus()'], header_color='#1E40AF')

    # Product
    draw_class(80, 202, 38, 29, 'Entity', 'Product',
               ['+ Long id', '+ User exporter', '+ String name', '+ String description', '+ String hsCode', '+ ProductCategory category', '+ BigDecimal price', '+ BigDecimal unitCost', '+ BigDecimal weight'],
               ['+ getFormattedHsCode()', '+ calculateMargin()'], header_color='#1E40AF')

    # ProductCategory
    draw_class(80, 172, 38, 18, 'Entity', 'ProductCategory',
               ['+ Long id', '+ String categoryName', '+ String description', '+ String chapterCode'],
               ['+ getChapterPrefix()'], header_color='#1E40AF')

    # Order
    draw_class(12, 130, 38, 30, 'Entity', 'Order',
               ['+ Long id', '+ User exporter', '+ Product product', '+ Country destinationCountry', '+ Integer quantity', '+ BigDecimal totalValue', '+ String pickupLocation', '+ OrderStatus status'],
               ['+ calculateTotal()', '+ transitionStatus()'], header_color='#1E40AF')

    # OrderStatus Enum
    draw_class(54, 130, 22, 20, 'Enumeration', 'OrderStatus',
               ['PENDING', 'ACCEPTED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'REJECTED'], [], header_color='#334155')

    # Shipment
    draw_class(80, 130, 42, 34, 'Entity', 'Shipment',
               ['+ Long id', '+ Order order', '+ User logisticsPartner', '+ ShipmentStatus status', '+ String trackingNumber', '+ String carrierName', '+ String originPort, destPort', '+ LocalDateTime pickupDate', '+ LocalDateTime deliveryDate'],
               ['+ updateMilestone()', '+ calculateETA()'], header_color='#1E40AF')

    # ═══════════════════════════════════════════════════════════════════
    # 2. REGULATORY ENTITIES (TOP RIGHT: x=132, y=125, w=118, h=116)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(132, 125, 118, 116, PKG_REGULATORY)

    # HsMasterEntity
    draw_class(136, 196, 36, 35, 'Entity (TradeData)', 'HsMasterEntity',
               ['+ Long id', '+ String country', '+ String nationalCode', '+ String hs6, hs4, hs2', '+ String officialDescription', '+ String category', '+ Boolean isCurrent', '+ LocalDateTime updatedAt'],
               ['+ getFormattedCode()', '+ isExemptCommodity()'], header_color='#6B21A8')

    # RegulationMasterEntity
    draw_class(176, 196, 36, 35, 'Entity (TradeData)', 'RegulationMasterEntity',
               ['+ Long id', '+ String country', '+ String authority', '+ String title', '+ String regulationType', '+ String summary', '+ String legalReference', '+ LocalDateTime effectiveDate'],
               ['+ isMandatory()', '+ getAuthorityUrl()'], header_color='#6B21A8')

    # RegulationHsMappingEntity
    draw_class(216, 196, 30, 35, 'Entity (TradeData)', 'RegulationHsMappingEntity',
               ['+ Long id', '+ Long regulationId', '+ String country', '+ String hsCode', '+ String hs6, hs4, hs2', '+ String matchLevel', '+ Double confidenceScore'],
               ['+ isExactMatch()', '+ getHierarchyDepth()'], header_color='#6B21A8')

    # HsRegulatoryCoverageEntity
    draw_class(136, 132, 36, 30, 'Entity (TradeData)', 'HsRegulatoryCoverageEntity',
               ['+ Long id', '+ String country', '+ String hsCode', '+ Boolean regulationFound', '+ Long regulationId', '+ String mappingMethod', '+ Double confidenceScore'],
               ['+ isAudited()', '+ getAuditSummary()'], header_color='#6B21A8')

    # RegulatorySourceEntity
    draw_class(176, 132, 36, 30, 'Entity (TradeData)', 'RegulatorySourceEntity',
               ['+ Long id', '+ String country', '+ String sourceName', '+ String sourceUrl', '+ String authority', '+ String status', '+ LocalDateTime lastCrawled'],
               ['+ isValid()', '+ getParsedHost()'], header_color='#6B21A8')

    # Country Entity
    draw_class(216, 132, 30, 30, 'Entity', 'Country',
               ['+ Long id', '+ String countryCode', '+ String countryName', '+ String region', '+ String currencyCode', '+ Double defaultGstRate', '+ Double riskScore'],
               ['+ isGccMember()', '+ getIso2()'], header_color='#1E40AF')

    # ═══════════════════════════════════════════════════════════════════
    # 5. DTOs & PAYLOADS (FAR RIGHT: x=256, y=125, w=116, h=116)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(256, 125, 116, 116, PKG_DTOS)

    draw_class(260, 202, 34, 28, 'DTO', 'LoginRequest',
               ['+ String email', '+ String password'], ['+ validate()'], header_color='#A16207')

    draw_class(298, 202, 34, 28, 'DTO', 'RegisterRequest',
               ['+ String email', '+ String password', '+ String fullName', '+ String phone', '+ Role role', '+ String companyName', '+ String iecNumber'], ['+ validate()'], header_color='#A16207')

    draw_class(336, 202, 32, 28, 'DTO', 'AuthResponse',
               ['+ String token', '+ String tokenType', '+ Long userId', '+ String email', '+ String role'], ['+ isSuccess()'], header_color='#A16207')

    draw_class(260, 164, 34, 30, 'DTO', 'CreateProductRequest',
               ['+ String name', '+ String description', '+ String hsCode', '+ Long categoryId', '+ BigDecimal price', '+ BigDecimal unitCost', '+ BigDecimal weight'], ['+ validate()'], header_color='#A16207')

    draw_class(298, 164, 34, 30, 'DTO', 'CreateOrderRequest',
               ['+ Long productId', '+ Long destinationCountryId', '+ Integer quantity', '+ String pickupLocation', '+ String shippingRequirements'], ['+ validate()'], header_color='#A16207')

    draw_class(336, 164, 32, 30, 'DTO', 'UpdateShipmentStatusRequest',
               ['+ ShipmentStatus status', '+ String currentLocation', '+ String remarks', '+ String trackingNumber'], ['+ validate()'], header_color='#A16207')

    draw_class(260, 130, 52, 28, 'DTO', 'RegulationResponse',
               ['+ String originCountry, destinationCountry', '+ String hsCode', '+ List<Authority> regulatoryAuthorities', '+ List<Requirement> originRequirements', '+ List<Requirement> destinationRequirements', '+ List<Document> requiredDocumentsDetailed', '+ DutiesAndTaxes dutiesAndTaxes'], [], header_color='#A16207')

    draw_class(316, 130, 52, 28, 'DTO', 'CostEstimationResponse',
               ['+ String calculationCurrency (INR)', '+ Map sellingPrice, invoiceValue', '+ Map sellerExportCost (CIF fulfillment)', '+ Map customsValue (WTO/ZATCA)', '+ Map customsDuty, importVat', '+ Map buyerLandedCost', '+ Double exporterMargin, markup', '+ Double breakEvenPriceSAR'], [], header_color='#A16207')

    # ═══════════════════════════════════════════════════════════════════
    # 3. BACKEND SERVICES (BOTTOM LEFT: x=8, y=8, w=172, h=110)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(8, 8, 172, 110, PKG_SERVICES)

    draw_class(12, 82, 38, 28, 'Service', 'AuthService',
               ['+ UserRepository userRepo', '+ PasswordEncoder encoder', '+ JwtTokenProvider jwt'],
               ['+ login(LoginRequest): AuthResp', '+ register(RegisterRequest): AuthResp', '+ getCurrentUser(): User'], header_color='#0F766E')

    draw_class(54, 82, 38, 28, 'Service', 'ProductService',
               ['+ ProductRepository productRepo', '+ ProductCategoryRepo catRepo'],
               ['+ getProductsByExporter(): List', '+ createProduct(req): Product', '+ deleteProduct(id): void'], header_color='#0F766E')

    draw_class(96, 82, 40, 28, 'Service', 'OrderService',
               ['+ OrderRepository orderRepo', '+ ProductRepo, UserRepo'],
               ['+ createOrder(req): Order', '+ acceptOrder(id): Order', '+ getExporterOrders(): List'], header_color='#0F766E')

    draw_class(140, 82, 36, 28, 'Service', 'ShipmentService',
               ['+ ShipmentRepository shipRepo', '+ ShipmentMilestoneRepo'],
               ['+ createShipment(Order): Shipment', '+ updateStatus(id, st): Shipment', '+ trackByNumber(num): Shipment'], header_color='#0F766E')

    draw_class(12, 44, 48, 32, 'Service', 'HsClassificationService',
               ['+ HsMasterRepository hsMasterRepo', '+ FullTextIndexStrategy strategy'],
               ['+ classify(productName): List<HsMasterEntity>', '+ searchHs(query, country): Page', '+ getHsHierarchy(hsCode): Map'], header_color='#0F766E')

    draw_class(64, 44, 52, 32, 'Service', 'RegulatoryRetrievalService',
               ['+ RegulationHsMappingRepo mappingRepo', '+ HsMasterRepo, SourceRepo'],
               ['+ getRegulations(country, hsCode): RegResult', '+ calculateCompliance(RegResult): CompScore', '+ resolveCountry(country): Optional'], header_color='#0F766E')

    draw_class(120, 44, 56, 32, 'Service', 'RegulatoryKnowledgeService',
               ['+ KnowledgeRuleEngine ruleEngine', '+ JurisdictionMappingService jurMap'],
               ['+ getKnowledgeBasedRegulations(origin, dest, hs, prod, cat): Map', '+ calculateScore(Map kb): int', '+ getComplexity(Map kb): String'], header_color='#0F766E')

    draw_class(12, 12, 52, 28, 'ML Service', 'MlExportRankingService',
               ['+ MlPredictionMatrix predictionCache', '+ XGBRankerModel v4_ranker'],
               ['+ predictBlock(hsCode, destination): Map', '+ lookup(hsCode, country): Optional', '+ rankAll(hsCode, countries): List'], header_color='#0F766E')

    draw_class(68, 12, 52, 28, 'Service', 'MarketOpportunityService',
               ['+ MlExportRankingService mlService', '+ RegulatoryRetrievalService retService', '+ CostEstimationService costService'],
               ['+ rankMarketOpportunityForCountries(hs, list): Map', '+ analyzeCountry(country, hs): Map'], header_color='#0F766E')

    draw_class(124, 12, 52, 28, 'AI Service', 'NvidiaAiService',
               ['+ String nvidiaApiKey', '+ RestTemplate restClient', '+ String chatModel = "Nemotron-Ultra"'],
               ['+ chat(systemPrompt, userMsg, ctx): String', '+ regulatoryChat(country, hs, q): String', '+ isAvailable(): boolean'], header_color='#0F766E')

    # ═══════════════════════════════════════════════════════════════════
    # 4. REST API CONTROLLERS (BOTTOM MIDDLE: x=186, y=8, w=84, h=110)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(186, 8, 84, 110, PKG_CONTROLLERS)

    draw_class(190, 84, 38, 26, 'RestController', 'AuthController',
               ['+ AuthService authService'],
               ['+ POST /api/v1/auth/login', '+ POST /api/v1/auth/register', '+ GET /api/v1/auth/me'], header_color='#C2410C')

    draw_class(230, 84, 36, 26, 'RestController', 'ProductController',
               ['+ ProductService productService'],
               ['+ GET /api/v1/products', '+ POST /api/v1/products', '+ DELETE /api/v1/products/{id}'], header_color='#C2410C')

    draw_class(190, 54, 38, 26, 'RestController', 'OrderController',
               ['+ OrderService orderService'],
               ['+ GET /api/v1/orders', '+ POST /api/v1/orders', '+ PUT /api/v1/orders/{id}/accept'], header_color='#C2410C')

    draw_class(230, 54, 36, 26, 'RestController', 'ShipmentController',
               ['+ ShipmentService shipmentService'],
               ['+ GET /api/v1/shipments', '+ GET /api/v1/shipments/{track}', '+ PUT /api/v1/shipments/{id}/status'], header_color='#C2410C')

    draw_class(190, 24, 38, 26, 'RestController', 'RegulatoryController',
               ['+ RegulatoryRetrievalService retService', '+ RegulatoryKnowledgeService kbService'],
               ['+ GET /api/v1/regulations/{cntry}/{hs}', '+ GET /api/v1/regulatory/compliance', '+ GET /api/v1/regulatory/hs/search'], header_color='#C2410C')

    draw_class(230, 24, 36, 26, 'RestController', 'ExportAnalysisController',
               ['+ RegulatoryRetrievalService retService', '+ RegulatoryKnowledgeService kbService', '+ MlExportRankingService mlService'],
               ['+ POST /api/v1/export/analyze', '+ GET /api/v1/export-guide/generate'], header_color='#C2410C')

    # ═══════════════════════════════════════════════════════════════════
    # 6. FRONTEND ARCHITECTURE & CLIENT SERVICES (BOTTOM RIGHT: x=276, y=8, w=96, h=110)
    # ═══════════════════════════════════════════════════════════════════
    draw_package(276, 8, 96, 110, PKG_FRONTEND)

    draw_class(280, 84, 42, 26, 'React Component', 'AppRouter / AuthGuard',
               ['+ State user, token, activeRole', '+ Route protection logic'],
               ['+ handleRoleRouting()', '+ renderDashboard()'], header_color='#15803D')

    draw_class(326, 84, 42, 26, 'React Component', 'ExporterDashboard',
               ['+ Views: Overview, Products, Analysis, Orders, Profile', '+ activeTab state'],
               ['+ fetchDashboardStats()', '+ handleProductSelect()'], header_color='#15803D')

    draw_class(280, 48, 88, 32, 'Calculation Engine', 'costEngine.js',
               ['+ INCOTERMS_RULES (10 Incoterms: EXW, FOB, CIF, DAP, DDP...)', '+ FX_CURRENCIES (INR, SAR, USD, EUR, GBP, AED)'],
               ['+ calculateTradeEconomics(transaction, fxRates, dutyData, taxData): TradeEconomicsResult',
                '+ calculateExportCost(inputs): LegacyAdapter',
                '+ runScenarioEngine(expected, variants): ScenarioResults',
                '+ runSensitivityMatrix(transaction): SensitivityData',
                '+ runQuantityEconomics(transaction, tiers): QuantityTable'], header_color='#15803D')

    draw_class(280, 12, 28, 32, 'Client Service', 'httpClient.js',
               ['+ BASE_URL = /api/v1', '+ authToken string'],
               ['+ get(url): Promise', '+ post(url, data): Promise', '+ put(url, data): Promise', '+ setToken(jwt): void'], header_color='#15803D')

    draw_class(310, 12, 30, 32, 'Client Service', 'intelligenceService.js',
               ['+ httpClient instance'],
               ['+ analyzeExport(params)', '+ rankMarketOpportunity()', '+ estimateCost(params)'], header_color='#15803D')

    draw_class(342, 12, 26, 32, 'Client Service', 'regulatoryService.js',
               ['+ httpClient instance'],
               ['+ getRegulations(c, hs)', '+ getCompliance(c, hs)', '+ regulatoryChat(q)'], header_color='#15803D')

    # ═══════════════════════════════════════════════════════════════════
    # RELATIONSHIP CONNECTORS & ANNOTATIONS
    # ═══════════════════════════════════════════════════════════════════
    # User to Profiles
    draw_assoc(26, 202, 26, 194, label='1 : 0..1', card1='', card2='', is_directed=True)
    draw_assoc(40, 202, 50, 194, label='1 : 0..1', card1='', card2='', is_directed=True)
    draw_assoc(40, 218, 44, 218, label='has role', card1='1', card2='1', is_directed=True)

    # User & Category to Product
    draw_assoc(40, 210, 80, 210, label='exports (1 : 0..*)', card1='', card2='', is_directed=True)
    draw_assoc(98, 190, 98, 202, label='classifies', card1='1', card2='0..*', is_directed=True)

    # Product & User to Order
    draw_assoc(80, 205, 50, 150, label='contains (1 : 0..*)', card1='', card2='', is_directed=True)
    draw_assoc(26, 168, 26, 160, label='places (1 : 0..*)', card1='', card2='', is_directed=True)

    # Order to Shipment & Country
    draw_assoc(50, 140, 80, 140, label='tracks (1 : 0..1)', card1='', card2='', is_directed=True)
    draw_assoc(80, 148, 50, 175, label='assigned partner', card1='', card2='', is_directed=True)

    # Regulatory mapping
    draw_assoc(172, 210, 216, 210, label='maps (1 : 0..*)', card1='', card2='', is_directed=True)
    draw_assoc(212, 210, 216, 210, label='', card1='', card2='', is_directed=True)

    # Controller to Service delegation (vertical bridges)
    draw_assoc(205, 84, 30, 82, label='delegates', style='--', color='#2563EB', is_directed=True)
    draw_assoc(245, 84, 70, 82, label='delegates', style='--', color='#2563EB', is_directed=True)
    draw_assoc(205, 54, 115, 82, label='delegates', style='--', color='#2563EB', is_directed=True)
    draw_assoc(245, 54, 155, 82, label='delegates', style='--', color='#2563EB', is_directed=True)
    draw_assoc(205, 24, 90, 44, label='retrieves', style='--', color='#9333EA', is_directed=True)
    draw_assoc(245, 24, 145, 44, label='enriches & ML predicts', style='--', color='#9333EA', is_directed=True)

    # Frontend Client Services to REST Controllers
    draw_assoc(295, 30, 230, 24, label='REST / JSON Calls (JWT Bearer)', style='--', color='#059669', is_directed=True)
    draw_assoc(324, 84, 324, 80, label='calculates trade economics', style='-', color='#16A34A', is_directed=True)

    # Save outputs
    output_png = r'd:\MAJOR PROJECT\TradeWise_Class_Diagram.png'
    output_svg = r'd:\MAJOR PROJECT\TradeWise_Class_Diagram.svg'
    artifact_png = r'C:\Users\DELL\.gemini\antigravity-ide\brain\fad105f1-54ee-4c47-a006-82c36481779e\TradeWise_Class_Diagram.png'

    plt.tight_layout()
    plt.savefig(output_png, format='png', bbox_inches='tight', dpi=300, facecolor=fig.get_facecolor())
    plt.savefig(output_svg, format='svg', bbox_inches='tight', facecolor=fig.get_facecolor())
    
    # Copy to artifacts directory
    shutil.copyfile(output_png, artifact_png)
    print("SUCCESS: Class diagram generated at:", output_png)
    print("SUCCESS: SVG generated at:", output_svg)
    print("SUCCESS: Artifact copy created at:", artifact_png)

if __name__ == '__main__':
    create_class_diagram()
