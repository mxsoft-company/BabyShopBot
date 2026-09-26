package uz.mxsell.workers_salary_platform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.dto.api.NotificationRequest;
import uz.mxsell.workers_salary_platform.service.NotificationService;

@RestController
@RequestMapping("/api/{slug}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(@PathVariable String slug,
            @Valid @RequestBody NotificationRequest request,
            @RequestAttribute("company") Company company) {
        notificationService.sendNotification(company.getId(), request);
        return ResponseEntity.ok().build();
    }
}
