package isp;

public class Subscription {
    private final Customer customer;
    private InternetPlan plan;
    private String status;

    public Subscription(Customer customer, InternetPlan plan) {
        if (customer == null || plan == null) {
            throw new IllegalArgumentException("Customer and plan are required.");
        }
        this.customer = customer;
        this.plan = plan;
        this.status = "ACTIVE";
    }

    public Customer getCustomer() { return customer; }
    public InternetPlan getPlan() { return plan; }
    public String getStatus() { return status; }

    public void changePlan(InternetPlan newPlan) {
        if (newPlan == null) {
            throw new IllegalArgumentException("New plan cannot be null.");
        }
        if ("CANCELLED".equals(status)) {
            throw new IllegalStateException("Cannot change a cancelled subscription.");
        }
        this.plan = newPlan;
        System.out.println("Plan changed successfully!");
    }

    public void cancelSubscription() {
        status = "CANCELLED";
        System.out.println("Subscription cancelled.");
    }

    public void displaySubscription() {
        System.out.println("Customer: " + customer.getName());
        System.out.println("Plan: " + plan.getPlanName());
        System.out.println("Status: " + status);
    }
}
