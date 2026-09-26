package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.mxsell.workers_salary_platform.client.OneCClient;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.onec.OneCBalanceDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPaymentDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPenaltyDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCProductDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCReconciliationDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCTaxDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final CompanyService companyService;
    private final OneCClient oneCClient;

    public List<OneCPaymentDto> getPayments(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getPayments(company, employeeId1c);
    }

    public List<OneCPenaltyDto> getPenalties(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getPenalties(company, employeeId1c);
    }

    public List<OneCTaxDto> getTax(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getTax(company, employeeId1c);
    }

    public List<OneCProductDto> getProducts(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getProducts(company, employeeId1c);
    }

    public OneCBalanceDto getBalance(Long companyId, String employeeId1c) {
        Company company = companyService.getById(companyId);
        return oneCClient.getBalance(company, employeeId1c);
    }

    public OneCReconciliationDto getReconciliation(Long companyId, String employeeId1c, String dateFrom, String dateTo) {
        Company company = companyService.getById(companyId);
        return oneCClient.getReconciliation(company, employeeId1c, dateFrom, dateTo);
    }
}
