package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * IAM Right entity.
 */
@Entity
@Table(name = "rights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class RightEntity extends AuditEntity {

    @Id
    @Column(name = "right_id", length = 100)
    private String rightId;

    @Column(name = "right_name", nullable = false, length = 150)
    private String rightName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecordStatus status = RecordStatus.ACTIVE;
}
