package isp;

/** Optional capability contract for plans that include priority support. */
public interface PrioritySupportPlan {
    default String getSupportLevel() {
        return "Priority customer support";
    }
}
