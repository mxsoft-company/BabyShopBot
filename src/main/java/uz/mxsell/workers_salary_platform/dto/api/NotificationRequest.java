package uz.mxsell.workers_salary_platform.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequest {
    private String employeeId;
    private java.util.List<String> employeeIds;
    private String departmentId;
    private String target;
    @NotBlank(message = "Tur ko'rsatilishi shart")
    private String type;
    @NotBlank(message = "Sarlavha bo'sh bo'lmasligi kerak")
    private String title;
    @NotBlank(message = "Xabar matni bo'sh bo'lmasligi kerak")
    private String message;
    private String objectId;

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("work_id")
    private String workId;

    @JsonProperty("document_id")
    private String documentId;

    private String order;
    private String work;
    private String subwork;
    private String product;
    private String batch;
    private String color;
    private Double qty;
    private String machine;
    private Double price;
    private Double sum;
    private Double defect;
    private String comment;
    private Boolean additional;
    private Double accrued;
    private Double tax;
    private Double net;
    private Double penalty;
}
