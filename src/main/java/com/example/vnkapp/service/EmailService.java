package com.example.vnkapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:VNK App}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to " + appName + "!");
        message.setText(buildWelcomeEmailBody(username));
        mailSender.send(message);
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - Password Reset Code");
        message.setText(buildPasswordResetEmailBody(code));
        mailSender.send(message);
    }

    @Async
    public void sendPasswordResetSuccess(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - Password Changed Successfully");
        message.setText(buildPasswordResetSuccessBody(username));
        mailSender.send(message);
    }

    private String buildWelcomeEmailBody(String username) {
        return String.format("""
            Hi %s,

            Welcome to %s! We're excited to have you on board.

            Your account has been created successfully. You can now log in and start exploring our features:

            - Browse our wide range of cosmetic products
            - Track your medications and set reminders
            - Add family members to manage their health too
            - Refer friends and earn rewards

            If you have any questions, feel free to reach out to our support team.

            Best regards,
            The %s Team
            """, username, appName, appName);
    }

    private String buildPasswordResetEmailBody(String code) {
        return String.format("""
            Hi,

            We received a request to reset your password for your %s account.

            Your password reset code is: %s

            This code will expire in 15 minutes.

            If you didn't request a password reset, please ignore this email or contact support if you have concerns.

            Best regards,
            The %s Team
            """, appName, code, appName);
    }

    private String buildPasswordResetSuccessBody(String username) {
        return String.format("""
            Hi %s,

            Your password has been changed successfully.

            If you did not make this change, please contact our support team immediately.

            Best regards,
            The %s Team
            """, username, appName);
    }
}
