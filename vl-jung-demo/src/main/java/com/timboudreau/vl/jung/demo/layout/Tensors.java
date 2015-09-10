/*
 * Copyright (c) 2015, tim
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
            Set<N> pused = new HashSet<>();
            for (Overlap<N> o : ol) {
                if (pused.contains(o.b)) {
                    continue;
                }
                pused.add(o.b);
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

    static class TensorArranger extends Arranger {

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
            for (Influences influence : influences) {
//                Set<N> unused = new HashSet<>(graph.getVertices());
                N n = (N) influence.node;
                double myX = layout.getX(n);
                double myY = layout.getY(n);
                for (Object key : influence.influencers.keySet()) {
                    double pushAmount = 0.985 * ((Double) influence.influencers.get(key));
                    double[] pos = push(layout, n, key, pushAmount, myX, myY, null); // unused);
                    myX = pos[0];
                    myY = pos[1];
//                    for (N vert : graph.getVertices()) {
//                        if (vert == n || vert == key) {
//                            continue;
//                        }
//                        double vx = layout.getX(vert);
//                        double vy = layout.getY(vert);
//                        Circle c = new Circle(new Point2D.Double(myX, myY));
//                        double dist = c.distanceToCenter(vx, vy);
//                        c.radius = dist;
//                        PR pr = new PR(n, vert);
//                        if (dist < 100 && !pushed.contains(pr)) {
//                            pushed.add(pr);
//                            c.radius *= 1.001;
//                            double angle = c.angleOf(vx, vy);
//                            pos = c.positionOf(angle);
//                            layout.setLocation(vert, pos[0] - 1, pos[1] + 1);
//                        }
//                    }
                }

//                Set<N> junk = new HashSet<>(unused);
//                for (Object key : unused) {
//                    double[] pos = push(layout, n, key, 1.000005, myX, myY, junk);
//                    myX = pos[0];
//                    myY = pos[1];
//                }
//                for (Object key : influence.neighbors.keySet()) {
//                    double pushAmount = 1.000001 * ((Double) influence.neighbors.get(key));
//                    double[] pos = push(layout, n, key, pushAmount, myX, myY, junk, 100D);
//                    myX = pos[0];
//                    myY = pos[1];
//                }
            }
//            for (N n : graph.getVertices()) {
//                double myX = layout.getX(n);
//                double myY = layout.getY(n);
//                for (N key : graph.getVertices()) {
//                    if (n == key) {
//                        continue;
//                    }
//                    double[] pos = push(layout, n, key, 1.001, myX, myY, null, 300D);
//                    myX = pos[0];
//                    myY = pos[1];
//                }
//            }
            return iterCount++ > maxIters;
        }

        private <N, E> double[] push(AbstractLayout layout, N n, Object key, double amount, double myX, double myY, Set<N> unused) {
            return push(layout, n, key, amount, myX, myY, unused, null);
        }

        static class PR {

            private final int acode;
            private final int bcode;

            PR(Object a, Object b) {
                acode = System.identityHashCode(a);
                bcode = System.identityHashCode(b);
            }

            public int hashCode() {
                return acode + 17 * bcode;
            }

            public boolean equals(Object o) {
                if (o instanceof PR) {
                    PR x = (PR) o;
                    return (acode == x.acode && bcode == x.bcode)
                            || (acode == x.bcode && bcode == x.acode);
                }
                return false;
            }
        }

        private final Circle circle = new Circle(0,0);
        private <N, E> double[] push(AbstractLayout layout, N n, Object key, double amount, double myX, double myY, Set<N> unused, Double limit) {
            if (limit == null) {
                limit = Double.MAX_VALUE;
            }
            double oX = layout.getX(key);
            double oY = layout.getY(key);
            circle.centerX = oX;
            circle.centerY = oY;
            double len = Math.abs(circle.distanceToCenter(myX, myY) * amount);
            double angle = circle.angleOf(myX, myY) - 1;
            circle.radius = len * amount;
            double[] pos = circle.positionOf(angle);
            if (Math.abs(pos[0] - oX) < 40) {
                pos[0] = myX;
            }
            if (Math.abs(pos[1] - oY) < 40) {
                pos[1] = myY;
            }
            if (unused != null) {
                unused.remove(key);
            }
//            Circle nue = new Circle(new Point2D.Double(myX, myY));
            circle.centerX = myX;
            circle.centerY = myY;
            double dist = circle.distanceToCenter(pos[0], pos[1]);
            if (limit != Double.MAX_VALUE && Math.abs(dist) < limit) {
                pos[0] = myX;
                pos[1] = myY;
            }
            layout.setLocation(n, pos[0], pos[1]);
            return pos;
        }

        private static final class Influences<N> {

            private final N node;
            private final Map<N, Double> influencers = new HashMap<>();
            private final Map<N, Double> neighbors = new HashMap<>();

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
                    influencers.put(n, baseAmount);
                    addInfluencers(n, inf, baseAmount / 2D);
                }
                for (N n : inf.getNeighbors(node)) {
                    if (n == this.node) {
                        continue;
                    }
                    neighbors.put(n, baseAmount);
                }
            }

        }

    }
}
