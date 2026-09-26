package uz.mxsell.workers_salary_platform.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.mxsell.workers_salary_platform.bot.BotManagerService;
import uz.mxsell.workers_salary_platform.company.Company;
import uz.mxsell.workers_salary_platform.company.CompanyService;
import uz.mxsell.workers_salary_platform.exception.DuplicateBotTokenException;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyService companyService;
    private final BotManagerService botManagerService;

    @GetMapping
    public String index() {
        return "redirect:/admin/companies";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "companies/login";
    }

    @GetMapping("/companies")
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyService.findAllVisible());
        return "companies/list";
    }

    @GetMapping("/companies/new")
    public String newCompany(Model model) {
        model.addAttribute("company", new Company());
        return "companies/form";
    }

    @GetMapping("/companies/{id}/edit")
    public String editCompany(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getById(id));
        return "companies/form";
    }

    @PostMapping("/companies")
    public String saveCompany(@ModelAttribute Company company, Model model) {
        try {
            if (company.getId() == null) {
                if (company.getSlug() == null || company.getSlug().isBlank()) {
                    company.setSlug(companyService.generateSlug(company.getName()));
                }
                Company saved = companyService.create(company);
                try {
                    botManagerService.registerCompany(saved);
                } catch (TelegramApiException e) {
                    log.error("Failed to register bot for new company {}: {}", company.getName(), e.getMessage());
                }
            } else {
                Company before = companyService.getById(company.getId());
                String oldToken = before.getBotToken();
                Company updated = companyService.update(company.getId(), company);
                boolean tokenChanged = company.getBotToken() != null && !company.getBotToken().isBlank()
                        && !company.getBotToken().equals(oldToken);
                try {
                    if (before.isActive() && !updated.isActive()) {
                        botManagerService.unregisterBot(company.getId());
                    } else if (!before.isActive() && updated.isActive()) {
                        botManagerService.registerCompany(updated);
                    } else if (updated.isActive() && tokenChanged) {
                        botManagerService.registerCompany(updated);
                    }
                } catch (TelegramApiException e) {
                    log.error("Failed to switch bot state for company {}: {}", company.getName(), e.getMessage(), e);
                }
            }
        } catch (DuplicateBotTokenException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("company", company);
            return "companies/form";
        }
        return "redirect:/admin/companies";
    }

    @PostMapping("/companies/{id}/toggle")
    public String toggleCompany(@PathVariable Long id) {
        Company updated = companyService.toggleActive(id);
        try {
            if (updated.isActive()) {
                botManagerService.registerCompany(updated);
            } else {
                botManagerService.unregisterBot(id);
            }
        } catch (TelegramApiException e) {
            log.error("Failed to switch bot state for company {}: {}", updated.getName(), e.getMessage());
        }
        return "redirect:/admin/companies";
    }

    @PostMapping("/companies/{id}/delete")
    public String deleteCompany(@PathVariable Long id) {
        companyService.delete(id);
        return "redirect:/admin/companies";
    }
}
