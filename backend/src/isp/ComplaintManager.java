package isp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

public class ComplaintManager {
    private static final String[] VALID_STATUSES = {"PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED"};
    private final ArrayList<Complaint> complaints = new ArrayList<>();
    private final Set<String> complaintTypes = new HashSet<>();

    public synchronized void addComplaint(Complaint complaint)
            throws InvalidComplaintException {
        if (complaint == null ||
                isBlank(complaint.getComplaintId()) ||
                isBlank(complaint.getCustomerId()) ||
                isBlank(complaint.getComplaintType()) ||
                isBlank(complaint.getDescription()) || complaint.getDescription().trim().length() < 8) {
            throw new InvalidComplaintException("Complaint details cannot be empty.");
        }

        if (findComplaint(complaint.getComplaintId()) != null) {
            throw new InvalidComplaintException("Complaint ID already exists.");
        }

        complaints.add(complaint);
        complaintTypes.add(complaint.getComplaintType());
        System.out.println("Complaint registered successfully!");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public synchronized Complaint findComplaint(String complaintId) {
        if (complaintId == null) return null;
        for (Complaint complaint : complaints) {
            if (complaint.getComplaintId().equalsIgnoreCase(complaintId)) {
                return complaint;
            }
        }
        return null;
    }

    public synchronized void viewAllComplaints() {
        if (complaints.isEmpty()) {
            System.out.println("No complaints found.");
            return;
        }
        for (Complaint complaint : complaints) {
            complaint.displayComplaint();
        }
    }

    public synchronized List<Complaint> getComplaints() {
        return Collections.unmodifiableList(new ArrayList<>(complaints));
    }

    public synchronized Set<String> getComplaintTypes() {
        return Collections.unmodifiableSet(new HashSet<>(complaintTypes));
    }

    public synchronized void removeForCustomer(int customerId) {
        complaints.removeIf(complaint -> Integer.parseInt(complaint.getCustomerId()) == customerId);
        complaintTypes.clear();
        for (Complaint complaint : complaints) complaintTypes.add(complaint.getComplaintType());
    }

    public synchronized void updateStatus(String complaintId, String newStatus)
            throws InvalidComplaintException {
        updateComplaint(complaintId, newStatus, null, null, "");
    }

    public synchronized void updateComplaint(String complaintId, String newStatus, String newPriority,
                                             String assignedTo, String note)
            throws InvalidComplaintException {
        Complaint complaint = findComplaint(complaintId);
        if (complaint == null) {
            throw new InvalidComplaintException("Complaint not found.");
        }

        if (newStatus != null && !newStatus.isBlank()
                && Arrays.stream(VALID_STATUSES).noneMatch(s -> s.equalsIgnoreCase(newStatus.trim()))) {
            throw new InvalidComplaintException("Invalid complaint status.");
        }
        if (newPriority != null && !newPriority.isBlank()
                && !List.of("LOW", "NORMAL", "HIGH", "URGENT").contains(newPriority.trim().toUpperCase())) {
            throw new InvalidComplaintException("Priority must be Low, Normal, High, or Urgent.");
        }
        if (assignedTo != null && assignedTo.trim().length() > 80) {
            throw new InvalidComplaintException("Assignee must be 80 characters or fewer.");
        }
        if (note != null && note.trim().length() > 500) {
            throw new InvalidComplaintException("Support note must be 500 characters or fewer.");
        }
        if ((newStatus == null || newStatus.isBlank()) && (newPriority == null || newPriority.isBlank())
                && assignedTo == null && (note == null || note.isBlank())) {
            throw new InvalidComplaintException("Provide a status, priority, assignee, or support note to record an update.");
        }
        complaint.recordUpdate(newStatus, newPriority, assignedTo, note, "ADMIN");
        System.out.println("Complaint status updated successfully!");
    }
}
