package com.expensetracker.big_brother.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {
    @Mock
    JavaMailSender mailSender;
    @InjectMocks
    EmailService service;

    private MimeMessage createMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "fromAddress", "expense.tracker.big.brother@gmail.com");
        ReflectionTestUtils.setField(service, "fromName", "Big Brother Team");
        ReflectionTestUtils.setField(service, "replyTo", "expense.tracker.big.brother@gmail.com");
    }

    @Test
    void sendHtml_Success() {
        when(mailSender.createMimeMessage()).thenReturn(createMessage());

        service.sendHtml("to@example.com", "Subject", "<html>body</html>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHtml_MailSendException_ThrowsRuntimeException() {
        when(mailSender.createMimeMessage()).thenReturn(createMessage());
        doThrow(new MailSendException("mail error"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.sendHtml("to@example.com", "Sub", "<p>Hi</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MailSendException.class)
                .hasMessage("Failed to send HTML email");
    }

    @Test
    void sendHtml_UnsupportedEncodingException_ThrowsRuntimeException() {
        when(mailSender.createMimeMessage()).thenReturn(createMessage());

        try (MockedConstruction<MimeMessageHelper> ignored = mockConstruction(MimeMessageHelper.class,
                (mock, context) -> {
                    doThrow(new UnsupportedEncodingException("bad encoding"))
                            .when(mock).setFrom(anyString(), anyString());
                })) {

            assertThatThrownBy(() -> service.sendHtml("to@example.com", "Sub", "<p>Hi</p>"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(UnsupportedEncodingException.class)
                    .hasMessage("Failed to send HTML email");
        }
    }

    @Test
    void sendHtml_VerifiesMimeMessageHelperConfiguration() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(createMessage());

        service.sendHtml("to@test.com", "Test Subject", "<p>Hello</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        message.saveChanges();

        assertThat(InternetAddress.toString(message.getFrom()))
                .contains("expense.tracker.big.brother@gmail.com");
        assertThat(InternetAddress.toString(message.getAllRecipients()))
                .isEqualTo("to@test.com");
        assertThat(message.getSubject()).isEqualTo("Test Subject");

        String body = extractEmailBody((MimeMultipart) message.getContent());
        assertThat(body).containsIgnoringCase("text/html");
        assertThat(body).contains("<p>Hello</p>");
    }

    @Test
    void sendHtmlAsync_DelegatesToSendHtml() {
        when(mailSender.createMimeMessage()).thenReturn(createMessage());

        service.sendHtmlAsync("to@example.com", "Subject", "<html>body</html>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    private String extractEmailBody(MimeMultipart multipart) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof MimeMultipart nested) {
                builder.append(extractEmailBody(nested));
            } else {
                builder.append(part.getContentType()).append(" ").append(part.getContent());
            }
        }
        return builder.toString();
    }
}
