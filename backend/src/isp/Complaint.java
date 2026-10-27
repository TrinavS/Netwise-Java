package isp;

public class Complaint {
    private final String complaintId;
    private final String customerId;
    private final Customer customer;
    private final String complaintType;
    private final String description;
    private String status;

    public Complaint(String complaintId, String customerId,
                     String complaintType, String description) {
        this(complaintId, customerId, null, complaintType, description);
    }

    public Complaint(String complaintId, Customer customer,
                     String complaintType, String description) {
        this(complaintId, customer == null ? null : String.valueOf(customer.getCustomerId()),
                customer, complaintType, description);
    }

    private Complaint(String complaintId, String customerId, Customer customer,
                      String complaintType, String description) {
        this.complaintId = complaintId;
        this.customerId = customerId;
        this.customer = customer;
        this.complaintType = complaintType;
        this.description = description;
        this.status = "PENDING";
    }

    public String getComplaintId() { return complaintId; }
    public String getCustomerId() { return customerId; }
    public Customer getCustomer() { return customer; }
    public String getComplaintType() { return complaintType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void displayComplaint() {
        System.out.println("Complaint ID: " + complaintId);
        System.out.println("Customer ID: " + customerId);
        System.out.println("Type: " + complaintType);
        System.out.println("Description: " + description);
        System.out.println("Status: " + status);
        System.out.println("--------------------------");
    }
}
