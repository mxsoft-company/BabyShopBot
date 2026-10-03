package uz.mxsell.workers_salary_platform.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mxsell.workers_salary_platform.bot.keyboards.InlineKeyboardBuilder;
import uz.mxsell.workers_salary_platform.bot.keyboards.ReplyKeyboardBuilder;
import uz.mxsell.workers_salary_platform.bot.state.BotState;
import uz.mxsell.workers_salary_platform.bot.state.ChatKey;
import uz.mxsell.workers_salary_platform.bot.state.UserStateManager;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.dto.onec.OneCBalanceDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCCompletedWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPaymentDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPenaltyDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCPendingWorkDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCProductDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCReconciliationDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCTaxDto;
import uz.mxsell.workers_salary_platform.dto.onec.OneCWorkDetailDto;
import uz.mxsell.workers_salary_platform.entity.AppealType;
import uz.mxsell.workers_salary_platform.entity.Employee;
import uz.mxsell.workers_salary_platform.exception.OneCCommunicationException;
import uz.mxsell.workers_salary_platform.service.AppealService;
import uz.mxsell.workers_salary_platform.service.AttendanceService;
import uz.mxsell.workers_salary_platform.service.EmployeeService;
import uz.mxsell.workers_salary_platform.service.EmployeeService.RegistrationResult;
import uz.mxsell.workers_salary_platform.service.FinanceService;
import uz.mxsell.workers_salary_platform.service.PdfService;
import uz.mxsell.workers_salary_platform.service.WorksService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateDispatcher {

    private static final int PAGE_SIZE = 5;

    private final EmployeeService employeeService;
    private final WorksService worksService;
    private final FinanceService financeService;
    private final PdfService pdfService;
    private final AppealService appealService;
    private final AttendanceService attendanceService;
    private final UserStateManager userStateManager;
    private final ReplyKeyboardBuilder replyKeyboardBuilder;
    private final InlineKeyboardBuilder inlineKeyboardBuilder;

    public void dispatch(Company company, TelegramLongPollingBot bot, Update update) {
        if (!company.isActive()) {
            return;
        }
        Long chatId = resolveChatId(update);
        if (chatId == null) {
            return;
        }
        ChatKey key = new ChatKey(company.getId(), chatId);

        try {
            if (update.hasCallbackQuery()) {
                handleCallback(company, bot, chatId, key, update.getCallbackQuery().getData());
                return;
            }

            if (update.hasMessage() && update.getMessage().hasContact()) {
                handleContact(company, bot, chatId, key, update.getMessage().getContact().getPhoneNumber());
                return;
            }

            if (update.hasMessage() && "/start".equals(update.getMessage().getText())) {
                handleStart(company, bot, chatId, key);
                return;
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();

                if (userStateManager.getState(key) == BotState.APPEAL_ENTER_TEXT) {
                    handleAppealText(bot, chatId, key, text);
                    return;
                }

                if (userStateManager.getState(key) == BotState.ATTENDANCE_ENTER_ID) {
                    handleAttendanceIdEntry(company, bot, chatId, key, text);
                    return;
                }

                switch (text) {
                    case "🏭 Mening ishlarim" -> {
                        userStateManager.setState(key, BotState.WORKS_MENU);
                        send(bot, chatId, "🏭 Ishlar bo'limi", replyKeyboardBuilder.worksMenuKeyboard());
                    }
                    case "💰 Hisob-kitobim" -> {
                        userStateManager.setState(key, BotState.FINANCE_MENU);
                        send(bot, chatId, "💰 Hisob-kitob bo'limi", replyKeyboardBuilder.financeMenuKeyboard());
                    }
                    case "🕐 Davomatim" -> handleAttendanceMenu(company, bot, chatId, key);
                    case "📅 Bugun" -> showAttendanceToday(company, bot, chatId);
                    case "📆 Oylik davomat" -> showAttendanceMonthly(company, bot, chatId);
                    case "🔄 ID ni o'zgartirish" -> {
                        userStateManager.setState(key, BotState.ATTENDANCE_ENTER_ID);
                        send(bot, chatId, "Iltimos, HR Pulse tizimidagi yangi xodim ID raqamingizni kiriting:");
                    }
                    case "⬅️ Ortga" -> {
                        userStateManager.setState(key, BotState.MAIN_MENU);
                        send(bot, chatId, "📌 Asosiy menyu", replyKeyboardBuilder.mainMenuKeyboard(company));
                    }
                    case "👤 Profilim" -> showProfile(company, bot, chatId);
                    case "🕐 Kutilayotgan ishlar" -> showPendingWorks(company, bot, chatId, 0);
                    case "✅ Bajarilgan ishlar" -> showCompletedWorks(company, bot, chatId, 0);
                    case "💵 To'lovlar" -> showPayments(company, bot, chatId);
                    case "⚠️ Jarimalar" -> showPenalties(company, bot, chatId);
                    case "🧾 Soliq (NDFL)" -> showTax(company, bot, chatId);
                    case "🛒 Mahsulotlar" -> showProducts(company, bot, chatId);
                    case "📊 Akt sverka" -> promptReconciliationPeriod(company, bot, chatId);
                    case "💰 Balans" -> showBalance(company, bot, chatId);
                    case "📩 Rahbariyatga murojaat" -> {
                        Employee employee = requireEmployee(company, bot, chatId);
                        if (employee == null) {
                            return;
                        }
                        userStateManager.setState(key, BotState.APPEAL_SELECT_TYPE);
                        send(bot, chatId, "📩 Murojaat turini tanlang:",
                                inlineKeyboardBuilder.appealTypeKeyboard());
                    }
                    default -> {
                    }
                }
            }
        } catch (Exception e) {
            log.error("Dispatch error for company {}: {}", company.getId(), e.getMessage(), e);
            try {
                send(bot, chatId, "⚠️ Xatolik yuz berdi. Iltimos, qaytadan urinib ko'ring.");
            } catch (Exception ex) {
                log.error("Error sending failure message: {}", ex.getMessage());
            }
        }
    }

    private void handleCallback(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key, String data)
            throws TelegramApiException {
        if ("profile:logout".equals(data)) {
            employeeService.logout(company.getId(), chatId);
            userStateManager.setState(key, BotState.START);
            send(bot, chatId, "Iltimos, telefon raqamingizni yuboring:",
                    replyKeyboardBuilder.contactRequestKeyboard());
            return;
        }
        if ("noop".equals(data)) {
            return;
        }
        if (data.startsWith("works:pending:page:")) {
            showPendingWorks(company, bot, chatId, parsePage(data.substring("works:pending:page:".length())));
            return;
        }
        if (data.startsWith("works:completed:page:")) {
            showCompletedWorks(company, bot, chatId, parsePage(data.substring("works:completed:page:".length())));
            return;
        }
        if (data.startsWith("work:detail:")) {
            showWorkDetail(company, bot, chatId, data.substring("work:detail:".length()));
            return;
        }
        if (data.startsWith("appeal:type:")) {
            handleAppealTypeSelected(bot, chatId, key, data.substring("appeal:type:".length()));
            return;
        }
        if ("appeal:confirm".equals(data)) {
            handleAppealConfirm(company, bot, chatId, key);
            return;
        }
        if (data.startsWith("finance:reconciliation:period:")) {
            String period = data.substring("finance:reconciliation:period:".length());
            LocalDate[] range = resolveReconciliationPeriod(period);
            showReconciliation(company, bot, chatId, range[0], range[1]);
            return;
        }
        if (data.startsWith("finance:reconciliation:pdf:")) {
            String[] parts = data.substring("finance:reconciliation:pdf:".length()).split(":");
            sendReconciliationPdf(company, bot, chatId, parts[0], parts[1]);
            return;
        }
        if ("appeal:cancel".equals(data)) {
            userStateManager.clearPendingAppeal(key);
            userStateManager.setState(key, BotState.MAIN_MENU);
            send(bot, chatId, "❌ Murojaat bekor qilindi", replyKeyboardBuilder.mainMenuKeyboard(company));
        }
    }

    private int parsePage(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleStart(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key)
            throws TelegramApiException {
        Optional<Employee> existing = employeeService.findByTelegramId(company.getId(), chatId);
        if (existing.isPresent()) {
            userStateManager.setState(key, BotState.MAIN_MENU);
            send(bot, chatId, "📌 Asosiy menyu", replyKeyboardBuilder.mainMenuKeyboard(company));
        } else {
            userStateManager.setState(key, BotState.AWAITING_CONTACT);
            send(bot, chatId, "Assalomu alaykum! 👋\n\n" +
                    "Korxona xodimlari uchun botga xush kelibsiz.\n" +
                    "Ro'yxatdan o'tish uchun telefon raqamingizni yuboring:",
                    replyKeyboardBuilder.contactRequestKeyboard());
        }
    }

    private void handleContact(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key, String rawPhone)
            throws TelegramApiException {
        String phone = rawPhone != null && !rawPhone.startsWith("+") ? "+" + rawPhone : rawPhone;
        try {
            RegistrationResult result = employeeService.registerEmployee(company.getId(), chatId, phone);
            switch (result.status()) {
                case SUCCESS -> {
                    userStateManager.setState(key, BotState.MAIN_MENU);
                    send(bot, chatId, result.message(), replyKeyboardBuilder.mainMenuKeyboard(company));
                }
                case ALREADY_REGISTERED -> {
                    userStateManager.setState(key, BotState.MAIN_MENU);
                    send(bot, chatId, result.message());
                }
                case NOT_FOUND -> {
                    userStateManager.setState(key, BotState.START);
                    send(bot, chatId, result.message());
                }
            }
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error during registration for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showProfile(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Optional<Employee> existing = employeeService.findByTelegramId(company.getId(), chatId);
        if (existing.isEmpty()) {
            send(bot, chatId, "Iltimos, avval ro'yxatdan o'ting: /start");
            return;
        }
        Employee employee = existing.get();

        StringBuilder sb = new StringBuilder("👤 Profil\n\n");
        sb.append("👤 Ism: ").append(employee.getFullName()).append("\n");
        sb.append("🏢 Bo'lim: ")
                .append(employee.getDepartment() != null ? employee.getDepartment() : "—").append("\n");
        sb.append("💼 Xodim raqami: ")
                .append(employee.getEmployeeNumber() != null ? employee.getEmployeeNumber() : "—");

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(sb.toString())
                .replyMarkup(inlineKeyboardBuilder.profileLogoutKeyboard())
                .build();
        bot.execute(message);
    }

    // ==================== Works ====================

    private void showPendingWorks(Company company, TelegramLongPollingBot bot, Long chatId, int page)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCPendingWorkDto> works = worksService.getPendingWorks(company.getId(), employee.getEmployeeId1c());

            if (works.isEmpty()) {
                send(bot, chatId, "🕐 Kutilayotgan ishlar topilmadi.");
                return;
            }

            int totalPages = (int) Math.ceil((double) works.size() / PAGE_SIZE);
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int start = safePage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, works.size());
            List<OneCPendingWorkDto> pageWorks = works.subList(start, end);

            StringBuilder sb = new StringBuilder();
            sb.append("🕐 Kutilayotgan ishlar (").append(works.size()).append(" ta)\n\n");
            for (int i = 0; i < pageWorks.size(); i++) {
                OneCPendingWorkDto work = pageWorks.get(i);
                sb.append(start + i + 1).append(". ");
                sb.append(work.getProduct() != null ? work.getProduct() : "—").append("\n");
                appendWorkName(sb, work.getWork(), work.getSubwork());
                if (work.getBatch() != null && !work.getBatch().isEmpty()) {
                    sb.append("   📦 Partiya: ").append(work.getBatch()).append("\n");
                }
                if (work.getColor() != null && !work.getColor().isEmpty()) {
                    sb.append("   🎨 Rang: ").append(work.getColor()).append("\n");
                }
                if (work.getQty() != null) {
                    sb.append("   📊 Miqdor: ").append(String.format("%,.2f", work.getQty())).append("\n");
                }
                if (work.getOrder() != null) {
                    sb.append("   📋 ").append(work.getOrder()).append("\n");
                }
                sb.append("\n");
            }

            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(sb.toString())
                    .replyMarkup(inlineKeyboardBuilder.pendingWorksPaginationKeyboard(safePage, totalPages))
                    .build());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (pending works) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showCompletedWorks(Company company, TelegramLongPollingBot bot, Long chatId, int page)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCCompletedWorkDto> works = worksService.getCompletedWorks(company.getId(),
                    employee.getEmployeeId1c());

            if (works.isEmpty()) {
                send(bot, chatId, "✅ Bajarilgan ishlar topilmadi.");
                return;
            }

            int totalPages = (int) Math.ceil((double) works.size() / PAGE_SIZE);
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int start = safePage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, works.size());
            List<OneCCompletedWorkDto> pageWorks = works.subList(start, end);

            StringBuilder sb = new StringBuilder();
            sb.append("✅ Bajarilgan ishlar (").append(works.size()).append(" ta)\n\n");
            for (int i = 0; i < pageWorks.size(); i++) {
                OneCCompletedWorkDto work = pageWorks.get(i);
                sb.append(start + i + 1).append(". ");
                sb.append(work.getProduct() != null ? work.getProduct() : "—").append("\n");
                appendWorkName(sb, work.getWork(), work.getSubwork());
                if (work.getBatch() != null && !work.getBatch().isEmpty()) {
                    sb.append("   📦 Partiya: ").append(work.getBatch()).append("\n");
                }
                if (work.getColor() != null && !work.getColor().isEmpty()) {
                    sb.append("   🎨 Rang: ").append(work.getColor()).append("\n");
                }
                if (work.getQty() != null) {
                    sb.append("   📊 Miqdor: ").append(String.format("%,.2f", work.getQty())).append("\n");
                }
                if (work.getSum() != null) {
                    sb.append("   💵 Summa: ").append(String.format("%,.2f", work.getSum())).append(" so'm\n");
                }
                if (work.getDefect() != null && work.getDefect() > 0) {
                    sb.append("   ⚠️ Brak: ").append(String.format("%,.2f", work.getDefect())).append("\n");
                }
                if (work.getPrice() != null) {
                    sb.append("   💰 Narx: ").append(String.format("%,.2f", work.getPrice())).append(" so'm\n");
                }
                if (work.getOrder() != null) {
                    sb.append("   📋 ").append(work.getOrder()).append("\n");
                }
                sb.append("\n");
            }

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (OneCCompletedWorkDto work : pageWorks) {
                rows.add(List.of(inlineKeyboardBuilder.workDetailButton(work.getWorkId())));
            }
            InlineKeyboardMarkup pagination = inlineKeyboardBuilder.completedWorksPaginationKeyboard(safePage,
                    totalPages);
            rows.addAll(pagination.getKeyboard());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(sb.toString())
                    .replyMarkup(markup)
                    .build());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (completed works) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showWorkDetail(Company company, TelegramLongPollingBot bot, Long chatId, String workId)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            OneCWorkDetailDto work = worksService.getWorkById(company.getId(), employee.getEmployeeId1c(), workId);
            if (work == null) {
                send(bot, chatId, "Bunday ish topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("📋 Ish Tafsiloti\n\n");
            sb.append("📋 Buyurtma: ").append(work.getOrder() != null ? work.getOrder() : "—").append("\n");
            sb.append("🔧 Ish: ").append(work.getWork() != null ? work.getWork() : "—").append("\n");
            if (work.getSubwork() != null && !work.getSubwork().isEmpty()) {
                sb.append("🔹 Qism ish: ").append(work.getSubwork()).append("\n");
            }
            sb.append("📦 Mahsulot: ").append(work.getProduct() != null ? work.getProduct() : "—").append("\n");
            if (work.getComment() != null && !work.getComment().isEmpty()) {
                sb.append("💬 Izoh: ").append(work.getComment());
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (work detail) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    // ==================== Finance ====================

    private void showPayments(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCPaymentDto> payments = financeService.getPayments(company.getId(), employee.getEmployeeId1c());

            if (payments.isEmpty()) {
                send(bot, chatId, "💵 To'lovlar topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("💵 To'lovlar (").append(payments.size()).append(" ta)\n\n");
            for (int i = 0; i < payments.size(); i++) {
                OneCPaymentDto p = payments.get(i);
                sb.append(i + 1).append(". To'lov\n");
                if (p.getDate() != null) {
                    sb.append("   📅 ").append(p.getDate()).append("\n");
                }
                sb.append("   💵 ").append(p.getAmount() != null ? String.format("%,.2f", p.getAmount()) : "0.00");
                if (p.getCurrency() != null) {
                    sb.append(" ").append(p.getCurrency());
                }
                sb.append("\n\n");
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (payments) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showPenalties(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCPenaltyDto> penalties = financeService.getPenalties(company.getId(), employee.getEmployeeId1c());

            if (penalties.isEmpty()) {
                send(bot, chatId, "⚠️ Jarimalar topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("⚠️ Jarimalar (").append(penalties.size()).append(" ta)\n\n");
            for (int i = 0; i < penalties.size(); i++) {
                OneCPenaltyDto p = penalties.get(i);
                sb.append(i + 1).append(". Jarima\n");
                if (p.getDate() != null) {
                    sb.append("   📅 ").append(p.getDate()).append("\n");
                }
                sb.append("   💵 ").append(p.getAmount() != null ? String.format("%,.2f", p.getAmount()) : "0.00")
                        .append(" so'm\n\n");
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (penalties) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showTax(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCTaxDto> taxes = financeService.getTax(company.getId(), employee.getEmployeeId1c());

            if (taxes.isEmpty()) {
                send(bot, chatId, "🧾 Soliqlar topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("🧾 Soliqlar (").append(taxes.size()).append(" ta)\n\n");
            for (int i = 0; i < taxes.size(); i++) {
                OneCTaxDto t = taxes.get(i);
                sb.append(i + 1).append(". Soliq\n");
                if (t.getDate() != null) {
                    sb.append("   📅 ").append(t.getDate()).append("\n");
                }
                sb.append("   💵 ").append(t.getAmount() != null ? String.format("%,.2f", t.getAmount()) : "0.00")
                        .append(" so'm\n\n");
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (tax) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void showProducts(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            List<OneCProductDto> products = financeService.getProducts(company.getId(), employee.getEmployeeId1c());

            if (products.isEmpty()) {
                send(bot, chatId, "🛒 Mahsulotlar topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("🛒 Mahsulotlar (").append(products.size()).append(" ta)\n\n");
            for (int i = 0; i < products.size(); i++) {
                OneCProductDto p = products.get(i);
                sb.append(i + 1).append(". ").append(p.getProduct() != null ? p.getProduct() : "—").append("\n");
                if (p.getDate() != null) {
                    sb.append("   📅 ").append(p.getDate()).append("\n");
                }
                if (p.getQty() != null) {
                    sb.append("   📊 Miqdor: ").append(String.format("%,.2f", p.getQty())).append("\n");
                }
                if (p.getPrice() != null) {
                    sb.append("   💰 Narx: ").append(String.format("%,.2f", p.getPrice())).append(" so'm\n");
                }
                if (p.getSum() != null) {
                    sb.append("   💵 Summa: ").append(String.format("%,.2f", p.getSum())).append(" so'm\n");
                }
                sb.append("\n");
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (products) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void promptReconciliationPeriod(Company company, TelegramLongPollingBot bot, Long chatId)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }
        send(bot, chatId, "📊 Qaysi davr uchun ko'rmoqchisiz?",
                inlineKeyboardBuilder.reconciliationPeriodKeyboard());
    }

    private LocalDate[] resolveReconciliationPeriod(String period) {
        LocalDate now = LocalDate.now();
        LocalDate from = switch (period) {
            case "MONTH" -> now.withDayOfMonth(1);
            case "DAYS7" -> now.minusDays(6);
            case "DAYS15" -> now.minusDays(14);
            case "DAYS30" -> now.minusDays(29);
            default -> now.withDayOfMonth(1);
        };
        return new LocalDate[]{from, now};
    }

    private void showReconciliation(Company company, TelegramLongPollingBot bot, Long chatId,
            LocalDate dateFrom, LocalDate dateTo)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            OneCReconciliationDto data = financeService.getReconciliation(
                    company.getId(), employee.getEmployeeId1c(), dateFrom.toString(), dateTo.toString());

            if (data == null) {
                send(bot, chatId, "📊 Akt sverka ma'lumoti topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("📊 Akt sverka\n\n");
            sb.append("👤 ").append(data.getName() != null ? data.getName() : "—").append("\n");
            sb.append("📅 Davr: ").append(data.getDateFrom() != null ? data.getDateFrom() : "—");
            sb.append(" — ").append(data.getDateTo() != null ? data.getDateTo() : "—").append("\n\n");
            sb.append("💰 Hisoblangan: ").append(formatAmount(data.getAccrued())).append("\n");
            sb.append("💵 To'langan: ").append(formatAmount(data.getPaid())).append("\n");
            sb.append("🏛 Soliq: ").append(formatAmount(data.getTax())).append("\n");
            sb.append("⚠️ Jarima: ").append(formatAmount(data.getPenalty())).append("\n");
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append("📊 Qoldiq: ").append(formatAmount(data.getBalance())).append("\n\n");

            if (data.getRows() != null && !data.getRows().isEmpty()) {
                sb.append("📋 Tafsilotlar:\n\n");
                for (OneCReconciliationDto.OneCReconciliationRowDto row : data.getRows()) {
                    String type = row.getType() != null ? row.getType() : "";
                    switch (type) {
                        case "WORK" -> {
                            sb.append("🔧 ");
                            if (row.getProduct() != null) {
                                sb.append(row.getProduct());
                            }
                            if (row.getQty() != null && row.getPrice() != null) {
                                sb.append(" (").append(String.format("%,.2f", row.getQty()))
                                        .append(" x ").append(String.format("%,.2f", row.getPrice())).append(")");
                            }
                            sb.append(" = ").append(formatAmount(row.getAmount())).append("\n");
                        }
                        case "PAYMENT" -> {
                            sb.append("💵 To'lov: ").append(formatAmount(row.getAmount()));
                            if (row.getCurrency() != null) {
                                sb.append(" ").append(row.getCurrency());
                            }
                            sb.append("\n");
                        }
                        case "PRODUCT" -> {
                            sb.append("🛒 ");
                            if (row.getProduct() != null) {
                                sb.append(row.getProduct());
                            }
                            if (row.getQty() != null && row.getPrice() != null) {
                                sb.append(" (").append(String.format("%,.2f", row.getQty()))
                                        .append(" x ").append(String.format("%,.2f", row.getPrice())).append(")");
                            }
                            sb.append(" = ").append(formatAmount(row.getAmount())).append("\n");
                        }
                        case "PENALTY" -> sb.append("⚠️ Jarima: ").append(formatAmount(row.getAmount())).append("\n");
                        default -> {
                            sb.append("📌 ");
                            if (row.getDescription() != null) {
                                sb.append(row.getDescription()).append(": ");
                            }
                            sb.append(formatAmount(row.getAmount())).append("\n");
                        }
                    }
                }
            }

            send(bot, chatId, sb.toString(),
                    inlineKeyboardBuilder.reconciliationPdfKeyboard(dateFrom.toString(), dateTo.toString()));
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (reconciliation) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private void sendReconciliationPdf(Company company, TelegramLongPollingBot bot, Long chatId,
            String dateFrom, String dateTo)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            OneCReconciliationDto data = financeService.getReconciliation(
                    company.getId(), employee.getEmployeeId1c(), dateFrom, dateTo);
            if (data == null) {
                send(bot, chatId, "📊 Akt sverka ma'lumoti topilmadi.");
                return;
            }
            byte[] pdfBytes = pdfService.generateReconciliationPdf(data);
            SendDocument doc = SendDocument.builder()
                    .chatId(chatId.toString())
                    .document(new InputFile(new ByteArrayInputStream(pdfBytes), "akt-sverka.pdf"))
                    .build();
            bot.execute(doc);
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (reconciliation PDF) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        } catch (IOException e) {
            log.error("PDF generation error for company {}: {}", company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ PDF yaratishda xatolik yuz berdi.");
        }
    }

    private void showBalance(Company company, TelegramLongPollingBot bot, Long chatId) throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        try {
            OneCBalanceDto balance = financeService.getBalance(company.getId(), employee.getEmployeeId1c());

            if (balance == null) {
                send(bot, chatId, "💰 Balans ma'lumoti topilmadi.");
                return;
            }

            StringBuilder sb = new StringBuilder("💰 Balans\n\n");
            sb.append("💰 Joriy balans: ").append(formatAmount(balance.getBalance())).append("\n");
            if (balance.getAsOf() != null) {
                sb.append("📅 Sana: ").append(balance.getAsOf());
            }

            send(bot, chatId, sb.toString());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (balance) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    // ==================== Appeals ====================

    private void handleAppealTypeSelected(TelegramLongPollingBot bot, Long chatId, ChatKey key, String type)
            throws TelegramApiException {
        try {
            AppealType.valueOf(type);
        } catch (IllegalArgumentException e) {
            send(bot, chatId, "Noma'lum murojaat turi. Iltimos, qaytadan tanlang:",
                    inlineKeyboardBuilder.appealTypeKeyboard());
            return;
        }

        userStateManager.setPendingAppealType(key, type);
        userStateManager.setState(key, BotState.APPEAL_ENTER_TEXT);

        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        bot.execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Murojaat matnini yozing:")
                .replyMarkup(keyboardRemove)
                .build());
    }

    private void handleAppealText(TelegramLongPollingBot bot, Long chatId, ChatKey key, String text)
            throws TelegramApiException {
        String pendingType = userStateManager.getPendingAppealType(key);
        if (pendingType == null) {
            userStateManager.setState(key, BotState.APPEAL_SELECT_TYPE);
            send(bot, chatId, "Murojaat turini tanlang:", inlineKeyboardBuilder.appealTypeKeyboard());
            return;
        }

        userStateManager.setPendingAppealText(key, text);
        userStateManager.setState(key, BotState.APPEAL_CONFIRM);

        StringBuilder sb = new StringBuilder("📩 Murojaat xulosasi\n\n");
        sb.append("📂 Turi: ").append(appealTypeLabel(pendingType)).append("\n");
        sb.append("📝 Matn: ").append(text);
        send(bot, chatId, sb.toString(), inlineKeyboardBuilder.appealConfirmKeyboard());
    }

    private void handleAppealConfirm(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null) {
            return;
        }

        String pendingType = userStateManager.getPendingAppealType(key);
        String pendingText = userStateManager.getPendingAppealText(key);
        if (pendingType == null || pendingText == null) {
            userStateManager.clearPendingAppeal(key);
            userStateManager.setState(key, BotState.APPEAL_SELECT_TYPE);
            send(bot, chatId, "Murojaat turini tanlang:", inlineKeyboardBuilder.appealTypeKeyboard());
            return;
        }

        try {
            AppealType type = AppealType.valueOf(pendingType);
            appealService.createAppeal(company.getId(), employee, type, pendingText);
            userStateManager.clearPendingAppeal(key);
            userStateManager.setState(key, BotState.MAIN_MENU);
            send(bot, chatId, "✅ Murojaatingiz qabul qilindi", replyKeyboardBuilder.mainMenuKeyboard(company));
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("1C communication error (appeal confirm) for company {}: {}",
                    company.getId(), e.getMessage(), e);
            send(bot, chatId, "⚠️ Murojaatni yuborishda xatolik yuz berdi. Iltimos, \"Tasdiqlash\"ni qaytadan bosing.");
        }
    }

    private String appealTypeLabel(String type) {
        try {
            return switch (AppealType.valueOf(type)) {
                case WORK -> "Ish bo'yicha";
                case FINANCE -> "Moliya bo'yicha";
                case PENALTY -> "Jarima bo'yicha";
                case PRODUCT -> "Mahsulot bo'yicha";
                case OTHER -> "Boshqa";
            };
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    // ==================== Helpers ====================

    private Employee requireEmployee(Company company, TelegramLongPollingBot bot, Long chatId)
            throws TelegramApiException {
        Optional<Employee> existing = employeeService.findByTelegramId(company.getId(), chatId);
        if (existing.isEmpty()) {
            send(bot, chatId, "Iltimos, avval ro'yxatdan o'ting: /start");
            return null;
        }
        return existing.get();
    }

    private void appendWorkName(StringBuilder sb, String work, String subwork) {
        String workName = work != null ? work : "";
        if (subwork != null && !subwork.isEmpty()) {
            workName += " / " + subwork;
        }
        if (!workName.isEmpty()) {
            sb.append("   🔧 ").append(workName).append("\n");
        }
    }

    private String formatAmount(Double val) {
        return val != null ? String.format("%,.2f so'm", val) : "0.00 so'm";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ── Attendance handlers ──────────────────────────────────────────────────

    /**
     * Entry point when user taps "🕐 Davomatim".
     * If company has no HR Pulse token configured → silently ignore (button should
     * not appear).
     * If employee has no HR Pulse ID yet → ask them to enter it.
     * Otherwise → show attendance sub-menu.
     */
    private void handleAttendanceMenu(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key)
            throws TelegramApiException {
        if (company.getHrpulseToken() == null || company.getHrpulseToken().isBlank()) {
            return; // safety guard; button should not appear for such companies
        }
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null)
            return;

        if (employee.getHrpulseEmployeeId() == null || employee.getHrpulseEmployeeId().isBlank()) {
            userStateManager.setState(key, BotState.ATTENDANCE_ENTER_ID);
            send(bot, chatId,
                    "🕐 Davomatim bo'limiga xush kelibsiz!\n\n" +
                            "Iltimos, HR Pulse tizimidagi xodim ID raqamingizni kiriting:\n" +
                            "(Masalan: 42)");
        } else {
            userStateManager.setState(key, BotState.ATTENDANCE_MENU);
            send(bot, chatId, "🕐 Davomat bo'limi", replyKeyboardBuilder.attendanceMenuKeyboard());
        }
    }

    /**
     * Handles the employee typing their HR Pulse employee ID for the first time.
     * Saves the ID and transitions to the attendance sub-menu.
     */
    private void handleAttendanceIdEntry(Company company, TelegramLongPollingBot bot, Long chatId, ChatKey key,
            String text)
            throws TelegramApiException {
        if ("⬅️ Ortga".equals(text)) {
            userStateManager.setState(key, BotState.ATTENDANCE_MENU);
            send(bot, chatId, "🕐 Davomat bo'limi", replyKeyboardBuilder.attendanceMenuKeyboard());
            return;
        }

        String id = text.trim();
        if (id.isBlank()) {
            send(bot, chatId, "⚠️ ID raqam bo'sh bo'lishi mumkin emas. Iltimos, qayta kiriting:");
            return;
        }
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null)
            return;

        employeeService.saveHrpulseEmployeeId(company.getId(), chatId, id);
        userStateManager.setState(key, BotState.ATTENDANCE_MENU);
        send(bot, chatId, "✅ HR Pulse ID saqlandi: " + id, replyKeyboardBuilder.attendanceMenuKeyboard());
    }

    /** Shows today's attendance record. */
    private void showAttendanceToday(Company company, TelegramLongPollingBot bot, Long chatId)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null)
            return;
        if (employee.getHrpulseEmployeeId() == null || employee.getHrpulseEmployeeId().isBlank()) {
            send(bot, chatId, "⚠️ HR Pulse ID topilmadi. Iltimos, 🕐 Davomatim menyusiga qayting.");
            return;
        }
        try {
            var record = attendanceService.getToday(company.getId(), employee.getHrpulseEmployeeId());
            if (record.isEmpty()) {
                send(bot, chatId, "📅 Bugungi davomat ma'lumoti topilmadi.");
                return;
            }
            var r = record.get();
            String checkIn = attendanceService.formatTime(r.getCheckIn());
            String checkOut = attendanceService.formatTime(r.getCheckOut());
            int lateness = attendanceService.calcLatenessMinutes(r);
            int earlyLeave = attendanceService.calcEarlyLeaveMinutes(r);
            String status = attendanceService.statusLabel(r);

            StringBuilder sb = new StringBuilder();
            sb.append("🕐 <b>Bugungi davomat</b>\n");
            sb.append("📅 ").append(escapeHtml(attendanceService.formatDate(r.getDate()))).append("\n\n");
            sb.append("Kelish:  ").append(escapeHtml(checkIn)).append("\n");
            sb.append("Ketish:  ").append(escapeHtml(checkOut)).append("\n");
            if (r.getWorkTime() != null) {
                String ws = attendanceService.formatTime(r.getWorkTime().getStart());
                String we = attendanceService.formatTime(r.getWorkTime().getEnd());
                sb.append("Ish grafigi:  ").append(escapeHtml(ws)).append(" – ").append(escapeHtml(we)).append("\n");
            }
            sb.append("Kechikish:  ").append(lateness > 0 ? lateness + " daqiqa" : "0 daqiqa").append("\n");
            if (earlyLeave > 0) {
                sb.append("Erta ketish:  ").append(earlyLeave).append(" daqiqa\n");
            }
            sb.append("Holat:  ").append(escapeHtml(status));

            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(sb.toString())
                    .parseMode("HTML")
                    .build());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("HR Pulse error (today) for company {}: {}", company.getId(), e.getMessage());
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    /** Shows monthly attendance summary + per-day table for the current month. */
    private void showAttendanceMonthly(Company company, TelegramLongPollingBot bot, Long chatId)
            throws TelegramApiException {
        Employee employee = requireEmployee(company, bot, chatId);
        if (employee == null)
            return;
        if (employee.getHrpulseEmployeeId() == null || employee.getHrpulseEmployeeId().isBlank()) {
            send(bot, chatId, "⚠️ HR Pulse ID topilmadi. Iltimos, 🕐 Davomatim menyusiga qayting.");
            return;
        }
        try {
            LocalDate now = LocalDate.now();
            var rows = attendanceService.getMonthly(company.getId(), employee.getHrpulseEmployeeId(),
                    now.getYear(), now.getMonthValue());

            String[] MONTHS = { "", "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
                    "Iyul", "Avgust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr" };
            String monthName = MONTHS[now.getMonthValue()];

            // ── summary counters ─────────────────────────────────────────────
            int presentDays = 0, absentDays = 0, lateDays = 0, earlyLeaveDays = 0;
            int totalLatenessMin = 0;

            for (var r : rows) {
                if (r.isSkipWorkday())
                    continue;
                if (r.isNotMarked()) {
                    absentDays++;
                    continue;
                }
                if (r.getCheckIn() == null) {
                    absentDays++;
                    continue;
                }
                presentDays++;
                int late = attendanceService.calcLatenessMinutes(r);
                int early = attendanceService.calcEarlyLeaveMinutes(r);
                if (late > 0) {
                    lateDays++;
                    totalLatenessMin += late;
                }
                if (early > 0)
                    earlyLeaveDays++;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📆 <b>").append(monthName).append(" ").append(now.getYear()).append("</b>\n\n");
            sb.append("Kelgan kunlar:        ").append(presentDays).append("\n");
            sb.append("Kelmagan kunlar:      ").append(absentDays).append("\n");
            sb.append("Kechikkan kunlar:     ").append(lateDays).append("\n");
            sb.append("Jami kechikish:       ").append(totalLatenessMin).append(" daqiqa\n");
            sb.append("Erta ketish:          ").append(earlyLeaveDays).append(" marta\n\n");
            sb.append("<code>");
            sb.append(String.format("%-8s %-6s %-6s  %s%n", "Sana", "Kelish", "Ketish", "Holat"));
            sb.append("─".repeat(38)).append("\n");

            for (var r : rows) {
                String dateShort = r.getDate() != null && r.getDate().length() >= 10
                        ? r.getDate().substring(8, 10) + "." + r.getDate().substring(5, 7)
                        : "—";
                String ci = attendanceService.formatTime(r.getCheckIn());
                String co = attendanceService.formatTime(r.getCheckOut());
                String st = attendanceService.statusLabel(r)
                        .replaceAll("[^a-zA-Z\\u0400-\\u04FFa-zA-Z ]", "").trim(); // remove emoji for table

                sb.append(String.format("%-8s %-6s %-6s  %s%n",
                        escapeHtml(dateShort), escapeHtml(ci), escapeHtml(co), escapeHtml(st)));
            }
            sb.append("</code>");

            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(sb.toString())
                    .parseMode("HTML")
                    .build());
        } catch (OneCCommunicationException | ResourceAccessException e) {
            log.error("HR Pulse error (monthly) for company {}: {}", company.getId(), e.getMessage());
            send(bot, chatId, "⚠️ Hozircha ma'lumot olishda muammo bo'ldi. Birozdan so'ng qaytadan urinib ko'ring.");
        }
    }

    private Long resolveChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private void send(TelegramLongPollingBot bot, Long chatId, String text) throws TelegramApiException {
        bot.execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
    }

    private void send(TelegramLongPollingBot bot, Long chatId, String text, ReplyKeyboardMarkup keyboard)
            throws TelegramApiException {
        bot.execute(SendMessage.builder().chatId(chatId.toString()).text(text).replyMarkup(keyboard).build());
    }

    private void send(TelegramLongPollingBot bot, Long chatId, String text, InlineKeyboardMarkup keyboard)
            throws TelegramApiException {
        bot.execute(SendMessage.builder().chatId(chatId.toString()).text(text).replyMarkup(keyboard).build());
    }
}
