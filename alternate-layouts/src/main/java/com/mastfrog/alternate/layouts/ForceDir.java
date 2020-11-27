/* 
 * Copyright (c) 2020, Tim Boudreau
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
package com.mastfrog.alternate.layouts;

import com.google.common.base.Function;
import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.abstractions.list.IndexedResolvable;
import com.mastfrog.graph.IntGraph;
import com.mastfrog.graph.IntGraphBuilder;
import com.mastfrog.graph.ObjectGraph;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An experimental force-directed graph which works well for some
 * data sets, vaguely similar in effect to how D3 lays things out.
 * For performance, uses a Mastfrog IntGraph under the hood - if the
 * input graph was created from one using the adapter project, it
 * will be detected with zero-overhead; else the graph may be copied
 * to use it.
 *
 * @author Tim Boudreau
 */
public class ForceDir<V, E> implements Layout<V, E>, IterativeContext {

    private final Graph<V, E> graph;
    private final Set<V> locked = new HashSet<>();
    private final ObjectGraph<V> delegate;
    private ForceDirected fd;
    private IndexedResolvable<? extends V> ir;
    private IntGraph ig;

    public ForceDir(Graph<V, E> graph, ObjectGraph<V> delegate) {
        this.graph = graph;
        this.delegate = delegate;
        delegate.toIntGraph((ir, ig) -> {
            this.ir = ir;
            this.ig = ig;
        });
        fd = new ForceDirected(ig, 40000, 40000);
    }

    public ForceDir(Graph<V, E> graph) {
        this(graph, fromGraph(graph));
    }

    private static <V, E> ObjectGraph<V> fromGraph(Graph<V, E> graph) {
        ObjectGraph<V> og = Wrapper.find(graph, ObjectGraph.class);
        if (og != null) {
            return og;
        }
        List<V> all = new ArrayList<>(graph.getVertices());
        IntGraphBuilder ib = IntGraph.builder();
        for (int i = 0; i < all.size(); i++) {
            V v = all.get(i);
            for (E edge : graph.getOutEdges(v)) {
                V v1 = graph.getDest(edge);
                if (v != v1) {
                    ib.addEdge(i, all.indexOf(v1));
                }
            }
        }
        return ib.build().toObjectGraph(all);
    }

    @Override
    public void initialize() {
        fd = new ForceDirected(ig, 40000, 40000);
    }

    @Override
    public void setInitializer(Function<V, Point2D> initializer) {
    }

    @Override
    public void setGraph(Graph<V, E> graph) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public Graph<V, E> getGraph() {
        return graph;
    }

    @Override
    public void reset() {
        initialize();
    }

    @Override
    public void setSize(Dimension d) {
    }

    @Override
    public Dimension getSize() {
        return fd.currentSize();
    }

    @Override
    public void lock(V v, boolean state) {
    }

    @Override
    public boolean isLocked(V v) {
        return false;
    }

    @Override
    public void setLocation(V v, Point2D location) {
        int ix = ir.indexOf(v);
        if (ix >= 0) {
            fd.setLocation(ix, location.getX(), location.getY());
        }
    }

    @Override
    public Point2D apply(V f) {
        int ix = ir.indexOf(f);
        return new Point2D.Double(fd.xNorm(ix), fd.yNorm(ix));
    }

    @Override
    public void step() {
        fd.iterate();
    }

    @Override
    public boolean done() {
        return fd.maxPerturb() <= 12;
    }

}
