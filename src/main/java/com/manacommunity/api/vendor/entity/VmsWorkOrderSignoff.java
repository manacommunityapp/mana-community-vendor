package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_work_order_signoffs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsWorkOrderSignoff {

    public enum SignoffType {
        RESIDENT_OTP,
        DIGITAL_SIGNATURE,
        SUPERVISOR_SIGNATURE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signoff_type", nullable = false, length = 30)
    private SignoffType signoffType;

    @Column(name = "signatory_name", length = 120)
    private String signatoryName;

    @Column(name = "signatory_phone", length = 20)
    private String signatoryPhone;

    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback_notes", length = 500)
    private String feedbackNotes;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
