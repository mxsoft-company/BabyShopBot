package uz.mxsell.workers_salary_platform.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.mxsell.workers_salary_platform.company.Company;

public class TenantTelegramBot extends TelegramLongPollingBot {

    private final Company company;
    private final UpdateDispatcher dispatcher;

    public TenantTelegramBot(Company company, UpdateDispatcher dispatcher) {
        this.company = company;
        this.dispatcher = dispatcher;
    }

    @Override
    public String getBotToken() {
        return company.getBotToken();
    }

    @Override
    public String getBotUsername() {
        return company.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        dispatcher.dispatch(company, this, update);
    }
}
