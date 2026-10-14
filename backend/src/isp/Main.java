package isp;

public class Main {
    public static void main(String[] args) {
        CustomerManager customerManager = new CustomerManager();

        Customer c1 = new Customer(101, "Rahul", "rahul@gmail.com", "9876543210");
        Customer c2 = new Customer(102, "Priya", "priya@gmail.com", "9876501234");
        customerManager.addCustomer(c1);
        customerManager.addCustomer(c2);

        InternetPlan basic = new BasicPlan(1, "Basic 100", 499, 100);
        InternetPlan premium = new PremiumPlan(2, "Premium 500", 799, 500);
        customerManager.addPlan(basic);
        customerManager.addPlan(premium);

        Subscription s1 = customerManager.subscribe(c1, basic);
        Subscription s2 = customerManager.subscribe(c2, premium);

        System.out.println("\n===== CUSTOMERS =====");
        customerManager.displayCustomers();

        System.out.println("\n===== PLANS =====");
        customerManager.displayPlans();

        System.out.println("\n===== SUBSCRIPTIONS =====");
        customerManager.displaySubscriptions();

        System.out.println("\n===== BILLING =====");
        Billable bill = new Bill(s1.getPlan().getPrice());
        billableDisplay(bill);

        System.out.println("\n===== USAGE =====");
        Usage usage = new Usage(s1.getPlan().getDataLimit());
        try {
            usage.addUsage(20);
            usage.addUsage(35);
            usage.displayUsage();
        } catch (InvalidUsageException e) {
            System.out.println("Usage error: " + e.getMessage());
        }

        System.out.println("\n===== COMPLAINTS =====");
        ComplaintManager complaintManager = new ComplaintManager();
        try {
            complaintManager.addComplaint(new Complaint(
                    "CMP101", String.valueOf(c1.getCustomerId()),
                    "Slow Internet", "Internet speed is very slow."));
            complaintManager.addComplaint(new Complaint(
                    "CMP102", String.valueOf(c2.getCustomerId()),
                    "Connection Issue", "Internet connection is not working."));
            complaintManager.updateStatus("CMP101", "IN_PROGRESS");
            complaintManager.viewAllComplaints();
        } catch (InvalidComplaintException e) {
            System.out.println("Complaint error: " + e.getMessage());
        }

        System.out.println("\n===== NOTIFICATION THREADS =====");
        NotificationThread t1 = new NotificationThread(
                String.valueOf(s1.getCustomer().getCustomerId()), 2);
        NotificationThread t2 = new NotificationThread(
                String.valueOf(s2.getCustomer().getCustomerId()), 10);
        t1.start();
        t2.start();
    }

    private static void billableDisplay(Billable bill) {
        System.out.printf("Plan Amount: ₹%.2f%n", bill instanceof Bill
                ? ((Bill) bill).getAmount() : 0.0);
        System.out.printf("GST (18%%): ₹%.2f%n", bill.calculateGST());
        System.out.printf("Total Bill: ₹%.2f%n", bill.calculateTotal());
    }
}
