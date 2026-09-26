package uz.mxsell.workers_salary_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mxsell.workers_salary_platform.bot.BotManagerService;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.dto.api.NotificationRequest;
import uz.mxsell.workers_salary_platform.entity.Employee;
import uz.mxsell.workers_salary_platform.entity.NotificationLog;
import uz.mxsell.workers_salary_platform.entity.NotificationStatus;
import uz.mxsell.workers_salary_platform.repository.EmployeeRepository;
import uz.mxsell.workers_salary_platform.repository.NotificationLogRepository;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final EmployeeRepository employeeRepository;
    private final BotManagerService botManagerService;
    private final CompanyService companyService;

    public void sendNotification(Long companyId, NotificationRequest request) {
        Company company = companyService.getById(companyId);
        if (!company.isActive()) {
            log.warn("Notification skipped: company {} ({}) is inactive", companyId, company.getName());
            return;
        }

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository
                    .findByCompanyIdAndEmployeeId1c(companyId, request.getEmployeeId()).orElse(null);
            if (employee != null) {
                sendToEmployee(companyId, employee, request);
            } else {
                log.warn("Employee not found for notification: 1c_id={}", request.getEmployeeId());
            }
        } else if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
            for (String empId : request.getEmployeeIds()) {
                employeeRepository.findByCompanyIdAndEmployeeId1c(companyId, empId)
                        .ifPresent(employee -> sendToEmployee(companyId, employee, request));
            }
        } else if (request.getDepartmentId() != null) {
            List<Employee> employees = employeeRepository
                    .findByCompanyIdAndDepartmentAndIsDeletedFalse(companyId, request.getDepartmentId());
            for (Employee employee : employees) {
                sendToEmployee(companyId, employee, request);
            }
        } else if ("ALL".equalsIgnoreCase(request.getTarget())) {
            List<Employee> employees = employeeRepository.findAllByCompanyIdAndIsDeletedFalse(companyId);
            for (Employee employee : employees) {
                sendToEmployee(companyId, employee, request);
            }
        } else {
            log.warn("Notification request has no valid target");
        }
    }

    private void sendToEmployee(Long companyId, Employee employee, NotificationRequest request) {
        NotificationStatus status;
        try {
            TelegramLongPollingBot bot = botManagerService.getBotForCompany(companyId);
            if (bot == null) {
                throw new IllegalStateException("No active bot registered for company: " + companyId);
            }

            String detail = buildDetailBlock(request);
            String text = "📢 *" + escapeMarkdown(request.getTitle()) + "*\n\n" +
                         escapeMarkdown(request.getMessage()) +
                         (detail.isEmpty() ? "" : "\n\n" + detail);

            SendMessage message = SendMessage.builder()
                    .chatId(employee.getTelegramId().toString())
                    .text(text)
                    .parseMode("MarkdownV2")
                    .build();
            bot.execute(message);
            status = NotificationStatus.SENT;
            log.info("Notification sent to employee: id={}, telegramId={}",
                    employee.getId(), employee.getTelegramId());
        } catch (TelegramApiException | IllegalStateException e) {
            status = NotificationStatus.FAILED;
            log.error("Failed to send notification to employee: id={}, error={}",
                    employee.getId(), e.getMessage());
        }

        // Log the notification
        NotificationLog logEntry = new NotificationLog();
        logEntry.setCompanyId(companyId);
        logEntry.setEmployeeId(employee.getId());
        logEntry.setType(request.getType());
        logEntry.setTitle(request.getTitle());
        logEntry.setMessage(request.getMessage());
        logEntry.setObjectId(request.getObjectId());
        logEntry.setStatus(status);
        notificationLogRepository.save(logEntry);
    }

    private String buildDetailBlock(NotificationRequest r) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(r.getAdditional())) {
            sb.append("🔥 *Qo'shimcha ish*\n");
        }
        if (r.getOrder() != null) {
            sb.append("📦 Buyurtma: ").append(escapeMarkdown(r.getOrder())).append("\n");
        }
        if (r.getWork() != null) {
            sb.append("🔧 Ish: ").append(escapeMarkdown(r.getWork()));
        }
        if (r.getSubwork() != null && !r.getSubwork().isBlank()) {
            sb.append(" / ").append(escapeMarkdown(r.getSubwork()));
        }
        if (r.getWork() != null) {
            sb.append("\n");
        }
        if (r.getProduct() != null) {
            sb.append("🏷 Mahsulot: ").append(escapeMarkdown(r.getProduct())).append("\n");
        }
        if (r.getBatch() != null) {
            sb.append("📋 Partiya: ").append(escapeMarkdown(r.getBatch())).append("\n");
        }
        if (r.getColor() != null) {
            sb.append("🎨 Rang: ").append(escapeMarkdown(r.getColor())).append("\n");
        }
        if (r.getQty() != null) {
            sb.append("🔢 Miqdor: ").append(escapeMarkdown(formatAmount(r.getQty()))).append("\n");
        }
        if (r.getMachine() != null) {
            sb.append("⚙️ Stanok: ").append(escapeMarkdown(r.getMachine())).append("\n");
        }
        if (r.getPrice() != null) {
            sb.append("💵 Narx: ").append(escapeMarkdown(formatAmount(r.getPrice()))).append(" so'm\n");
        }
        if (r.getDefect() != null && r.getDefect() > 0) {
            sb.append("⚠️ Brak: ").append(escapeMarkdown(formatAmount(r.getDefect()))).append("\n");
        }
        if (r.getSum() != null) {
            sb.append("💰 Summa: ").append(escapeMarkdown(formatAmount(r.getSum()))).append(" so'm\n");
        }
        if (r.getAccrued() != null) {
            sb.append("💰 Hisoblangan: ").append(escapeMarkdown(formatAmount(r.getAccrued()))).append(" so'm\n");
        }
        if (r.getTax() != null) {
            sb.append("🏛 Soliq: ").append(escapeMarkdown(formatAmount(r.getTax()))).append(" so'm\n");
        }
        if (r.getNet() != null) {
            sb.append("💵 Qo'lga: ").append(escapeMarkdown(formatAmount(r.getNet()))).append(" so'm\n");
        }
        if (r.getPenalty() != null) {
            sb.append("⚠️ Jarima: ").append(escapeMarkdown(formatAmount(r.getPenalty()))).append(" so'm\n");
        }
        if (r.getComment() != null && !r.getComment().isBlank()) {
            sb.append("💬 Izoh: ").append(escapeMarkdown(r.getComment())).append("\n");
        }
        return sb.toString();
    }

    private String formatAmount(Double val) {
        return String.format(Locale.US, "%,.2f", val);
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                   .replace("*", "\\*")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("~", "\\~")
                   .replace("`", "\\`")
                   .replace(">", "\\>")
                   .replace("#", "\\#")
                   .replace("+", "\\+")
                   .replace("-", "\\-")
                   .replace("=", "\\=")
                   .replace("|", "\\|")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace(".", "\\.")
                   .replace("!", "\\!");
    }
}
