package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.mxsell.workers_salary_platform.client.HrPulseClient;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.hrpulse.HrPulseAttendanceDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final CompanyService companyService;
    private final HrPulseClient hrPulseClient;

    // ── public methods ───────────────────────────────────────────────────────

    /** Returns today's attendance record, or empty if none found. */
    public Optional<HrPulseAttendanceDto> getToday(Long companyId, String hrpulseEmployeeId) {
        Company company = companyService.getById(companyId);
        LocalDate today = LocalDate.now();
        List<HrPulseAttendanceDto> list = hrPulseClient.getAttendances(company, hrpulseEmployeeId, today, today);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Returns all attendance rows for the given year/month.
     * Splits into two requests if the month has more than 31 days (it never does,
     * but kept safe). In practice one request covers any calendar month ≤ 31 days.
     */
    public List<HrPulseAttendanceDto> getMonthly(Long companyId, String hrpulseEmployeeId, int year, int month) {
        Company company = companyService.getById(companyId);
        LocalDate start = LocalDate.of(year, month, 1);
        // end = last day of month, but never later than today (no future records)
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        if (end.isAfter(LocalDate.now())) {
            end = LocalDate.now();
        }
        return hrPulseClient.getAttendances(company, hrpulseEmployeeId, start, end);
    }

    // ── calculation helpers (called by UpdateDispatcher) ─────────────────────

    /**
     * How many minutes late the employee arrived compared to work_time.start.
     * Returns 0 if on time or early.
     */
    public int calcLatenessMinutes(HrPulseAttendanceDto record) {
        if (record.getCheckIn() == null || record.getWorkTime() == null)
            return 0;
        try {
            LocalTime checkIn = parseTime(record.getCheckIn());
            LocalTime scheduleStart = LocalTime.parse(record.getWorkTime().getStart()); // "HH:mm:ss"
            if (checkIn.isAfter(scheduleStart)) {
                return (int) java.time.Duration.between(scheduleStart, checkIn).toMinutes();
            }
        } catch (DateTimeParseException e) {
            log.warn("Could not parse times for lateness calc: checkIn={}, schedule={}",
                    record.getCheckIn(), record.getWorkTime().getStart());
        }
        return 0;
    }

    /**
     * How many minutes early the employee left compared to work_time.end.
     * Returns 0 if left on time or late.
     */
    public int calcEarlyLeaveMinutes(HrPulseAttendanceDto record) {
        if (record.getCheckOut() == null || record.getWorkTime() == null)
            return 0;
        try {
            LocalTime checkOut = parseTime(record.getCheckOut());
            LocalTime scheduleEnd = LocalTime.parse(record.getWorkTime().getEnd()); // "HH:mm:ss"
            if (checkOut.isBefore(scheduleEnd)) {
                return (int) java.time.Duration.between(checkOut, scheduleEnd).toMinutes();
            }
        } catch (DateTimeParseException e) {
            log.warn("Could not parse times for early-leave calc: checkOut={}, schedule={}",
                    record.getCheckOut(), record.getWorkTime().getEnd());
        }
        return 0;
    }

    /**
     * Human-readable status label + emoji for a single attendance record.
     * Priority: skipWorkday → notMarked (absent) → AT_WORK → late/early
     * combinations → present
     */
    public String statusLabel(HrPulseAttendanceDto record) {
        if (record.isSkipWorkday())
            return "😴 Dam olish kuni";
        if (record.isNotMarked())
            return "❌ Kelmagan";
        if (record.getCheckIn() != null && record.getCheckOut() == null)
            return "🔵 Ishda";

        int late = calcLatenessMinutes(record);
        int early = calcEarlyLeaveMinutes(record);

        if (late > 0 && early > 0)
            return "⚠️↩️ Kechikkan + erta ketgan";
        if (late > 0)
            return "⚠️ Kechikkan";
        if (early > 0)
            return "↩️ Erta ketgan";
        return "✅ Kelgan";
    }

    /**
     * Extracts HH:mm time string from an ISO-8601 offset datetime or plain "HH:mm"
     * / "HH:mm:ss".
     */
    public String formatTime(String isoOrTime) {
        if (isoOrTime == null)
            return "—";
        try {
            OffsetDateTime odt = OffsetDateTime.parse(isoOrTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException ignored) {
        }
        // fall back: treat as "HH:mm:ss" or "HH:mm"
        try {
            LocalTime t = LocalTime.parse(isoOrTime);
            return t.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            return isoOrTime;
        }
    }

    /** Formats "YYYY-MM-DD" to "DD.MM.YYYY". */
    public String formatDate(String iso) {
        if (iso == null)
            return "—";
        try {
            LocalDate d = LocalDate.parse(iso);
            return d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private LocalTime parseTime(String isoOrTime) {
        try {
            return OffsetDateTime.parse(isoOrTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalTime();
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(isoOrTime); // "HH:mm" or "HH:mm:ss"
        }
    }
}
