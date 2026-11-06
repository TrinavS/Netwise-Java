package isp;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class Subscription {
    private static final AtomicInteger GENERATED_IDS = new AtomicInteger(1);
    private final String subscriptionId;
    private final Customer customer;
    private InternetPlan plan;
    private InternetPlan pendingPlan;
    private LocalDate planChangeEffectiveDate;
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
    public InternetPlan getPendingPlan() { return pendingPlan; }
    public LocalDate getPlanChangeEffectiveDate() { return planChangeEffectiveDate; }
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
        this.pendingPlan = null;
        this.planChangeEffectiveDate = null;
        this.daysRemaining = 30;
        System.out.println("Plan changed successfully!");
    }

    public void schedulePlanChange(InternetPlan newPlan, LocalDate effectiveDate) {
        if (newPlan == null || effectiveDate == null) throw new IllegalArgumentException("A plan and effective date are required.");
        if ("CANCELLED".equals(status)) throw new IllegalStateException("Cannot change a cancelled subscription.");
        this.pendingPlan = newPlan;
        this.planChangeEffectiveDate = effectiveDate;
    }

    public void cancelScheduledPlanChange() {
        this.pendingPlan = null;
        this.planChangeEffectiveDate = null;
    }

    public boolean applyScheduledPlanChange(LocalDate asOf) {
        if (pendingPlan == null || planChangeEffectiveDate == null || planChangeEffectiveDate.isAfter(asOf)) return false;
        this.plan = pendingPlan;
        cancelScheduledPlanChange();
        this.daysRemaining = 30;
        return true;
    }

    public void refreshPlanDetails(InternetPlan updatedPlan) {
        if (updatedPlan == null) throw new IllegalArgumentException("Updated plan is required.");
        if (updatedPlan.getPlanId() == plan.getPlanId()) this.plan = updatedPlan;
        else if (pendingPlan != null && updatedPlan.getPlanId() == pendingPlan.getPlanId()) this.pendingPlan = updatedPlan;
        else throw new IllegalArgumentException("Updated plan must match the current or scheduled plan.");
    }

    public void cancelSubscription() {
        status = "CANCELLED";
        System.out.println("Subscription cancelled.");
    }

    public void displaySubscription() {
        System.out.println("Subscription ID: " + subscriptionId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Plan: " + plan.getPlanName());
        if (pendingPlan != null) System.out.println("Next plan: " + pendingPlan.getPlanName() + " from " + planChangeEffectiveDate);
        System.out.println("Status: " + status);
        System.out.println("Started: " + startDate);
    }
}
