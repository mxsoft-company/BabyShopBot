package uz.mxsell.workers_salary_platform.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySlugAndIsActiveTrue(String slug);

    List<Company> findAllByIsActiveTrue();

    List<Company> findAllByIsDeletedFalse();

    Optional<Company> findByBotToken(String botToken);

    boolean existsBySlug(String slug);
}
