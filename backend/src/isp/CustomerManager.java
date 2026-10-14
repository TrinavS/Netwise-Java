package isp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerManager {
    private final ArrayList<Customer> customers = new ArrayList<>();
    private final ArrayList<InternetPlan> plans = new ArrayList<>();
    private final ArrayList<Subscription> subscriptions = new ArrayList<>();

    public void addCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        if (findCustomer(customer.getCustomerId()) != null) {
            throw new IllegalArgumentException("Customer ID already exists.");
        }
        customers.add(customer);
        System.out.println("Customer added successfully!");
    }

    public Customer findCustomer(int customerId) {
        for (Customer customer : customers) {
            if (customer.getCustomerId() == customerId) return customer;
        }
        return null;
    }

    public void displayCustomers() {
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        for (Customer customer : customers) {
            customer.displayCustomer();
            System.out.println("------------------");
        }
    }

    public void addPlan(InternetPlan plan) {
        if (plan == null) throw new IllegalArgumentException("Plan cannot be null.");
        if (findPlan(plan.getPlanId()) != null) {
            throw new IllegalArgumentException("Plan ID already exists.");
        }
        plans.add(plan);
        System.out.println("Plan added successfully!");
    }

    public InternetPlan findPlan(int planId) {
        for (InternetPlan plan : plans) {
            if (plan.getPlanId() == planId) return plan;
        }
        return null;
    }

    public void displayPlans() {
        if (plans.isEmpty()) {
            System.out.println("No plans found.");
            return;
        }
        for (InternetPlan plan : plans) {
            plan.displayPlan();
            System.out.println("------------------");
        }
    }

    public Subscription subscribe(Customer customer, InternetPlan plan) {
        if (customer == null || plan == null) {
            throw new IllegalArgumentException("Customer and plan are required.");
        }
        if (findCustomer(customer.getCustomerId()) == null ||
                findPlan(plan.getPlanId()) == null) {
            throw new IllegalArgumentException("Add the customer and plan before subscribing.");
        }
        Subscription subscription = new Subscription(customer, plan);
        subscriptions.add(subscription);
        System.out.println("Subscription created successfully!");
        return subscription;
    }

    public List<Subscription> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    public void displaySubscriptions() {
        if (subscriptions.isEmpty()) {
            System.out.println("No subscriptions found.");
            return;
        }
        for (Subscription subscription : subscriptions) {
            subscription.displaySubscription();
            System.out.println("------------------");
        }
    }
}
