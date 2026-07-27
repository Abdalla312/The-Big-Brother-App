package com.expensetracker.big_brother.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Service
public class EmailTemplateService {

    private final SpringTemplateEngine templateEngine;

    @Value("${app.brand.name:Big Brother}")
    private String brandName;

    @Value("${app.brand.primary-color:#1E40AF}")
    private String brandColor;

    @Value("${app.brand.support-email:expense.tracker.big.brother@gmail.com}")
    private String supportEmail;

    public EmailTemplateService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderVerificationEmail(String name, String verificationUrl, int expiryHours) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("name", name);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("expiryHours", expiryHours);
        context.setVariable("brandName", brandName);
        context.setVariable("brandColor", brandColor);
        context.setVariable("supportEmail", supportEmail);
        return templateEngine.process("email/verification-email", context);
    }

    public String renderEmailChangeVerification(String name, String newEmail, String verificationUrl, int expiryHours) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("name", name);
        context.setVariable("newEmail", newEmail);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("expiryHours", expiryHours);
        context.setVariable("brandName", brandName);
        context.setVariable("brandColor", brandColor);
        context.setVariable("supportEmail", supportEmail);
        return templateEngine.process("email/email-change-verification", context);
    }

    public String renderPasswordReset(String name, String resetUrl, int expiryMinutes) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("name", name);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expiryMinutes", expiryMinutes);
        context.setVariable("brandName", brandName);
        context.setVariable("brandColor", brandColor);
        context.setVariable("supportEmail", supportEmail);
        return templateEngine.process("email/password-reset", context);
    }

    public String renderEmailChangeNotification(String name, String newEmail, int expiryHours) {
        Context context = new Context(Locale.getDefault());
        context.setVariable("name", name);
        context.setVariable("newEmail", newEmail);
        context.setVariable("expiryHours", expiryHours);
        context.setVariable("brandName", brandName);
        context.setVariable("brandColor", brandColor);
        context.setVariable("supportEmail", supportEmail);
        return templateEngine.process("email/email-change-notification", context);
    }
}