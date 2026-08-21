package com.trade.service;

import com.trade.dto.product.ProductRequest;
import com.trade.dto.product.ProductResponse;

import java.util.List;

/**
 * Contract for product catalog operations.
 */
public interface ProductService {

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getProductsByExporter(String exporterEmail);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request, String exporterEmail);

    ProductResponse updateProduct(Long id, ProductRequest request, String exporterEmail);

    void deleteProduct(Long id, String exporterEmail);
}
