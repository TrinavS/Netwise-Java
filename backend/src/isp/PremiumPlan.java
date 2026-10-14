package isp;

public class PremiumPlan extends InternetPlan {
    public PremiumPlan(int planId, String planName, double price, int dataLimit) {
        super(planId, planName, price, dataLimit);
    }

    @Override
    public void displayPlan() {
        System.out.println("Premium Plan");
        System.out.println("Plan ID: " + getPlanId());
        System.out.println("Plan Name: " + getPlanName());
        System.out.printf("Price: ₹%.2f%n", getPrice());
        System.out.println("Data Limit: " + getDataLimit() + " GB");
    }
}
