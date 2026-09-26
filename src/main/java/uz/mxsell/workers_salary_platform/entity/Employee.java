package uz.mxsell.workers_salary_platform.entity;

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
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "employee_id_1c", nullable = false, length = 100)
    private String employeeId1c;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "department")
    private String department;

    @Column(name = "employee_number", length = 100)
    private String employeeNumber;

    @Column(name = "hrpulse_employee_id", length = 50)
    private String hrpulseEmployeeId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
