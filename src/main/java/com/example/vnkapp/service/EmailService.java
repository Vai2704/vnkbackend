package com.example.vnkapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Sends transactional emails through Amazon SES (SESv2 API).
 * Authentication uses the ECS task role, so no credentials live in config.
 * Enabled only when {@code app.ses.enabled=true}; otherwise callers get no EmailService
 * (they hold an {@code Optional<EmailService>}) and emails are silently skipped.
 */
@Service
@ConditionalOnProperty(name = "app.ses.enabled", havingValue = "true")
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SesV2Client sesClient;

    @Value("${app.ses.from-email}")
    private String fromEmail;

    @Value("${app.ses.from-name:VNK Supplements}")
    private String fromName;

    @Value("${app.ses.configuration-set:}")
    private String configurationSet;

    @Value("${app.name:VNK App}")
    private String appName;

    public EmailService(SesV2Client sesClient) {
        this.sesClient = sesClient;
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

    /**
     * Sends an order status update (e.g. CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED).
     *
     * @param status      raw status name; shown to the customer in a friendly form
     * @param extraDetail optional extra line, e.g. tracking info or cancellation reason (may be null)
     */
    @Async
    public void sendOrderStatusUpdate(String toEmail, String username, String orderNumber,
                                      String status, String extraDetail) {
        String friendly = friendlyStatus(status);
        sendEmail(
                toEmail,
                "order-status-" + status + "-" + orderNumber,
                appName + " - Order #" + orderNumber + " " + friendly,
                buildOrderStatusBody(username, orderNumber, friendly, extraDetail)
        );
    }

    private void sendEmail(String toEmail, String emailType, String subject, String body) {
        log.info("Attempting to send {} email to: {}", emailType, toEmail);
        try {
            SendEmailRequest.Builder request = SendEmailRequest.builder()
                    .fromEmailAddress(fromName + " <" + fromEmail + ">")
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(subject).charset("UTF-8").build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(body).charset("UTF-8").build())
                                            .build())
                                    .build())
                            .build());
            if (configurationSet != null && !configurationSet.isBlank()) {
                request.configurationSetName(configurationSet);
            }
            String messageId = sesClient.sendEmail(request.build()).messageId();
            log.info("Sent {} email to: {} (SES messageId: {})", emailType, toEmail, messageId);
        } catch (Exception ex) {
            // Email must never break the business flow (sign-up, order placement).
            log.error("Failed to send {} email to: {}: {}", emailType, toEmail, ex.getMessage(), ex);
        }
    }

    private String friendlyStatus(String status) {
        return switch (status == null ? "" : status) {
            case "CONFIRMED" -> "Confirmed";
            case "PROCESSING" -> "is Being Processed";
            case "SHIPPED" -> "Has Been Shipped";
            case "OUT_FOR_DELIVERY" -> "is Out for Delivery";
            case "DELIVERED" -> "Has Been Delivered";
            case "CANCELLED" -> "Cancelled";
            case "RETURNED" -> "Returned";
            case "REFUNDED" -> "Refunded";
            default -> "Updated";
        };
    }

    private String buildOrderStatusBody(String username, String orderNumber,
                                        String friendlyStatus, String extraDetail) {
        String detail = (extraDetail != null && !extraDetail.isBlank()) ? "\n" + extraDetail + "\n" : "";
        return String.format("""
            Hi %s,

            There is an update on your order #%s.

            Status: %s
            %s
            You can view the full details of your order in the %s app.

            If you have any questions, please contact our support team.

            Best regards,
            The %s Team
            """, username, orderNumber, friendlyStatus, detail, appName, appName);
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
