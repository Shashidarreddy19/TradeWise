package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_chapter", uniqueConstraints = {
    @UniqueConstraint(name = "uq_category_chapter", columnNames = {"category_id", "chapter"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryMasterEntity category;

    @Column(name = "chapter", nullable = false, length = 2)
    private String chapter;
}
