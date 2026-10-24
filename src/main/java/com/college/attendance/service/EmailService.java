
package com.college.attendance.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${brevo.api.key:}")
    private String apiKey;

    @Value("${brevo.sender.email:}")
    private String senderEmail;

    @Value("${brevo.sender.name:EduTrack}")
    private String senderName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void sendOtpEmail(String toEmail, String otp) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "Brevo API key or sender email is not configured");
        }

        try {
            String json = """
                    {
                      "sender": {
                        "name": "%s",
                        "email": "%s"
                      },
                      "to": [{"email": "%s"}],
                      "subject": "Your Login OTP",
                      "textContent": "Your One-Time Password (OTP) for login is: %s\\n\\nThis OTP is valid for 5 minutes."
                    }
                    """.formatted(
                    escapeJson(senderName),
                    escapeJson(senderEmail),
                    escapeJson(toEmail),
                    escapeJson(otp));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Brevo email request failed with HTTP status "
                                + response.statusCode());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Email sending was interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to send OTP email", e);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}






// package com.college.attendance.service;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.stereotype.Service;

// @Service
// public class EmailService {

//     private final JavaMailSender mailSender;

//     @Value("${spring.mail.username:noreply@college.edu}")
//     private String fromEmail;

//     public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
//         this.mailSender = mailSender;
//     }

//     public void sendOtpEmail(String toEmail, String otp) {
//         if (mailSender == null) {
//             return;
//         }
//         SimpleMailMessage message = new SimpleMailMessage();
//         message.setFrom(fromEmail);
//         message.setTo(toEmail);
//         message.setSubject("Your Login OTP");
//         message.setText("Your One-Time Password (OTP) for login is: " + otp + "\n\nThis OTP is valid for 5 minutes.");
//         mailSender.send(message);
//     }
// }
