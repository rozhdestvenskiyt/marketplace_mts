package ru.avito.avito.notification;

public interface NotificationSender {

    void sendEmailCode(String email, String code);

    void sendSmsCode(String phone, String code);
}
