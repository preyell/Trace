package com.sybyl.trace.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.public-base-url}")
    private String baseUrl;
    /**
     * Sends an HTML OTP to the specified user email synchronously.
     * @return true if sent successfully, false otherwise.
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // true flag indicates multipart message (HTML)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔒 Your Trace Verification Code");

            // Modern, responsive HTML template with inline styles
            String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 40px 20px;">
                    <div style="max-width: 450px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                        <div style="padding: 30px 40px; text-align: center;">
                            <h2 style="color: #0f172a; margin-top: 0; margin-bottom: 8px;">Verification Code</h2>
                            <p style="color: #64748b; font-size: 15px; margin-bottom: 30px;">Please use the single-use security code below to complete your sign-in request.</p>
                            
                            <div style="background-color: #f8fafc; border: 1px dashed #cbd5e1; padding: 15px 20px; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #2563eb; display: inline-block; border-radius: 6px; margin-bottom: 25px;">
                                %s
                            </div>
                            
                            <p style="color: #94a3b8; font-size: 13px; margin-top: 10px;">
                                This code is highly time-sensitive and will expire in <strong style="color: #64748b;">5 minutes</strong>.
                            </p>
                        </div>
                        <div style="background-color: #fafafa; padding: 20px; text-align: center; border-top: 1px solid #f1f5f9;">
                            <p style="margin: 0; color: #cbd5e1; font-size: 11px; line-height: 1.4;">
                                If you did not make this authorization request, you can safely ignore this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(otp);

            helper.setText(htmlTemplate, true); // true indicates this is HTML text
            
            // This is synchronous (blocking). It will wait until the SMTP server accepts it.
            mailSender.send(mimeMessage);
            
            logger.info("HTML OTP email successfully dispatched to: {}", toEmail);
            return true;
            
        } catch (Exception e) {
            // Catches MailException or MessagingException
            logger.error("SMTP Delivery Failure to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
    
    
    /**
     * Sends a formatted HTML notification to the Sales Manager when a new order is created.
     */
    public boolean sendOrderCreatedEmail(String toEmail, String orderId, String customerName, String location, String description) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // true flag indicates multipart message (HTML)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Action Required: New Sales Order Created (" + orderId + ")");
            
            String desc = description != null && !description.isBlank() ? description : "N/A";
            String orderUrl = baseUrl + "/orders/" + orderId;

            // Modern, responsive HTML template for enterprise alerts
            String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 40px 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                        <div style="background-color: #0f172a; padding: 20px 40px;">
                            <h2 style="color: #ffffff; margin: 0; font-size: 20px;">New Sales Order Action Required</h2>
                        </div>
                        <div style="padding: 30px 40px;">
                            <p style="color: #334155; font-size: 16px; margin-bottom: 20px;">Dear Sales Manager,</p>
                            <p style="color: #64748b; font-size: 15px; margin-bottom: 30px;">A new sales order has been successfully generated in the Trace system and is ready for your review.</p>
                            
                            <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 20px; margin-bottom: 30px;">
                                <h3 style="color: #0f172a; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; margin-top: 0; margin-bottom: 15px; border-bottom: 1px solid #e2e8f0; padding-bottom: 10px;">Order Details</h3>
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 8px 0; color: #64748b; font-size: 14px; width: 120px;"><strong>Order ID:</strong></td>
                                        <td style="padding: 8px 0; color: #0f172a; font-size: 14px; font-weight: 500;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; color: #64748b; font-size: 14px;"><strong>Customer:</strong></td>
                                        <td style="padding: 8px 0; color: #0f172a; font-size: 14px;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; color: #64748b; font-size: 14px;"><strong>Location:</strong></td>
                                        <td style="padding: 8px 0; color: #0f172a; font-size: 14px;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px 0; color: #64748b; font-size: 14px; vertical-align: top;"><strong>Description:</strong></td>
                                        <td style="padding: 8px 0; color: #0f172a; font-size: 14px;">%s</td>
                                    </tr>
                                </table>
                            </div>
                            
                            <div style="text-align: center; margin-bottom: 20px;">
                                <a href="%s" style="background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: 600; font-size: 15px; display: inline-block;">Review Order in Trace</a>
                            </div>
                        </div>
                        <div style="background-color: #fafafa; padding: 20px; text-align: center; border-top: 1px solid #f1f5f9;">
                            <p style="margin: 0; color: #94a3b8; font-size: 12px;">This is an automated security message from the Trace System. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(orderId, customerName, location, desc, orderUrl);

            helper.setText(htmlTemplate, true); 
            mailSender.send(mimeMessage);
            
            logger.info("Order creation HTML notification successfully dispatched to: {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("SMTP Delivery Failure for Order {} to {}: {}", orderId, toEmail, e.getMessage());
            return false;
        }
    }
}