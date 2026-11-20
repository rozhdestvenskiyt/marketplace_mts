package ru.avito.avito.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!production")
public class DevNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DevNotificationSender.class);

    @Override
    public void sendEmailCode(String email, String code) {
        log.info("DEV email verification code for {}: {}", email, code);
    }

    @Override
    public void sendSmsCode(String phone, String code) {
        log.info("DEV SMS verification code for {}: {}", phone, code);
    }
}
