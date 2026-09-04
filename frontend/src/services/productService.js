/**
 * productService.js — Products CRUD API (Exporter role).
 *
 * Backend: ProductController @ /api/products
 */

import { get, post, put, del } from './httpClient';

export const productsApi = {
  /**
   * GET /api/products — list all products for authenticated exporter
   */
  getAll() {
    return get('/products');
  },

  /**
   * GET /api/products/:id
   */
  getById(id) {
    return get(`/products/${id}`);
  },

  /**
   * POST /api/products — create a new product
   * @param {object} product - { name, category, hsCode, description, price, weight, ... }
   */
  create(product) {
    return post('/products', product);
  },

  /**
   * PUT /api/products/:id — update an existing product
   */
  update(id, product) {
    return put(`/products/${id}`, product);
  },

  /**
   * DELETE /api/products/:id
   */
  delete(id) {
    return del(`/products/${id}`);
  },
};
