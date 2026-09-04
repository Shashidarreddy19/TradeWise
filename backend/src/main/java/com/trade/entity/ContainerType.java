package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "container_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code; // 20FT, 40FT, 40HC, LCL, FCL, REEFER, OPEN_TOP, FLAT_RACK, TANK

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_volume_cbm", precision = 10, scale = 2)
    private BigDecimal maxVolumeCbm;

    @Column(name = "max_payload_kg", precision = 10, scale = 2)
    private BigDecimal maxPayloadKg;

    @Column(name = "tare_weight_kg", precision = 10, scale = 2)
    private BigDecimal tareWeightKg;

    @Column(name = "length_meters", precision = 6, scale = 2)
    private BigDecimal lengthMeters;

    @Column(name = "width_meters", precision = 6, scale = 2)
    private BigDecimal widthMeters;

    @Column(name = "height_meters", precision = 6, scale = 2)
    private BigDecimal heightMeters;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
