/**
 * authService.js — Authentication API endpoints.
 *
 * Backend: AuthController @ /api/auth
 * Endpoints: register, login, profile, check-email, check-phone
 */

import { get, post, put } from './httpClient';

export const authApi = {
  /**
   * POST /api/auth/register
   * @param {object} data - { firstName, lastName, email, phone, password, role, company, country, ... }
   */
  register(data) {
    return post('/auth/register', data);
  },

  /**
   * POST /api/auth/login
   * @param {{email: string, password: string}} credentials
   */
  login(credentials) {
    return post('/auth/login', credentials);
  },

  /**
   * GET /api/auth/profile — current user profile
   */
  getProfile() {
    return get('/auth/profile');
  },

  /**
   * PUT /api/auth/profile — update current user profile
   * @param {object} data
   */
  updateProfile(data) {
    return put('/auth/profile', data);
  },

  /**
   * GET /api/auth/check-email?email=...
   * Real-time duplicate detection (debounced on frontend).
   */
  checkEmail(email) {
    return get(`/auth/check-email?email=${encodeURIComponent(email)}`);
  },

  /**
   * GET /api/auth/check-phone?phone=...
   * Real-time duplicate detection (debounced on frontend).
   */
  checkPhone(phone) {
    return get(`/auth/check-phone?phone=${encodeURIComponent(phone)}`);
  },
};
