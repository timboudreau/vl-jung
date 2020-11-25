/*
 * Copyright (c) 2013, Tim Boudreau
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

import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.util.Pair;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Base class for Visual Library scenes which use Jung to manage layout and
 * connection drawing for graphs. You pass a Graph and a Layout to the
 * constructor; if the graph implements ObservableGraph, then the scene will
 * automatically update itself when the graph is modified; otherwise call
 * <code>sync()</code> when the graph is modified.
 * <p/>
 * To be pre-populated, subclasses should call init() at the end of their
 * constructor.
 *
 * @author Tim Boudreau
 */
public abstract class JungScene<N, E> extends GraphScene<N, E> {

    protected final Graph<N, E> graph;
    protected Layout<N, E> layout;
    private final LayoutAdapter sceneLayout = new LayoutAdapter();
    private boolean initialized;
    private SelectByClickAction clickAction;
    private final GraphSelection selection = new GraphSelection<>(this);
    private LayoutAnimationEvaluator evaluator = new LayoutAnimationEvaluator();
    private boolean animate = true;
    private final ActionListener timerListener = new TimerListener();
    private int fastForwardIterations = 300;
    private final Timer timer = new Timer(1000 / 24, timerListener);
    private final Lookup lkp;

    /**
     * Create a new Scene backed by the passed graph, and whose initial layout
     * is done using the passed layout.
     *
     * @param graph A JUNG graph which will be used to provide the graph
     * @param layout A JUNG layout to be used as the initial layout
     */
    @SuppressWarnings("unchecked")
    protected JungScene(Graph<N, E> graph, Layout layout) {
        this.graph = graph;
        this.layout = layout;
        timer.setRepeats(true);
        timer.setCoalesce(true);
        timer.setInitialDelay(200);
        timer.stop();
        if (graph instanceof ObservableGraph<?, ?>) {
            ((ObservableGraph<N, E>) graph).addGraphEventListener(new GraphEventAdapter());
        }
        final InstanceContent content = new InstanceContent();
        lkp = new AbstractLookup(content);
        // Export the selection as the contents of the lookup, for external
        // components to monitor the selection
        addObjectSceneListener(new ObjectSceneAdapter() {
            @Override
            public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
                // Compute the intersections and remove and add only those
                // that have changed
                Set<Object> toRemove = new HashSet<>(previousSelection);
                toRemove.removeAll(newSelection);
                for (Object o : toRemove) {
                    content.remove(o);
                }
                Set<Object> toAdd = new HashSet<>(newSelection);
                toAdd.removeAll(previousSelection);
                for (Object o : toAdd) {
                    content.add(o);
                }
            }
        }, ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
    }

    public Widget findNodeWidget(N node) {
        return findWidget(node);
    }

    public Widget findEdgeWidget(E edge) {
        return findWidget(edge);
    }

    public N nodeForWidget(Widget w, Class<N> type) {
        return objFor(w, type);
    }

    public E edgeForWidget(Widget w, Class<E> type) {
        return objFor(w, type);
    }

    private <T> T objFor(Widget w, Class<T> type) {
        Object result = findObject(w);
        return type.isInstance(result) ? type.cast(result) : null;
    }

    private MultiMoveAction.RelatedWidgetProvider relatedProvider;

    public MultiMoveAction.RelatedWidgetProvider relatedProvider() {
        if (relatedProvider == null) {
            relatedProvider = new RelatedProvider();
        }
        return relatedProvider;
    }

    private class RelatedProvider implements MultiMoveAction.RelatedWidgetProvider {

        @Override
        public void findWidgets(Widget relatedTo, Collection<? super Widget> addTo, int depth) {
            N model = (N) findObject(relatedTo);
            Set<N> all = new HashSet<>();
            findEdges(model, all, depth);
            for (N mdl : all) {
                Widget w = findNodeWidget(mdl);
                if (w != null) {
                    addTo.add(w);
                }
            }
        }

        private void findEdges(N model, Set<N> related, int depth) {
            Collection<E> edges = findNodeEdges(model, true, false);
            Set<N> found = new HashSet<>();
            for (E n : edges) {
                found.add(graph.getDest(n));
            }
            if (depth > 0) {
                for (N f : found) {
                    findEdges(f, related, depth - 1);
                }
            }
            related.addAll(found);
        }
    };

