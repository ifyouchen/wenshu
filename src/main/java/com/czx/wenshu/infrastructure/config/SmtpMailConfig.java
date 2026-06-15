package com.czx.wenshu.infrastructure.config;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.infrastructure.mail.DelegatingPasswordResetEmailSender;
import com.czx.wenshu.infrastructure.mail.DelegatingSecurityAlertEmailSender;
import com.czx.wenshu.infrastructure.mail.DelegatingVerificationEmailSender;
import com.czx.wenshu.infrastructure.mail.EmailService;
import com.czx.wenshu.infrastructure.mail.SmtpEmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@Configuration
@ConditionalOnProperty(name = "wenshu.mail.enabled", havingValue = "true")
public class SmtpMailConfig {

    @Bean
    public EmailService emailService(JavaMailSender mailSender, TemplateEngine templateEngine, WenshuProperties properties) {
        return new SmtpEmailService(mailSender, templateEngine, properties);
    }

    @Bean
    public VerificationEmailSender verificationEmailSender(EmailService emailService) {
        return new DelegatingVerificationEmailSender(emailService);
    }

    @Bean
    public PasswordResetEmailSender passwordResetEmailSender(EmailService emailService) {
        return new DelegatingPasswordResetEmailSender(emailService);
    }

    @Bean
    public SecurityAlertEmailSender securityAlertEmailSender(EmailService emailService) {
        return new DelegatingSecurityAlertEmailSender(emailService);
    }
}