package isp;

public class CustomPlan extends InternetPlan {
    public CustomPlan(int planId, String planName, double price, int dataLimit,
                      String speed, String description, boolean featured) {
        super(planId, planName, price, dataLimit, speed, description, featured);
    }

    @Override public String getTier() { return "Standard"; }

    @Override
    public void displayPlan() {
        System.out.println("Custom ISP Plan");
        System.out.println(getPlanSummary());
    }
}
