/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timboudreau.vl.jung;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Timer;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public final class AnimationController {

    private LayoutAnimationEvaluator evaluator = new LayoutAnimationEvaluator();
    private final Timer animationTimer;
    private final Set<AnimationCallback> tickListeners = new HashSet<>();
    private int maxIterations = Integer.MAX_VALUE;
    private int iterations;

    AnimationController() {
        this.animationTimer = new Timer(1000 / 24, new TimerListener());
        animationTimer.setRepeats(true);
        animationTimer.setCoalesce(true);
        animationTimer.setInitialDelay(200);
        animationTimer.stop();
    }

    interface AnimationCallback {

        void onTick();

        boolean canAnimate();
    }

    void addAnimationCallback(AnimationCallback r) {
        tickListeners.add(r);
    }

    void removeAnimationCallback(AnimationCallback r) {
        tickListeners.remove(r);
    }

    public void start() {
        System.out.println("start animation");
        evaluator.reset();
        animationTimer.start();
    }

    public void stop() {
        System.out.println("stop animation");
        animationTimer.stop();
        iterations = 0;
    }

    public void setMaxIterations(int maxIterations) {
        if (maxIterations < 0) {
            throw new IllegalArgumentException("Max iterations < 0");
        }
        this.maxIterations = maxIterations;
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). This property sets the frame
     * rate for animating them, in frames per second.
     *
     * @param fps Frames per second - must be >= 1
     */
    public final void setFramesPerSecond(int fps) {
        if (fps < 1) {
            throw new IllegalArgumentException("Frame rate must be at least 1. "
                    + "Use setAnimateIterativeLayouts() to disable animation.");
        }
        animationTimer.setDelay(1000 / fps);
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). This is the frame rate for
     * the animation timer (actual results may vary if the machine cannot keep
     * up with the math involved).
     *
     * @return The requested frame rate
     */
    public int getFramesPerSecond() {
        int delay = animationTimer.getDelay();
        return delay / 1000;
    }

    private int fastForwardIterations;

    /**
     * Set a number of iterations to pre-run before making a layout active -
     * either when not allowing animation to run for a layout that evolves
     * toward a stable state, or to start a slow animating layout with it
     * already progressed substantially.
     *
     * @param amt The number of iterations
     */
    public void setFastForwardIterations(int amt) {
        if (amt < 0) {
            throw new IllegalArgumentException("Negative amount " + amt);
        }
        this.fastForwardIterations = amt;
    }

    public int getFastForwardIterations() {
        return fastForwardIterations;
    }

    public boolean isCurrentlyAnimating() {
        return animationTimer.isRunning();
    }

    void onAfterLayout(double minDist, double maxDist, double avgDist) {
        if (isCurrentlyAnimating() && evaluator.animationIsFinished(minDist, maxDist, avgDist)) {
            animationTimer.stop();
        }
    }

    void resetEvaluator() {
        evaluator.reset();
    }

    private boolean animate = true;

    public void setAnimate(boolean val) {
        boolean old = this.animate;
        if (old != val) {
            System.out.println("setAnimate " + val);
            this.animate = val;
            boolean anyCanAnimate = false;
            if (val) {
                for (AnimationCallback cb : tickListeners) {
                    anyCanAnimate = cb.canAnimate();
                    if (anyCanAnimate) {
                        start();
                        break;
                    }
                }
            }
            if (!val || !anyCanAnimate) {
                stop();
            }
        }
    }

    public boolean isAnimate() {
        return animate;
    }

    /**
     * Set the object which will decide when animated JUNG layouts have done
     * everything useful they're going to do.
     *
     * @param eval An evaluator
     * @see LayoutAnimationEvaluator
     */
    public void setLayoutAnimationEvaluator(LayoutAnimationEvaluator eval) {
        Parameters.notNull("eval", eval);
        this.evaluator = eval;
    }

    /**
     * Get the object which decides when any animated JUNG layout has done
     * everything useful it is going to do
     *
     * @return An evaluator
     * @see LayoutAnimationEvaluator
     */
    public LayoutAnimationEvaluator getLayoutAnimationEvaluator() {
        return evaluator;
    }

    final class TimerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            for (AnimationCallback r : tickListeners) {
                try {
                    if (iterations % 250 == 0) {
                        System.out.println("iteration " + iterations);
                    }
                    r.onTick();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            if (iterations++ == maxIterations && maxIterations != Integer.MAX_VALUE) {
                stop();
            }
        }
    }
}
