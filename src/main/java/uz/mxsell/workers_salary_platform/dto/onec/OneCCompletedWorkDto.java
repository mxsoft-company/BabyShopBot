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
public class OneCCompletedWorkDto {

    @JsonProperty("work_id")
    private String workId;

    @JsonProperty("task_id")
    private String taskId;

    private String date;
    private String order;
    private String work;
    private String subwork;
    private String product;
    private String batch;
    private String color;
    private Double qty;
    private Double defect;
    private Double price;
    private Double sum;
}
