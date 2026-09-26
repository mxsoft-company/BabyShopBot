package uz.mxsell.workers_salary_platform.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(name = "bot_token", unique = true)
    private String botToken;

    @Column(name = "bot_username", nullable = false)
    private String botUsername;

    @Column(name = "onec_base_url", nullable = false, length = 500)
    private String onecBaseUrl;

    @Column(name = "onec_username", nullable = false, length = 100)
    private String onecUsername;

    @Column(name = "onec_password", nullable = false)
    private String onecPassword;

    @Column(name = "service_auth_username", nullable = false, length = 100)
    private String serviceAuthUsername;

    @Column(name = "service_auth_password", nullable = false)
    private String serviceAuthPassword;

    @Column(name = "hrpulse_base_url", length = 500)
    private String hrpulseBaseUrl;

    @Column(name = "hrpulse_token", length = 500)
    private String hrpulseToken;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
