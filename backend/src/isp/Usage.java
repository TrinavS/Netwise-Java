package isp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Usage {
    private double dataLimit;
    private double dataUsed;
    private final List<Double> history;

    public Usage(double dataLimit) {
        if (!Double.isFinite(dataLimit) || dataLimit < 0) {
            throw new IllegalArgumentException("Data limit cannot be negative.");
        }
        this.dataLimit = dataLimit;
        this.dataUsed = 0;
        this.history = new ArrayList<>(List.of(0.0, 0.0, 0.0, 0.0));
    }

    public Usage(double dataLimit, double dataUsed, List<Double> history) {
        this(dataLimit);
        if (!Double.isFinite(dataUsed) || dataUsed < 0) throw new IllegalArgumentException("Usage must be a finite non-negative number.");
        this.dataUsed = dataUsed;
        this.history.clear();
        if (history != null) this.history.addAll(history);
        while (this.history.size() < 4) this.history.add(0.0);
    }

    public synchronized void addUsage(double data) throws InvalidUsageException {
        if (!Double.isFinite(data) || data <= 0) {
            throw new InvalidUsageException("Usage must be a finite number greater than zero.");
        }
        dataUsed += data;
        history.set(history.size() - 1, history.get(history.size() - 1) + data);
        if (dataUsed > dataLimit) {
            System.out.println("Warning: Data limit exceeded!");
        }
    }

    public synchronized double getRemaining() {
        return dataLimit - dataUsed;
    }

    public synchronized double getDataUsed() { return dataUsed; }
    public synchronized double getDataLimit() { return dataLimit; }
    public synchronized void updateDataLimit(double dataLimit) {
        if (!Double.isFinite(dataLimit) || dataLimit < 0) throw new IllegalArgumentException("Data limit cannot be negative.");
        this.dataLimit = dataLimit;
    }
    public synchronized List<Double> getHistory() { return Collections.unmodifiableList(new ArrayList<>(history)); }

    public void displayUsage() {
        System.out.printf("Data Limit: %.2f GB%n", dataLimit);
        System.out.printf("Data Used: %.2f GB%n", dataUsed);
        System.out.printf("Remaining: %.2f GB%n", getRemaining());
    }
}
