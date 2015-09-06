/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timboudreau.vl.jung;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.MoveStrategy;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public final class MultiMoveAction extends WidgetAction.LockedAdapter {

    private final MoveStrategy strategy;
    private final MoveProvider provider;
    private final RelatedWidgetProvider related;
    private MoveData moveData;

    public MultiMoveAction(RelatedWidgetProvider related, MoveProvider provider) {
        this.strategy = ActionFactory.createFreeMoveStrategy();
        this.provider = provider;
        this.related = related;
    }

    public interface RelatedWidgetProvider {

        public void findWidgets(Widget relatedTo, Collection<? super Widget> addTo, int additionalDepth);
    }

    protected boolean isLocked() {
        return moveData != null;
    }

    static final class MoveData implements Iterable<MoveData> {

        final Widget movingWidget;
        final Point dragSceneLocation;
        final Point originalSceneLocation;
        final Point initialMouseLocation;
        final List<MoveData> md = new ArrayList<>(20);

        public MoveData(Widget widget, WidgetAction.WidgetMouseEvent event, MoveProvider provider) {
            movingWidget = widget;
            initialMouseLocation = event.getPoint();
            Point originalSceneLocation = provider.getOriginalLocation(widget);
            if (originalSceneLocation == null) {
                originalSceneLocation = new Point();
            }
            this.originalSceneLocation = originalSceneLocation;
            dragSceneLocation = widget.convertLocalToScene(event.getPoint());
            md.add(this);
        }

        boolean contains(Widget widget) {
            if (widget == movingWidget) {
                return true;
            }
            for (MoveData m : this) {
                if (widget == m.movingWidget) {
                    return true;
                }
            }
            return false;
        }

        public MoveData add(Widget widget, WidgetAction.WidgetMouseEvent event, MoveProvider provider) {
            md.add(new MoveData(widget, event, provider));
            return this;
        }

        @Override
        public Iterator<MoveData> iterator() {
            return md.iterator();
        }
    }

    public WidgetAction.State mousePressed(Widget widget, WidgetAction.WidgetMouseEvent event) {
        if (isLocked()) {
            return WidgetAction.State.createLocked(widget, this);
        }
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1 && (event.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == KeyEvent.SHIFT_DOWN_MASK) {
            int depth = 0;
            if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK) {
                depth++;
            }
            if (depth == 1 && (event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) {
                depth++;
            }
            if (depth == 2 && (event.getModifiersEx() & KeyEvent.META_DOWN_MASK) == KeyEvent.META_DOWN_MASK) {
                depth++;
            }
            moveData = new MoveData(widget, event, provider);
            Set<Widget> others = new HashSet<>();
            related.findWidgets(widget, others, depth);
            provider.movementStarted(widget);
            for (Widget w : others) {
                moveData.add(w, event, provider);
                provider.movementStarted(w);
            }
            return WidgetAction.State.createLocked(widget, this);
        }
        return WidgetAction.State.REJECTED;
    }

    public WidgetAction.State mouseReleased(Widget widget, WidgetAction.WidgetMouseEvent event) {
        boolean state;
        if (moveData.initialMouseLocation != null && moveData.initialMouseLocation.equals(event.getPoint())) {
            state = true;
        } else {
            state = move(widget, event.getPoint());
        }
        if (state) {
            MoveData data = moveData;
            moveData = null;
            for (MoveData other : data) {
                provider.movementFinished(other.movingWidget);
            }
        }
        return state ? WidgetAction.State.CONSUMED : WidgetAction.State.REJECTED;
    }

    @Override
    public WidgetAction.State mouseDragged(Widget widget, WidgetAction.WidgetMouseEvent event) {
        Point point = event.getPoint();
        boolean moved = move(widget, point);
        if (moved) {
            for (MoveData md : moveData) {
                move(md.movingWidget, point);
            }
        }
        return moved ? WidgetAction.State.createLocked(widget, this) : WidgetAction.State.REJECTED;
    }

    private boolean move(Widget widget, Point newLocation) {
        if (moveData == null || (moveData != null && !moveData.contains(widget))) {
            System.out.println("No move for " + widget);
            return false;
        }
        newLocation = widget.convertLocalToScene(newLocation);
        for (MoveData md : moveData) {
            Point location = new Point(md.originalSceneLocation.x + newLocation.x - md.dragSceneLocation.x, md.originalSceneLocation.y + newLocation.y - md.dragSceneLocation.y);
            provider.setNewLocation(widget, strategy.locationSuggested(widget, md.originalSceneLocation, location));
        }
        return true;
    }

}
