package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mxsell.workers_salary_platform.client.OneCClient;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.onec.OneCEmployeeInfoDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCRegisterResponse;
import uz.mxsell.workers_salary_platform.entity.Employee;
import uz.mxsell.workers_salary_platform.exception.EmployeeNotFoundException;
import uz.mxsell.workers_salary_platform.repository.EmployeeRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyService companyService;
    private final OneCClient oneCClient;

    /**
     * Registers an employee:
     * 1. If an active (is_deleted=false) row exists for this telegram_id ->
     * ALREADY_REGISTERED
     * 2. If a soft-deleted row exists -> re-verify with 1C, then reactivate that
     * row
     * 3. Otherwise -> register in 1C and save a new Employee
     */
    public RegistrationResult registerEmployee(Long companyId, Long telegramId, String phone) {
        Optional<Employee> existing = employeeRepository.findByCompanyIdAndTelegramId(companyId, telegramId);
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getIsDeleted())) {
            return new RegistrationResult(RegistrationResult.Status.ALREADY_REGISTERED,
                    "Siz allaqachon ro'yxatdan o'tgansiz!", existing.get());
        }
        boolean reactivation = existing.isPresent();

        // Step 1: Register/verify in 1C
        OneCRegistration registration = checkEmployeeInOneC(companyId, telegramId, phone);
        if (registration == null || registration.employeeId() == null) {
            // On reactivation failure the old row stays is_deleted=true, untouched
            return new RegistrationResult(RegistrationResult.Status.NOT_FOUND,
                    "Sizning telefon raqamingiz korxona tizimida topilmadi. " +
                            "Iltimos, HR bo'limiga murojaat qiling.",
                    null);
        }

        // Step 2: Get detailed employee info from 1C
        String employeeId = registration.employeeId();
        String fullName = registration.name();
        String department = null;
        String employeeNumber = null;
        String hrpulseEmployeeId = null;

        try {
            OneCEmployeeInfo empInfo = getEmployeeInfoFromOneC(companyId, employeeId);
            if (empInfo != null) {
                // Use more detailed name from getEmployeeInfo if available
                if (empInfo.name() != null) {
                    fullName = empInfo.name();
                }
                department = empInfo.department();
                employeeNumber = empInfo.employeeNumber();
                hrpulseEmployeeId = empInfo.hrpulseEmployeeId();
            }
        } catch (Exception e) {
            log.warn("Could not get detailed employee info from 1C for {}: {}. " +
                    "Proceeding with basic info from register response.", employeeId, e.getMessage());
        }

        // Step 3: Reactivate the existing row or save a new one
        Employee employee;
        if (reactivation) {
            employee = existing.get();
            employee.setPhone(phone);
            employee.setEmployeeId1c(employeeId);
            employee.setFullName(fullName != null ? fullName : "—");
            employee.setDepartment(department);
            employee.setEmployeeNumber(employeeNumber);
            if (hrpulseEmployeeId != null && !hrpulseEmployeeId.isBlank()) {
                employee.setHrpulseEmployeeId(hrpulseEmployeeId);
            }
            employee.setIsDeleted(false);
            log.info("Reactivating employee {} (telegramId: {})", employee.getId(), telegramId);
        } else {
            employee = new Employee();
            employee.setCompanyId(companyId);
            employee.setTelegramId(telegramId);
            employee.setPhone(phone);
            employee.setEmployeeId1c(employeeId);
            employee.setFullName(fullName != null ? fullName : "—");
            employee.setDepartment(department);
            employee.setEmployeeNumber(employeeNumber);
            if (hrpulseEmployeeId != null && !hrpulseEmployeeId.isBlank()) {
                employee.setHrpulseEmployeeId(hrpulseEmployeeId);
            }
        }
        employee = employeeRepository.save(employee);

        log.info("Employee registered: {} (telegramId: {})", employee.getFullName(), telegramId);

        return new RegistrationResult(RegistrationResult.Status.SUCCESS,
                "Ro'yxatdan muvaffaqiyatli o'tdingiz! ✅\n\n" +
                        "👤 " + employee.getFullName() + "\n" +
                        "🏢 " + (employee.getDepartment() != null ? employee.getDepartment() : "—"),
                employee);
    }

    public void logout(Long companyId, Long telegramId) {
        Employee employee = employeeRepository.findByCompanyIdAndTelegramIdAndIsDeletedFalse(companyId, telegramId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Faol xodim topilmadi: telegramId=" + telegramId));
        employee.setIsDeleted(true);
        employeeRepository.save(employee);
        log.info("Employee logged out: id={}, telegramId={}", employee.getId(), telegramId);
    }

    public Optional<Employee> findByTelegramId(Long companyId, Long telegramId) {
        return employeeRepository.findByCompanyIdAndTelegramIdAndIsDeletedFalse(companyId, telegramId);
    }

    private OneCRegistration checkEmployeeInOneC(Long companyId, Long telegramId, String phone) {
        Company company = companyService.getById(companyId);
        OneCRegisterResponse response = oneCClient.checkEmployee(company, telegramId, phone);
        if (response == null || !response.isSuccess()) {
            return null;
        }
        return new OneCRegistration(response.getEmployeeId(), response.getName());
    }

    private OneCEmployeeInfo getEmployeeInfoFromOneC(Long companyId, String employeeId) {
        Company company = companyService.getById(companyId);
        OneCEmployeeInfoDto info = oneCClient.getEmployeeInfo(company, employeeId);
        if (info == null) {
            return null;
        }
        return new OneCEmployeeInfo(info.getName(), info.getDepartment(), info.getEmployeeNumber(),
                info.getHrpulseEmployeeId());
    }

    private record OneCRegistration(String employeeId, String name) {
    }

    /**
     * Saves the HR Pulse employee ID for an already-registered employee.
     * Called when the employee enters their HR Pulse ID in the bot for the first
     * time.
     */
    public void saveHrpulseEmployeeId(Long companyId, Long telegramId, String hrpulseEmployeeId) {
        Employee employee = employeeRepository.findByCompanyIdAndTelegramId(companyId, telegramId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Xodim topilmadi: companyId=" + companyId + ", telegramId=" + telegramId));
        employee.setHrpulseEmployeeId(hrpulseEmployeeId);
        employeeRepository.save(employee);
    }

    private record OneCEmployeeInfo(String name, String department, String employeeNumber, String hrpulseEmployeeId) {
    }

    // Inner record for registration result
    public record RegistrationResult(Status status, String message, Employee employee) {
        public enum Status {
            SUCCESS,
            ALREADY_REGISTERED,
            NOT_FOUND
        }
    }
}
