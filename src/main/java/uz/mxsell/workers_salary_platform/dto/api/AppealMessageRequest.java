package uz.mxsell.workers_salary_platform.dto.api;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppealMessageRequest {
    @NotBlank(message = "Xabar matni bo'sh bo'lmasligi kerak")
    private String message;
}
