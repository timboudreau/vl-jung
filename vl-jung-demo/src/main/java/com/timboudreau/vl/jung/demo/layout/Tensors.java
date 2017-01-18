/*
 * Copyright (c) 2015, Tim Boudreau
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
package com.timboudreau.vl.jung.demo.layout;

import com.timboudreau.vl.jung.demo.layout.YLayout.Arranger;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public class Tensors {

    static class GetOutOfEachOthersWay extends Arranger {

        private static final int THRESHOLD = 80;

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            Set<Overlap<N>> ol = new HashSet<>();
            for (N n : graph.getVertices()) {
                double myX = layout.getX(n);
                double myY = layout.getY(n);
                for (N key : graph.getVertices()) {
                    if (key == n) {
                        continue;
                    }
                    double oX = layout.getX(key);
                    double oY = layout.getY(key);
                    if (oX == myX && oY == myY) {
                        oX -= 1D;
                        oY -= 1D;
                    }
                    Circle circ = new Circle(new Point2D.Double(myX, myY));
                    double dist = circ.distanceToCenter(oX, oY);
                    if (dist < THRESHOLD) {
                        circ.radius = dist * 1.01D;
                        ol.add(new Overlap(n, key));
                    }
                }
            }
            Set<N> pushed = new HashSet<>();
            for (Overlap<N> o : ol) {
                if (pushed.contains(o.b)) {
                    continue;
                }
                pushed.add(o.b);
                double myX = layout.getX(o.a);
                double myY = layout.getY(o.a);
                double oX = layout.getX(o.b);
                double oY = layout.getY(o.b);
                Circle circ = new Circle(new Point2D.Double(myX, myY));
                double dist = circ.distanceToCenter(oX, oY);
                double angle = circ.angleOf(oX, oY) + 1;
                circ.radius = dist * 1.1D;
                double[] pos = circ.positionOf(angle);
                layout.setLocation(o.b, pos[0], pos[1]);
            }
            return ol.size() == 0;
        }
    }

    static class Overlap<T> {

        final T a;
        final T b;

        public Overlap(T a, T b) {
            this.a = a;
            this.b = b;
        }

        public boolean equals(Object o) {
            if (o instanceof Overlap) {
                Overlap oo = (Overlap) o;
                return (oo.a.equals(a) && oo.b.equals(b))
                        || (oo.b.equals(a) && oo.a.equals(b));
            }
            return false;
        }

        public String toString() {
            return a + ":" + b;
        }

    }

    static class TensorArranger extends YLayout.Arranger {

        private final List<Influences> influences = new LinkedList<>();
        int iterCount;
        private final int maxIters;

        TensorArranger() {
            this(Integer.MAX_VALUE);
        }

        TensorArranger(int maxIters) {
            this.maxIters = maxIters;
        }

        @Override
        protected <N, E> void onStart(Graph<N, E> graph, Dimension size) {
            for (N node : graph.getVertices()) {
                influences.add(new Influences(node, graph));
            }
        }

        @Override
        protected void reset() {
            influences.clear();
            iterCount = 0;
        }

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            if (influences.isEmpty()) {
                onStart(graph, layout.getSize());
            }
//            Set<PR> pushed = new HashSet<>();
            double[] tmp = new double[2];
            Set<N> used = new HashSet<>();
            Collection<N> verts = graph.getVertices();
            for (Influences influence : influences) {
                Set<N> unused = new HashSet<>(verts);
                N n = (N) influence.node;
                unused.remove(n);
                used.add(n);
                tmp[0] = layout.getX(n);
                tmp[1] = layout.getY(n);
                Influences<N> i = influence;

//                for (Object key : influence.influencers.keySet()) {
                for (Influences.Influence inf : i.influencers) {
                    N node = (N) inf.node;
                    if (node == n) {
                        continue;
                    }
                    unused.remove(node);
                    used.add(node);
                    double pushAmount = 0.985 * inf.influence;//((Double) influence.influencers.get(key));
                    double oX = layout.getX(node);
                    double oY = layout.getY(node);
                    Force force = new Force(oX, oY, Force.NO_DROPOFF.multiply(pushAmount));
                    force.apply(tmp);
                }
//                for (Object key : influence.neighbors.keySet()) {
                for (Influences.Influence inf : i.neighbors) {
                    N node = (N) inf.node;
                    if (node == n) {
                        continue;
                    }
                    used.add(node);
                    double pushAmount = 0.75 * inf.influence; //((Double) influence.neighbors.get(node));
                    double oX = layout.getX(node);
                    double oY = layout.getY(node);
                    Force force = new Force(oX, oY, Force.NO_DROPOFF.multiply(pushAmount).negate().bound(250));
                    force.apply(tmp);
                    unused.remove(node);
                }
                for (N key : unused) {
                    double pushAmount = -0.25;
                    double oX = layout.getX(key);
                    double oY = layout.getY(key);
                    Force force = new Force(oX, oY, Force.NO_DROPOFF.multiply(pushAmount).bound(150));
                    force.apply(tmp);
                }
                layout.setLocation(n, tmp[0], tmp[1]);
            }
            return iterCount++ > maxIters;
        }

        private static final class Influences<N> {

            private final N node;
//            private final Map<N, Double> influencers = new HashMap<>();
//            private final Map<N, Double> neighbors = new HashMap<>();
            private final Set<Influence<N>> influencers = new HashSet<>();
            private final Set<Influence<N>> neighbors = new HashSet<>();

            public Influences(N node, Graph<N, ?> graph) {
                this.node = node;
                addInfluencers(node, graph, 1D);
            }

            private void addInfluencers(N node, Graph<N, ?> inf, double baseAmount) {
                if (baseAmount <= 0.000001D) {
                    return;
                }
                for (N n : inf.getPredecessors(node)) {
                    if (n == this.node) {
                        continue;
                    }
//                    influencers.put(n, baseAmount);
                    influencers.add(new Influence<N>(baseAmount, n));
                    addInfluencers(n, inf, baseAmount / 2D);
                }
                for (N n : inf.getNeighbors(node)) {
                    if (n == this.node) {
                        continue;
                    }
//                    neighbors.put(n, baseAmount);
                    neighbors.add(new Influence<N>(baseAmount, n));
                }
            }
            
            static final class Influence<N> {
                final double influence;
                final N node;

                public Influence(double influence, N node) {
                    this.influence = influence;
                    this.node = node;
                }
                
                public boolean equals(Object o) {
                    return o instanceof Influence && ((Influence) o).node.equals(node);
                }
                
                public int hashCode() {
                    return node.hashCode() * 37;
                }
            }
        }
    }
}
