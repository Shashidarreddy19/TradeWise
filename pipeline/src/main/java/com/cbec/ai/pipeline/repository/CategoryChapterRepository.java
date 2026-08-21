package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CategoryChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryChapterRepository extends JpaRepository<CategoryChapterEntity, Long> {

    @Query("SELECT cc FROM CategoryChapterEntity cc JOIN FETCH cc.category WHERE cc.chapter = :chapter")
    List<CategoryChapterEntity> findByChapter(@Param("chapter") String chapter);
}
