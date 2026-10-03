package uz.mxsell.workers_salary_platform.bot.keyboards;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class InlineKeyboardBuilder {

    public InlineKeyboardMarkup profileLogoutKeyboard() {
        InlineKeyboardButton logoutButton = new InlineKeyboardButton();
        logoutButton.setText("🚪 Chiqish");
        logoutButton.setCallbackData("profile:logout");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(logoutButton)));
        return markup;
    }

    public InlineKeyboardMarkup appealTypeKeyboard() {
        InlineKeyboardButton work = new InlineKeyboardButton();
        work.setText("🏭 Ish bo'yicha");
        work.setCallbackData("appeal:type:WORK");

        InlineKeyboardButton finance = new InlineKeyboardButton();
        finance.setText("💰 Moliya bo'yicha");
        finance.setCallbackData("appeal:type:FINANCE");

        InlineKeyboardButton penalty = new InlineKeyboardButton();
        penalty.setText("⚠️ Jarima bo'yicha");
        penalty.setCallbackData("appeal:type:PENALTY");

        InlineKeyboardButton product = new InlineKeyboardButton();
        product.setText("🛒 Mahsulot bo'yicha");
        product.setCallbackData("appeal:type:PRODUCT");

        InlineKeyboardButton other = new InlineKeyboardButton();
        other.setText("📝 Boshqa");
        other.setCallbackData("appeal:type:OTHER");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(work, finance),
                List.of(penalty, product),
                List.of(other)));
        return markup;
    }

    public InlineKeyboardMarkup appealConfirmKeyboard() {
        InlineKeyboardButton confirm = new InlineKeyboardButton();
        confirm.setText("✅ Tasdiqlash");
        confirm.setCallbackData("appeal:confirm");

        InlineKeyboardButton cancel = new InlineKeyboardButton();
        cancel.setText("❌ Bekor qilish");
        cancel.setCallbackData("appeal:cancel");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(confirm, cancel)));
        return markup;
    }

    public InlineKeyboardMarkup reconciliationPdfKeyboard(String dateFrom, String dateTo) {
        InlineKeyboardButton pdfButton = new InlineKeyboardButton();
        pdfButton.setText("📄 PDF yuklab olish");
        pdfButton.setCallbackData("finance:reconciliation:pdf:" + dateFrom + ":" + dateTo);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(pdfButton)));
        return markup;
    }

    public InlineKeyboardMarkup reconciliationPeriodKeyboard() {
        InlineKeyboardButton month = new InlineKeyboardButton();
        month.setText("📅 Bu oy");
        month.setCallbackData("finance:reconciliation:period:MONTH");

        InlineKeyboardButton days7 = new InlineKeyboardButton();
        days7.setText("📅 So'nggi 7 kun");
        days7.setCallbackData("finance:reconciliation:period:DAYS7");

        InlineKeyboardButton days15 = new InlineKeyboardButton();
        days15.setText("📅 So'nggi 15 kun");
        days15.setCallbackData("finance:reconciliation:period:DAYS15");

        InlineKeyboardButton days30 = new InlineKeyboardButton();
        days30.setText("📅 So'nggi 30 kun");
        days30.setCallbackData("finance:reconciliation:period:DAYS30");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(month, days7),
                List.of(days15, days30)));
        return markup;
    }

    public InlineKeyboardMarkup completedWorksPaginationKeyboard(int page, int totalPages) {
        return paginationKeyboard("works:completed:page:", page, totalPages);
    }

    public InlineKeyboardMarkup pendingWorksPaginationKeyboard(int page, int totalPages) {
        return paginationKeyboard("works:pending:page:", page, totalPages);
    }

    public InlineKeyboardButton workDetailButton(String workId) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("📋 Batafsil");
        button.setCallbackData("work:detail:" + workId);
        return button;
    }

    private InlineKeyboardMarkup paginationKeyboard(String callbackPrefix, int page, int totalPages) {
        List<InlineKeyboardButton> row = new ArrayList<>();

        if (page > 0) {
            InlineKeyboardButton prev = new InlineKeyboardButton();
            prev.setText("⬅️ Oldingi");
            prev.setCallbackData(callbackPrefix + (page - 1));
            row.add(prev);
        }

        InlineKeyboardButton pageInfo = new InlineKeyboardButton();
        pageInfo.setText((page + 1) + "/" + totalPages);
        pageInfo.setCallbackData("noop");
        row.add(pageInfo);

        if (page < totalPages - 1) {
            InlineKeyboardButton next = new InlineKeyboardButton();
            next.setText("➡️ Keyingi");
            next.setCallbackData(callbackPrefix + (page + 1));
            row.add(next);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }
}
