package isp;

public class Invoice {
    private final String id;
    private final int customerId;
    private final String month;
    private final double amount;
    private final Bill bill;
    private final String dueDate;
    private String status;

    public Invoice(String id, int customerId, String month, double amount,
                   double taxRate, String dueDate, String status) {
        this.id = id;
        this.customerId = customerId;
        this.month = month;
        this.amount = amount;
        this.bill = new Bill(amount, taxRate);
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getId() { return id; }
    public int getCustomerId() { return customerId; }
    public String getMonth() { return month; }
    public double getAmount() { return amount; }
    public double getTax() { return bill.calculateGST(); }
    public double getTotal() { return bill.calculateTotal(); }
    public String getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public void markPaid() { status = "PAID"; }

    public String getSummary() {
        StringBuffer summary = new StringBuffer();
        summary.append(id).append(" | ").append(month).append(" | ₹")
                .append(String.format("%.2f", getTotal())).append(" | ").append(status);
        return summary.toString();
    }
}
