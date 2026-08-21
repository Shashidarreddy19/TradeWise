package com.trade.service;

import com.trade.dto.category.CategoryResponse;

import java.util.List;

/**
 * Contract for product category lookup operations.
 */
public interface CategoryService {

    List<CategoryResponse> getAllCategories();
}
