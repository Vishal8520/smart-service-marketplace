package com.example.marketplace.service;

import com.example.marketplace.dto.request.PaymentOrderRequest;
import com.example.marketplace.dto.response.PaymentResponse;
import com.example.marketplace.entity.Booking;
import com.example.marketplace.entity.Payment;
import com.example.marketplace.entity.PaymentStatus;
import com.example.marketplace.entity.User;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.exception.UnauthorizedException;
import com.example.marketplace.repository.BookingRepository;
import com.example.marketplace.repository.PaymentRepository;
import com.example.marketplace.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepo, BookingRepository bookingRepo, UserRepository userRepo) {
        this.paymentRepo = paymentRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public PaymentResponse createOrder(PaymentOrderRequest request, String customerEmail) {
        User customer = getUserByEmail(customerEmail);
        Booking booking = bookingRepo.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new UnauthorizedException("This booking does not belong to you");
        }
        if (paymentRepo.findByBookingId(booking.getId()).isPresent()) {
            throw new IllegalStateException("Payment already exists for this booking");
        }

        BigDecimal amount = booking.getService().getPrice();
        String gatewayOrderId = createRazorpayOrder(amount);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .gatewayOrderId(gatewayOrderId)
                .build();

        return toResponse(paymentRepo.save(payment));
    }

    @Transactional
    public void handleWebhook(Map<String, String> params) {
        String razorpayOrderId = params.get("razorpay_order_id");
        String razorpayPaymentId = params.get("razorpay_payment_id");
        String razorpaySignature = params.get("razorpay_signature");

        Payment payment = paymentRepo.findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", razorpayOrderId));

        boolean signatureValid = verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (signatureValid) {
            payment.setGatewayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepo.save(payment);
    }

    private String createRazorpayOrder(BigDecimal amount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject options = new JSONObject();
            options.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            options.put("currency", "INR");
            options.put("receipt", "receipt_" + System.currentTimeMillis());
            Order order = client.orders.create(options);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment gateway error. Please try again later.");
        }
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBooking().getId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .gatewayOrderId(p.getGatewayOrderId())
                .gatewayPaymentId(p.getGatewayPaymentId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
