package com.trade.service.impl;

import com.trade.dto.category.CategoryResponse;
import com.trade.repository.ProductCategoryRepository;
import com.trade.service.CategoryService;
import com.trade.util.MappingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(MappingUtil::toCategoryResponse)
                .toList();
    }
}
