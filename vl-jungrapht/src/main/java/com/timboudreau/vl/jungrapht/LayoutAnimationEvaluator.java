package com.timboudreau.vl.jungrapht;


import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;

/**
 * Many jungrapht layouts never <i>quite</i> settle into a stable state, but leave
 * the nodes permanently jiggling by a few pixels.  Animated jungrapht layouts implement
 * an interface <code>IterativeContext</code>, which has a <code>done()</code>
 * method which is supposed to provide this value, but many implementations never
 * return true from it;  and in other cases, reaching a stable state may take a
 * very long time, while the node positions are fairly good fairly quickly.
 * An endlessly jiggling graph is interesting looking but not necessarily great
 * to work with or read.
 * <p/>
 * This class exists to augment the layouts evaluation of "done" with our own,
 * based on what we think the user needs.
 * <p/>
 * This class evaluates the minimum and maximum distances one frame of animation
 * has caused nodes to move, and is used to turn off the animation timer once
 * the movements are minimal enough.  Subclasses would override the <code>test()</code>
 * method to return <code>true</code> if no node has moved by a distance considered
 * significant - i.e. if no node has moved more than 10 pixels for 10 iterations,
 * the default implementation will consider it done.
 * <p/>
 * This class is only used if the associated scene's animation timer is running.
 *
 * @author Tim Boudreau
 */
public class LayoutAnimationEvaluator {

    private int callCount;
    /**
     * The default number of iterations that no significant change has to happen
     * for the animation to be considered done.
     */
    public static final int DEFAULT_MINIMUM_ITERATIONS_STABLE_STATE = 480;
    /**
     * The default number of pixels any node needs to have moved for the 
     * default implementation to consider the animation as still doing
     * productive things.
     */
    public static final double DEfAULT_MAX_DISTANCE = 9D;

    private int minIterations = DEFAULT_MINIMUM_ITERATIONS_STABLE_STATE;
    private double distance = DEfAULT_MAX_DISTANCE;

    /**
     * An instance which lets the animation run forever.
     */
    public static LayoutAnimationEvaluator NO_OP = new LayoutAnimationEvaluator() {
        @Override
        protected boolean animationIsFinished(double min, double max, double average, LayoutAlgorithm<?> layoutAlgorithm) {
            return false;
        }
    };

    /**
     * Get the number of consecutive times that <code>test()</code> must return true
     * for the animation to be considered done.
     * 
     * @return the number of iterations
     */
    public int getMinimumIterations() {
        return minIterations;
    }

    /**
     * Set the number of consecutive times that <code>test()</code> must return
     * true for the animation to be considered done.
     * 
     * @param iterations The number of iterations
     */
    public final void setMinimumIterations(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be at least one, not " + iterations);
        }
        this.minIterations = iterations;
    }

    /**
     * Get the maximum distance between points for which the default implementation
     * of <code>test()</code> should return true.  If the max parameter passed to
     * <code>test()</code> is less than this number <code>minimumIterations</code>
     * <i>consecutive</code> times, the <code>animationIsFinished()</code> will return true.
     * 
     * @return the distance
     */
    public final double getDistanceConsideredStable() {
        return distance;
    }

    /**
     * Get the maximum distance between points for which the default implementation
     * of <code>test()</code> should return true.  If the max parameter passed to
     * <code>test()</code> is less than this number <code>minimumIterations</code>
     * <i>consecutive</code> times, the <code>animationIsFinished()</code> will return true.
     * 
     * @param distance The distance, non-negative
     */
    public final void setDistanceConsideredStable(double distance) {
        if (distance < 0D) {
            throw new IllegalArgumentException("Negative values not allowed: " + distance);
        }
        this.distance = distance;
    }

    /**
     * Reset the counter that determines how many times this has been called
     * with insignificant changes.
     */
    protected void reset() {
        callCount = 0;
    }
    
    /**
     * Perform the test which decides.  The default implementation is
     * <code> return max <= getDistance()</code>.
     * 
     * @param min The minimum distance any point moved
     * @param max The maximum distance any point moved
     * @param average The average distance points moved
     * @param layoutAlgorithm The jungrapht layoutAlgorithm - may want to special case layouts with
     * different characteristics
     * @return True if no significant moves have occurred
     */
    protected boolean test (double min, double max, double average, LayoutAlgorithm<?> layoutAlgorithm) {
        return max <= getDistanceConsideredStable();
    }
    
    /**
     * Get the number of <i>consecutive</i> times <code>animationIsFinished()</code>
     * <i>and</i> <code>test()</code> returned true since the last call to reset().
     * 
     * @return The number of times
     */
    protected int getConsecutiveCallCount() {
        return callCount;
    }

    /**
     * Test if the animation has finished running, using the standard that
     * a call to <code>test()</code> must have returned true more than some
     * threshold number of previous calls.
     * 
     * @param min The minimum distance any point moved
     * @param max The maximum distance any point moved
     * @param average The average distance points moved
     * @param layoutAlgorithm The layout being used. Some jungrapht layouts never settle but
     * repeatedly produce large moves;  this parameter can be used to evaluate if
     * this is such a case
     * @return True if no significant moves have occurred
     */
    protected boolean animationIsFinished(double min, double max, double average, LayoutAlgorithm<?> layoutAlgorithm) {
        if (callCount++ < minIterations) {
            return false;
        }
//        System.out.println(min + " <-> " + max + "  ::  " + average);
        boolean result = test(min, max, average, layoutAlgorithm);
        if (!result) {
            reset();
        } else {
            if (callCount++ < minIterations) {
                result = false;
            }
        }
        return result;
    }
}
