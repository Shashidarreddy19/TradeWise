package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CategoryMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryMasterRepository extends JpaRepository<CategoryMasterEntity, Long> {
    Optional<CategoryMasterEntity> findByCategoryName(String categoryName);
    List<CategoryMasterEntity> findByActiveTrue();
}
