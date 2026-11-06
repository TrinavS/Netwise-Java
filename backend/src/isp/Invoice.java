package isp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An issued invoice is an immutable billing snapshot; only payment status/history can change. */
public class Invoice {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final String id;
    private final int customerId;
    private final String customerName;
    private final YearMonth billingPeriod;
    private final int planId;
    private final String planName;
    private final double planPrice;
    private final double taxRate;
    private final Bill bill;
    private final double taxAmount;
    private final double totalAmount;
    private final LocalDate dueDate;
    private final List<PaymentRecord> paymentHistory = new ArrayList<>();
    private String paymentStatus;

    public Invoice(String id, int customerId, String customerName, YearMonth billingPeriod, int planId,
                   String planName, double planPrice, double taxRate, LocalDate dueDate,
                   String paymentStatus, List<PaymentRecord> paymentHistory) {
        if (id == null || id.isBlank() || billingPeriod == null || dueDate == null) {
            throw new IllegalArgumentException("Invoice ID, billing period, and due date are required.");
        }
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName == null ? "Customer #" + customerId : customerName;
        this.billingPeriod = billingPeriod;
        this.planId = planId;
        this.planName = planName == null ? "Internet service" : planName;
        this.planPrice = planPrice;
        this.taxRate = taxRate;
        this.bill = new Bill(planPrice, taxRate);
        this.taxAmount = bill.calculateGST();
        this.totalAmount = bill.calculateTotal();
        this.dueDate = dueDate;
        this.paymentStatus = "PAID".equalsIgnoreCase(paymentStatus) ? "PAID" : "UNPAID";
        if (paymentHistory != null) this.paymentHistory.addAll(paymentHistory);
    }

    public String getId() { return id; }
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public YearMonth getBillingPeriod() { return billingPeriod; }
    public String getMonth() { return billingPeriod.format(MONTH_FORMAT); }
    public String getPeriodKey() { return billingPeriod.toString(); }
    public int getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public double getPlanPrice() { return planPrice; }
    /** Kept as an alias for older UI/report code. */
    public double getAmount() { return getPlanPrice(); }
    public double getTaxRate() { return taxRate; }
    public double getTax() { return taxAmount; }
    public double getTotal() { return totalAmount; }
    public LocalDate getDueDateValue() { return dueDate; }
    public String getDueDate() { return dueDate.toString(); }
    public synchronized String getStatus() {
        if ("PAID".equals(paymentStatus)) return paymentStatus;
        return dueDate.isBefore(LocalDate.now()) ? "OVERDUE" : "UNPAID";
    }
    public synchronized String getPaymentStatus() { return paymentStatus; }
    public synchronized List<PaymentRecord> getPaymentHistory() {
        return Collections.unmodifiableList(new ArrayList<>(paymentHistory));
    }
    public synchronized void markPaid(PaymentRecord payment) {
        if ("PAID".equals(paymentStatus)) throw new IllegalStateException("Invoice has already been paid.");
        if (payment == null || Math.abs(payment.getAmount() - getTotal()) > 0.001) {
            throw new IllegalArgumentException("The payment must cover the invoice total.");
        }
        paymentHistory.add(payment);
        paymentStatus = "PAID";
    }

    public String getSummary() {
        StringBuffer summary = new StringBuffer();
        summary.append(id).append(" | ").append(getMonth()).append(" | ₹")
                .append(String.format("%.2f", getTotal())).append(" | ").append(getStatus());
        return summary.toString();
    }
}