    /**
     * The lookup contains the currently selected nodes, and you can listen for
     * changes on it to get the current set of selected nodes
     *
     * @return The selection
     */
    @Override
    public final Lookup getLookup() {
        return lkp;
    }

    /**
     * Gets the JUNG layout
     *
     * @return
     */
    public final Layout layout() {
        return layout;
    }

    /**
     * Gets the JUNG graph
     *
     * @return
     */
    public final Graph<N, E> graph() {
        return graph;
    }

    /**
     * Gets an object which can answer graph-oriented questions about the
     * selection (i.e. is a node connected to the selection by one or more edges
     * indirection)
     *
     * @return
     */
    public final GraphSelection getSelection() {
        return selection;
    }

    /**
     * Overridden to inform the layout of the view's size
     *
     * @return The view
     */
    @Override
    public JComponent createView() {
        boolean was = initialized;
        if (!initialized) {
            initialized = true;
            sync();
            sceneLayout.performLayout();
        }
        JComponent view = super.createView();
        if (!was && layout instanceof IterativeContext && animate) {
            startAnimation();
        }
        return view;
    }

    /**
     * Re-layout the scene using the JUNG layout.
     */
    public final void performLayout() {
        performLayout(false);
    }

    /**
     * Re-layout the scene, optionally animating the node positions
     *
     * @param animate Animate the transition of node positions
     */
    public final void performLayout(boolean animate) {
//        sceneLayout.invokeLayout();
        sceneLayout.performLayout(animate);
    }

    /**
     * Set the number of pre-iterations to perform on layouts which implement
     * IterativeContext, if <i>not</i> animating the layout with a timer.
     * <p/>
     * Some JUNG layouts implement IterativeContext and are expected to be
     * re-laid out repeatedly until the system reaches a good state; the initial
     * layout is often simply random placement of nodes.
     * <p/>
     * In the case that animation is off, we still want to display a good
     * looking graph, so calls to setGraphLayout can automatically iterate the
     * layout a bunch of steps immediately. Depending on how computationally
     * expensive the layout is, and the number of nodes and edges in the graph,
     * this could be slow, so we provide this way to limit the number of
     * iterations.
     *
     * @param val The number of iterations; &lt;=0 equals none.
     */
    public final void setFastForwardIterations(int val) {
        this.fastForwardIterations = val;
    }

