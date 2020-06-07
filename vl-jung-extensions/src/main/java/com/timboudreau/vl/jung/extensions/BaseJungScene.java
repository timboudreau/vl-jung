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
package com.timboudreau.vl.jung.extensions;

import com.timboudreau.vl.jung.JungConnectionWidget;
import com.timboudreau.vl.jung.JungScene;
import com.timboudreau.vl.jung.ObjectSceneAdapter;
import com.timboudreau.vl.jung.RingsWidget;
import static com.timboudreau.vl.jung.extensions.States.CONNECTED_TO_SELECTION;
import static com.timboudreau.vl.jung.extensions.States.HOVERED;
import static com.timboudreau.vl.jung.extensions.States.INDIRECTLY_CONNECTED_TO_SELECTION;
import static com.timboudreau.vl.jung.extensions.States.SELECTED;
import static com.timboudreau.vl.jung.extensions.States.UNRELATED_TO_SELECTION;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;

import org.jgrapht.Graph;
import org.jungrapht.visualization.layout.algorithms.BalloonLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.general.IconNodeWidget;

/**
 * A convenience base class for JUNG scenes which draws connections and 
 * handles layers correctly.
 * 
 * @author Tim Boudreau
 * @param <N>
 * @param <E> 
 */
public class BaseJungScene<N, E> extends JungScene<N, E> {

    protected final LayerWidget mainLayer = new LayerWidget(this);
    protected final LayerWidget connectionLayer = new LayerWidget(this);
    protected final LayerWidget selectionLayer = new LayerWidget(this);
    private final HoverAndSelectionHandler hover = new HoverAndSelectionHandler();
    protected final Widget decorationLayer = new RingsWidget(this);
    private final GraphTheme colors;
    private WidgetAction scrollZoom;
    private WidgetAction edgeClickSelect;
    private Dimension lastSize = new Dimension();

