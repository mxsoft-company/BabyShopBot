package uz.mxsell.workers_salary_platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.mxsell.workers_salary_platform.entity.AppealMessage;

public interface AppealMessageRepository extends JpaRepository<AppealMessage, Long> {
}
