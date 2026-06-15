package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;

public class DelegatingSecurityAlertEmailSender implements SecurityAlertEmailSender {

    private final EmailService emailService;

    public DelegatingSecurityAlertEmailSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime) {
        emailService.sendSecurityAlertEmail(email, alertType, alertDetail, alertTime);
    }
}