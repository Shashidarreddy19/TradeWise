/**
 * httpClient.js — Core HTTP client for the Trade Platform frontend.
 *
 * Responsibilities:
 * - Base URL configuration (Spring Boot backend at localhost:8081)
 * - Automatic JWT Bearer token attachment from localStorage/sessionStorage
 * - 401 handling (expired token → clear auth → redirect to /login)
 * - 403 handling (forbidden)
 * - 429 / quota exhaustion detection
 * - Typed HTTP method shortcuts: get, post, put, patch, del
 */

export const BASE_URL = 'http://localhost:8081/api';

// ── Token & Auth helpers ─────────────────────────────────────────────────────

export function getToken() {
  return localStorage.getItem('trade_token') || sessionStorage.getItem('trade_token');
}

export function setToken(token, remember = true) {
  if (remember) {
    localStorage.setItem('trade_token', token);
  } else {
    sessionStorage.setItem('trade_token', token);
  }
}

export function getUser() {
  const raw = localStorage.getItem('trade_user') || sessionStorage.getItem('trade_user');
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function setUser(user, remember = true) {
  const serialized = JSON.stringify(user);
  if (remember) {
    localStorage.setItem('trade_user', serialized);
  } else {
    sessionStorage.setItem('trade_user', serialized);
  }
}

export function clearAuth() {
  localStorage.removeItem('trade_token');
  localStorage.removeItem('trade_user');
  sessionStorage.removeItem('trade_token');
  sessionStorage.removeItem('trade_user');
}

export function isAuthenticated() {
  return !!getToken();
}

// ── Core fetch wrapper ───────────────────────────────────────────────────────

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...options.headers,
  };

  const config = { ...options, headers };

  const response = await fetch(url, config);

  // Handle 401 — token expired or invalid
  if (response.status === 401) {
    clearAuth();
    window.location.href = '/login';
    throw new Error('Session expired. Please log in again.');
  }

  // Handle 403 — forbidden
  if (response.status === 403) {
    throw new Error('Access denied. You do not have permission for this action.');
  }

  // Parse JSON response
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message || data?.error || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;

    // Flag quota exhaustion for ML/AI features
    if (
      message.toLowerCase().includes('quota') ||
      message.toLowerCase().includes('rate limit') ||
      response.status === 429
    ) {
      error.isQuotaExhausted = true;
      error.message = 'API quota exhausted. The service will resume shortly. Using cached data in the meantime.';
    }

    throw error;
  }

  return data;
}

// ── HTTP method shortcuts ────────────────────────────────────────────────────

export function get(endpoint) {
  return request(endpoint, { method: 'GET' });
}

export function post(endpoint, body) {
  return request(endpoint, { method: 'POST', body: JSON.stringify(body) });
}

export function put(endpoint, body) {
  return request(endpoint, { method: 'PUT', body: JSON.stringify(body) });
}

export function patch(endpoint, body) {
  return request(endpoint, { method: 'PATCH', body: JSON.stringify(body) });
}

export function del(endpoint) {
  return request(endpoint, { method: 'DELETE' });
}
