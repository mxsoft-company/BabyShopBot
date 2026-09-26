package uz.mxsell.workers_salary_platform.bot.keyboards;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.mxsell.workers_salary_platform.company.Company;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReplyKeyboardBuilder {

    public ReplyKeyboardMarkup contactRequestKeyboard() {
        KeyboardButton contactButton = new KeyboardButton("📱 Telefon raqamimni yuborish");
        contactButton.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }

    /**
     * Main menu keyboard.
     * Shows "🕐 Davomatim" row only when the company has HR Pulse configured.
     */
    public ReplyKeyboardMarkup mainMenuKeyboard(Company company) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🏭 Mening ishlarim");
        row1.add("💰 Hisob-kitobim");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📩 Rahbariyatga murojaat");
        row2.add("👤 Profilim");

        List<KeyboardRow> rows = new ArrayList<>(List.of(row1, row2));

        // Show attendance row only if HR Pulse is configured for this company
        if (StringUtils.hasText(company.getHrpulseToken())) {
            KeyboardRow attendanceRow = new KeyboardRow();
            attendanceRow.add("🕐 Davomatim");
            rows.add(1, attendanceRow); // insert between row1 and row2
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);
        return markup;
    }

    public ReplyKeyboardMarkup worksMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🕐 Kutilayotgan ishlar");
        row1.add("✅ Bajarilgan ishlar");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("⬅️ Ortga");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public ReplyKeyboardMarkup financeMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("💵 To'lovlar");
        row1.add("⚠️ Jarimalar");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🛒 Mahsulotlar");
        row2.add("📊 Akt sverka");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("💰 Balans");
        row3.add("🧾 Soliq (NDFL)");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("⬅️ Ortga");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row1, row2, row3, row4));
        markup.setResizeKeyboard(true);
        return markup;
    }

    /** Sub-menu shown after tapping "🕐 Davomatim". */
    public ReplyKeyboardMarkup attendanceMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📅 Bugun");
        row1.add("📆 Oylik davomat");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔄 ID ni o'zgartirish");
        row2.add("⬅️ Ortga");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        return markup;
    }
}
