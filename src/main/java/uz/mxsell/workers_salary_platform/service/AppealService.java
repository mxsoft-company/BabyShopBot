package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.mxsell.workers_salary_platform.client.OneCClient;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.onec.OneCAppealResponse;
import uz.mxsell.workers_salary_platform.entity.Appeal;
import uz.mxsell.workers_salary_platform.entity.AppealMessage;
import uz.mxsell.workers_salary_platform.entity.AppealStatus;
import uz.mxsell.workers_salary_platform.entity.AppealType;
import uz.mxsell.workers_salary_platform.entity.Employee;
import uz.mxsell.workers_salary_platform.entity.MessageSender;
import uz.mxsell.workers_salary_platform.exception.AppealNotFoundException;
import uz.mxsell.workers_salary_platform.exception.OneCCommunicationException;
import uz.mxsell.workers_salary_platform.repository.AppealMessageRepository;
import uz.mxsell.workers_salary_platform.repository.AppealRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppealService {

    private final AppealRepository appealRepository;
    private final AppealMessageRepository appealMessageRepository;
    private final CompanyService companyService;
    private final OneCClient oneCClient;

    @Transactional
    public Appeal createAppeal(Long companyId, Employee employee, AppealType type, String message) {
        // 1C is source-of-truth: create there first. If OneCClient throws, it
        // propagates — no local save
        String oneCAppealId = createAppealInOneC(companyId, employee.getEmployeeId1c(), type.name(), message);

        Appeal appeal = new Appeal();
        appeal.setCompanyId(companyId);
        appeal.setEmployeeId(employee.getId());
        appeal.setType(type);
        appeal.setMessage(message);
        appeal.setOneCAppealId(oneCAppealId);
        appeal.setStatus(AppealStatus.PENDING);
        appeal = appealRepository.save(appeal);
        log.info("Appeal created: localId={}, oneCId={}, type={}, employeeId={}",
                appeal.getId(), appeal.getOneCAppealId(), type, employee.getId());
        return appeal;
    }

    @Transactional
    public AppealMessage addManagementMessage(Long companyId, String oneCAppealId, String message) {
        Appeal appeal = appealRepository.findByCompanyIdAndOneCAppealId(companyId, oneCAppealId)
                .orElseThrow(() -> new AppealNotFoundException("Murojaat topilmadi: oneCAppealId=" + oneCAppealId));

        AppealMessage appealMessage = new AppealMessage();
        appealMessage.setCompanyId(companyId);
        appealMessage.setAppealId(appeal.getId());
        appealMessage.setSender(MessageSender.MANAGEMENT);
        appealMessage.setMessage(message);
        appealMessage = appealMessageRepository.save(appealMessage);

        appeal.setStatus(AppealStatus.ANSWERED);
        appealRepository.save(appeal);

        log.info("Management replied to appeal: oneCAppealId={}", oneCAppealId);
        return appealMessage;
    }

    public Appeal getAppealByOneCId(Long companyId, String oneCAppealId) {
        return appealRepository.findByCompanyIdAndOneCAppealId(companyId, oneCAppealId)
                .orElseThrow(() -> new AppealNotFoundException("Murojaat topilmadi: oneCAppealId=" + oneCAppealId));
    }

    private String createAppealInOneC(Long companyId, String employeeId1c, String type, String message) {
        Company company = companyService.getById(companyId);
        OneCAppealResponse response = oneCClient.createAppeal(company, employeeId1c, type, message);
        if (response == null || !response.isSuccess() || response.getAppealId() == null) {
            throw new OneCCommunicationException("1C da murojaat yaratishda xatolik: muvaffaqiyatsiz javob");
        }
        return response.getAppealId();
    }
}
