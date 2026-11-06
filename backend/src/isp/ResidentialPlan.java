package isp;

/** Intermediate residential plan type used to share behavior across home-service plans. */
public abstract class ResidentialPlan extends InternetPlan {
    protected ResidentialPlan(int planId, String planName, double price, int dataLimit,
                              String speed, String description, boolean featured) {
        super(planId, planName, price, dataLimit, speed, description, featured);
    }
}
