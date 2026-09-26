package uz.mxsell.workers_salary_platform.company;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mxsell.workers_salary_platform.bot.BotManagerService;
import uz.mxsell.workers_salary_platform.exception.DuplicateBotTokenException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final BotManagerService botManagerService;

    // BotManagerService @Lazy: CompanyService -> BotManagerService -> UpdateDispatcher
    // -> EmployeeService -> CompanyService zanjirida aylanma bog'liqlik yuzaga kelmasligi uchun
    public CompanyService(CompanyRepository companyRepository,
            @Lazy BotManagerService botManagerService) {
        this.companyRepository = companyRepository;
        this.botManagerService = botManagerService;
    }

    @Transactional
    public Company create(Company company) {
        checkDuplicateBotToken(company, null);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Optional<Company> findBySlug(String slug) {
        return companyRepository.findBySlugAndIsActiveTrue(slug);
    }

    @Transactional(readOnly = true)
    public List<Company> findAllActive() {
        return companyRepository.findAllByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Company getById(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + companyId));
    }

    @Transactional(readOnly = true)
    public List<Company> findAllVisible() {
        return companyRepository.findAllByIsDeletedFalse();
    }

    @Transactional
    public Company update(Long id, Company updated) {
        checkDuplicateBotToken(updated, id);
        Company existing = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + id));
        existing.setName(updated.getName());
        if (updated.getSlug() != null && !updated.getSlug().isBlank()) {
            existing.setSlug(updated.getSlug());
        }
        if (updated.getBotToken() != null && !updated.getBotToken().isBlank()) {
            existing.setBotToken(updated.getBotToken().trim());
        }
        existing.setBotUsername(updated.getBotUsername());
        existing.setOnecBaseUrl(updated.getOnecBaseUrl());
        existing.setOnecUsername(updated.getOnecUsername());
        existing.setOnecPassword(updated.getOnecPassword());
        existing.setServiceAuthUsername(updated.getServiceAuthUsername());
        existing.setServiceAuthPassword(updated.getServiceAuthPassword());
        existing.setHrpulseBaseUrl(updated.getHrpulseBaseUrl());
        existing.setHrpulseToken(updated.getHrpulseToken());
        existing.setActive(updated.isActive());
        return companyRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public String generateSlug(String name) {
        String base = name == null ? "" : name.toLowerCase().trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-{2,}", "-");
        if (base.startsWith("-")) {
            base = base.substring(1);
        }
        if (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "company";
        }
        String slug = base;
        int counter = 2;
        while (companyRepository.existsBySlug(slug)) {
            slug = base + "-" + counter;
            counter++;
        }
        return slug;
    }

    @Transactional
    public Company toggleActive(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + id));
        company.setActive(!company.isActive());
        return companyRepository.save(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = getById(id);
        botManagerService.unregisterBot(id);
        company.setBotToken(null);
        company.setIsDeleted(true);
        company.setActive(false);
        companyRepository.save(company);
        log.info("Company soft-deleted: id={}, name={}", id, company.getName());
    }

    private void checkDuplicateBotToken(Company company, Long currentId) {
        if (company.getBotToken() == null || company.getBotToken().isBlank()) {
            return;
        }
        Optional<Company> existing = companyRepository.findByBotToken(company.getBotToken().trim());
        if (existing.isPresent() && (currentId == null || !existing.get().getId().equals(currentId))) {
            throw new DuplicateBotTokenException("Bu bot tokeni allaqachon boshqa kompaniyada ishlatilmoqda");
        }
    }
}
