package uz.mxsell.workers_salary_platform.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.dto.hrpulse.HrPulseAttendanceDto;
import uz.mxsell.workers_salary_platform.exception.OneCCommunicationException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client for the HR Pulse REST API.
 * Auth: rest_token query parameter (no headers).
 *
 * NOTE: uses the existing oneCRestTemplate bean — network/timeout settings are
 * identical for both integrations. If different timeouts are ever needed a
 * separate RestTemplate bean can be introduced.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrPulseClient {

    private final RestTemplate oneCRestTemplate;

    /**
     * Returns attendance records for the given employee within [startDate,
     * endDate].
     * The range MUST NOT exceed 31 days (HR Pulse API constraint).
     *
     * @throws ResourceAccessException    on network / timeout errors
     * @throws OneCCommunicationException on unexpected HTTP errors
     */
    public List<HrPulseAttendanceDto> getAttendances(Company company,
            String hrpulseEmployeeId,
            LocalDate startDate,
            LocalDate endDate) {
        String url = UriComponentsBuilder
                .fromUriString(company.getHrpulseBaseUrl() + "/restapi/attendances")
                .queryParam("employee", hrpulseEmployeeId)
                .queryParam("start_date", startDate.toString())
                .queryParam("end_date", endDate.toString())
                .queryParam("rest_token", company.getHrpulseToken())
                .toUriString();

        log.info("HR Pulse so'rovi: {}", maskToken(url));

        try {
            ResponseEntity<List<HrPulseAttendanceDto>> response = oneCRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    });
            List<HrPulseAttendanceDto> body = response.getBody();
            log.info("HR Pulse javobi: {} ta yozuv qaytdi", body != null ? body.size() : 0);
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("HR Pulse javobi: 404 — yozuv topilmadi");
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new OneCCommunicationException("HR Pulse dan ma'lumot olishda xatolik", e);
        }
    }

    /** rest_token qiymatini yashiradi: oxirgi 4 belgidan boshqalari "***" bilan almashtiriladi. */
    private String maskToken(String url) {
        String prefix = "rest_token=";
        int idx = url.indexOf(prefix);
        if (idx < 0) {
            return url;
        }
        String token = url.substring(idx + prefix.length());
        String masked = token.length() <= 4 ? "***" : "***" + token.substring(token.length() - 4);
        return url.substring(0, idx + prefix.length()) + masked;
    }
}