    public BaseJungScene(Graph<N, E> graph, LayoutAlgorithm<N> layoutAlgorithm) throws IOException {
        super(graph, layoutAlgorithm);
        colors = createColors();
        // Selection layer is behind everything
        addChild(selectionLayer);
        // Rings also drawn behind everything but the selection
        addChild(decorationLayer);
        // Connections are drawn below the node widgets
        addChild(connectionLayer);
        // The layer where node widgets live
        addChild(mainLayer);

        // Use the built in rectangular selection action
        getActions().addAction(ActionFactory.createRectangularSelectAction(this,
                selectionLayer));

        // Set some layouts
        connectionLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        mainLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        // Zoom on scroll by default
        getActions().addAction(createScrollWheelAction());
        
        // Add the listener which will notice when we hover and update
        // the node color
        addObjectSceneListener(hover, ObjectSceneEventType.OBJECT_HOVER_CHANGED,
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
    }

    /**
     * Create a color theme.  The default one starts with one color and
     * modifies it to portray different selection states.
     * @return A theme
     */
    protected GraphTheme createColors() {
        return new GraphThemeImpl();
    }

    /**
     * Re-layout the graph
     */
    public void relayout(boolean animate) {
        JComponent vw = getView();
        if (vw != null) {
            try {
                Dimension size = vw.getSize();
                if (!lastSize.equals(size)) {
                    layoutModel.setSize(size.width, size.height);
                }
            } catch (UnsupportedOperationException e) {
                // not supported by some graph layouts, and they tell us
                // in an ugly way
            }
        }
        super.performLayout(animate);
    }

    protected Widget createNodeWidget(N node) {
        IconNodeWidget result = new IconNodeWidget(this);
        result.setLabel(node + "");
        return result;
    }
    
    protected void attachActionsToNodeWidget(Widget widget) {
        widget.getActions().addAction(createNodeMoveAction());
        widget.getActions().addAction(createObjectHoverAction());
        widget.getActions().addAction(createSelectAction());
        widget.getActions().addAction(createSelectByClickAction());
    }

    @Override
    protected Widget attachNodeWidget(final N node) {
        Widget widget = createNodeWidget(node);
        // Set up the colors and actions
        widget.setBackground(colors.getBackground());
        widget.setForeground(colors.getForeground());
        attachActionsToNodeWidget(widget);
        mainLayer.addChild(widget);
        validate();
        return widget;
    }

    protected Widget createEdgeWidget(E edge) {
        JungConnectionWidget w = JungConnectionWidget.createQuadratic(this, edge);
        return w;
    }

    @Override
    protected Widget attachEdgeWidget(final E edge) {
        Widget w = createEdgeWidget(edge);
        w.setForeground(colors.getEdgeColor());
        w.getActions().addAction(createObjectHoverAction());
        w.getActions().addAction(createEdgeClickSelectAction());
        connectionLayer.addChild(w);
        return w;
    }

    protected WidgetAction createEdgeClickSelectAction() {
        if (edgeClickSelect == null) {
            edgeClickSelect = new EdgeClickSelectAction();
        }
        return edgeClickSelect;
    }

    @Override
    public void onMove(N n, Widget widget) {
        connectionLayer.repaint();
        if (layoutAlgorithm instanceof BalloonLayoutAlgorithm) {
            decorationLayer.revalidate();
        }
    }

    @Override
    protected void attachEdgeSourceAnchor(E edge, N oldSourceNode, N sourceNode) {
        Widget w = findWidget(edge);
        // Not the case by default, but could be overridden
        if (w instanceof ConnectionWidget) {
            Widget sourceNodeWidget = findWidget(sourceNode);
            Anchor sourceAnchor = AnchorFactory.createCenterAnchor(sourceNodeWidget);
            ConnectionWidget edgeWidget = (ConnectionWidget) w;
            edgeWidget.setSourceAnchor(sourceAnchor);
        }
    }

    @Override
    protected void attachEdgeTargetAnchor(E edge, N oldTargetNode, N targetNode) {
        Widget w = findWidget(edge);
        // Not the case by default, but could be overridden
        if (w instanceof ConnectionWidget) {
            Widget targetNodeWidget = findWidget(targetNode);
            Anchor targetAnchor = AnchorFactory.createCenterAnchor(targetNodeWidget);
            ConnectionWidget edgeWidget = (ConnectionWidget) findWidget(edge);
            edgeWidget.setTargetAnchor(targetAnchor);
        }
    }

    /**
     * Called when an edge stops being hovered
     * @param edge The edge
     * @param w The widget
     */
    protected void onEdgeUnhover(E edge, Widget w) {
        Color c = colors.getEdgeColor();
        getSceneAnimator().animateForegroundColor(w, c);
    }

    /**
     * Called when an edge becomes hovered
     * @param edge The edge
     * @param w The widget
     */
    protected void onEdgeHover(E edge, Widget w) {
        Color c = colors.getEdgeColor(HOVERED);
        getSceneAnimator().animateForegroundColor(w, c);
    }

    /**
     * Called when a node stops being hovered
     * @param n The node
     * @param w The widget
     */
    protected void onNodeUnhover(N n, Widget w) {
        ObjectState state = getObjectState(n);
        boolean hasSelection = !getSelectedObjects().isEmpty();
        boolean connected = getSelection().isConnectedToSelection(n);
        boolean indirect = !connected && !state.isSelected() && getSelection().isIndirectlyConnectedToSelection(n);
        States[] states = state.isSelected() ? new States[]{SELECTED}
                : hasSelection ? new States[]{connected ? CONNECTED_TO_SELECTION : indirect ? INDIRECTLY_CONNECTED_TO_SELECTION : UNRELATED_TO_SELECTION} : new States[0];
        Color c = colors.getEdgeColor(states);
        for (E edge : findNodeEdges(n, true, false)) {
            Widget w1 = findWidget(edge);
            getSceneAnimator().animateForegroundColor(w1, c);
        }
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(states));
    }

