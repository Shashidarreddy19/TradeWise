/**
 * aiService.js — AI Regulatory Intelligence API.
 *
 * Backend: AiRegulatoryController @ /api/v1/ai
 * Powered by NVIDIA Nemotron LLM + hybrid RAG pipeline.
 */

import { get, post } from './httpClient';

export const aiApi = {
  /**
   * POST /api/v1/ai/regulatory-chat — AI-grounded regulatory Q&A
   * @param {string} country - destination country
   * @param {string} hsCode - HS code (no dots)
   * @param {string} question - user's question
   */
  regulatoryChat(country, hsCode, question) {
    return post('/v1/ai/regulatory-chat', { country, hsCode, question });
  },

  /**
   * POST /api/v1/ai/summarize-document — Document summarization
   * @param {string} text - document text content
   * @param {string} title - document title
   */
  summarizeDocument(text, title) {
    return post('/v1/ai/summarize-document', { text, title });
  },

  /**
   * GET /api/v1/ai/status — AI service availability check
   */
  getStatus() {
    return get('/v1/ai/status');
  },

  /**
   * GET /api/v1/export-guide/{country}/{hsCode} — Step-by-step export guide
   */
  getExportGuide(country, hsCode) {
    return get(`/v1/export-guide/${encodeURIComponent(country)}/${hsCode}`);
  },
};
