/*
 * Copyright (c) 2020, Mastfrog Technologies
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
package com.mastfrog.graph.jgrapht.adapter;

import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.abstractions.list.IndexedResolvable;
import com.mastfrog.graph.IntGraph;
import com.mastfrog.graph.ObjectGraph;
import com.mastfrog.graph.ObjectGraphVisitor;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Adapts com.mastfrog:graph's super-lightweight bit-set based graphs as Jung
 * graphs.
 *
 * @author Tim Boudreau
 */
public final class GraphAdapter {

    /**
     * Create a Jung graph for a Mastfrog graph.
     *
     * @param <V> The vertex type
     * @param <E> The edge type
     * @param graph The original graph
     * @param directed Whether or not to return a graph that represents itself
     * to Jung as being a directed graph (be very sure!)
     * @param edgeFactory The factory for edge objects
     * @return A graph
     */
    public static <V, E> Graph<V, ? extends E> wrap(ObjectGraph<V> graph,
            boolean directed, EdgeFactory<V, E> edgeFactory) {
        if (directed) {
            return new WrappedDirectedGraph<>(graph, edgeFactory);
        }
        return new WrappedObjectGraph<>(graph, edgeFactory);
    }

    /**
     * Create a Jung graph for a Mastfrog graph, using the built-in edge type,
     * which unwraps the underlying IntGraph and represents edges internally as
     * a pair of ints, for the most lightweight possible edge objects.
     *
     * @param <V> The vertex type
     * @param <E> The edge type
     * @param graph The original graph
     * @param directed Whether or not to return a graph that represents itself
     * to Jung as being a directed graph (be very sure!)
     * @return A graph
     */
    public static <V> Graph<V, ? extends Edge<V>> wrap(ObjectGraph<V> graph,
            boolean directed) {
        DefaultEdgeFactory<V> def = new DefaultEdgeFactory(graph, directed);
        if (directed) {
            return new WrappedDirectedGraph<>(graph, def);
        }
        return new WrappedObjectGraph<>(graph, def);
    }

    /**
     * Create a Jung graph for a Mastfrog graph, using the built-in edge type,
     * which unwraps the underlying IntGraph and represents edges internally as
     * a pair of ints, for the most lightweight possible edge objects.
     * <p>
     * This method will probe the graph to determine if it is directed or
     * undirected; if the graph is very large and has very few cycles, this may
     * be expensive - if you know for sure the graph can or cannot be directed,
     * you should use one of the other methods that lets you pass that
     * information.
     * </p>
     *
     * @param <V> The vertex type
     * @param <E> The edge type
     * @param graph The original graph
     * @return A graph
     */
    public static <V> Graph<V, ? extends Edge<V>> wrap(ObjectGraph<V> graph) {
        DefaultEdgeFactory<V> def = new DefaultEdgeFactory(graph, null);
        if (def.undirected) {
            return new WrappedObjectGraph<>(graph, def);
        }
        return new WrappedDirectedGraph<>(graph, def);
    }

    private GraphAdapter() {
        throw new AssertionError();
    }

    /**
     * A factory for edges which can provide information Jung graphs need.
     * Resulting edge objects must have the following characteristics:
     * <ul>
     * <li><code>equals()</code> must return true for identical edges (same
     * source, same destination)</li>
     * <li><code>hashCode()</code> must return the same value for identical
     * edges</li>
     * <li>If the graph is undirected, <code>equals()</code> must return true
     * for identical edges (same source, same destination)
     * <i>and for mirror-image edges</i> (source=dest, dest=source)</li>
     * <li>If the graph is undirected <code>hashCode()</code> must return the
     * same value for identical edges
     * <i>and for mirror-image edges (hint, before hashing, sort on
     * <code>hashCode()</code> or some similar means of achieving consistency)
     * </li>
     * </ul>
     *
     * @param <V> The vertex type
     * @param <E> The edge type
     */
    public interface EdgeFactory<V, E> extends BiFunction<V, V, E> {

        V sourceOf(E e);

        V destOf(E e);
    }

