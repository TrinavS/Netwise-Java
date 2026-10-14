package isp;

public class Customer {
    private final int customerId;
    private final String name;
    private final String email;
    private final String phone;

    public Customer(int customerId, String name, String email, String phone) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }
        if (isBlank(name) || isBlank(email) || isBlank(phone)) {
            throw new IllegalArgumentException("Customer name, email, and phone are required.");
        }
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public int getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void displayCustomer() {
        System.out.println("Customer ID: " + customerId);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Phone: " + phone);
    }
}
