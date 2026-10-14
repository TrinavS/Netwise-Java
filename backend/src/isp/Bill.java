package isp;

public class Bill implements Billable {
    private static final double GST_RATE = 0.18;
    private final double amount;

    public Bill(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bill amount cannot be negative.");
        }
        this.amount = amount;
    }

    public double getAmount() { return amount; }

    @Override
    public double calculateGST() {
        return amount * GST_RATE;
    }

    @Override
    public double calculateTotal() {
        return amount + calculateGST();
    }

    public void displayBill() {
        System.out.printf("Plan Amount: ₹%.2f%n", amount);
        System.out.printf("GST (18%%): ₹%.2f%n", calculateGST());
        System.out.printf("Total Bill: ₹%.2f%n", calculateTotal());
    }
}
