package isp;

public class NotificationThread extends Thread {
    private final String customerId;
    private final int daysRemaining;

    public NotificationThread(String customerId, int daysRemaining) {
        this.customerId = customerId;
        this.daysRemaining = daysRemaining;
    }

    @Override
    public void run() {
        try {
            System.out.println("Checking subscription for Customer: " + customerId);
            Thread.sleep(2000);

            if (daysRemaining <= 3 && daysRemaining >= 0) {
                System.out.println("Notification: Customer " + customerId
                        + ", your subscription expires in " + daysRemaining + " days.");
            } else if (daysRemaining < 0) {
                System.out.println("Notification: Customer " + customerId
                        + ", your subscription has expired.");
            } else {
                System.out.println("Customer " + customerId + ": Subscription is active.");
            }
        } catch (InterruptedException e) {
            System.out.println("Notification thread interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}