    private static final class DefaultEdgeFactory<V> implements
            EdgeFactory<V, DefaultEdgeFactory<V>.Edg> {

        private final ObjectGraph<V> graph;
        private final IntGraph ig;
        private final IndexedResolvable<? extends V> contents;
        private boolean undirected;

        DefaultEdgeFactory(ObjectGraph<V> graph, Boolean directed) {
            this.graph = graph;
            Object[] stuff = dissect(graph);
            contents = (IndexedResolvable<? extends V>) stuff[0];
            ig = (IntGraph) stuff[1];
            if (directed == null) {
                ig.connectors().forEachSetBitAscending(id -> {
                    if (ig.isRecursive(id)) {
                        undirected = true;
                        return false;
                    }
                    return true;
                });
            } else {
                undirected = !directed.booleanValue();
            }
        }

        private static <V> Object[] dissect(ObjectGraph<V> graph) {
            Object[] result = new Object[2];
            graph.toIntGraph((contents, intGraph) -> {
                result[0] = contents;
                result[1] = intGraph;
            });
            return result;
        }

        @Override
        public V sourceOf(Edg e) {
            return e.source();
        }

        @Override
        public V destOf(Edg e) {
            return e.dest();
        }

        @Override
        public Edg apply(V t, V u) {
            return new Edg(contents.indexOf(t), contents.indexOf(u));
        }

        class Edg extends Edge<V> {

            private final int src;
            private final int dest;

            public Edg(int src, int dest) {
                this.src = src;
                this.dest = dest;
            }

            @Override
            public V source() {
                return contents.forIndex(src);
            }

            @Override
            public V dest() {
                return contents.forIndex(dest);
            }

            @Override
            public boolean equalsDirected(Edge<?> other) {
                if (!(other.getClass() == Edg.class)) {
                    return false;
                }
                Edg o = (Edg) other;
                return o.src == src && o.dest == dest;
            }

            @Override
            public boolean equalsUndirected(Edge<?> other) {
                if (!(other.getClass() == Edg.class)) {
                    return false;
                }
                Edg o = (Edg) other;
                return (o.src == src && o.dest == dest)
                        || (o.src == dest && o.dest == src);
            }

            public String toString() {
                return source() + ":" + dest();
            }

            @Override
            public int hashCode() {
                if (undirected && dest < src) {
                    return (7 * dest) + (41131 * src);
                }
                return (7 * src) + (41131 * dest);
            }

            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                } else if (o == null || o.getClass() != Edg.class) {
                    return false;
                }
                Edg other = (Edg) o;
                if (undirected) {
                    return equalsUndirected(other);
                } else {
                    return equalsDirected(other);
                }
            }
        }
    }

    public static <V> Edge<V> create(V a, V b, boolean directed) {
        return new DefaultEdge<V>(a, b, directed);
    }

    public static abstract class Edge<V> {

        public abstract V source();

        public abstract V dest();

        public boolean equalsDirected(Edge<?> other) {
            return source().equals(other.source()) && dest().equals(other.dest());
        }

        public boolean equalsUndirected(Edge<?> other) {
            return equalsDirected(other)
                    || (source().equals(other.dest()) && dest().equals(other.source()));
        }
    }

    private static final class DefaultEdge<V> extends Edge<V> {

        private final V source;
        private final V dest;
        private final boolean directed;

        public DefaultEdge(V source, V dest, boolean directed) {
            this.source = source;
            this.dest = dest;
            this.directed = directed;
        }

        public V source() {
            return source;
        }

        public V dest() {
            return dest;
        }

        @Override
        public String toString() {
            if (directed) {
                return source + ":" + dest;
            } else {
                if (source.hashCode() > dest.hashCode()) {
                    return dest + ":" + source;
                } else {
                    return source + ":" + dest;
                }
            }
        }

        @Override
        public int hashCode() {
            if (directed || source.hashCode() < dest.hashCode()) {
                return source.hashCode() + (24133 * dest.hashCode());
            }
            return dest.hashCode() + (24133 * source.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || o.getClass() != DefaultEdge.class) {
                return false;
            }
            DefaultEdge other = (DefaultEdge) o;
            boolean dir = directed || other.directed;
            if (dir) {
                return equalsDirected(other);
            } else {
                return equalsUndirected(other);
            }
        }
    }

    static class WrappedObjectGraph<V, E> implements Graph<V, E>, Wrapper<ObjectGraph<?>> {

        private final ObjectGraph<V> graph;
        private final EdgeFactory<V, E> edgeFactory;

        public WrappedObjectGraph(ObjectGraph<V> graph, EdgeFactory<V, E> edgeFactory) {
            this.graph = graph;
            this.edgeFactory = edgeFactory;
        }

        @Override
        public ObjectGraph<?> wrapped() {
            return graph;
        }

        @Override
        public <F> F find(Class<? super F> what) {
            if (IntGraph.class == what) {
                IntGraph[] ig = new IntGraph[1];
                graph.toIntGraph((ir, g) -> ig[0] = g);
                return (F) what.cast(ig[0]);
            }
            return Wrapper.super.find(what);
        }

        @Override
        public <F> boolean has(Class<? super F> what) {
            if (IntGraph.class == what) {
                return true;
            }
            return Wrapper.super.has(what);
        }

        @Override
        public ObjectGraph<?> root() {
            return graph;
        }

        @Override
        public Collection<E> getInEdges(V vertex) {
            Set<E> result = new HashSet<>();
            graph.parents(vertex).forEach(o -> result.add(edgeFactory.apply(vertex, o)));
            return result;
        }

        @Override
        public Collection<E> getOutEdges(V vertex) {
            Set<E> result = new HashSet<>();
            graph.children(vertex).forEach(o -> result.add(edgeFactory.apply(vertex, o)));
            return result;
        }

        @Override
        public Collection<V> getPredecessors(V vertex) {
            return graph.reverseClosureOf(vertex);
        }

        @Override
        public Collection<V> getSuccessors(V vertex) {
            return graph.closureOf(vertex);
        }

        @Override
        public int inDegree(V vertex) {
            return graph.inboundReferenceCount(vertex);
        }

        @Override
        public int outDegree(V vertex) {
            return graph.outboundReferenceCount(vertex);
        }

        @Override
        public boolean isPredecessor(V v1, V v2) {
            return graph.reverseClosureOf(v1).contains(v2);
        }

        @Override
        public boolean isSuccessor(V v1, V v2) {
            return graph.closureOf(v1).contains(v2);
        }

        @Override
        public int getPredecessorCount(V vertex) {
            return getPredecessors(vertex).size();
        }

        @Override
        public int getSuccessorCount(V vertex) {
            return getSuccessors(vertex).size();
        }

        @Override
        public V getSource(E directed_edge) {
            return edgeFactory.sourceOf(directed_edge);
        }

        @Override
        public V getDest(E directed_edge) {
            return edgeFactory.destOf(directed_edge);
        }

        @Override
        public boolean isSource(V vertex, E edge) {
            return vertex.equals(edgeFactory.sourceOf(edge));
        }

        @Override
        public boolean isDest(V vertex, E edge) {
            return vertex.equals(edgeFactory.destOf(edge));
        }

        @Override
        public boolean addEdge(E e, V v1, V v2) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean addEdge(E e, V v1, V v2, EdgeType edgeType) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public Pair<V> getEndpoints(E edge) {
            return new Pair<>(edgeFactory.sourceOf(edge), edgeFactory.destOf(edge));
        }

        @Override
        public V getOpposite(V vertex, E edge) {
            if (vertex.equals(edgeFactory.sourceOf(edge))) {
                return edgeFactory.destOf(edge);
            } else if (vertex.equals(edgeFactory.destOf(edge))) {
                return edgeFactory.sourceOf(edge);
            } else {
                return null;
            }
        }

        private Set<E> edges;

        @Override
        public Collection<E> getEdges() {
            if (edges != null) {
                return edges;
            }
            Set<E> result = new HashSet<>();
            graph.walk(new ObjectGraphVisitor<V>() {
                LinkedList<V> stack = new LinkedList<>();

                @Override
                public void enterNode(V node, int depth) {
                    if (stack.size() > 0) {
                        V curr = stack.peek();
                        result.add(edgeFactory.apply(curr, node));
                    }
                    stack.push(node);
                }

                @Override
                public void exitNode(V node, int depth) {
                    stack.pop();
                }
            });
            return edges = Collections.unmodifiableSet(result);
        }

        private Collection<V> allVertices;

        @Override
        public Collection<V> getVertices() {
            if (allVertices != null) {
                return allVertices;
            }
            Set<V> items = new HashSet<>();
            for (int i = 0; i < graph.size(); i++) {
                items.add(graph.toNode(i));
            }
            return allVertices = Collections.unmodifiableSet(items);
        }

        @Override
        public boolean containsVertex(V vertex) {
            return getVertices().contains(vertex);
        }

        @Override
        public boolean containsEdge(E edge) {
            V src = edgeFactory.sourceOf(edge);
            V dest = edgeFactory.destOf(edge);
            return graph.children(src).contains(dest);
        }

        int edgeCount = -1;

        @Override
        public int getEdgeCount() {
            if (edgeCount >= 0) {
                return edgeCount;
            }
            return edgeCount = getEdges().size();
        }

        @Override
        public int getVertexCount() {
            return getVertices().size();
        }

        @Override
        public Collection<V> getNeighbors(V vertex) {
            Set<V> all = new HashSet<>(graph.parents(vertex));
            all.addAll(graph.children(vertex));
            return all;
        }

        @Override
        public Collection<E> getIncidentEdges(V vertex) {
            Set<E> result = new HashSet<>();
            graph.parents(vertex).forEach(node -> {
                result.add(edgeFactory.apply(node, vertex));
            });
            graph.children(vertex).forEach(node -> {
                result.add(edgeFactory.apply(vertex, node));
            });
            return result;
        }

        @Override
        public Collection<V> getIncidentVertices(E edge) {
            return Arrays.asList(edgeFactory.sourceOf(edge), edgeFactory.destOf(edge));
        }

        @Override
        public E findEdge(V v1, V v2) {
            if (graph.parents(v2).contains(v1)) {
                return edgeFactory.apply(v1, v2);
            } else if (graph.parents(v2).contains(v1)) {
                return edgeFactory.apply(v2, v1);
            }
            return null;
        }

        @Override
        public Collection<E> findEdgeSet(V v1, V v2) {
            Set<E> edges = new HashSet<>();
            if (graph.parents(v2).contains(v1)) {
                edges.add(edgeFactory.apply(v1, v2));
            }
            if (graph.parents(v2).contains(v1)) {
                edges.add(edgeFactory.apply(v2, v1));
            }
            return edges;
        }

        @Override
        public boolean addVertex(V vertex) {
            throw new UnsupportedOperationException("Immutable.");
        }

        @Override
        public boolean addEdge(E edge, Collection<? extends V> vertices) {
            throw new UnsupportedOperationException("immutable");
        }

        @Override
        public boolean addEdge(E edge, Collection<? extends V> vertices, EdgeType edge_type) {
            throw new UnsupportedOperationException("immutable");
        }

        @Override
        public boolean removeVertex(V vertex) {
            throw new UnsupportedOperationException("immutable");
        }

        @Override
        public boolean removeEdge(E edge) {
            throw new UnsupportedOperationException("immutable");
        }

        @Override
        public boolean isNeighbor(V v1, V v2) {
            return graph.parents(v1).contains(v2)
                    || graph.children(v1).contains(v2);
        }

        @Override
        public boolean isIncident(V vertex, E edge) {
            return edgeFactory.sourceOf(edge).equals(vertex)
                    || edgeFactory.destOf(edge).equals(vertex);
        }

        @Override
        public int degree(V vertex) {
            return inDegree(vertex) + outDegree(vertex);
        }

        @Override
        public int getNeighborCount(V vertex) {
            return degree(vertex);
        }

        @Override
        public int getIncidentCount(E edge) {
            if (edgeFactory.sourceOf(edge).equals(edgeFactory.destOf(edge))) {
                return 1;
            }
            return 2;
        }

        @Override
        public EdgeType getEdgeType(E edge) {
            return EdgeType.UNDIRECTED;
        }

        @Override
        public EdgeType getDefaultEdgeType() {
            return EdgeType.UNDIRECTED;
        }

        @Override
        public Collection<E> getEdges(EdgeType edge_type) {
            return getEdges();
        }

        @Override
        public int getEdgeCount(EdgeType edge_type) {
            return getEdges().size();
        }

    }

    static class WrappedDirectedGraph<V, E> extends WrappedObjectGraph<V, E> implements DirectedGraph<V, E> {

        public WrappedDirectedGraph(ObjectGraph<V> graph, EdgeFactory<V, E> edgeFactory) {
            super(graph, edgeFactory);
        }

        @Override
        public EdgeType getEdgeType(E edge) {
            return EdgeType.DIRECTED;
        }

        @Override
        public EdgeType getDefaultEdgeType() {
            return EdgeType.DIRECTED;
        }
    }
}
