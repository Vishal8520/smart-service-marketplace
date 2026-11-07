package com.example.marketplace.service;

import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.EmailOutbox;
import com.example.marketplace.entity.Payment;
import com.example.marketplace.repository.EmailOutboxRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_DELIVERY_ATTEMPTS = 5;

    private final JavaMailSender mailSender;
    private final EmailOutboxRepository emailOutboxRepository;

    @Value("${spring.mail.username:noreply@servenow.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, EmailOutboxRepository emailOutboxRepository) {
        this.mailSender = mailSender;
        this.emailOutboxRepository = emailOutboxRepository;
    }

    @Async("taskExecutor")
    public void sendBookingSubmittedNotification(Booking booking) {
        String customerEmail = booking.getCustomer().getEmail();
        String customerName = booking.getCustomer().getName();
        String serviceTitle = booking.getService().getTitle();

        String htmlBody = String.format(
                """
                        <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e3e6e6; border-radius: 8px; overflow: hidden;">
                            <div style="background: #131921; padding: 20px; text-align: center; color: #ffffff;">
                                <h2 style="margin: 0; color: #febd69;">ServeNow Marketplace</h2>
                                <p style="margin: 5px 0 0 0; font-size: 14px; color: #b0bbc7;">Booking Received</p>
                            </div>
                            <div style="padding: 24px; color: #0f1111; background: #ffffff;">
                                <p>Hi <strong>%s</strong>,</p>
                                <p>Thank you for submitting your booking for <strong>%s</strong>!</p>
                                <table style="width: 100%%; border-collapse: collapse; margin: 20px 0; background: #f3f3f3; border-radius: 6px;">
                                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6; font-weight: 600;">Booking ID:</td><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6;">#%d</td></tr>
                                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6; font-weight: 600;">Service:</td><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6;">%s</td></tr>
                                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6; font-weight: 600;">Amount:</td><td style="padding: 10px 14px; border-bottom: 1px solid #e3e6e6;">₹%s</td></tr>
                                    <tr><td style="padding: 10px 14px; font-weight: 600;">Scheduled Date:</td><td style="padding: 10px 14px;">%s</td></tr>
                                </table>
                                <p style="color: #555; font-size: 13px;">Please complete the UPI reference payment to unlock provider contact information.</p>
                            </div>
                            <div style="background: #232f3e; padding: 12px; text-align: center; font-size: 12px; color: #888;">
                                © ServeNow Marketplace — On-Demand Professional Services
                            </div>
                        </div>
                        """,
                customerName, serviceTitle, booking.getId(), serviceTitle, booking.getService().getPrice(),
                booking.getScheduledAt());

        queueAndSendEmail(customerEmail, "Booking Submitted — #" + booking.getId() + " (" + serviceTitle + ")", htmlBody);
    }

    @Async("taskExecutor")
    public void sendPaymentSubmittedNotification(Payment payment) {
        Booking booking = payment.getBooking();
        String customerEmail = booking.getCustomer().getEmail();

        String htmlBody = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e3e6e6; border-radius: 8px;">
                            <div style="background: #131921; padding: 18px; text-align: center; color: #fff;">
                                <h3 style="margin: 0; color: #febd69;">Payment Reference Submitted</h3>
                            </div>
                            <div style="padding: 20px; color: #333;">
                                <p>Hi <strong>%s</strong>,</p>
                                <p>We received your UPI payment reference <code>%s</code> for Booking <strong>#%d</strong> (₹%s).</p>
                                <p>Our admin team is reviewing the transaction reference. You will receive an email confirmation once verified.</p>
                            </div>
                        </div>
                        """,
                booking.getCustomer().getName(), payment.getUpiReferenceId(), booking.getId(), payment.getAmount());

        queueAndSendEmail(customerEmail, "Payment Under Review — Booking #" + booking.getId(), htmlBody);
    }

    @Async("taskExecutor")
    public void sendPaymentConfirmedNotification(Payment payment) {
        Booking booking = payment.getBooking();
        String customerEmail = booking.getCustomer().getEmail();
        String providerName = booking.getService().getProvider().getName();
        String providerPhone = booking.getService().getProvider().getPhone() != null
                ? booking.getService().getProvider().getPhone()
                : "Not provided";
        String providerEmail = booking.getService().getProvider().getEmail();

        String htmlBody = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #2da44e; border-radius: 8px; overflow: hidden;">
                            <div style="background: #007600; padding: 20px; text-align: center; color: #fff;">
                                <h2 style="margin: 0;">✔ Payment Confirmed!</h2>
                                <p style="margin: 5px 0 0 0; font-size: 14px;">Booking #%d is now CONFIRMED</p>
                            </div>
                            <div style="padding: 24px; color: #0f1111;">
                                <p>Hi <strong>%s</strong>,</p>
                                <p>Your UPI payment of <strong>₹%s</strong> (Ref: <code>%s</code>) has been verified and confirmed!</p>
                                <div style="background: #f0fff4; border: 1px solid #2da44e; border-radius: 6px; padding: 16px; margin: 20px 0;">
                                    <h4 style="margin: 0 0 10px 0; color: #007600;">🔓 Service Provider Contact Unlocked</h4>
                                    <p style="margin: 4px 0;"><strong>Provider Name:</strong> %s</p>
                                    <p style="margin: 4px 0;"><strong>Phone:</strong> %s</p>
                                    <p style="margin: 4px 0;"><strong>Email:</strong> %s</p>
                                </div>
                            </div>
                        </div>
                        """,
                booking.getId(), booking.getCustomer().getName(), payment.getAmount(), payment.getUpiReferenceId(),
                providerName, providerPhone, providerEmail);

        queueAndSendEmail(customerEmail, "Payment Confirmed! Contact Details Unlocked for Booking #" + booking.getId(), htmlBody);
    }

    @Async("taskExecutor")
    public void sendPaymentRejectedNotification(Payment payment) {
        Booking booking = payment.getBooking();
        String customerEmail = booking.getCustomer().getEmail();

        String htmlBody = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #c40000; border-radius: 8px;">
                            <div style="background: #c40000; padding: 18px; text-align: center; color: #fff;">
                                <h3 style="margin: 0;">Payment Reference Rejected</h3>
                            </div>
                            <div style="padding: 20px; color: #333;">
                                <p>Hi <strong>%s</strong>,</p>
                                <p>Your payment reference <code>%s</code> for Booking <strong>#%d</strong> was rejected by admin.</p>
                                <p><strong>Reason / Note:</strong> %s</p>
                                <p>Please re-submit a valid 12-digit UPI UTR transaction reference string in the marketplace.</p>
                            </div>
                        </div>
                        """,
                booking.getCustomer().getName(), payment.getUpiReferenceId(), booking.getId(), payment.getNotes());

        queueAndSendEmail(customerEmail, "Payment Action Required — Booking #" + booking.getId(), htmlBody);
    }

    /**
     * Persists email to transactional outbox table and executes immediate dispatch attempt.
     */
    @Transactional
    public void queueAndSendEmail(String toEmail, String subject, String htmlContent) {
        EmailOutbox outbox = EmailOutbox.builder()
                .recipient(toEmail)
                .subject(subject)
                .body(htmlContent)
                .status("PENDING")
                .attempts(0)
                .createdAt(Instant.now())
                .build();
        EmailOutbox saved = emailOutboxRepository.save(outbox);

        tryDeliverEmail(saved);
    }

    public boolean tryDeliverEmail(EmailOutbox outbox) {
        outbox.setAttempts(outbox.getAttempts() + 1);
        outbox.setLastAttemptAt(Instant.now());

        try {
            sendHtmlEmail(outbox.getRecipient(), outbox.getSubject(), outbox.getBody());
            outbox.setStatus("SENT");
            outbox.setErrorMessage(null);
            emailOutboxRepository.save(outbox);
            log.info("Email outbox #{} successfully delivered to {}", outbox.getId(), outbox.getRecipient());
            return true;
        } catch (Exception e) {
            outbox.setStatus(outbox.getAttempts() >= MAX_DELIVERY_ATTEMPTS ? "PERMANENTLY_FAILED" : "FAILED");
            outbox.setErrorMessage(e.getMessage());
            emailOutboxRepository.save(outbox);
            log.warn("Email delivery attempt #{} failed for outbox #{}: {}", outbox.getAttempts(), outbox.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Scheduled Outbox Retry Dispatcher:
     * Scans for failed or pending outbox items and retries delivery with exponential backoff.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processOutboxRetries() {
        List<EmailOutbox> pendingEmails = emailOutboxRepository.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
                "FAILED", MAX_DELIVERY_ATTEMPTS, PageRequest.of(0, 20));

        for (EmailOutbox email : pendingEmails) {
            log.info("Retrying email delivery for outbox item #{} to {}", email.getId(), email.getRecipient());
            tryDeliverEmail(email);
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
