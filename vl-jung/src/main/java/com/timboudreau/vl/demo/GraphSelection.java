package com.timboudreau.vl.demo;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Object for performing graph-related queries about selection
 *
 * @author Tim Boudreau
 */
public final class GraphSelection<N, E> {

    private final JungScene<N, E> scene;

    public GraphSelection(JungScene<N, E> scene) {
        this.scene = scene;
    }

    public boolean isSelected(N node) {
        return scene.getSelectedObjects().contains(node);
    }

    public boolean isIndirectlyConnectedToSelection(N node) {
        Set<?> selected = scene.getSelectedObjects();
        for (E e : scene.graph.getInEdges(node)) {
            N opposite = scene.graph.getOpposite(node, e);
            for (E e1 : scene.graph.getInEdges(opposite)) {
                if (e1 != e) {
                    N opposite2 = scene.graph.getOpposite(opposite, e1);
                    if (selected.contains(opposite2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isConnectedToSelection(N node) {
        Set<N> nodes = new HashSet<>(scene.graph.getVertices());
        nodes.retainAll(scene.getSelectedObjects());
        if (nodes.contains(node)) {
            return false;
        }
        for (N n : nodes) {
            for (E edge : scene.graph.getOutEdges(n)) {
                if (scene.graph.getEndpoints(edge).contains(node)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<N> getSelection() {
        Set<N> nodes = new HashSet<>(scene.graph.getVertices());
        nodes.retainAll(scene.getSelectedObjects());
        return nodes;
    }
    
    public enum EdgeTypes {
        IN,
        OUT
    }
    
    public Set<N> getNodesConnectedToSelection(EdgeTypes... et) {
        Set<N> sel = getSelection();
        Set<E> edges = getEdgesTouchingSelection(et);
        Set<N> result = new HashSet<>();
        for (E e : edges) {
            Pair<N> p = scene.graph.getEndpoints(e);
            if (!sel.contains(p.getFirst())) {
                result.add(p.getFirst());
            }
            if (!sel.contains(p.getSecond())) {
                result.add(p.getSecond());
            }
        }
        return result;
    }
    
    public Set<E> getEdgesTouchingSelection(EdgeTypes... types) {
        List<EdgeTypes> tps = Arrays.asList(types);
        Set<E> result = new HashSet<>();
        Graph<N,E> gp = scene.graph;
        for (N n : getSelection()) {
            if (tps.contains(EdgeTypes.IN)) {
                result.addAll(gp.getInEdges(n));
            }
            if (tps.contains(EdgeTypes.OUT)) {
                result.addAll(gp.getOutEdges(n));
            }
        }
        return result;
    }

    public boolean isAttachedToSelection(E edge) {
        Set<N> nodes = getSelection();
        nodes.retainAll(scene.graph.getEndpoints(edge));
        return !nodes.isEmpty();
    }
}
