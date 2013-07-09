/*
 * Copyright (c) 2013, Tim Boudreau
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

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections15.Transformer;

/**
 *
 * @author Tim Boudreau
 */
public class XLayout<N, E> implements Layout<N, E> {

    private Graph<N, E> graph;
    private int xDist = 100;
    private int yDist = 60;
    private final int padX;
    private final int padY;

    public XLayout(Graph<N, E> graph, Dimension cellSize, int padX, int padY) {
        this.graph = graph;
        this.xDist = cellSize.width;
        this.yDist = cellSize.height;
        this.padX = padX;
        this.padY = padY;
        initialize();
    }

    public XLayout(Graph<N, E> graph) {
        this(graph, new Dimension(60, 60), 3, 3);
    }

    @Override
    public void initialize() {
        Collection<N> nodes = graph.getVertices();
        if (nodes.isEmpty()) {
            return;
        }
        List<NodeInfo<N>> infos = new LinkedList<>();
        for (N n : nodes) {
            int count = graph.getSuccessorCount(n);
            NodeInfo<N> info = new NodeInfo<>(n, count);
            infos.add(info);
        }
        int cells = nodes.size() * nodes.size();
        Collections.sort(infos);
        
        Grid<N> grid = new Grid<>(cells);
        int rowWidth = Math.max(2, (int) Math.sqrt(nodes.size()));
        Iterator<NodeInfo<N>> ns = infos.iterator();
        for (int row = 0; ns.hasNext(); row++) {
            for (int col = 0; col < rowWidth && ns.hasNext(); col++) {
                N n = ns.next().node;
                grid.add(row, n);
            }
        }
        for (int i = 0; i < nodes.size() * 2; i++) {
            int changeCount = grid.refine(graph);
            if (changeCount == 0) {
                break;
            }
        }
        points = grid.getLocations(new Dimension(xDist, yDist), padX, padY);
    }
    private Map<N, Point2D> points = new HashMap<>();

    @Override
    public void setInitializer(Transformer<N, Point2D> t) {
    }

    @Override
    public void setGraph(Graph<N, E> graph) {
        this.graph = graph;
    }

    @Override
    public Graph<N, E> getGraph() {
        return graph;
    }

    @Override
    public void reset() {
        initialize();
    }

    private Dimension size = new Dimension(500, 500);

    @Override
    public void setSize(Dimension dmnsn) {
        this.size.setSize(dmnsn);
    }

    @Override
    public Dimension getSize() {
        return new Dimension(this.size);
    }

    @Override
    public void lock(N v, boolean bln) {
    }

    @Override
    public boolean isLocked(N v) {
        return false;
    }

    @Override
    public void setLocation(N v, Point2D pd) {
        points.put(v, pd);
    }

    @Override
    public Point2D transform(N n) {
        Point2D p = points.get(n);
        if (p == null) {
            p = new Point(0, 0);
        }
        return p;
    }

}
