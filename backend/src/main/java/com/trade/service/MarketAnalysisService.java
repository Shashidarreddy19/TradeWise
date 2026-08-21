package com.trade.service;

import com.trade.dto.market.MarketAnalysisRequest;
import com.trade.dto.market.MarketAnalysisResponse;

/**
 * Contract for market analysis operations.
 * Data is sourced entirely from the Compliance table — no AI involved.
 * Designed so a future RAG module can augment the response without schema changes.
 */
public interface MarketAnalysisService {

    MarketAnalysisResponse analyze(MarketAnalysisRequest request);
}