    /**
     * Set the JUNG layout, triggering a re-layout of the scene
     *
     * @param layout The layout, may not be null
     * @param animate If true, animate the node widgets to their new locations
     */
    public final void setGraphLayout(Layout<N, E> layout, boolean animate) {
        assert layout != null : "Layout null";
        timer.stop();
        this.layout = layout;
        sceneLayout.performLayout(animate);
        if (this.animate && layout instanceof IterativeContext && getView() != null) {
            startAnimation();
        } else if (!this.animate && layout instanceof IterativeContext) {
            // Fast forward it a bit
            IterativeContext ctx = (IterativeContext) layout;
            if (!ctx.done() && fastForwardIterations > 0) {
                try {
                    for (int i = 0; i < fastForwardIterations; i++) {
                        ctx.step();
                        if (ctx.done()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Logger.getLogger(JungScene.class.getName()).log(Level.INFO, null, e);
                }
            }
            sceneLayout.performLayout(true);
            validate();
            repaint();
        }
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). If true, setting one of these
     * layouts will trigger a timer that re-layouts the scene until the layout
     * says it has reached an optimal state or this property is set to false.
     *
     * @return Whether or not to animate
     */
    public final boolean isAnimateIterativeLayouts() {
        return animate;
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). If true, setting one of these
     * layouts will trigger a timer that re-layouts the scene until the layout
     * says it has reached an optimal state or this property is set to false.
     *
     * @param val to animate or not
     */
    public final void setAnimateIterativeLayouts(boolean val) {
        boolean old = this.animate;
        if (old != val) {
            this.animate = val;
            if (val && this.layout instanceof IterativeContext && getView() != null) {
                startAnimation();
            } else if (!val) {
                timer.stop();
            }
        }
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). This property sets the frame
     * rate for animating them, in frames per second.
     *
     * @param fps Frames per second - must be >= 1
     */
    public final void setLayoutAnimationFramesPerSecond(int fps) {
        if (fps < 1) {
            throw new IllegalArgumentException("Frame rate must be at least 1. "
                    + "Use setAnimateIterativeLayouts() to disable animation.");
        }
        timer.setDelay(1000 / fps);
    }

    /**
     * Some JUNG layouts support iteratively evolving toward an optimal layout
     * (where precomputing this is too expensive). This is the frame rate for
     * the animation timer (actual results may vary if the machine cannot keep
     * up with the math involved).
     *
     * @return The requested frame rate
     */
    public int getAnimationFramesPerSecond() {
        int delay = timer.getDelay();
        return delay / 1000;
    }

    /**
     * Start the animation timer and reset the evaluator's count of
     * insignificant changes.
     */
    private void startAnimation() {
        evaluator.reset();
        timer.start();
    }

    /**
     * Set the JUNG layout, triggering a re-layout of the scene
     *
     * @param layout
     */
    public final void setGraphLayout(Layout<N, E> layout) {
        setGraphLayout(layout, false);
    }

    /**
     * Returns an AutoCloseable which can be used in a try-with-resources loop
     * to modify the graph and automatically sync the widgets on-screen with the
     * nodes on close.
     *
     * @return A GraphMutator.
     */
    public final GraphMutator<N, E> modifyGraph() {
        return new GraphMutator<N, E>(this);
    }

    /**
     * Add a node to this graph. Use this method in place of
     * <code>addNode()</code>. If the graph passed to this scene's constructor
     * does not implement <code>ObservableGraph</code>, you will need to
     * manually call <code>sync()</code> to update the scene from the graph (or
     * use <code>GraphMutator</code> in a try-with-resources loop).
     *
     * @param node A node
     */
    public final void addGraphNode(N node) {
        graph.addVertex(node);
    }

    /**
     * Add an edge to this graph, associating it with the nodes it connects. If
     * the graph passed to this scene's constructor does not implement
     * <code>ObservableGraph</code>, you will need to manually call
     * <code>sync()</code> to update the scene from the graph (or use
     * <code>GraphMutator</code> in a try-with-resources loop).
     *
     * @param edge The edge
     * @param source The source node
     * @param target The target node
     */
    public final void addGraphEdge(E edge, N source, N target) {
        graph.addEdge(edge, source, target);
    }

    public static final class GraphMutator<N, E> implements AutoCloseable {

        private final JungScene<N, E> scene;

        GraphMutator(JungScene<N, E> scene) {
            this.scene = scene;
        }

        /**
         * Add a node to this graph. Use this method in place of
         * <code>addNode()</code>. If the graph passed to this scene's
         * constructor does not implement <code>ObservableGraph</code>, you will
         * need to manually call <code>sync()</code> to update the scene from
         * the graph (or use <code>GraphMutator</code> in a try-with-resources
         * loop).
         *
         * @param node A node
         */
        public void addGraphNode(N node) {
            scene.addGraphNode(node);
        }

        /**
         * Add an edge to this graph, associating it with the nodes it connects.
         * If the graph passed to this scene's constructor does not implement
         * <code>ObservableGraph</code>, you will need to manually call
         * <code>sync()</code> to update the scene from the graph (or use
         * <code>GraphMutator</code> in a try-with-resources loop).
         *
         * @param edge The edge
         * @param source The source node
         * @param target The target node
         */
        public void addGraphEdge(E edge, N source, N target) {
            scene.addGraphEdge(edge, source, target);
        }

        /**
         * Synchronizes the scene's graph's contents with the nodes the scene
         * knows about.
         */
        @Override
        public void close() {
            scene.sync();
            scene.performLayout(true);
        }
    }

    /**
     * Cause the scene to sync itself with the graph. If the Graph passed to the
     * constructor implements ObservableGraph, you will not need to call this;
     * otherwise, call this if the graph has been externally modified.
     */
    public final void sync() {
        // Get the current set of nodes the scene knows about
        Set<N> currNodes = new HashSet<>(super.getNodes());
        // Get the set the graph knows about
        Collection<N> nodes = graph.getVertices();
        // Add any not present in the current set
        for (N n : nodes) {
            if (!currNodes.contains(n)) {
                addNode(n);
                currNodes.add(n);
            }
        }
        // Remove all of the ones still part of the graph so we are
        // left with the set of nodes which are still held by the scene
        // but were removed from the graph, and remove them
        currNodes.removeAll(nodes);
        for (N n : currNodes) {
            Widget w = findWidget(n);
            w.removeFromParent();
            removeNode(n);
        }
        // Remove any edges we need to, and add any we don't know about
        Set<E> currEdges = new HashSet<>(super.getEdges());
        for (E e : graph.getEdges()) {
            if (!currEdges.contains(e)) {
                N src = graph.getSource(e);
                N dest = graph.getDest(e);
                if (src == null && dest == null) {
                    Pair<N> p = graph.getEndpoints(e);
                    src = p.getFirst();
                    dest = p.getSecond();
                }
                addEdge(e);
                setEdgeSource(e, src);
                setEdgeTarget(e, dest);
                currEdges.add(e);
            }
        }
        validate();
    }

    /**
     * Get a MoveAction for node widgets which will update the graph's layout so
     * that connections will be updated correctly. Use this if you are using
     * JungConnectionWidget.
     *
     * @return An action
     */
    public final WidgetAction createNodeMoveAction() {
        return ActionFactory.createMoveAction(ActionFactory.createFreeMoveStrategy(),
                sceneLayout);
    }

    protected MoveProvider moveProvider() {
        return sceneLayout;
    }

    /**
     * Set the object which creates Shape objects for edges when using
     * JungConnectionWidget. JUNG's class EdgeShape contains a number of useful
     * implementations.
     *
     * @param transformer An thing which makes line shapes
     */
    @SuppressWarnings(value = "unchecked")
    public void setConnectionEdgeShape(Function<E, Shape> transformer) {
        Set<Widget> parents = new HashSet<>();
        for (E edge : getEdges()) {
            Widget w = findWidget(edge);
            if (w instanceof JungConnectionWidget) {
                parents.add(w.getParentWidget());
                ((JungConnectionWidget<N, E>) w).setTransformer(transformer);
                w.revalidate();
            }
        }
        if (!parents.isEmpty()) {
            for (Widget connectionLayer : parents) { //typically there is only one
                connectionLayer.revalidate();
                connectionLayer.repaint();
            }
        }
        repaint();
    }

    /**
     * Create a move provider which will update the JUNG layout as needed, so
     * that user-dragged locations are not discarded by the layout, and dragging
     * while animating works as expected.
     *
     * @return A move provider
     */
    protected final MoveProvider createMoveProvider() {
        return sceneLayout;
    }

    /**
     * Get the SceneLayout implementation which wrappers the JUNG layout and
     * lays out the widgets
     *
     * @return A SceneLayout
     */
    public final SceneLayout getSceneLayout() {
        return sceneLayout;
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

    /**
     * Adapter which implements SceneLayout and uses the layout logic of the
     * JUNG layout. Also acts as our MoveProvider which will tell the layout
     * about user-made changes to node positions, so these are remembered
     */
    private class LayoutAdapter extends SceneLayout implements MoveProvider {

        LayoutAdapter() {
            super(JungScene.this);
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

        private double minDist = Double.MAX_VALUE;
        private double maxDist = Double.MIN_VALUE;
        private double avgDist = 0D;

        protected void performLayout(boolean animate) {
            // Make sure the layout knows about the size of the view
            JComponent vw = getView();
            if (vw != null) {
                try {
                    layout.setSize(vw.getSize());
                } catch (UnsupportedOperationException e) {
                    // some layouts dont support this
                }
            }

            minDist = Double.MAX_VALUE;
            maxDist = Double.MIN_VALUE;
            avgDist = 0D;

            boolean animating = timer.isRunning();

            // Iterate the vertices and make sure the widgets locations
            // match the graph
            Collection<N> nodes = graph.getVertices();
            for (N n : nodes) {
                Widget widget = findWidget(n);
                Point2D oldLocation = widget.getPreferredLocation();
                Point2D newLocation = layout.apply(n);

                if (oldLocation != null && animating) {
                    double length = Math.abs(oldLocation.distance(newLocation));
                    minDist = Math.min(minDist, length);
                    maxDist = Math.max(maxDist, length);
                    avgDist += length;
                }

                Point p1 = toPoint(newLocation);
                if (animate) {
                    getSceneAnimator().animatePreferredLocation(widget, p1);
                } else {
                    widget.setPreferredLocation(p1);
                }
            }
            // Avoid div by zero
            avgDist /= nodes.isEmpty() ? 0D : (double) nodes.size();
            for (E e : graph.getEdges()) {
                Widget w = (Widget) findWidget(e);
                if (w instanceof ConnectionWidget) {
                    ((ConnectionWidget) w).reroute();
                } else {
                    w.revalidate();
                }
            }
            JungScene.this.validate();
            if (animating && evaluator.animationIsFinished(minDist, maxDist, avgDist, layout)) {
                timer.stop();
            }
        }

        private MoveProvider delegate = ActionFactory.createDefaultMoveProvider();

        @Override
        public void movementStarted(Widget widget) {
            delegate.movementStarted(widget);
        }

        @Override
        public void movementFinished(Widget widget) {
            delegate.movementFinished(widget);
            onMove((N) findObject(widget), widget);
        }

        @Override
        public Point getOriginalLocation(Widget widget) {
            return delegate.getOriginalLocation(widget);
        }

        @Override
        public void setNewLocation(Widget widget, Point location) {
            N node = null;
            for (N n : getNodes()) {
                if (findWidget(n) == widget) {
                    node = n;
                    break;
                }
            }
            if (node != null) {
                layout.setLocation(node, location);
                for (E e : graph.getOutEdges(node)) {
                    Widget w = findWidget(e);
                    w.revalidate();
                }
                for (E e : graph.getInEdges(node)) {
                    Widget w = findWidget(e);
                    w.revalidate();

                }
                onMove(node, widget);
            }
            delegate.setNewLocation(widget, location);
        }
    }

    /**
     * Create an action for selecting a widget by clicking it. To disable that
     * behavior, override and return a do-nothing action.
     *
     * @return An action
     */
    public WidgetAction createSelectByClickAction() {
        if (clickAction == null) {
            clickAction = new SelectByClickAction();
        }
        return clickAction;
    }

    private class SelectByClickAction extends WidgetAction.Adapter {

        @Override
        public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
            Object o = findObject(widget);
            if (o != null && getNodes().contains(o)) {
                if (event.isShiftDown()) {
                    Set<Object> sel = new HashSet<>(getSelectedObjects());
                    if (sel.contains(o)) {
                        sel.remove(o);
                    } else {
                        sel.add(o);
                    }
                    setSelectedObjects(sel);
                } else {
                    setSelectedObjects(new HashSet<>(Arrays.asList(o)));
                }
                return WidgetAction.State.CONSUMED;
            }
            return WidgetAction.State.REJECTED;
        }
    }

    /**
     * Subclasses should override this to revalidate the connection layer if
     * using JungConnectionWidget
     */
    protected void onMove(N n, Widget widget) {
    }

    /**
     * If the graph is an ObservableGraph, listens for changes in it and
     * adds/removes nodes appropriately
     */
    private class GraphEventAdapter implements GraphEventListener<N, E> {

        @Override
        public void handleGraphEvent(GraphEvent<N, E> ge) {
            switch (ge.getType()) {
                case VERTEX_ADDED: {
                    GraphEvent.Vertex<N, E> v = (GraphEvent.Vertex<N, E>) ge;
                    addNode(v.getVertex());
                    break;
                }
                case VERTEX_REMOVED: {
                    GraphEvent.Vertex<N, E> v = (GraphEvent.Vertex<N, E>) ge;
                    removeNode(v.getVertex());
                    break;
                }
                case EDGE_ADDED: {
                    GraphEvent.Edge<N, E> e = (GraphEvent.Edge<N, E>) ge;
                    N src = graph.getSource(e.getEdge());
                    N dest = graph.getDest(e.getEdge());
                    addEdge(e.getEdge());
                    setEdgeSource(e.getEdge(), src);
                    setEdgeTarget(e.getEdge(), dest);
                    break;
                }
                case EDGE_REMOVED: {
                    GraphEvent.Edge<N, E> e = (GraphEvent.Edge<N, E>) ge;
                    removeEdge(e.getEdge());
                    break;
                }
                default:
                    throw new AssertionError(ge.getType());
            }
            validate();
        }
    }

    private class TimerListener implements ActionListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            if (!animate) {
                return;
            }
            if (layout instanceof IterativeContext) {
                IterativeContext c = (IterativeContext) layout;
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
                    timer.stop();
                }
                getSceneLayout().invokeLayout();
                validate();
                repaint();
            }
        }
    }
}
