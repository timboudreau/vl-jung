/*
 * Copyright (c) 2018, Tim Boudreau
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.timboudreau.vl.jung;

import com.timboudreau.vl.jung.AnimationController.AnimationCallback;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.graph.GraphPinScene;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.model.ObjectScene;
import org.netbeans.api.visual.widget.Widget;

/**
 * Adapter which implements SceneLayout and uses the layout logic of the JUNG
 * layout. Also acts as our MoveProvider which will tell the layout about
 * user-made changes to node positions, so these are remembered
 */
public class LayoutAdapter<N, E> extends SceneLayout implements JungLayoutAdapter {

    private Layout<N, E> jungLayout;
    private final ObjectScene scene;
    private final GraphAdapter<N, E> graph;
    private final AnimationController animator;
    private final NodeMovedNotifier nodeMoved;

    public LayoutAdapter(GraphScene<N, E> scene, Layout<N, E> jungLayout, AnimationController animator, NodeMovedNotifier nodeMoved) {
        this(scene, jungLayout, new GraphSceneAdapter<>(scene), animator, nodeMoved);
    }

    public <P> LayoutAdapter(GraphPinScene<N, E, P> scene, Layout<N, E> jungLayout, AnimationController animator, NodeMovedNotifier nodeMoved) {
        this(scene, jungLayout, new GraphPinSceneAdapter<N, E, P>(scene), animator, nodeMoved);
    }

    public LayoutAdapter(ObjectScene scene, Layout<N, E> jungLayout, GraphAdapter<N, E> graph, AnimationController animator, NodeMovedNotifier nodeMoved) {
        super(scene);
        this.scene = scene;
        this.jungLayout = jungLayout;
        this.graph = graph;
        this.animator = animator;
        this.nodeMoved = nodeMoved;
        this.animator.addAnimationCallback(new TickCallback());
    }

    AnimationController animator() {
        return animator;
    }

