package com.example.marketplace.service;

import com.example.marketplace.entity.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(booking.getCustomer().getEmail());
            message.setSubject("Booking Confirmed — " + booking.getService().getTitle());
            message.setText(String.format(
                    "Hi %s,\n\nYour booking for \"%s\" has been received.\n" +
                            "Scheduled at: %s\nBooking ID: %d\n\nThank you for using Smart Service Marketplace!",
                    booking.getCustomer().getName(),
                    booking.getService().getTitle(),
                    booking.getScheduledAt(),
                    booking.getId()));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email for booking {}: {}",
                    booking.getId(), e.getMessage());
        }
    }

    @Async
    public void sendStatusUpdateNotification(Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(booking.getCustomer().getEmail());
            message.setSubject("Booking Status Updated — " + booking.getService().getTitle());
            message.setText(String.format(
                    "Hi %s,\n\nYour booking (ID: %d) for \"%s\" status has changed to: %s\n\nThank you!",
                    booking.getCustomer().getName(),
                    booking.getId(),
                    booking.getService().getTitle(),
                    booking.getStatus()));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send status update email for booking {}: {}",
                    booking.getId(), e.getMessage());
        }
    }
}
