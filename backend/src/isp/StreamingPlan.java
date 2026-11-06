package isp;

/** Optional capability contract for plans positioned for streaming households. */
public interface StreamingPlan {
    default String getStreamingFeature() {
        return "Optimized for household streaming";
    }
}
