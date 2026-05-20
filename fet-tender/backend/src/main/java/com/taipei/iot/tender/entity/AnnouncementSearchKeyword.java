package com.taipei.iot.tender.entity;

import com.taipei.iot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "announcement_search_keywords",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ann_kw",
        columnNames = {"solution", "keyword"}
    )
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementSearchKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solution", nullable = false, length = 255)
    private String solution;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
