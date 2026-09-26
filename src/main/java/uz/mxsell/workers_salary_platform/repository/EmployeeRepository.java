package uz.mxsell.workers_salary_platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.mxsell.workers_salary_platform.entity.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByCompanyIdAndTelegramIdAndIsDeletedFalse(Long companyId, Long telegramId);

    Optional<Employee> findByCompanyIdAndTelegramId(Long companyId, Long telegramId);

    Optional<Employee> findByCompanyIdAndEmployeeId1c(Long companyId, String employeeId1c);

    List<Employee> findByCompanyIdAndDepartmentAndIsDeletedFalse(Long companyId, String department);

    List<Employee> findAllByCompanyIdAndIsDeletedFalse(Long companyId);
}
