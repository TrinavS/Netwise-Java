package isp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Complaint {
    private final String complaintId;
    private final String customerId;
    private final Customer customer;
    private final String complaintType;
    private final String description;
    private String status;
    private String priority;
    private String assignedTo;
    private final String createdAt;
    private final List<ComplaintEvent> timeline = new ArrayList<>();

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
        this(complaintId, customerId, customer, complaintType, description,
                "PENDING", "NORMAL", "", LocalDateTime.now().withNano(0).toString(), List.of());
    }

    public Complaint(String complaintId, Customer customer, String complaintType, String description,
                     String status, String priority, String assignedTo, String createdAt,
                     List<ComplaintEvent> events) {
        this(complaintId, customer == null ? null : String.valueOf(customer.getCustomerId()), customer,
                complaintType, description, status, priority, assignedTo, createdAt, events);
    }

    private Complaint(String complaintId, String customerId, Customer customer,
                      String complaintType, String description, String status, String priority,
                      String assignedTo, String createdAt, List<ComplaintEvent> events) {
        this.complaintId = complaintId;
        this.customerId = customerId;
        this.customer = customer;
        this.complaintType = complaintType;
        this.description = description;
        this.status = status == null || status.isBlank() ? "PENDING" : status.toUpperCase();
        this.priority = priority == null || priority.isBlank() ? "NORMAL" : priority.toUpperCase();
        this.assignedTo = assignedTo == null ? "" : assignedTo;
        this.createdAt = createdAt == null || createdAt.isBlank()
                ? LocalDateTime.now().withNano(0).toString() : createdAt;
        if (events != null) this.timeline.addAll(events);
        if (this.timeline.isEmpty()) {
            this.timeline.add(new ComplaintEvent("CREATED", "Complaint submitted", "CUSTOMER", this.createdAt));
            if (!"PENDING".equals(this.status)) {
                this.timeline.add(new ComplaintEvent("STATUS", "Initial status: " + this.status.replace('_', ' '),
                        "SYSTEM", this.createdAt));
            }
        }
    }

    public String getComplaintId() { return complaintId; }
    public String getCustomerId() { return customerId; }
    public Customer getCustomer() { return customer; }
    public String getComplaintType() { return complaintType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getAssignedTo() { return assignedTo; }
    public String getCreatedAt() { return createdAt; }
    public List<ComplaintEvent> getTimeline() { return Collections.unmodifiableList(new ArrayList<>(timeline)); }

    public void recordUpdate(String newStatus, String newPriority, String newAssignee,
                             String note, String actor) {
        if (newStatus != null && !newStatus.isBlank()) this.status = newStatus.toUpperCase();
        if (newPriority != null && !newPriority.isBlank()) this.priority = newPriority.toUpperCase();
        if (newAssignee != null) this.assignedTo = newAssignee.trim();
        String detail = note == null ? "" : note.trim();
        if (detail.isEmpty()) detail = "Ticket updated: " + this.status.replace('_', ' ')
                + " · " + this.priority.toLowerCase() + " priority";
        timeline.add(new ComplaintEvent("UPDATED", detail,
                actor == null || actor.isBlank() ? "ADMIN" : actor.trim(), LocalDateTime.now().withNano(0).toString()));
    }

    public void displayComplaint() {
        System.out.println("Complaint ID: " + complaintId);
        System.out.println("Customer ID: " + customerId);
        System.out.println("Type: " + complaintType);
        System.out.println("Description: " + description);
        System.out.println("Status: " + status);
        System.out.println("Priority: " + priority);
        System.out.println("--------------------------");
    }
}
