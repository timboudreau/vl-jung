package com.timboudreau.vl.jung.extensions;

import com.timboudreau.vl.jung.extensions.States;
import com.timboudreau.vl.demo.JungConnectionWidget;
import com.timboudreau.vl.demo.JungScene;
import com.timboudreau.vl.demo.RingsWidget;
import static com.timboudreau.vl.jung.extensions.States.CONNECTED_TO_SELECTION;
import static com.timboudreau.vl.jung.extensions.States.HOVERED;
import static com.timboudreau.vl.jung.extensions.States.INDIRECTLY_CONNECTED_TO_SELECTION;
import static com.timboudreau.vl.jung.extensions.States.SELECTED;
import static com.timboudreau.vl.jung.extensions.States.UNRELATED_TO_SELECTION;
import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.util.Context;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import org.apache.commons.collections15.Transformer;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.model.ObjectSceneListener;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.general.IconNodeWidget;

public class SimpleJungScene<N, E> extends JungScene<N, E> {

    private final LayerWidget mainLayer = new LayerWidget(this);
    private final LayerWidget connectionLayer = new LayerWidget(this);
    private final LayerWidget selectionLayer = new LayerWidget(this);
    private final HoverHandler hover = new HoverHandler();
    private final Widget decorationLayer = new RingsWidget(this);
    private final GraphTheme colors;
    private WidgetAction edgeClickSelect;
    private Dimension lastSize = new Dimension();

