package uz.mxsell.workers_salary_platform.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.dto.onec.OneCAppealResponse;
import uz.mxsell.workers_salary_platform.dto.onec.OneCBalanceDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCCompletedWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCEmployeeInfoDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPaymentDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPenaltyDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPendingWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCProductDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCReconciliationDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCRegisterResponse;
import uz.mxsell.workers_salary_platform.dto.onec.OneCTaxDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCWorkDetailDto;
import uz.mxsell.workers_salary_platform.exception.OneCCommunicationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OneCClient {

    private final RestTemplate oneCRestTemplate;

    private HttpHeaders authHeaders(Company company) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(company.getOnecUsername(), company.getOnecPassword());
        return headers;
    }

    private <T> T getOrNull(Company company, String url, Class<T> responseType) {
        try {
            ResponseEntity<T> response = oneCRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(company)), responseType);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new OneCCommunicationException("Ma'lumot olishda xatolik", e);
        }
    }

    private <T> List<T> getListOrEmpty(Company company, String url, ParameterizedTypeReference<List<T>> responseType) {
        try {
            ResponseEntity<List<T>> response = oneCRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders(company)), responseType);
            List<T> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new OneCCommunicationException("Ma'lumot olishda xatolik", e);
        }
    }

    private <T> T postOrNull(Company company, String url, Object body, Class<T> responseType) {
        try {
            HttpHeaders headers = authHeaders(company);
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<T> response = oneCRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new OneCCommunicationException("Ma'lumot olishda xatolik", e);
        }
    }

    // ==================== Auth / Employee ====================

    public OneCRegisterResponse checkEmployee(Company company, Long telegramId, String phone) {
        String url = company.getOnecBaseUrl() + "/api/auth/register/";
        Map<String, Object> body = Map.of("telegram_id", telegramId, "phone", phone);
        return postOrNull(company, url, body, OneCRegisterResponse.class);
    }

    public OneCEmployeeInfoDto getEmployeeInfo(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/employees/getEmployeeInfo/")
                .queryParam("employee_id", employeeId1c).toUriString();
        return getOrNull(company, url, OneCEmployeeInfoDto.class);
    }

    // ==================== Works ====================

    public List<OneCPendingWorkDto> getPendingWorks(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/works/getWorksPending/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public List<OneCCompletedWorkDto> getCompletedWorks(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/works/getWorksCompleted/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public OneCWorkDetailDto getWorkById(Company company, String employeeId1c, String workId) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/works/getWork/")
                .queryParam("employee_id", employeeId1c)
                .queryParam("work_id", workId)
                .toUriString();
        return getOrNull(company, url, OneCWorkDetailDto.class);
    }

    // ==================== Finance ====================

    public List<OneCPaymentDto> getPayments(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeePayments/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public List<OneCPenaltyDto> getPenalties(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeePenalties/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public List<OneCTaxDto> getTax(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeeTax/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public List<OneCProductDto> getProducts(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeeProducts/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getListOrEmpty(company, url, new ParameterizedTypeReference<>() {});
    }

    public OneCBalanceDto getBalance(Company company, String employeeId1c) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeeBalance/")
                .queryParam("employee_id", employeeId1c)
                .toUriString();
        return getOrNull(company, url, OneCBalanceDto.class);
    }

    public OneCReconciliationDto getReconciliation(Company company, String employeeId1c, String dateFrom, String dateTo) {
        String url = UriComponentsBuilder.fromUriString(company.getOnecBaseUrl() + "/api/finance/getEmployeeReconciliation/")
                .queryParam("employee_id", employeeId1c)
                .queryParam("date_from", dateFrom)
                .queryParam("date_to", dateTo)
                .toUriString();
        return getOrNull(company, url, OneCReconciliationDto.class);
    }

    // ==================== Appeals ====================

    public OneCAppealResponse createAppeal(Company company, String employeeId1c, String type, String message) {
        String url = company.getOnecBaseUrl() + "/api/appeals/createAppeal/";
        Map<String, Object> body = Map.of("employee_id", employeeId1c, "type", type, "message", message);
        return postOrNull(company, url, body, OneCAppealResponse.class);
    }
}
