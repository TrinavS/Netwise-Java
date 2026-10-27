package isp;

public class HomePlan extends InternetPlan {
    public HomePlan(int planId, String planName, double price, int dataLimit,
                    String speed, String description, boolean featured) {
        super(planId, planName, price, dataLimit, speed, description, featured);
    }

    @Override public String getTier() { return "Standard"; }

    @Override
    public void displayPlan() {
        System.out.println("Home Internet Plan");
        System.out.println(getPlanSummary());
    }
}
