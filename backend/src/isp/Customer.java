package isp;

public class Customer {
    private final int customerId;
    private String name;
    private String email;
    private String phone;
    private String address;

    public Customer(int customerId, String name, String email, String phone) {
        this(customerId, name, email, phone, "");
    }

    public Customer(int customerId, String name, String email, String phone, String address) {
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
        this.address = address == null ? "" : address.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public int getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    public void updateProfile(String name, String email, String phone, String address) {
        if (isBlank(name) || isBlank(email) || isBlank(phone)) {
            throw new IllegalArgumentException("Customer name, email, and phone are required.");
        }
        this.name = name.trim(); this.email = email.trim(); this.phone = phone.trim();
        this.address = address == null ? "" : address.trim();
    }

    public void displayCustomer() {
        System.out.println("Customer ID: " + customerId);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Phone: " + phone);
        if (!address.isBlank()) System.out.println("Address: " + address);
    }
}
