package com.example.marketplace.service;

import com.example.marketplace.entity.EmailOutbox;
import com.example.marketplace.repository.EmailOutboxRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailOutboxRepository emailOutboxRepository;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@servenow.com");
    }

    @Test
    @DisplayName("queueAndSendEmail saves outbox entry and successfully delivers email")
    void queueAndSendEmail_success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailOutboxRepository.save(any(EmailOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailService.queueAndSendEmail("user@example.com", "Welcome", "<p>Hello</p>");

        verify(emailOutboxRepository, atLeastOnce()).save(any(EmailOutbox.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("tryDeliverEmail marks outbox as FAILED when mail sender throws exception")
    void tryDeliverEmail_failureUpdatesOutboxStatus() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        EmailOutbox outbox = EmailOutbox.builder()
                .id(1L)
                .recipient("user@example.com")
                .subject("Booking confirmation")
                .body("<p>Details</p>")
                .status("PENDING")
                .attempts(0)
                .createdAt(Instant.now())
                .build();

        when(emailOutboxRepository.save(any(EmailOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean success = emailService.tryDeliverEmail(outbox);

        assertFalse(success);
        assertEquals("FAILED", outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertNotNull(outbox.getErrorMessage());
        verify(emailOutboxRepository, times(1)).save(outbox);
    }

    @Test
    @DisplayName("tryDeliverEmail marks outbox as PERMANENTLY_FAILED after 5 failed attempts")
    void tryDeliverEmail_permanentlyFailedAfterMaxAttempts() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server error")).when(mailSender).send(any(MimeMessage.class));

        EmailOutbox outbox = EmailOutbox.builder()
                .id(2L)
                .recipient("user@example.com")
                .subject("Booking confirmation")
                .body("<p>Details</p>")
                .status("FAILED")
                .attempts(4) // This attempt will be #5 (max)
                .createdAt(Instant.now())
                .build();

        when(emailOutboxRepository.save(any(EmailOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean success = emailService.tryDeliverEmail(outbox);

        assertFalse(success);
        assertEquals("PERMANENTLY_FAILED", outbox.getStatus());
        assertEquals(5, outbox.getAttempts());
    }

    @Test
    @DisplayName("processOutboxRetries retries failed emails")
    void processOutboxRetries_retriesPendingFailedEmails() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailOutbox failedEmail = EmailOutbox.builder()
                .id(3L)
                .recipient("retry@example.com")
                .subject("Retry Subject")
                .body("<p>Retry Body</p>")
                .status("FAILED")
                .attempts(1)
                .createdAt(Instant.now())
                .build();

        when(emailOutboxRepository.findByStatusAndAttemptsLessThanOrderByCreatedAtAsc(eq("FAILED"), eq(5), any(Pageable.class)))
                .thenReturn(List.of(failedEmail));
        when(emailOutboxRepository.save(any(EmailOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailService.processOutboxRetries();

        assertEquals("SENT", failedEmail.getStatus());
        assertEquals(2, failedEmail.getAttempts());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
