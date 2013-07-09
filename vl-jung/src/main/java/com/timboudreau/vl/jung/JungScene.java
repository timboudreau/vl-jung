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

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import org.apache.commons.collections15.Transformer;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.model.ObjectSceneListener;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Base class for Visual Library scenes which use Jung to manage layout and
 * connection drawing for graphs. You pass a Graph and a Layout to the
 * constructor; if the graph implements ObservableGraph, then the scene will
 * automatically update itself when the graph is modified; otherwise call
 * <code>sync()</code> when the graph is modified.
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
    private final Lookup lkp;

    /**
     * Create a new Scene backed by the passed graph, and whose initial layout
     * is done using the passed layout.
     *
     * @param graph A JUNG graph which will be used to provide the graph
     * @param layout A JUNG layout to be used as the initial layout
     */
    protected JungScene(Graph<N, E> graph, Layout layout) {
        this.graph = graph;
        this.layout = layout;
        if (graph instanceof ObservableGraph) {
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
                System.out.println("CONTENTS NOW " + lkp.lookupAll(String.class));
            }
        }, ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
    }

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
        if (!initialized) {
            initialized = true;
            sync();
            sceneLayout.performLayout();
        }
        return super.createView();
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
        sceneLayout.performLayout();
    }
    /**
     * Set the JUNG layout, triggering a re-layout of the scene
     *
     * @param layout The layout, may not be null
     * @param animate If true, animate the node widgets to their new locations
     */
    public final void setGraphLayout(Layout<N, E> layout, boolean animate) {
        assert layout != null : "Layout null";
        this.layout = layout;
        // XXX animate optionally
        sceneLayout.performLayout(animate);
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

    /**
     * Set the object which creates Shape objects for edges when using
     * JungConnectionWidget. JUNG's class EdgeShape contains a number of useful
     * implementations.
     *
     * @param transformer An thing which makes line shapes
     */
    @SuppressWarnings(value = "unchecked")
    public void setConnectionEdgeShape(Transformer<Context<Graph<N, E>, E>, Shape> transformer) {
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
     * Adapter which implements SceneLayout and uses the layout logic of the
     * JUNG layout. Also acts as our MoveProvider which will tell the layout
     * about user-made changes to node positions, so these are remembered
     */
    private class LayoutAdapter extends org.netbeans.api.visual.layout.SceneLayout implements MoveProvider {

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
            // Iterate the vertices and make sure the widgets locations
            // match the graph
            for (N n : graph.getVertices()) {
                Point2D p = layout.transform(n);
                Point p1 = toPoint(p);
                Widget widget = findWidget(n);
                if (animate) {
                    getSceneAnimator().animatePreferredLocation(widget, p1);
                } else {
                    widget.setPreferredLocation(p1);
                }
            }
            for (E e : graph.getEdges()) {
                Widget w = (Widget) findWidget(e);
                if (w instanceof ConnectionWidget) {
                    ((ConnectionWidget) w).reroute();
                } else {
                    w.revalidate();
                }
            }
            JungScene.this.validate();
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
}
