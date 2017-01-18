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

import com.timboudreau.vl.jung.demo.layout.Tensors.GetOutOfEachOthersWay;
import com.timboudreau.vl.jung.demo.layout.Tensors.TensorArranger;
import com.timboudreau.vl.jung.demo.layout.YLayout.Arranger;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public class Arrangers {

    public static AbstractLayout newLayout(Graph<?, ?> graph) {
        ConcentricCircles circs = new ConcentricCircles();
//        return new YLayout(graph, circs, new CircleSwapper(circs));
//        return new YLayout(graph, circs, new TensorArranger(), new GetOutOfEachOthersWay());
//        return new YLayout(graph, circs, new TensorArranger(1200), new GetOutOfEachOthersWay());
        return new YLayout(graph, circs, new LineFinder(), new TensorArranger());
//        return new YLayout(graph, new GroupCircles(), new Nothing());
    }

    static class Vectors extends Arranger {
        private final List<Vector<?>> vectors = new ArrayList<>();

        @Override
        protected <N, E> void onStart(Graph<N, E> graph, Dimension size) {
            for (N n : graph.getVertices()) {
                vectors.add(new Vector(n));
            }
        }

        @Override
        protected void reset() {
            vectors.clear();
        }


        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            if (vectors.isEmpty()) {
                this.onStart(graph, layout.getSize());
            }
            System.out.println("With " + vectors.size());
            for (Vector v : vectors) {
                N node = (N) v.node;
                double myX = layout.getX(node);
                double myY = layout.getY(node);
                double[] d = v.computeForce(graph, layout);
                myX += d[0];
                myY += d[1];
                layout.setLocation(node, myX, myY);
            }
            return false;
        }

        private static class Vector<N> {
            private final N node;

            public Vector(N node) {
                this.node = node;
            }

            <E> double[] computeForce(Graph<N,E> graph, AbstractLayout layout) {
                double[] result = new double[2];
                double myX = layout.getX(node);
                double myY = layout.getY(node);
                for (N n : graph.getPredecessors(node)) {
                    if (n == node) {
                        continue;
                    }
                    double x = layout.getX(n);
                    double y = layout.getY(n);
                    double offX = myX - x;
                    double offY = myY - y;
                    result[0] += offX * 0.1D;
                    result[0] += offY * 0.1D;
                }
                System.out.println(node + ": " + result[0] + "," + result[1]);
                return result;
            }

        }

    }

    private static class LineFinder extends Arranger {

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            List<LN<N>> lines = new LinkedList<>();
            Collection<N> vertices = graph.getVertices();
            Set<N> orphans = new HashSet<>(graph.getVertices());
            for (N n : vertices) {
                if (graph.getNeighborCount(n) == 1) {
                    N singleNeighbor = graph.getNeighbors(n).iterator().next();
                    int ct = graph.getNeighborCount(singleNeighbor);
                    if (ct > 3) {
                        continue;
                    }
                    boolean found = false;
                    for (LN<N> line : lines) {
                        if (line.contains(singleNeighbor)) {
                            line.add(n);
                            orphans.remove(n);
                            orphans.remove(singleNeighbor);
                            line.add(singleNeighbor);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LN<N> line = new LN<N>();
                        line.add(n);
                        line.add(singleNeighbor);
                        orphans.remove(n);
                        orphans.remove(singleNeighbor);
                        lines.add(line);
                    }
                }
            }
            for (Iterator<LN<N>> it=lines.iterator(); it.hasNext();) {
                LN<N> ln = it.next();
                if (ln.size() == 2) {
                    orphans.addAll(ln.nodes);
                    it.remove();
                }
            }
            System.out.println("LINES: ");
            for (LN<N> line : lines) {
                System.out.println("LINE: " + line);
            }
            System.out.println("ORPHANS: " + orphans);

            int y = 0;
            int x;
            for (LN<N> line : lines) {
                x = 20;
                for (N n : line.sort(graph)) {
                    layout.setLocation(n, x, y);
                    x+= 60;
                }
                y+=60;
            }

            return true;
        }

        static class LN<N> implements Iterable<N> {

            private final Set<N> nodes = new HashSet<>();

            void add(N node) {
                nodes.add(node);
            }

            int size() {
                return nodes.size();
            }

            boolean contains(N node) {
                return nodes.contains(node);
            }

            @Override
            public Iterator<N> iterator() {
                return nodes.iterator();
            }

            public String toString() {
                return "Line " + nodes;
            }

            List<N> sort(final Graph<N, ?> graph) {
                List<N> result = new ArrayList<>(nodes);
                class C implements Comparator<N> {

                    @Override
                    public int compare(N o1, N o2) {
                        if (o1 == o2) {
                            return 0;
                        }
                        if (graph.getNeighbors(o1).contains(o2)) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }

                }
                Collections.sort(result, new C());
                return result;
            }
        }
    }

    private static class GroupCircles extends Arranger {

        private final Map<Integer, CircleEntry> entryForCircle = new HashMap<>();

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {

//            PageRank pr = new PageRank(graph, 0.1);
//            Map<N, Integer> rankForNode = new HashMap<>();
//
//            for (N node : graph.getVertices()) {
//                rankForNode.put(node, pr.getVertexScore(pr));
//            }
            for (N node : graph.getVertices()) {
                Entry e = new Arrangers.Entry(graph.getNeighborCount(node), node);
                CircleEntry circ = entryForCircle.get(e.neighborCount);
                if (circ == null) {
                    Circle circle = new Circle(new Point2D.Double());
                    entryForCircle.put(e.neighborCount, circ = new CircleEntry(circle));
                }
                circ.add(e);
            }
            List<Rectangle> grid = new ArrayList<>(entryForCircle.size());
            System.out.println(entryForCircle.size() + " circles");
            int cols = 3;
            int rows = Math.max(1, entryForCircle.size() / 3);
            int rowHeight = layout.getSize().height / rows;
            int colWidth = layout.getSize().width / 3;
            int top = 0;
            for (int i = 0; i < entryForCircle.size(); i++) {
                int left = 0;
                for (int j = 0; j < 3; j++) {
                    Rectangle r = new Rectangle(left, top, colWidth, rowHeight);
                    grid.add(r);
                    left += colWidth;
                }
            }
            List<Integer> keys = new ArrayList<>(entryForCircle.keySet());
            Collections.sort(keys);
//            Collections.reverse(keys);
//            reverseFirstHalf(keys);
            Map<CircleEntry, Rectangle> rectForEntry = new HashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                int key = keys.get(i);
                Rectangle r;
                rectForEntry.put(entryForCircle.get(key), r = grid.get(i));
                System.out.println("RECT " + i + " for " + key + " " + r);
                entryForCircle.get(key).circle.setCenter(r.getCenterX(), r.getCenterY());
                entryForCircle.get(key).circle.setRadius(Math.min(r.width, r.height));
            }
            for (Map.Entry<CircleEntry, Rectangle> e : rectForEntry.entrySet()) {
                CircleEntry ce = e.getKey();
                Rectangle r = e.getValue();
                Iterator<Entry> entr = ce.iterator();
                for (double[] coords : ce.circle.iterable(ce.entries.size())) {
                    if (!entr.hasNext()) {
                        break;
                    }
                    layout.setLocation(entr.next().node, coords[0], coords[1]);
                }
            }
            return true;
        }

        private <T> void reverseFirstHalf(List<T> list) {
            List<T> reversed = list.subList(0, list.size() / 2);
            Collections.reverse(reversed);
            for (int i = 0; i < reversed.size(); i++) {
                list.set(i, reversed.get(i));
            }
        }
    }

    private static class ConcentricCircles extends Arranger {

        private final Map<Integer, CircleEntry> entryForCircle = new HashMap<>();

        @Override
        protected <N, E> void onStart(Graph<N, E> graph, Dimension size) {
            int centerX = size.width / 2;
            int centerY = size.height / 2;

            System.out.println("SIZE " + size + " center: " + centerX + "," + centerY);
            Point2D.Double center = new Point2D.Double(centerX, centerY);

            for (N node : graph.getVertices()) {
                Entry e = new Arrangers.Entry(graph.getNeighborCount(node), node);
                CircleEntry circ = entryForCircle.get(e.neighborCount);
                if (circ == null) {
                    Circle circle = new Circle(center);
//                    circle.setUsablePercentage(0.5);
                    entryForCircle.put(e.neighborCount, circ = new CircleEntry(circle));
                }
                circ.add(e);
            }
            List<Integer> keys = new ArrayList<>(entryForCircle.keySet());
            Collections.sort(keys);
            Collections.reverse(keys);
            double radius = Math.max(size.width / 2, size.height / 2);
//            double radius = 2000;

            double radiusStep = radius / keys.size();

            for (int i : keys) {
                CircleEntry ce = entryForCircle.get(i);
                swapForAdjacency(ce, graph);
                ce.circle.setRadius(radius);
                radius -= radiusStep;
            }            
        }

        private <N, E> void swapForAdjacency(CircleEntry entry, Graph<N, E> graph) {
            List<Entry> nue = new ArrayList<>(entry.entries);
            for (int i = 0; i < entry.entries.size(); i++) {
                Entry e = entry.entries.get(i);
                for (int j = 0; j < entry.entries.size(); j++) {
                    if (j == i || Math.abs(j - 1) == 1) {
                        continue;
                    }
                    Entry e1 = entry.entries.get(j);
                    if (isAdjacent(e, e1, graph)) {
                        nue.set(j, e);
                        nue.set(i, e1);
                    }
                }
            }
            entry.entries.clear();
            entry.entries.addAll(nue);
        }

        private <N, E> boolean isAdjacent(Entry a, Entry b, Graph graph) {
            return graph.isNeighbor(a.node, b.node);
        }

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            System.out.println("ConcentricCircles.step() - " + entryForCircle.size() + " rings");
            for (Map.Entry<Integer, CircleEntry> e : entryForCircle.entrySet()) {
                Iterable<double[]> iter = e.getValue().circle.iterable(e.getValue().entries.size());
                Iterator<Entry> entries = e.getValue().iterator();
                for (double[] coords : iter) {
                    if (!entries.hasNext()) {
                        break;
                    }
                    Entry entry = entries.next();
                    layout.setLocation(entry.node, coords[0], coords[1]);
                }
            }
            return true;
        }
    }

    private static <N, E> int countIntersections(Graph<N, E> graph, AbstractLayout layout) {
        int count = 0;
        for (E e : graph.getEdges()) {
            count += countIntersections(e, graph, layout);
        }
        return count;
    }

    private static <N, E> int countIntersections(E e, Graph<N, E> graph, AbstractLayout layout) {
        int count = 0;
        Line2D.Double a = lineFor(graph.getEndpoints(e), layout);
        for (E e1 : graph.getEdges()) {
            if (e == e1) {
                continue;
            }
            Line2D.Double b = lineFor(graph.getEndpoints(e1), layout);
            if (a.intersectsLine(b)) {
                count++;
            }
        }
        return count;
    }

    private static <N> Line2D.Double lineFor(Pair<N> pair, AbstractLayout layout) {
        double ax = layout.getX(pair.getFirst());
        double ay = layout.getY(pair.getFirst());
        double bx = layout.getX(pair.getSecond());
        double by = layout.getY(pair.getSecond());
        return new Line2D.Double(ax, ay, bx, by);
    }

    static class Nothing extends Arranger {

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            return true;
        }

    }

    private static class CircleSwapper extends Arranger {

        private final Map<Integer, CircleEntry> entryForCircle;

        private CircleSwapper(ConcentricCircles circs) {
            entryForCircle = circs.entryForCircle;
        }

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            System.out.println("Swapper.step()");
            List<EdgeEntry<E>> counts = new ArrayList<>();
            for (E edge : graph.getEdges()) {
                int count = countIntersections(edge, graph, layout);
                counts.add(new EdgeEntry<E>(edge, count));
            }
            Collections.sort(counts);
            int cumulativeCount = 0;
            for (EdgeEntry<E> en : counts) {
                if (en.intersectionCount == 0) {
                    continue;
                }
                Pair<N> pair = graph.getEndpoints(en.edge);
                N toMove;
                if (graph.getNeighborCount(pair.getFirst()) > graph.getNeighborCount(pair.getSecond())) {
                    toMove = pair.getSecond();
                } else {
                    toMove = pair.getFirst();
                }
                if (graph.getNeighborCount(toMove) > 2) {
                    continue;
                }
                double[] direction = getBestDirection(toMove, graph, layout);
                System.out.println("DIRS: " + direction[0] + "," + direction[1]);
                if (direction[0] != 0D || direction[1] != 0D) {
                    double x = layout.getX(toMove) + direction[0];
                    double y = layout.getY(toMove) + direction[1];
                    layout.setLocation(toMove, x, y);
                    System.out.println("NEW " + toMove + ": " + x + "," + y);
                    int newCount = countIntersections(en.edge, graph, layout);
                    if (newCount > en.intersectionCount) {
                        // If it makes things worse, undo
                        layout.setLocation(toMove, x - direction[0], y - direction[0]);
                    } else {
                        cumulativeCount++;
                        en.intersectionCount = newCount;
                    }
                }
            }
            return cumulativeCount == 0;
        }

        private static final double MIN_DIST = 20;
        private static final double MOVE_DIST = 2;

        static Random r = new Random(2823939);

        private <N, E> double[] getBestDirection(N toMove, Graph<N, E> graph, AbstractLayout layout) {
            double[] result = new double[2];
            for (N other : graph.getNeighbors(toMove)) {
                Pair<N> p = new Pair<N>(toMove, other);
                Line2D.Double line = lineFor(p, layout);
                Point2D p1 = line.getP1();
                Point2D p2 = line.getP2();
                double dist = p1.distance(p2);
                if (dist > MIN_DIST) {
                    if (p1.getX() < p2.getX()) {
                        result[0] += MOVE_DIST;
                    } else if (p1.getX() < p2.getX()) {
                        result[0] += -MOVE_DIST;
                    }
                    if (p1.getY() < p2.getY()) {
                        result[1] += MOVE_DIST;
                    } else if (p1.getY() < p2.getY()) {
                        result[1] += -MOVE_DIST;
                    }
                }
            }
            return result;
        }

        private static class EdgeEntry<E> implements Comparable<EdgeEntry<?>> {

            private final E edge;
            private int intersectionCount;

            public EdgeEntry(E edge, int intersectionCount) {
                this.edge = edge;
                this.intersectionCount = intersectionCount;
            }

            public String toString() {
                return edge + ": " + intersectionCount;
            }

            @Override
            public int compareTo(EdgeEntry<?> o) {
                return o.intersectionCount == intersectionCount ? 0 : o.intersectionCount > intersectionCount ? 1 : -1;
            }

        }

    }

    private static class CircleEntry implements Iterable<Entry> {

        public final Circle circle;
        private final List<Entry> entries = new LinkedList<>();

        public CircleEntry(Circle circle) {
            this.circle = circle;
        }

        void add(Entry entry) {
            entries.add(entry);
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }
    }

    static class Entry implements Comparable<Entry> {

        public final int neighborCount;
        public final Object node;

        public Entry(int neighbors, Object node) {
            this.neighborCount = neighbors;
            this.node = node;
        }

        @Override
        public int compareTo(Entry o) {
            return neighborCount > o.neighborCount ? 1 : neighborCount == o.neighborCount ? 0 : -1;
        }
    }
}