    public SimpleJungScene(ObservableGraph<N, E> graph, Layout layout) throws IOException {
        super(graph, layout);
        colors = createColors();
        addChild(selectionLayer);
        addChild(decorationLayer);
        addChild(connectionLayer);
        addChild(mainLayer);

        getActions().addAction(ActionFactory.createRectangularSelectAction(this,
                selectionLayer));

        connectionLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        mainLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        getActions().addAction(createScrollWheelAction());
        addObjectSceneListener(hover, ObjectSceneEventType.OBJECT_HOVER_CHANGED,
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
    }
    
    protected GraphTheme createColors() {
        return new GraphThemeImpl();
    }

    public void relayout() {
        JComponent vw = getView();
        if (vw != null) {
            try {
                Dimension size = vw.getSize();
                if (!lastSize.equals(size)) {
                    layout.setSize(size);
                }
            } catch (UnsupportedOperationException e) {
                //not supported by layout
            }
        }
        super.performLayout();
    }

    protected Widget createNodeWidget(N node) {
        IconNodeWidget result = new IconNodeWidget(this);
        result.setLabel(node + "");
        return result;
    }

    @Override
    protected Widget attachNodeWidget(final N node) {
//        IconNodeWidget widget = new CompositeIconNodeWidget(this);
//        DemoWidget widget = new DemoWidget(this);
        Widget widget = createNodeWidget(node);
//        widget.setImage(icon);
        widget.setBackground(colors.getBackground());
        widget.setForeground(colors.getForeground());
        widget.getActions().addAction(createNodeMoveAction());
        widget.getActions().addAction(createObjectHoverAction());
        widget.getActions().addAction(createSelectAction());
        widget.getActions().addAction(createSelectByClickAction());

        mainLayer.addChild(widget);
        validate();
        return widget;
    }

    protected Widget createEdgeWidget(E edge) {
        JungConnectionWidget w = JungConnectionWidget.createQuadratic(this, edge);
        w.setToolTipText(edge + "");
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
        if (layout instanceof BalloonLayout) {
            decorationLayer.revalidate();
        }
    }

    @Override
    protected void attachEdgeSourceAnchor(E edge, N oldSourceNode, N sourceNode) {
        Widget w = findWidget(edge);
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
        if (w instanceof ConnectionWidget) {
            Widget targetNodeWidget = findWidget(targetNode);
            Anchor targetAnchor = AnchorFactory.createCenterAnchor(targetNodeWidget);
            ConnectionWidget edgeWidget = (ConnectionWidget) findWidget(edge);
            edgeWidget.setTargetAnchor(targetAnchor);
        }
    }

    protected void onEdgeUnhover(E edge, Widget w) {
        Color c = colors.getEdgeColor();
        getSceneAnimator().animateForegroundColor(w, c);
    }

    protected void onEdgeHover(E edge, Widget w) {
        Color c = colors.getEdgeColor(States.HOVERED);
        getSceneAnimator().animateForegroundColor(w, c);
    }

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

    protected void onSelectionCleared(Widget w, N n) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(null, n)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(null, n)));
    }

    protected void onEdgeSelectionCleared(Widget w, E e) {
        getSceneAnimator().animateForegroundColor(w, colors.getEdgeColor(statesFor(null, e)));
    }

    protected void onNodeSelected(Widget w, Object o) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(SELECTED, o)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(SELECTED, o)));
    }

    protected void onNodeConnectedToSelection(Widget w, N opp) {
        getSceneAnimator().animateBackgroundColor(w,
                colors.getBackground(statesFor(CONNECTED_TO_SELECTION, opp)));
        getSceneAnimator().animateForegroundColor(w,
                colors.getForeground(statesFor(CONNECTED_TO_SELECTION, opp)));
    }

    protected void onNodeIndirectlyConnectedToSelection(Widget w, N opp) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, opp)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, opp)));
    }

    protected void onNodeUnrelatedToSelection(Widget w, Object o) {
        getSceneAnimator().animateBackgroundColor(w, colors.getBackground(statesFor(UNRELATED_TO_SELECTION, o)));
        getSceneAnimator().animateForegroundColor(w, colors.getForeground(statesFor(UNRELATED_TO_SELECTION, o)));
    }

    protected void onEdgeConnectedToSelection(E e) {
        getSceneAnimator().animateForegroundColor(findWidget(e), colors.getEdgeColor(statesFor(CONNECTED_TO_SELECTION, e)));
    }

    protected void onEdgeIndirectlyConnectedToSelection(E e) {
        getSceneAnimator().animateForegroundColor(findWidget(e), colors.getEdgeColor(statesFor(INDIRECTLY_CONNECTED_TO_SELECTION, e)));
    }

    public States[] statesFor(States curr, Object o) {
        ObjectState st = getObjectState(o);
        if (st.isHovered()) {
            return curr == null ? new States[]{States.HOVERED} : new States[]{curr, States.HOVERED};
        } else {
            return curr == null ? new States[0] : new States[]{curr};
        }
    }

    private class HoverHandler implements ObjectSceneListener {

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
        public void objectAdded(ObjectSceneEvent event, Object addedObject) {
        }

        @Override
        public void objectRemoved(ObjectSceneEvent event, Object removedObject) {
        }

        @Override
        public void objectStateChanged(ObjectSceneEvent event, Object changedObject, ObjectState previousState, ObjectState newState) {
        }

        @Override
        public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
            if (newSelection.isEmpty()) {
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
            Set<Object> noLongerSelected = new HashSet<>(previousSelection);
            noLongerSelected.removeAll(newSelection);

            Set<Object> remainingNodes = new HashSet<Object>(getNodes());

            Set<E> otherEdges = new HashSet<>(getEdges());
            Set<E> closeEdges = new HashSet<>();
            Set<E> farEdges = new HashSet<>();

            Set<Object> newlySelected = new HashSet<>(newSelection);
            newlySelected.removeAll(previousSelection);
            for (Object o : newlySelected) {
                remainingNodes.remove(o);
                Widget w = findWidget(o);
                onNodeSelected(w, o);
            }
            Set<N> connectedNodes = new HashSet<>();
            for (Object o : newlySelected) {
                N n = (N) o;
                Collection<E> edges = graph.getOutEdges(n);
                if (edges != null) {
                    for (E edge : graph.getOutEdges(n)) {
                        closeEdges.add(edge);
                        otherEdges.remove(edge);
                        N opp = graph.getOpposite(n, edge);
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
                Collection<E> edges = graph.getInEdges(n);
                if (edges != null) {
                    for (E edge : graph.getOutEdges(n)) {
                        closeEdges.add(edge);
                        otherEdges.remove(edge);
                        N opp = graph.getOpposite(n, edge);
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
                onNodeUnrelatedToSelection(w, o);
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

        @Override
        public void highlightingChanged(ObjectSceneEvent event, Set<Object> previousHighlighting, Set<Object> newHighlighting) {
        }

        @Override
        public void focusChanged(ObjectSceneEvent event, Object previousFocusedObject, Object newFocusedObject) {
        }
    }

    private class EdgeClickSelectAction extends WidgetAction.Adapter {

        @Override
        public WidgetAction.State mouseClicked(Widget widget, WidgetAction.WidgetMouseEvent event) {
            E edge = (E) findObject(widget);
            HashSet<N> nue = new HashSet<>(graph.getEndpoints(edge));
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

    private WidgetAction scrollZoom;

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
