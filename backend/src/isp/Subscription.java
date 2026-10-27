package isp;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class Subscription {
    private static final AtomicInteger GENERATED_IDS = new AtomicInteger(1);
    private final String subscriptionId;
    private final Customer customer;
    private InternetPlan plan;
    private String status;
    private final LocalDate startDate;
    private int daysRemaining;

    public Subscription(Customer customer, InternetPlan plan) {
        this("SUB-" + String.format("%03d", GENERATED_IDS.getAndIncrement()), customer, plan, LocalDate.now(), 30);
    }

    public Subscription(String subscriptionId, Customer customer, InternetPlan plan,
                        LocalDate startDate, int daysRemaining) {
        if (customer == null || plan == null) {
            throw new IllegalArgumentException("Customer and plan are required.");
        }
        this.subscriptionId = subscriptionId;
        this.customer = customer;
        this.plan = plan;
        this.status = "ACTIVE";
        this.startDate = startDate == null ? LocalDate.now() : startDate;
        this.daysRemaining = daysRemaining;
    }

    public Customer getCustomer() { return customer; }
    public String getSubscriptionId() { return subscriptionId; }
    public InternetPlan getPlan() { return plan; }
    public String getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public int getDaysRemaining() { return daysRemaining; }

    public void changePlan(InternetPlan newPlan) {
        if (newPlan == null) {
            throw new IllegalArgumentException("New plan cannot be null.");
        }
        if ("CANCELLED".equals(status)) {
            throw new IllegalStateException("Cannot change a cancelled subscription.");
        }
        this.plan = newPlan;
        this.daysRemaining = 30;
        System.out.println("Plan changed successfully!");
    }

    public void refreshPlanDetails(InternetPlan updatedPlan) {
        if (updatedPlan == null || updatedPlan.getPlanId() != plan.getPlanId()) {
            throw new IllegalArgumentException("Updated plan must have the same plan ID.");
        }
        this.plan = updatedPlan;
    }

    public void cancelSubscription() {
        status = "CANCELLED";
        System.out.println("Subscription cancelled.");
    }

    public void displaySubscription() {
        System.out.println("Subscription ID: " + subscriptionId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Plan: " + plan.getPlanName());
        System.out.println("Status: " + status);
        System.out.println("Started: " + startDate);
    }
}
