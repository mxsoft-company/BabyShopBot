package uz.mxsell.workers_salary_platform.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotManagerService {

    private final Map<Long, CompanyBotContext> activeBots = new ConcurrentHashMap<>();
    private final CompanyRepository companyRepository;
    private final TelegramBotsApi telegramBotsApi;
    private final UpdateDispatcher updateDispatcher;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        for (Company company : companyRepository.findAllByIsActiveTrue()) {
            try {
                registerCompany(company);
            } catch (Exception e) {
                log.error("Failed to register bot for company {}: {}", company.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Registers (or re-registers) the long polling bot of the given company
     * and stores the returned BotSession in the CompanyBotContext.
     * If a bot is already registered for this company, it is stopped first.
     */
    public synchronized void registerCompany(Company company) throws TelegramApiException {
        CompanyBotContext existing = activeBots.get(company.getId());
        if (existing != null) {
            stopSession(existing);
            activeBots.remove(company.getId());
        }

        TenantTelegramBot bot = new TenantTelegramBot(company, updateDispatcher);
        BotSession session = telegramBotsApi.registerBot(bot);
        activeBots.put(company.getId(), new CompanyBotContext(company.getId(), bot, session));
        log.info("Bot registered for company: {} (active={})", company.getName(), company.isActive());
    }

    /**
     * Stops the long polling session of the given company's bot, if one is running.
     * Never throws: failures are logged so admin flows keep working.
     */
    public synchronized void unregisterBot(Long companyId) {
        CompanyBotContext context = activeBots.remove(companyId);
        if (context == null) {
            log.info("No registered bot to stop for company {}", companyId);
            return;
        }
        stopSession(context);
        log.info("Bot unregistered (polling stopped) for company {}", companyId);
    }

    private void stopSession(CompanyBotContext context) {
        try {
            BotSession session = context.getSession();
            if (session != null && session.isRunning()) {
                session.stop();
            }
        } catch (Exception e) {
            log.error("Failed to stop bot session for company {}: {}",
                    context.getCompanyId(), e.getMessage(), e);
        }
    }

    public TelegramLongPollingBot getBotForCompany(Long companyId) {
        CompanyBotContext context = activeBots.get(companyId);
        return context != null ? context.getBotInstance() : null;
    }
}
