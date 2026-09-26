package uz.mxsell.workers_salary_platform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.dto.api.AppealMessageRequest;
import uz.mxsell.workers_salary_platform.dto.api.ErrorResponse;
import uz.mxsell.workers_salary_platform.dto.api.NotificationRequest;
import uz.mxsell.workers_salary_platform.entity.Appeal;
import uz.mxsell.workers_salary_platform.entity.Employee;
import uz.mxsell.workers_salary_platform.exception.AppealNotFoundException;
import uz.mxsell.workers_salary_platform.repository.EmployeeRepository;
import uz.mxsell.workers_salary_platform.service.AppealService;
import uz.mxsell.workers_salary_platform.service.NotificationService;

@Slf4j
@RestController
@RequestMapping("/api/{slug}/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    @PostMapping("/{oneCAppealId}/message")
    public ResponseEntity<?> addMessage(@PathVariable String slug,
            @PathVariable String oneCAppealId,
            @Valid @RequestBody AppealMessageRequest request,
            @RequestAttribute("company") Company company) {

        try {
            appealService.addManagementMessage(company.getId(), oneCAppealId, request.getMessage());

            Appeal appeal = appealService.getAppealByOneCId(company.getId(), oneCAppealId);
            employeeRepository.findById(appeal.getEmployeeId()).ifPresentOrElse(
                    employee -> sendResponseNotification(company, appeal, employee, request.getMessage()),
                    () -> log.warn("Employee not found for appeal notification: appealId={}, employeeId={}",
                            appeal.getId(), appeal.getEmployeeId()));

            return ResponseEntity.ok().build();
        } catch (AppealNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage()));
        }
    }

    private void sendResponseNotification(Company company, Appeal appeal, Employee employee, String responseText) {
        String typeLabel = switch (appeal.getType()) {
            case WORK -> "Ish bo'yicha";
            case FINANCE -> "Moliya bo'yicha";
            case PENALTY -> "Jarima bo'yicha";
            case PRODUCT -> "Mahsulot bo'yicha";
            default -> "Boshqa";
        };

        NotificationRequest notification = new NotificationRequest();
        notification.setEmployeeId(employee.getEmployeeId1c());
        notification.setType("APPEAL_RESPONSE");
        notification.setTitle("📢 Murojaatingizga javob berildi (" + typeLabel + ")");
        notification.setMessage("Sizning murojaatingiz:\n\"" + appeal.getMessage() + "\"\n\nJavob:\n" + responseText);
        notification.setObjectId(oneCObjectId(appeal));
        notificationService.sendNotification(company.getId(), notification);
    }

    private String oneCObjectId(Appeal appeal) {
        return appeal.getOneCAppealId();
    }
}
