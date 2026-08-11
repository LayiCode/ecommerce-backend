package com.Group2.Ecommerce.Order;

import com.Group2.Ecommerce.Common.BrevoEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final BrevoEmailService brevoEmailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${brevo.sender-name}")
    private String shopName;

    public void sendConfirmation(Order order) {
        String subject = "Your " + shopName + " order #" + order.getId() + " is confirmed";
        String html = "<p>Hi " + HtmlUtils.htmlEscape(order.getUser().getName()) + ",</p>"
                + "<p>Thank you for your order! Your payment was successful.</p>"
                + "<p><strong>Order number:</strong> #" + order.getId() + "</p>"
                + itemTable(order)
                + "<p><strong>Total: " + formatAmount(order.getTotalAmount()) + "</strong></p>"
                + "<p>We will email you again once your order ships.</p>";
        brevoEmailService.sendTransactionalEmail(order.getUser().getEmail(), subject, html);
    }

    public void sendShipped(Order order) {
        String subject = "Your " + shopName + " order #" + order.getId() + " has shipped";
        String html = "<p>Hi " + HtmlUtils.htmlEscape(order.getUser().getName()) + ",</p>"
                + "<p>Great news — your order is on its way!</p>"
                + "<p><strong>Order number:</strong> #" + order.getId() + "</p>"
                + itemTable(order);
        brevoEmailService.sendTransactionalEmail(order.getUser().getEmail(), subject, html);
    }

    public void sendReviewRequest(Order order) {
        String subject = "How was your " + shopName + " order #" + order.getId() + "?";
        StringBuilder products = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String name = HtmlUtils.htmlEscape(item.getProduct().getName());
            String url = frontendUrl + "/products/" + item.getProduct().getId();
            products.append("<li><a href=\"").append(url).append("\">").append(name).append("</a></li>");
        }
        String html = "<p>Hi " + HtmlUtils.htmlEscape(order.getUser().getName()) + ",</p>"
                + "<p>We hope you're enjoying your recent order! If you have a moment, "
                + "we'd love to hear your thoughts:</p>"
                + "<ul>" + products + "</ul>"
                + "<p>Tap any product above to leave a review.</p>"
                + "<p>Thanks for shopping with us!</p>";
        brevoEmailService.sendTransactionalEmail(order.getUser().getEmail(), subject, html);
    }

    private String itemTable(Order order) {
        StringBuilder rows = new StringBuilder("<table style=\"border-collapse: collapse; width: 100%;\">")
                .append("<tr><th align=\"left\">Item</th><th align=\"left\">Qty</th><th align=\"right\">Total</th></tr>");
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            rows.append("<tr>")
                    .append("<td>").append(HtmlUtils.htmlEscape(item.getProduct().getName())).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td align=\"right\">").append(formatAmount(lineTotal)).append("</td>")
                    .append("</tr>");
        }
        return rows.append("</table>").toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }
}