    /**
     * Called when a node starts being hovered
     * @param n The node
     * @param w The widget
     */
    protected void onNodeHover(N n, Widget w) {
        ObjectState state = getObjectState(n);
        boolean hasSelection = !getSelectedObjects().isEmpty();
        boolean connected = getSelection().isConnectedToSelection(n);
        boolean indirect = !connected && !state.isSelected() && getSelection().isIndirectlyConnectedToSelection(n);
        States[] states = state.isSelected()
                ? new States[]{SELECTED, HOVERED}
                : hasSelection
                ? new States[]{HOVERED, connected ? CONNECTED_TO_SELECTION : indirect ? INDIRECTLY_CONNECTED_TO_SELECTION : UNRELATED_TO_SELECTION}
                : new States[]{HOVERED};

        Color c = colors.getEdgeColor(states);
        for (E edge : findNodeEdges(n, true, false)) {
            Widget w1 = findWidget(edge);
            getSceneAnimator().animateForegroundColor(w1, c);
        }
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(states));
    }

    /**
     * Called when the selection is cleared, once for every node widget
     * @param w The widget
     * @param n The node
     */
    protected void onSelectionCleared(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(null, n)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(null, n)));
    }

    /**
     * Called when the selection is cleared, once for every edge widget
     * @param w The widget
     * @param e The edge
     */
    protected void onEdgeSelectionCleared(Widget w, E e) {
        getSceneAnimator().animateForegroundColor(w, colors.getEdgeColor(statesFor(null, e)));
    }

    /**
     * Called when a node becomes selected
     * @param w The widget
     * @param n The node
     */
    protected void onNodeSelected(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(SELECTED, n)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(SELECTED, n)));
    }

    /**
     * Called when a node connected to this one was selected assuming it is not
     * also selected
     * @param w The widget
     * @param n The node
     */
    protected void onNodeConnectedToSelection(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w,
                colors.getBackground(statesFor(CONNECTED_TO_SELECTION, n)));
        getSceneAnimator().animateForegroundColor(w,
                colors.getForeground(statesFor(CONNECTED_TO_SELECTION, n)));
    }

    /**
     * Called when a node connected to this one is connected to the selected node,
     * assuming this one is not selected or directly connected to the selected node
     * @param w The widget
     * @param n The node 
     */
    protected void onNodeIndirectlyConnectedToSelection(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, n)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, n)));
    }

    /**
     * Called when a node becomes unconnected to any selected node due to a 
     * selection change
     * @param w The widget
     * @param n The node
     */
    protected void onNodeUnrelatedToSelection(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(UNRELATED_TO_SELECTION, n)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(UNRELATED_TO_SELECTION, n)));
    }

    /**
     * Called when an edge becomes connected to the selection due to a selection
     * change
     * @param e The edge
     */
    protected void onEdgeConnectedToSelection(E e) {
        getSceneAnimator().animateForegroundColor(findWidget(e), colors.getEdgeColor(statesFor(CONNECTED_TO_SELECTION, e)));
    }

    /**
     * Called when an edge becomes connected to a node which is connected to the
     * selection but not selected itself, due to a selection change
     * @param e The edge
     */
    protected void onEdgeIndirectlyConnectedToSelection(E e) {
        getSceneAnimator().animateForegroundColor(findWidget(e), colors.getEdgeColor(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, e)));
    }

    /**
     * Get the set of states which apply to this object, in terms of an array
     * of States.
     * @param curr A state to include in the reuslt if non-null
     * @param o The object - may be an edge or node
     * @return 
     */
    public States[] statesFor(States curr, Object o) {
        ObjectState st = getObjectState(o);
        if (st.isHovered()) {
            return curr == null ? new States[]{States.HOVERED} : new States[]{curr, States.HOVERED};
        } else {
            return curr == null ? new States[0] : new States[]{curr};
        }
    }

    private class HoverAndSelectionHandler extends ObjectSceneAdapter {

        @Override
        public void hoverChanged(ObjectSceneEvent event, Object previousHoveredObject, Object newHoveredObject) {
            @SuppressWarnings("element-type-mismatch")
            boolean wasEdge = getEdges().contains(previousHoveredObject);
            @SuppressWarnings("element-type-mismatch")
            boolean wasNode = getNodes().contains(previousHoveredObject);
            @SuppressWarnings("element-type-mismatch")
            boolean isEdge = getEdges().contains(newHoveredObject);
            @SuppressWarnings("element-type-mismatch")
            boolean isNode = getNodes().contains(newHoveredObject);
            if (wasEdge) {
                E edge = (E) previousHoveredObject;
                onEdgeUnhover(edge, findWidget(edge));
            } else if (wasNode) {
                N node = (N) previousHoveredObject;
                onNodeUnhover(node, findWidget(node));
            }
            if (isEdge) {
                E edge = (E) newHoveredObject;
                onEdgeHover(edge, findWidget(edge));
            } else if (isNode) {
                N node = (N) newHoveredObject;
                onNodeHover(node, findWidget(node));
            }
        }

        @Override
        public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
            if (newSelection.isEmpty()) {
                // Special case the selection being empty
                for (N n : getNodes()) {
                    Widget w = findWidget(n);
                    onSelectionCleared(w, n);
                }
                for (E e : getEdges()) {
                    Widget w = findWidget(e);
                    if (w != null) {
                        onEdgeSelectionCleared(w, e);
                    }
                }
                return;
            }
            // Find all the nodes which used to be selected but are not anymore
            Set<Object> noLongerSelected = new HashSet<>(previousSelection);
            noLongerSelected.removeAll(newSelection);

            // Get the set of all nodes we know about in the sscene
            Set<Object> remainingNodes = new HashSet<Object>(getNodes());

            // Get the set of all edges we know about in the scene
            Set<E> otherEdges = new HashSet<>(getEdges());
            Set<E> closeEdges = new HashSet<>();
            Set<E> farEdges = new HashSet<>();

            // Walk the graph, finding all the relevant edges and nodes and
            // their selection state
            Set<Object> newlySelected = new HashSet<>(newSelection);
            newlySelected.removeAll(previousSelection);
            for (Object o : newlySelected) {
                remainingNodes.remove(o);
                Widget w = findWidget(o);
                onNodeSelected(w, (N) o);
            }
            Set<N> connectedNodes = new HashSet<>();
            for (Object o : newlySelected) {
                N n = (N) o;
                Collection<E> edges = graph.outgoingEdgesOf(n);
                if (edges != null) {
                    for (E edge : graph.outgoingEdgesOf(n)) {
                        closeEdges.add(edge);
                        otherEdges.remove(edge);
                        N opp = graph.getEdgeTarget(edge);
                        remainingNodes.remove(opp);
                        connectedNodes.add(opp);
                        if (!newlySelected.contains(opp)) {
                            Widget w = findWidget(opp);
                            onNodeConnectedToSelection(w, opp);
                        }
                    }
                }
            }
            for (N n : connectedNodes) {
                Collection<E> edges = graph.incomingEdgesOf(n);
                if (edges != null) {
                    for (E edge : graph.outgoingEdgesOf(n)) {
                        closeEdges.add(edge);
                        otherEdges.remove(edge);
                        N opp = graph.getEdgeTarget(edge);
                        remainingNodes.remove(opp);
                        if (!newSelection.contains(opp) && !connectedNodes.contains(opp)) {
                            Widget w = findWidget(opp);
                            onNodeIndirectlyConnectedToSelection(w, opp);
                        }
                    }
                }
            }
            for (Object o : remainingNodes) {
                Widget w = findWidget(o);
                onNodeUnrelatedToSelection(w, (N) o);
            }
            for (E e : closeEdges) {
                onEdgeConnectedToSelection(e);
            }
            for (E e : farEdges) {
                onEdgeIndirectlyConnectedToSelection(e);
            }
            for (E e : otherEdges) {
                onEdgeSelectionCleared(findWidget(e), e);
            }
            mainLayer.repaint();
        }
    }

    private class EdgeClickSelectAction extends WidgetAction.Adapter {

        @Override
        public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
            E edge = (E) findObject(widget);
            HashSet<N> nue = new HashSet<>();
            nue.add(graph.getEdgeSource(edge));
            nue.add(graph.getEdgeTarget(edge));
//                    graph.getEndpoints(edge));
            Set<Object> selection = new HashSet<>(getSelectedObjects());
            if (selection.isEmpty() || !event.isShiftDown()) {
                setSelectedObjects(nue);
            } else {
                if (selection.containsAll(nue) && !event.isShiftDown()) {
                    selection.removeAll(nue);
                    setSelectedObjects(selection);
                } else {
                    selection.addAll(nue);
                    setSelectedObjects(selection);
                }
            }
            return WidgetAction.State.REJECTED;
        }
    }

    protected WidgetAction createScrollWheelAction() {
        if (scrollZoom == null) {
            scrollZoom = new ScrollWheelZoomAction();
        }
        return scrollZoom;
    }

    private class ScrollWheelZoomAction extends WidgetAction.Adapter {

        @Override
        public WidgetAction.State mouseWheelMoved(Widget widget, WidgetAction.WidgetMouseWheelEvent event) {
            double zoom = getZoomFactor();
            int units = event.getUnitsToScroll();
            double amt = (double) units * 0.025D;
            zoom = Math.max(0.1D, zoom + amt);
            setZoomFactor(zoom);
            repaint();
            return WidgetAction.State.CONSUMED;
        }
    }
}
