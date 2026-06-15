package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.infrastructure.config.WenshuProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final WenshuProperties properties;

    public SmtpEmailService(JavaMailSender mailSender, TemplateEngine templateEngine, WenshuProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    @Override
    @Async
    public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        String verificationUrl = properties.getBaseUrl() + "/api/v1/auth/verify-email?token=" + rawToken;
        Context context = new Context();
        context.setVariables(Map.of(
                "verificationUrl", verificationUrl
        ));
        String htmlContent = templateEngine.process("mail/verify-email", context);
        sendHtmlEmail(email.value(), "【文枢】验证您的邮箱", htmlContent);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        String resetUrl = properties.getBaseUrl() + "/reset-password?token=" + rawToken;
        Context context = new Context();
        context.setVariables(Map.of(
                "resetUrl", resetUrl
        ));
        String htmlContent = templateEngine.process("mail/reset-password", context);
        sendHtmlEmail(email.value(), "【文枢】重置您的密码", htmlContent);
    }

    @Override
    @Async
    public void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime) {
        Context context = new Context();
        context.setVariables(Map.of(
                "alertType", alertType,
                "alertDetail", alertDetail,
                "alertTime", alertTime
        ));
        String htmlContent = templateEngine.process("mail/security-alert", context);
        sendHtmlEmail(email.value(), "【文枢】安全告警", htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(properties.getMail().getFrom());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);
        } catch (Exception exception) {
            log.error("Failed to send email: to={}, subject={}", to, subject, exception);
        }
    }
}