package isp;

import java.time.LocalDate;

/** Immutable record of a payment attempt applied to an invoice. */
public class PaymentRecord {
    private final String paymentId;
    private final double amount;
    private final LocalDate paidOn;
    private final String method;

    public PaymentRecord(String paymentId, double amount, LocalDate paidOn, String method) {
        if (paymentId == null || paymentId.isBlank() || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("A payment needs an ID and a positive finite amount.");
        }
        this.paymentId = paymentId;
        this.amount = amount;
        this.paidOn = paidOn == null ? LocalDate.now() : paidOn;
        this.method = method == null || method.isBlank() ? "SIMULATED" : method;
    }

    public String getPaymentId() { return paymentId; }
    public double getAmount() { return amount; }
    public LocalDate getPaidOn() { return paidOn; }
    public String getMethod() { return method; }
}
