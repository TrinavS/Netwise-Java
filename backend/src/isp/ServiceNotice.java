package isp;

import java.time.LocalDateTime;

/** A simulated ISP maintenance or service-impact notice shown in the customer portal. */
public final class ServiceNotice {
    private final String id;
    private final String title;
    private final String message;
    private final String area;
    private final String startsAt;
    private final String createdAt;
    private String status;

    public ServiceNotice(String id, String title, String message, String area,
                         String startsAt, String status, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.area = area;
        this.startsAt = startsAt == null ? "" : startsAt;
        this.status = status == null || status.isBlank() ? "SCHEDULED" : status.toUpperCase();
        this.createdAt = createdAt == null || createdAt.isBlank()
                ? LocalDateTime.now().withNano(0).toString() : createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getArea() { return area; }
    public String getStartsAt() { return startsAt; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public void setStatus(String status) { this.status = status.toUpperCase(); }
}
