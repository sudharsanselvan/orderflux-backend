package com.orderflux.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — Handles all outgoing email communication.
 *
 * JavaMailSender:
 *   Spring's email abstraction layer.
 *   Auto-configured by Spring Boot when spring-boot-starter-mail
 *   is on classpath AND spring.mail.* properties are set.
 *   We inject it via constructor — Spring provides the Bean.
 *
 * @Async on sendOtpEmail():
 *   Email sending over SMTP takes 1-3 seconds.
 *   Without @Async: user waits 3 seconds for registration response.
 *   With @Async: email sends in background thread.
 *               User gets 201 response immediately.
 *
 *   Requires @EnableAsync on a @Configuration class.
 *   We'll add that to AppConfig.
 *
 * MimeMessage vs SimpleMailMessage:
 *   SimpleMailMessage → plain text only
 *   MimeMessage       → HTML, attachments, inline images
 *   We use MimeMessage for a professional HTML email template.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    /**
     * Send OTP verification email.
     *
     * @Async: runs in a separate thread from Spring's task executor.
     *   The calling method (UserService.registerUser) returns
     *   BEFORE this method finishes executing.
     *
     * @param toEmail    recipient's email address
     * @param otpCode    the 6-digit OTP to include
     * @param firstName  used to personalize the greeting
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String firstName) {
        try {
            log.info("Sending OTP email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();

            /**
             * MimeMessageHelper:
             *   Simplifies building MimeMessage.
             *   multipart = true → allows HTML content.
             *   "UTF-8"         → supports all characters.
             */
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8"
            );

            helper.setFrom(fromEmail, "OrderFlux");
            helper.setTo(toEmail);
            helper.setSubject("Your OrderFlux Verification Code: " + otpCode);
            helper.setText(buildOtpEmailTemplate(firstName, otpCode), true);
            // true = isHtml → renders as HTML not plain text

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MailException | MessagingException e) {
            /**
             * Why catch here and not rethrow?
             *
             * This method runs @Async — in a background thread.
             * If we rethrow, the exception has NOWHERE to go.
             * The main thread already returned 201 to the user.
             *
             * In production: send to a dead letter queue or
             * retry mechanism. For now: log the error clearly.
             *
             * The OTP is still saved in DB — user can use
             * resend-otp endpoint if they don't receive email.
             */
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * HTML email template for OTP.
     *
     * Design principles:
     *   1. Inline CSS — email clients strip <style> tags
     *   2. Table layout — most email clients don't support flexbox
     *   3. Clear OTP display — large font, obvious
     *   4. Expiry warning — user knows they have 5 minutes
     *   5. Security note — tell user to ignore if not requested
     */
    private String buildOtpEmailTemplate(String firstName, String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f4f4f4;
                             font-family: Arial, sans-serif;">
                
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background-color:#f4f4f4; padding: 40px 0;">
                    <tr>
                      <td align="center">
                
                        <!-- Main Container -->
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#ffffff;
                                      border-radius:8px;
                                      box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                
                          <!-- Header -->
                          <tr>
                            <td style="background-color:#1a1a2e;
                                       padding: 30px;
                                       border-radius: 8px 8px 0 0;
                                       text-align: center;">
                              <h1 style="color:#e94560;
                                         margin:0;
                                         font-size:28px;
                                         letter-spacing:2px;">
                                OrderFlux
                              </h1>
                              <p style="color:#ffffff;
                                        margin:8px 0 0 0;
                                        font-size:14px;">
                                Email Verification
                              </p>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding: 40px 30px;">
                
                              <p style="color:#333333;
                                        font-size:16px;
                                        margin:0 0 20px 0;">
                                Hi <strong>%s</strong>,
                              </p>
                
                              <p style="color:#555555;
                                        font-size:15px;
                                        line-height:1.6;
                                        margin:0 0 30px 0;">
                                Thank you for registering with OrderFlux.
                                Use the verification code below to complete
                                your registration.
                              </p>
                
                              <!-- OTP Box -->
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td align="center" style="padding: 0 0 30px 0;">
                                    <div style="background-color:#f8f9fa;
                                                border: 2px dashed #e94560;
                                                border-radius: 8px;
                                                padding: 25px;
                                                display: inline-block;">
                                      <p style="margin:0 0 8px 0;
                                                color:#888888;
                                                font-size:13px;
                                                text-transform:uppercase;
                                                letter-spacing:1px;">
                                        Your Verification Code
                                      </p>
                                      <p style="margin:0;
                                                color:#1a1a2e;
                                                font-size:42px;
                                                font-weight:bold;
                                                letter-spacing:12px;">
                                        %s
                                      </p>
                                    </div>
                                  </td>
                                </tr>
                              </table>
                
                              <!-- Expiry Warning -->
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="background-color:#fff3cd;
                                             border-left: 4px solid #ffc107;
                                             padding: 15px;
                                             border-radius: 0 4px 4px 0;
                                             margin-bottom: 20px;">
                                    <p style="margin:0;
                                              color:#856404;
                                              font-size:14px;">
                                      ⏱ This code expires in
                                      <strong>%d minutes</strong>.
                                      Do not share it with anyone.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                
                              <!-- Security Note -->
                              <p style="color:#888888;
                                        font-size:13px;
                                        margin: 20px 0 0 0;
                                        line-height:1.6;">
                                If you did not create an account with OrderFlux,
                                please ignore this email. No action is required.
                              </p>
                
                            </td>
                          </tr>
                
                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#f8f9fa;
                                       padding: 20px 30px;
                                       border-radius: 0 0 8px 8px;
                                       text-align: center;
                                       border-top: 1px solid #e9ecef;">
                              <p style="margin:0;
                                        color:#aaaaaa;
                                        font-size:12px;">
                                © 2026 OrderFlux. All rights reserved.
                              </p>
                            </td>
                          </tr>
                
                        </table>
                        <!-- End Main Container -->
                
                      </td>
                    </tr>
                  </table>
                
                </body>
                </html>
                """.formatted(firstName, otpCode, otpExpiryMinutes);
    }
}