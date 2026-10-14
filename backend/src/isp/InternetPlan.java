package isp;

public abstract class InternetPlan {
    private final int planId;
    private final String planName;
    private final double price;
    private final int dataLimit;

    public InternetPlan(int planId, String planName, double price, int dataLimit) {
        if (planId <= 0) {
            throw new IllegalArgumentException("Plan ID must be positive.");
        }
        if (planName == null || planName.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan name cannot be empty.");
        }
        if (price < 0 || dataLimit < 0) {
            throw new IllegalArgumentException("Price and data limit cannot be negative.");
        }
        this.planId = planId;
        this.planName = planName;
        this.price = price;
        this.dataLimit = dataLimit;
    }

    public int getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public double getPrice() { return price; }
    public int getDataLimit() { return dataLimit; }

    public abstract void displayPlan();
}
