package uz.mxsell.workers_salary_platform.dto.hrpulse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HrPulseAttendanceDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    @JsonProperty("employee_id")
    private String employeeId;

    /** ISO-8601 datetime with timezone, e.g. "2026-09-17T08:57:00+05:00" */
    @JsonProperty("check_in")
    private String checkIn;

    /** ISO-8601 datetime with timezone, or null if still at work */
    @JsonProperty("check_out")
    private String checkOut;

    /** True when the employee has no attendance record for an expected workday */
    @JsonProperty("not_marked")
    private boolean notMarked;

    /**
     * True when this day is excluded from work calculations (holiday override,
     * etc.)
     */
    @JsonProperty("skip_workday")
    private boolean skipWorkday;

    @JsonProperty("is_full_salary_policy_active")
    private boolean isFullSalaryPolicyActive;

    /** Primary work schedule for the day */
    @JsonProperty("work_time")
    private WorkTime workTime;

    /** All applicable work schedules (may contain multiple) */
    @JsonProperty("work_times")
    private List<WorkTime> workTimes;

    @Data
    public static class WorkTime {
        /** "HH:mm:ss" format, e.g. "09:00:00" */
        @JsonProperty("start")
        private String start;

        /** "HH:mm:ss" format, e.g. "18:00:00" */
        @JsonProperty("end")
        private String end;
    }
}
