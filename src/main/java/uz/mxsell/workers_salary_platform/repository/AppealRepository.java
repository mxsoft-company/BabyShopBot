package uz.mxsell.workers_salary_platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.mxsell.workers_salary_platform.entity.Appeal;

import java.util.Optional;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

    Optional<Appeal> findByCompanyIdAndOneCAppealId(Long companyId, String oneCAppealId);
}
