package com.expensetracker.big_brother.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name:Big Brother Team}")
    private String fromName;

    @Value("${app.mail.reply-to}")
    private String replyTo;

    public void sendHtml(String to, String subject, String htmlBody){
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_RELATED,
                    "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(extractPlainText(htmlBody), htmlBody);
            helper.setReplyTo(replyTo);
            message.setHeader("List-Unsubscribe", "<mailto:" + replyTo + "?subject=unsubscribe>");

            mailSender.send(message);

        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    @Async("mailExecutor")
    public void sendHtmlAsync(String to, String subject, String htmlBody) {
        sendHtml(to, subject, htmlBody);
    }

    private String extractPlainText(String html) {
        if (html == null) return "";

        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

}
