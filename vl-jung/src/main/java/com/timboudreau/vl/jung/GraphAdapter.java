/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timboudreau.vl.jung;

import java.awt.Point;
import java.util.Collection;
import org.netbeans.api.visual.graph.GraphPinScene;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.widget.Widget;

/**
 * GraphScene and GraphPinScene do not share any common interface for getting
 * nodes and edges; this adapter class allows the same code to work with both.
 *
 * @author Tim Boudreau
 */
public interface GraphAdapter<N, E> {

    Collection<E> getEdges();

    Collection<N> getNodes();

    Point getPreferredLocation(N node);

    void revalidateEdge(E edge);

    void revalidateNode(N node);

    E findEdge(Widget widget);

    N findNode(Widget widget);

    Collection<E> getEdges(N node, boolean input, boolean output);

    Collection<Object> getDependentObjects(N node);

    public static <N, E> GraphAdapter<N, E> create(GraphScene<N, E> scene) {
        return new GraphSceneAdapter<>(scene);
    }

    public static <N, E, P> GraphAdapter<N, E> create(GraphPinScene<N, E, P> scene) {
        return new GraphPinSceneAdapter<>(scene);
    }

}
