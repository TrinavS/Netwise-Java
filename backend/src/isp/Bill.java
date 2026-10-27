package isp;

public class Bill implements Billable {
    private static final double GST_RATE = 0.18;
    private final double amount;
    private final double taxRate;

    public Bill(double amount) {
        this(amount, GST_RATE);
    }

    public Bill(double amount, double taxRate) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bill amount cannot be negative.");
        }
        if (taxRate < 0 || taxRate > 1) throw new IllegalArgumentException("Tax rate must be between 0 and 1.");
        this.amount = amount;
        this.taxRate = taxRate;
    }

    public double getAmount() { return amount; }

    @Override
    public double calculateGST() {
        return amount * taxRate;
    }

    @Override
    public double calculateTotal() {
        return amount + calculateGST();
    }

    public void displayBill() {
        System.out.printf("Plan Amount: ₹%.2f%n", amount);
        System.out.printf("GST (%.0f%%): ₹%.2f%n", taxRate * 100, calculateGST());
        System.out.printf("Total Bill: ₹%.2f%n", calculateTotal());
    }
}
