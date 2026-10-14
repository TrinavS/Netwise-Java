package isp;

import java.util.ArrayList;

public class ComplaintManager {
    private final ArrayList<Complaint> complaints = new ArrayList<>();

    public synchronized void addComplaint(Complaint complaint)
            throws InvalidComplaintException {
        if (complaint == null ||
                isBlank(complaint.getComplaintId()) ||
                isBlank(complaint.getCustomerId()) ||
                isBlank(complaint.getComplaintType()) ||
                isBlank(complaint.getDescription())) {
            throw new InvalidComplaintException("Complaint details cannot be empty.");
        }

        if (findComplaint(complaint.getComplaintId()) != null) {
            throw new InvalidComplaintException("Complaint ID already exists.");
        }

        complaints.add(complaint);
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

    public synchronized void updateStatus(String complaintId, String newStatus)
            throws InvalidComplaintException {
        Complaint complaint = findComplaint(complaintId);
        if (complaint == null) {
            throw new InvalidComplaintException("Complaint not found.");
        }

        if (newStatus == null ||
                !(newStatus.equalsIgnoreCase("PENDING") ||
                  newStatus.equalsIgnoreCase("IN_PROGRESS") ||
                  newStatus.equalsIgnoreCase("RESOLVED") ||
                  newStatus.equalsIgnoreCase("CLOSED"))) {
            throw new InvalidComplaintException("Invalid complaint status.");
        }

        complaint.setStatus(newStatus.toUpperCase());
        System.out.println("Complaint status updated successfully!");
    }
}
