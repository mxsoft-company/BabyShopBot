package uz.mxsell.workers_salary_platform.dto.onec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OneCAppealResponse {

    private boolean success;

    @JsonProperty("appeal_id")
    private String appealId;

    private String type;

    private String status;

    @JsonProperty("created_at")
    private String createdAt;
}