    @Override
    public void setGraphLayout(Layout layout, boolean animate) {
        this.jungLayout = layout;
        assert layout != null : "Layout null";
        boolean canAnimate = canAnimate();
//        if (canAnimate) {
        animator.stop();
//        }
        if (canAnimate && layout instanceof IterativeContext && scene.getView() != null) {
            performLayout(true);
            animator.start();
        } else if (!canAnimate() && layout instanceof IterativeContext) {
            // Fast forward it a bit
            int ffw = animator.getFastForwardIterations();
            IterativeContext ctx = (IterativeContext) layout;
            if (!ctx.done() && ffw > 0) {
                try {
                    for (int i = 0; i < ffw; i++) {
                        ctx.step();
                        if (ctx.done()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Logger.getLogger(JungScene.class.getName()).log(Level.INFO, null, e);
                }
            }
            performLayout(false);
            scene.validate();
            scene.repaint();
        } else {
            performLayout(animate);
        }
    }

    private Point toPoint(Point2D p) {
        // A little pointless conversion code to get a java.awt.Point from a
        // Point2D
        Point result;
        if (p instanceof Point) {
            result = (Point) p;
        } else {
            result = new Point((int) p.getX(), (int) p.getY());
        }
        return result;
    }

    @Override
    protected void performLayout() {
        performLayout(false);
    }

    protected void performLayout(boolean animate) {
        // Make sure the layout knows about the size of the view
        JComponent comp = scene.getView();
        if (comp != null) {
            try {
                jungLayout.setSize(comp.getSize());
            } catch (UnsupportedOperationException e) {
                // some layouts dont support this
            }
        }
        double minDist = Double.MAX_VALUE;
        double maxDist = Double.MIN_VALUE;
        double avgDist = 0D;
        boolean animating = animator.isCurrentlyAnimating() && animator.isAnimate();
        // Iterate the vertices and make sure the widgets locations
        // match the graph
        Collection<N> nodes = graph.getNodes();
        int nodeCount = 0;
        Set<Object> toRevalidate = new HashSet<>();
        for (N n : nodes) {
            Widget w = scene.findWidget(n);
            Point2D oldLocation = w.getPreferredLocation();
            Point2D newLocation = jungLayout.apply(n);
            if (oldLocation != null && animating) {
                double length = Math.abs(oldLocation.distance(newLocation));
                minDist = Math.min(minDist, length);
                maxDist = Math.max(maxDist, length);
                avgDist += length;
            }
            if (oldLocation == null || !oldLocation.equals(newLocation)) {
                nodeCount++;
                Point p1 = toPoint(newLocation);
                if (animate) {
                    scene.getSceneAnimator().animatePreferredLocation(w, p1);
                } else {
                    w.setPreferredLocation(p1);
                }
                toRevalidate.addAll(graph.getDependentObjects(n));
//                for (E e : jungLayout.getGraph().getOutEdges(n)) {
//                    Widget ew = scene.findWidget(e);
//                    ew.revalidate();
//                }
//                for (E e : jungLayout.getGraph().getInEdges(n)) {
//                    Widget ew = scene.findWidget(e);
//                    ew.revalidate();
//                }
            }
        }
        for (Object o : toRevalidate) {
            Widget w = scene.findWidget(o);
            if (w != null) {
                w.revalidate(false);
            }
        }
        // Avoid div by zero
        avgDist = nodeCount == 0 ? 0D : avgDist / (double) nodeCount;
//        for (E e : graph.getEdges()) {
//            Widget w = scene.findWidget(e);
//            if (w instanceof ConnectionWidget) {
//                ((ConnectionWidget) w).reroute();
//            }
//            w.revalidate();
//            w.repaint();
////            graph.revalidateEdge(e);
//        }
        if (nodeCount > 0) {
//            scene.revalidate();
            scene.validate();
        }
        animator.onAfterLayout(minDist, maxDist, avgDist);
    }

    @SuppressWarnings(value = "unchecked")
    public void setNewLocation(Widget widget, Point location) {
        N node = (N) scene.findObject(widget);
        if (node != null) {
            jungLayout.setLocation(node, location);
            for (E e : jungLayout.getGraph().getOutEdges(node)) {
                Widget w = scene.findWidget(e);
                w.revalidate();
            }
            for (E e : jungLayout.getGraph().getInEdges(node)) {
                Widget w = scene.findWidget(e);
                w.revalidate();
            }
//                onMove(node, widget);
            if (nodeMoved != null) {
                nodeMoved.nodeMoved();
            }
        } else {
            System.out.println("No node for " + widget);
        }
    }

    class MP implements MoveProvider {

        private final MoveProvider delegate;

        public MP(MoveProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public void movementStarted(Widget widget) {
            delegate.movementStarted(widget);
        }

        @Override
        @SuppressWarnings(value = "unchecked")
        public void movementFinished(Widget widget) {
            delegate.movementFinished(widget);
            if (nodeMoved != null) {
                nodeMoved.nodeMoved();
            }
        }

        @Override
        public Point getOriginalLocation(Widget widget) {
            return delegate.getOriginalLocation(widget);
        }

        @Override
        @SuppressWarnings(value = "unchecked")
        public void setNewLocation(Widget widget, Point location) {
            LayoutAdapter.this.setNewLocation(widget, location);
            delegate.setNewLocation(widget, location);
        }
    }

    @Override
    public MoveProvider moveProvider() {
        return new MP(ActionFactory.createDefaultMoveProvider());
    }

    @Override
    public MoveProvider moveProvider(MoveProvider delegate) {
        return new MP(delegate);
    }

    private boolean canAnimate() {
        return jungLayout instanceof IterativeContext && scene.getView() != null;
    }

    private class TickCallback implements AnimationCallback {

        @Override
        public void onTick() {
            if (jungLayout instanceof IterativeContext) {
                IterativeContext c = (IterativeContext) jungLayout;
                try {
                    c.step();
                } catch (Exception ex) {
                    // e.g. IllegalArgumentException: Unexpected mathematical result in FRLayout:calcPositions
                    // Some layouts are buggy.
//                    Logger.getLogger(JungScene.class.getName()).log(Level.INFO, null, ex);
                    ex.printStackTrace();
//                    timer.stop();
                }
                if (c.done()) {
                    animator.stop();
                }
                LayoutAdapter.this.invokeLayout();
                scene.validate();
                scene.repaint();
            }
        }

        @Override
        public boolean canAnimate() {
            return LayoutAdapter.this.canAnimate();
        }
    }
}
