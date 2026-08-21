package com.trade.repository;

import com.trade.entity.Product;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByExporter(User exporter);

    List<Product> findByExporterId(Long exporterId);

    long countByExporter(User exporter);

    List<Product> findByCategoryId(Long categoryId);
}
