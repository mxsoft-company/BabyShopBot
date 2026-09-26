package uz.mxsell.workers_salary_platform.dto.onec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OneCReconciliationDto {

    @JsonProperty("employee_id")
    private String employeeId;

    private String name;

    @JsonProperty("date_from")
    private String dateFrom;

    @JsonProperty("date_to")
    private String dateTo;

    private Double accrued;
    private Double paid;
    private Double tax;
    private Double penalty;
    private Double balance;

    private List<OneCReconciliationRowDto> rows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OneCReconciliationRowDto {
        private String date;
        private String type;

        @JsonProperty("document_id")
        private String documentId;

        private String description;
        private String product;
        private Double qty;
        private Double price;
        private Double amount;
        private String currency;
    }
}
