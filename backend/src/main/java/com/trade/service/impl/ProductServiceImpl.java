package com.trade.service.impl;

import com.trade.dto.product.ProductRequest;
import com.trade.dto.product.ProductResponse;
import com.trade.entity.Product;
import com.trade.entity.ProductCategory;
import com.trade.entity.User;
import com.trade.exception.ResourceNotFoundException;
import com.trade.exception.UnauthorizedException;
import com.trade.repository.ProductCategoryRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.UserRepository;
import com.trade.service.ProductService;
import com.trade.util.MappingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(MappingUtil::toProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByExporter(String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        return productRepository.findByExporter(exporter)
                .stream()
                .map(MappingUtil::toProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);
        return MappingUtil.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        ProductCategory category = request.getCategoryId() != null
                ? findCategoryById(request.getCategoryId()) : null;

        Product product = Product.builder()
                .exporter(exporter)
                .category(category)
                .categoryName(category != null ? category.getCategoryName() : null)
                .name(request.getName())
                .hsCode(request.getHsCode() != null && !request.getHsCode().isBlank()
                        ? request.getHsCode() : null)
                .description(request.getDescription())
                .material(request.getMaterial())
                .composition(request.getComposition())
                .function(request.getFunction())
                .manufacturingProcess(request.getManufacturingProcess())
                .physicalForm(request.getPhysicalForm())
                .specifications(request.getSpecifications())
                .manufacturingCost(request.getPrice()) // default manufacturing cost to price
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .weight(request.getWeight())
                .build();

        product = productRepository.save(product);
        log.info("Product created [id={}] by exporter [{}]", product.getId(), exporterEmail);
        return MappingUtil.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String exporterEmail) {
        Product product = findProductById(id);
        verifyOwnership(product, exporterEmail);

        ProductCategory category = request.getCategoryId() != null
                ? findCategoryById(request.getCategoryId()) : product.getCategory();

        product.setCategory(category);
        product.setCategoryName(category != null ? category.getCategoryName() : null);
        product.setName(request.getName());
        product.setHsCode(request.getHsCode() != null && !request.getHsCode().isBlank()
                ? request.getHsCode() : product.getHsCode());
        product.setDescription(request.getDescription());
        product.setMaterial(request.getMaterial());
        product.setComposition(request.getComposition());
        product.setFunction(request.getFunction());
        product.setManufacturingProcess(request.getManufacturingProcess());
        product.setPhysicalForm(request.getPhysicalForm());
        product.setSpecifications(request.getSpecifications());
        product.setManufacturingCost(request.getPrice());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setWeight(request.getWeight());

        product = productRepository.save(product);
        log.info("Product updated [id={}] by exporter [{}]", product.getId(), exporterEmail);
        return MappingUtil.toProductResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, String exporterEmail) {
        Product product = findProductById(id);
        verifyOwnership(product, exporterEmail);
        productRepository.delete(product);
        log.info("Product deleted [id={}] by exporter [{}]", id, exporterEmail);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ProductCategory findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductCategory", "id", id));
    }

    private void verifyOwnership(Product product, String exporterEmail) {
        if (!product.getExporter().getEmail().equals(exporterEmail)) {
            throw new UnauthorizedException("You do not have permission to modify this product");
        }
    }
}
