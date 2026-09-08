package com.example.vnkapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:VNK App}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username, String memberId) {
        sendEmail(
                toEmail,
                "welcome",
                "Welcome to " + appName + "!",
                buildWelcomeEmailBody(username, memberId)
        );
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        sendEmail(
                toEmail,
                "password-reset-code",
                appName + " - Password Reset Code",
                buildPasswordResetEmailBody(code)
        );
    }

    @Async
    public void sendPasswordResetSuccess(String toEmail, String username) {
        sendEmail(
                toEmail,
                "password-reset-success",
                appName + " - Password Changed Successfully",
                buildPasswordResetSuccessBody(username)
        );
    }

    @Async
    public void sendOrderConfirmation(String toEmail, String username, String orderNumber,
                                      String totalAmount, String shippingAddress) {
        sendOrderConfirmation(toEmail, username, orderNumber, totalAmount, shippingAddress, null);
    }

    @Async
    public void sendOrderConfirmation(String toEmail, String username, String orderNumber,
                                      String totalAmount, String shippingAddress, String paymentUrl) {
        sendEmail(
                toEmail,
                "order-confirmation-" + orderNumber,
                appName + " - Order Confirmation #" + orderNumber,
                buildOrderConfirmationBody(username, orderNumber, totalAmount, shippingAddress, paymentUrl)
        );
    }

    private void sendEmail(String toEmail, String emailType, String subject, String body) {
        log.info("Attempting to send {} email to: {} (from: {})", emailType, toEmail, fromEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Successfully sent {} email to: {}", emailType, toEmail);
        } catch (Exception ex) {
            log.error("Failed to send {} email to: {} (from: {}): {}",
                    emailType, toEmail, fromEmail, ex.getMessage(), ex);
        }
    }

    private String buildWelcomeEmailBody(String username, String memberId) {
        return String.format("""
            Hi %s,

            Welcome to %s! We're excited to have you on board.

            Your account has been created successfully. You can now log in and start exploring our features:

            - Browse our wide range of cosmetic products
            - Track your medications and set reminders
            - Add family members to manage their health too
            - Refer friends and earn rewards

            Your Member ID is: %s

            Share this Member ID with friends and family. When they sign up using it, you'll both earn rewards through our Refer & Earn program.

            If you have any questions, feel free to reach out to our support team.

            Best regards,
            The %s Team
            """, username, appName, memberId, appName);
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

    private String buildOrderConfirmationBody(String username, String orderNumber,
                                              String totalAmount, String shippingAddress,
                                              String paymentUrl) {
        String paymentSection = (paymentUrl != null && !paymentUrl.isBlank())
                ? """

            Your order is pending payment. Complete payment using this link:
            %s
            """.formatted(paymentUrl)
                : "";

        return String.format("""
            Hi %s,

            Thank you for your order! We're pleased to confirm that we've received your order.

            Order Number: %s
            Total Amount: %s
            %s
            Shipping Address:
            %s

            We'll send you another email once your order has been shipped.

            If you have any questions about your order, please don't hesitate to contact us.

            Best regards,
            The %s Team
            """, username, orderNumber, totalAmount, paymentSection, shippingAddress, appName);
    }
}
