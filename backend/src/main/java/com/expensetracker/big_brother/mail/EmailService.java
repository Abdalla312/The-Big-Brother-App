package com.expensetracker.big_brother.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;


    public void sendHtml(String to, String subject, String htmlBody){
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    true, "UTF-8");
            helper.setFrom("Big Brother Team <expense.tracker.big.brother@gmail.com>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);

        }  catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

}
