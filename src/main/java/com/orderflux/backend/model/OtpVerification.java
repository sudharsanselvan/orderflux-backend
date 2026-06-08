package com.orderflux.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp")
@Getter
@Setter
@NoArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Static factory method — replaces @Builder.
     * Eclipse can always see this because it's plain Java.
     * No Lombok processing required.
     */
    public static OtpVerification create(User user,
                                          String otpCode,
                                          LocalDateTime expiryTime) {
        OtpVerification otp = new OtpVerification();
        otp.user = user;
        otp.otpCode = otpCode;
        otp.expiryTime = expiryTime;
        otp.isUsed = false;
        return otp;
    }
}