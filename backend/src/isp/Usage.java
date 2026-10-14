package isp;

public class Usage {
    private final double dataLimit;
    private double dataUsed;

    public Usage(double dataLimit) {
        if (dataLimit < 0) {
            throw new IllegalArgumentException("Data limit cannot be negative.");
        }
        this.dataLimit = dataLimit;
        this.dataUsed = 0;
    }

    public void addUsage(double data) throws InvalidUsageException {
        if (data < 0) {
            throw new InvalidUsageException("Usage cannot be negative.");
        }
        dataUsed += data;
        if (dataUsed > dataLimit) {
            System.out.println("Warning: Data limit exceeded!");
        }
    }

    public double getRemaining() {
        return dataLimit - dataUsed;
    }

    public void displayUsage() {
        System.out.printf("Data Limit: %.2f GB%n", dataLimit);
        System.out.printf("Data Used: %.2f GB%n", dataUsed);
        System.out.printf("Remaining: %.2f GB%n", getRemaining());
    }
}
