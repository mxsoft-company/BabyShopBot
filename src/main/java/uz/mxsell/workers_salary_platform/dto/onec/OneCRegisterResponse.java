package uz.mxsell.workers_salary_platform.dto.onec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OneCRegisterResponse {
    private boolean success;

    @JsonProperty("employee_id")
    private String employeeId;

    private String name;
}
