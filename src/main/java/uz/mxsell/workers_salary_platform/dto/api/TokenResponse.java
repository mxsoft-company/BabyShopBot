package uz.mxsell.workers_salary_platform.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String token;
    private String type;
    private long expiresIn;

    public static TokenResponse of(String token, long expiresIn) {
        return new TokenResponse(token, "Bearer", expiresIn);
    }
}
