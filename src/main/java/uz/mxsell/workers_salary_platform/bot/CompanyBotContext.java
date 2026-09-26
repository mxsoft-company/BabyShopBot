package uz.mxsell.workers_salary_platform.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.generics.BotSession;

@Getter
@AllArgsConstructor
public class CompanyBotContext {

    private final Long companyId;
    private final TelegramLongPollingBot botInstance;
    private final BotSession session;
}
