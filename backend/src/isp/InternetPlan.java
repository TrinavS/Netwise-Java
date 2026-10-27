package isp;

public abstract class InternetPlan implements PlanDetails {
    private final int planId;
    private final String planName;
    private final double price;
    private final int dataLimit;
    private final String speed;
    private final String description;
    private final boolean featured;

    public InternetPlan(int planId, String planName, double price, int dataLimit) {
        this(planId, planName, price, dataLimit, "50 Mbps", "Reliable everyday browsing", false);
    }

    public InternetPlan(int planId, String planName, double price, int dataLimit,
                        String speed, String description, boolean featured) {
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
        this.speed = speed == null ? "50 Mbps" : speed;
        this.description = description == null ? "Internet service plan" : description;
        this.featured = featured;
    }

    public int getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public double getPrice() { return price; }
    public int getDataLimit() { return dataLimit; }
    public String getSpeed() { return speed; }
    public String getDescription() { return description; }
    public boolean isFeatured() { return featured; }

    @Override
    public String getPlanSummary() {
        return getPlanName() + " - " + speed + ", " + dataLimit + " GB, ₹" + price + "/month";
    }

    public abstract void displayPlan();
    public abstract String getTier();
}
