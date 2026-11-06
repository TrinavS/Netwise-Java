package isp;

/** One dated entry in a complaint's customer support timeline. */
public final class ComplaintEvent {
    private final String action;
    private final String detail;
    private final String actor;
    private final String timestamp;

    public ComplaintEvent(String action, String detail, String actor, String timestamp) {
        this.action = action == null ? "UPDATED" : action;
        this.detail = detail == null ? "" : detail;
        this.actor = actor == null ? "SYSTEM" : actor;
        this.timestamp = timestamp == null ? "" : timestamp;
    }

    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getActor() { return actor; }
    public String getTimestamp() { return timestamp; }
}
