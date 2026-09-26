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
public class OneCEmployeeInfoDto {

    @JsonProperty("employee_id")
    private String employeeId;

    private String name;

    private String phone;

    // @JsonIgnore: qo'shimcha getTelegramId() metodi yozilmaydi -- kerak bo'lmasa xatoga yo'l qo'ymaymiz
    @JsonProperty("telegram_id")
    private String telegramIdRaw;

    // 1C dagi imlo xatosi ataylab saqlanadi
    @JsonProperty("emploee_number")
    private String employeeNumber;

    // 1C hali yubarmasligi mumkin -- kelmasa shunchaki null qoladi
    @JsonProperty("hrpulse_employee_id")
    private String hrpulseEmployeeId;

    @JsonProperty("parent")
    private String department;
}
