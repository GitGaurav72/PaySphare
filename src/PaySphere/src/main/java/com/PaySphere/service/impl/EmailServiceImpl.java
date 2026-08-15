package com.PaySphere.service.impl;

import com.PaySphere.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendPayslip(String toEmail, String employeeName, byte[] payslipExcel, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your PaySphere payslip");
            helper.setText(
                    "Hi %s,\n\nYour salary payment has been processed. Please find your payslip attached.\n\n— PaySphere"
                            .formatted(employeeName));
            helper.addAttachment(fileName, new ByteArrayResource(payslipExcel));

            mailSender.send(message);
            log.info("Payslip email sent to {}", toEmail);
        } catch (Exception ex) {
            log.warn("Failed to send payslip email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
