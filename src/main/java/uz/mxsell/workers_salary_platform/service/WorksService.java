package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.mxsell.workers_salary_platform.client.OneCClient;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.onec.OneCCompletedWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPendingWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCWorkDetailDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorksService {

    private final CompanyService companyService;
    private final OneCClient oneCClient;

    public List<OneCPendingWorkDto> getPendingWorks(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getPendingWorks(company, employeeId1c);
    }

    public List<OneCCompletedWorkDto> getCompletedWorks(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getCompletedWorks(company, employeeId1c);
    }

    public OneCWorkDetailDto getWorkById(Long companyId, String employeeId1c, String workId) {
        Company company = companyService.getById(companyId);
        return oneCClient.getWorkById(company, employeeId1c, workId);
    }
}
