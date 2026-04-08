package com.omkaar.bank.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text emails via SMTP.
 * Configuration lives in application.properties (spring.mail.*).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a login OTP email to the user.
     *
     * @param toEmail   recipient address
     * @param userName  first name to personalise the greeting
     * @param otp       6-digit code
     */
    public void sendOtp(String toEmail, String userName, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("SecureBank — Your Login OTP");
        msg.setText(buildOtpBody(userName, otp));
        mailSender.send(msg);
    }

    /**
     * Send a confirmation email after a successful login.
     */
    public void sendLoginAlert(String toEmail, String userName) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("SecureBank — New Login Detected");
        msg.setText(
            "Hi " + userName + ",\n\n" +
            "A successful login was recorded on your SecureBank account.\n\n" +
            "If this wasn't you, please contact support immediately and change your password.\n\n" +
            "— SecureBank Security Team"
        );
        mailSender.send(msg);
    }

    /**
     * Send a welcome email after successful registration.
     */
    public void sendWelcome(String toEmail, String userName, String accountNumber) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Welcome to SecureBank!");
        msg.setText(
            "Hi " + userName + ",\n\n" +
            "Your SecureBank account has been created successfully.\n\n" +
            "Your Account Number: " + accountNumber + "\n\n" +
            "You can now log in and start banking.\n\n" +
            "— SecureBank Team"
        );
        mailSender.send(msg);
    }

    /* ── private helpers ── */

    private static String buildOtpBody(String name, String otp) {
        return  "Hi " + name + ",\n\n" +
                "Your SecureBank login OTP is:\n\n" +
                "        " + otp + "\n\n" +
                "This code is valid for 5 minutes and can only be used once.\n" +
                "Do not share it with anyone — SecureBank will never ask for your OTP.\n\n" +
                "If you did not attempt to log in, please ignore this email.\n\n" +
                "— SecureBank Security Team";
    }
}
