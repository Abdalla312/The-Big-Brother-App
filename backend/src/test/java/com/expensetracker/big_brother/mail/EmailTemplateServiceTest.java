package com.expensetracker.big_brother.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EmailTemplateServiceTest {

    @Mock
    SpringTemplateEngine engine;

    @InjectMocks
    EmailTemplateService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "brandName", "Big Brother");
        ReflectionTestUtils.setField(service, "brandColor", "#1E40AF");
        ReflectionTestUtils.setField(service, "supportEmail", "support@bigbrother.com");
    }

    @Test
    void renderVerificationEmail_Success() {
        when(engine.process(anyString(), any(Context.class))).thenReturn("<html>verified</html>");

        String result = service.renderVerificationEmail("Alice", "https://verify", 24);

        assertThat(result).isEqualTo("<html>verified</html>");

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(eq("email/verification-email"), captor.capture());

        Context ctx = captor.getValue();
        assertThat(ctx.getVariable("name")).isEqualTo("Alice");
        assertThat(ctx.getVariable("verificationUrl")).isEqualTo("https://verify");
        assertThat(ctx.getVariable("expiryHours")).isEqualTo(24);
        assertThat(ctx.getVariable("brandName")).isEqualTo("Big Brother");
        assertThat(ctx.getVariable("brandColor")).isEqualTo("#1E40AF");
        assertThat(ctx.getVariable("supportEmail")).isEqualTo("support@bigbrother.com");
    }

    @Test
    void renderEmailChangeVerification_Success() {
        when(engine.process(anyString(), any(Context.class))).thenReturn("<html>change</html>");

        String result = service.renderEmailChangeVerification("Bob", "new@email.com", "https://verify", 48);

        assertThat(result).isEqualTo("<html>change</html>");

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(eq("email/email-change-verification"), captor.capture());

        Context ctx = captor.getValue();

        assertThat(ctx.getVariable("name")).isEqualTo("Bob");
        assertThat(ctx.getVariable("newEmail")).isEqualTo("new@email.com");
        assertThat(ctx.getVariable("verificationUrl")).isEqualTo("https://verify");
        assertThat(ctx.getVariable("expiryHours")).isEqualTo(48);
        assertThat(ctx.getVariable("brandName")).isEqualTo("Big Brother");
        assertThat(ctx.getVariable("brandColor")).isEqualTo("#1E40AF");
        assertThat(ctx.getVariable("supportEmail")).isEqualTo("support@bigbrother.com");

    }

    @Test
    void renderPasswordReset_Success() {
        when(engine.process(anyString(), any(Context.class))).thenReturn("<html>reset</html>");

        String result = service.renderPasswordReset("Charlie", "https://reset", 30);

        assertThat(result).isEqualTo("<html>reset</html>");

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(eq("email/password-reset"), captor.capture());

        Context ctx = captor.getValue();

        assertThat(ctx.getVariable("name")).isEqualTo("Charlie");
        assertThat(ctx.getVariable("resetUrl")).isEqualTo("https://reset");
        assertThat(ctx.getVariable("expiryMinutes")).isEqualTo(30);
        assertThat(ctx.getVariable("brandName")).isEqualTo("Big Brother");
        assertThat(ctx.getVariable("brandColor")).isEqualTo("#1E40AF");
        assertThat(ctx.getVariable("supportEmail")).isEqualTo("support@bigbrother.com");

    }

    @Test
    void renderEmailChangeNotification_Success() {
        when(engine.process(anyString(), any(Context.class))).thenReturn("<html>notification</html>");

        String result = service.renderEmailChangeNotification("Diana", "new@email.com", 24);

        assertThat(result).isEqualTo("<html>notification</html>");

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(eq("email/email-change-notification"), captor.capture());

        Context ctx = captor.getValue();

        assertThat(ctx.getVariable("name")).isEqualTo("Diana");
        assertThat(ctx.getVariable("newEmail")).isEqualTo("new@email.com");
        assertThat(ctx.getVariable("expiryHours")).isEqualTo(24);
        assertThat(ctx.getVariable("brandName")).isEqualTo("Big Brother");
        assertThat(ctx.getVariable("brandColor")).isEqualTo("#1E40AF");
        assertThat(ctx.getVariable("supportEmail")).isEqualTo("support@bigbrother.com");
    }

    @Test
    void renderVerificationEmail_BrandVariablesInjected() {
        ReflectionTestUtils.setField(service, "brandName", "CustomBrand");
        ReflectionTestUtils.setField(service, "brandColor", "#FF0000");
        ReflectionTestUtils.setField(service, "supportEmail", "help@email.com");

        when(engine.process(anyString(), any(Context.class))).thenReturn("ok");

        service.renderVerificationEmail("Eve", "https://v", 12);

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);

        verify(engine).process(eq("email/verification-email"), captor.capture());

        Context ctx = captor.getValue();
        assertThat(ctx.getVariable("brandName")).isEqualTo("CustomBrand");
        assertThat(ctx.getVariable("brandColor")).isEqualTo("#FF0000");
        assertThat(ctx.getVariable("supportEmail")).isEqualTo("help@email.com");
    }

    @Test
    void renderPasswordReset_NullName_Handled() {
        when(engine.process(anyString(), any(Context.class))).thenReturn("<html>reset</html>");

        String result = service.renderPasswordReset(null, "https://reset", 30);

        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(eq("email/password-reset"), captor.capture());

        Context ctx = captor.getValue();
        assertThat(ctx.getVariable("name")).isNull();
        assertThat(ctx.getVariable("resetUrl")).isEqualTo("https://reset");
        assertThat(ctx.getVariable("expiryMinutes")).isEqualTo(30);
    }

}
