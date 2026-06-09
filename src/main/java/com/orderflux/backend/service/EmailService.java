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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    // ─── Inner Record ─────────────────────────────────────────

    /**
     * OrderItemInfo — data carrier for order confirmation email.
     * Plain Java record — no JPA, no Lombok needed.
     * Keeps email concerns separate from domain model.
     */
    public record OrderItemInfo(
            String productName,
            int quantity,
            String unitPrice,
            String totalPrice
    ) {}

    // ─── OTP Email ────────────────────────────────────────────

    /**
     * Send OTP verification email.
     *
     * @Async: runs in background thread.
     * Caller returns immediately — user doesn't wait for email.
     */
    @Async
    public void sendOtpEmail(String toEmail,
                              String otpCode,
                              String firstName) {
        try {
            log.info("Sending OTP email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8"
            );

            helper.setFrom(fromEmail, "OrderFlux");
            helper.setTo(toEmail);
            helper.setSubject("Your OrderFlux Verification Code: " + otpCode);
            helper.setText(buildOtpEmailTemplate(firstName, otpCode), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MailException | MessagingException e) {
            log.error("Failed to send OTP email to {}: {}",
                    toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─── Order Confirmation Email ─────────────────────────────

    /**
     * Send order confirmation email after successful order placement.
     *
     * @Async: sends in background thread.
     * User gets 201 Created response immediately.
     * Email arrives within seconds.
     *
     * Email failure must NEVER fail the order.
     * Order is already saved — that's what matters.
     */
    @Async
    public void sendOrderConfirmationEmail(String toEmail,
                                            String firstName,
                                            String orderNumber,
                                            String status,
                                            String totalAmount,
                                            String shippingAddress,
                                            List<OrderItemInfo> items) {
        try {
            log.info("Sending order confirmation email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8"
            );

            helper.setFrom(fromEmail, "OrderFlux");
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmed — " + orderNumber);
            helper.setText(
                buildOrderConfirmationTemplate(
                    firstName, orderNumber, status,
                    totalAmount, shippingAddress, items
                ),
                true
            );

            mailSender.send(message);
            log.info("Order confirmation email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send order confirmation to {}: {}",
                    toEmail, e.getMessage());
        }
    }

    // ─── HTML Templates ───────────────────────────────────────

    private String buildOtpEmailTemplate(String firstName, String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0;
                             background-color:#f4f4f4;
                             font-family: Arial, sans-serif;">

                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background-color:#f4f4f4; padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#ffffff;
                                      border-radius:8px;
                                      box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                          <!-- Header -->
                          <tr>
                            <td style="background-color:#1a1a2e;
                                       padding:30px;
                                       border-radius:8px 8px 0 0;
                                       text-align:center;">
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
                            <td style="padding:40px 30px;">

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
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0">
                                <tr>
                                  <td align="center"
                                      style="padding:0 0 30px 0;">
                                    <div style="background-color:#f8f9fa;
                                                border:2px dashed #e94560;
                                                border-radius:8px;
                                                padding:25px;
                                                display:inline-block;">
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
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0">
                                <tr>
                                  <td style="background-color:#fff3cd;
                                             border-left:4px solid #ffc107;
                                             padding:15px;
                                             border-radius:0 4px 4px 0;">
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

                              <p style="color:#888888;
                                        font-size:13px;
                                        margin:20px 0 0 0;
                                        line-height:1.6;">
                                If you did not create an account with
                                OrderFlux, please ignore this email.
                              </p>

                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#f8f9fa;
                                       padding:20px 30px;
                                       border-radius:0 0 8px 8px;
                                       text-align:center;
                                       border-top:1px solid #e9ecef;">
                              <p style="margin:0;
                                        color:#aaaaaa;
                                        font-size:12px;">
                                © 2026 OrderFlux. All rights reserved.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>

                </body>
                </html>
                """.formatted(firstName, otpCode, otpExpiryMinutes);
    }

    private String buildOrderConfirmationTemplate(
            String firstName,
            String orderNumber,
            String status,
            String totalAmount,
            String shippingAddress,
            List<OrderItemInfo> items) {

        // Build item rows dynamically
        StringBuilder itemRows = new StringBuilder();
        for (OrderItemInfo item : items) {
            itemRows.append("""
                    <tr>
                      <td style="padding:12px;
                                 border-bottom:1px solid #f0f0f0;
                                 color:#333333;
                                 font-size:14px;">%s</td>
                      <td style="padding:12px;
                                 border-bottom:1px solid #f0f0f0;
                                 color:#333333;
                                 font-size:14px;
                                 text-align:center;">%d</td>
                      <td style="padding:12px;
                                 border-bottom:1px solid #f0f0f0;
                                 color:#333333;
                                 font-size:14px;
                                 text-align:right;">₹%s</td>
                      <td style="padding:12px;
                                 border-bottom:1px solid #f0f0f0;
                                 color:#1a1a2e;
                                 font-size:14px;
                                 font-weight:bold;
                                 text-align:right;">₹%s</td>
                    </tr>
                    """.formatted(
                        item.productName(),
                        item.quantity(),
                        item.unitPrice(),
                        item.totalPrice()
                    ));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0;
                             background-color:#f4f4f4;
                             font-family:Arial, sans-serif;">

                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background-color:#f4f4f4; padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background-color:#ffffff;
                                      border-radius:8px;
                                      box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                          <!-- Header -->
                          <tr>
                            <td style="background-color:#1a1a2e;
                                       padding:30px;
                                       border-radius:8px 8px 0 0;
                                       text-align:center;">
                              <h1 style="color:#e94560;
                                         margin:0;
                                         font-size:28px;
                                         letter-spacing:2px;">
                                OrderFlux
                              </h1>
                              <p style="color:#ffffff;
                                        margin:8px 0 0 0;
                                        font-size:14px;">
                                Order Confirmation
                              </p>
                            </td>
                          </tr>

                          <!-- Success Banner -->
                          <tr>
                            <td style="background-color:#28a745;
                                       padding:20px;
                                       text-align:center;">
                              <p style="margin:0;
                                        color:#ffffff;
                                        font-size:18px;
                                        font-weight:bold;">
                                ✅ Your Order Has Been Placed Successfully!
                              </p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:35px 30px;">

                              <p style="color:#333333;
                                        font-size:16px;
                                        margin:0 0 20px 0;">
                                Hi <strong>%s</strong>,
                              </p>
                              <p style="color:#555555;
                                        font-size:15px;
                                        line-height:1.6;
                                        margin:0 0 25px 0;">
                                Thank you for shopping with OrderFlux!
                                Your order has been received and is being
                                processed. Here are your order details:
                              </p>

                              <!-- Order Info Box -->
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0"
                                     style="background-color:#f8f9fa;
                                            border-radius:8px;
                                            margin-bottom:25px;">
                                <tr>
                                  <td style="padding:20px;">
                                    <table width="100%%">
                                      <tr>
                                        <td style="color:#888888;
                                                   font-size:13px;
                                                   padding-bottom:8px;">
                                          ORDER NUMBER
                                        </td>
                                        <td style="color:#1a1a2e;
                                                   font-size:14px;
                                                   font-weight:bold;
                                                   text-align:right;
                                                   padding-bottom:8px;">
                                          %s
                                        </td>
                                      </tr>
                                      <tr>
                                        <td style="color:#888888;
                                                   font-size:13px;
                                                   padding-bottom:8px;">
                                          STATUS
                                        </td>
                                        <td style="text-align:right;
                                                   padding-bottom:8px;">
                                          <span style="background-color:#fff3cd;
                                                       color:#856404;
                                                       padding:3px 10px;
                                                       border-radius:12px;
                                                       font-size:12px;
                                                       font-weight:bold;">
                                            %s
                                          </span>
                                        </td>
                                      </tr>
                                      <tr>
                                        <td style="color:#888888;
                                                   font-size:13px;">
                                          SHIPPING TO
                                        </td>
                                        <td style="color:#333333;
                                                   font-size:13px;
                                                   text-align:right;">
                                          %s
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <!-- Items Table -->
                              <h3 style="color:#1a1a2e;
                                         font-size:16px;
                                         margin:0 0 15px 0;
                                         border-bottom:2px solid #e94560;
                                         padding-bottom:8px;">
                                Order Items
                              </h3>
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0"
                                     style="margin-bottom:25px;">
                                <tr style="background-color:#1a1a2e;">
                                  <th style="padding:12px;
                                             color:#ffffff;
                                             font-size:13px;
                                             text-align:left;">
                                    Product
                                  </th>
                                  <th style="padding:12px;
                                             color:#ffffff;
                                             font-size:13px;
                                             text-align:center;">
                                    Qty
                                  </th>
                                  <th style="padding:12px;
                                             color:#ffffff;
                                             font-size:13px;
                                             text-align:right;">
                                    Unit Price
                                  </th>
                                  <th style="padding:12px;
                                             color:#ffffff;
                                             font-size:13px;
                                             text-align:right;">
                                    Total
                                  </th>
                                </tr>
                                %s
                              </table>

                              <!-- Total Amount -->
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0">
                                <tr>
                                  <td style="padding:15px 20px;
                                             background-color:#1a1a2e;
                                             border-radius:8px;">
                                    <table width="100%%">
                                      <tr>
                                        <td style="color:#ffffff;
                                                   font-size:16px;
                                                   font-weight:bold;">
                                          Total Amount
                                        </td>
                                        <td style="color:#e94560;
                                                   font-size:20px;
                                                   font-weight:bold;
                                                   text-align:right;">
                                          ₹%s
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <!-- Next Steps -->
                              <table width="100%%" cellpadding="0"
                                     cellspacing="0"
                                     style="margin-top:25px;">
                                <tr>
                                  <td style="background-color:#e8f5e9;
                                             border-left:4px solid #28a745;
                                             padding:15px;
                                             border-radius:0 4px 4px 0;">
                                    <p style="margin:0;
                                              color:#1b5e20;
                                              font-size:14px;
                                              line-height:1.6;">
                                      📦 We will notify you when your order
                                      is shipped. You can track your order
                                      status by logging into your account.
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <p style="color:#888888;
                                        font-size:13px;
                                        margin:20px 0 0 0;
                                        line-height:1.6;">
                                Questions? Contact us at
                                support@orderflux.com
                              </p>

                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#f8f9fa;
                                       padding:20px 30px;
                                       border-radius:0 0 8px 8px;
                                       text-align:center;
                                       border-top:1px solid #e9ecef;">
                              <p style="margin:0;
                                        color:#aaaaaa;
                                        font-size:12px;">
                                © 2026 OrderFlux. All rights reserved.
                              </p>
                              <p style="margin:8px 0 0 0;
                                        color:#aaaaaa;
                                        font-size:11px;">
                                This is an automated email.
                                Please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>

                </body>
                </html>
                """.formatted(
                    firstName,
                    orderNumber,
                    status,
                    shippingAddress,
                    itemRows.toString(),
                    totalAmount
                );
    }
}