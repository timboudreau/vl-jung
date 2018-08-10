/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.timboudreau.vl.jung;

import java.awt.Dimension;
import java.awt.Point;
import java.util.function.Supplier;
import javax.swing.JComponent;
import org.netbeans.api.visual.model.ObjectScene;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
abstract class AbstractGraphProvider<N, E, S extends ObjectScene> implements GraphAdapter<N, E>, Supplier<Dimension> {

    protected final S scene;

    AbstractGraphProvider(S scene) {
        if (scene == null) {
            throw new NullPointerException("scene is null");
        }
        this.scene = scene;
    }

    @Override
    public Point getPreferredLocation(N node) {
        Widget widget = scene.findWidget(node);
        return widget == null ? null : widget.getPreferredLocation();
    }

    @Override
    public void revalidateEdge(E e) {
        Widget w = scene.findWidget(e);
        if (w instanceof ConnectionWidget) {
            ((ConnectionWidget) w).reroute();
        } else if (w != null) {
            w.revalidate();
        }
    }

    @Override
    public Dimension get() {
        JComponent view = scene.getView();
        return view == null ? new Dimension(640, 480) : scene.getView().getSize();
    }

    @Override
    public void revalidateNode(N node) {
        Widget w = scene.findWidget(node);
        if (w != null) {
            w.revalidate();
        }
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public E findEdge(Widget widget) {
        return (E) scene.findObject(widget);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public N findNode(Widget widget) {
        return (N) scene.findObject(widget);
    }

}
