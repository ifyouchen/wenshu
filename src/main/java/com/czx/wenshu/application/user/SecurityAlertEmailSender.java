package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.EmailAddress;

public interface SecurityAlertEmailSender {

    void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime);
}