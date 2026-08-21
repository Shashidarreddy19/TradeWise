package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.product.ProductRequest;
import com.trade.dto.product.ProductResponse;
import com.trade.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages the exporter's product catalog.
 * All endpoints require EXPORTER role.
 *
 * GET    /api/products          – list all products for authenticated exporter
 * GET    /api/products/{id}     – get one product by ID
 * POST   /api/products          – create a new product
 * PUT    /api/products/{id}     – update an existing product
 * DELETE /api/products/{id}     – delete a product
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXPORTER')")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ProductResponse> products = productService.getProductsByExporter(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProductResponse product = productService.createProduct(request, userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProductResponse product = productService.updateProduct(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        productService.deleteProduct(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
