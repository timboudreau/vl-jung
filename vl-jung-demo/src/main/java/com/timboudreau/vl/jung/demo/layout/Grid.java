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

import edu.uci.ics.jung.graph.Graph;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class Grid<N> {

    private final List<Row<N>> rows = new LinkedList<>();

    Grid(int cells) {
        for (int i = 0; i < cells; i++) {
            rows.add(new Row());
        }
    }

    public Map<N, Point2D> getLocations(Dimension distance, int padX, int padY) {
        Map<N, Point2D> result = new HashMap<>();
        int rowCount = rows.size();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Row<N> row = rows.get(rowIndex);
            if (row.isEmpty()) {
                break;
            }
            int colCount = row.size();
            for (int column = 0; column < colCount; column++) {
                N node = row.get(column);
                int x = (distance.width * column) + (column * padX);
                int y = (distance.height * rowIndex) + (rowIndex * padY);
                if (rowIndex % 2 == 0) {
                    x += distance.width / 2;
                }
                result.put(node, new Point(x, y));
            }
        }
        return result;
    }

    public void add(int row, N obj) {
        rows.get(row).add(obj);
    }

    public int refine(Graph<N, ?> graph) {
        return refineInternal(graph);
    }

    private <E> int wideSwap(Graph<N, E> graph, List<? extends Iteration<N>> rows) {
        int result = 0;
        Random r = new Random(System.currentTimeMillis());
        int rowCount = rows.size();
        for (int i = 0; i < rowCount; i++) {
            Iteration<N> row = rows.get(i);
            if (row.isEmpty()) {
                break;
            }
            int ct = row.size();
            for (int j = 0; j < ct; j++) {
                for (int otherIndex = ct - 1; otherIndex >= 0; otherIndex--) {
                    otherIndex = r.nextInt(ct);
                    if (otherIndex == j) {
                        continue;
                    }
                    N n = row.get(i);
                    N otherN = row.get(otherIndex);
                    int currentNeighbors = countNeighbors(i, j, n, graph);
                    int otherNeighbors = countNeighbors(i, otherIndex, otherN, graph);
                    row.swap(j, otherIndex);
                    int newNeighbors = countNeighbors(i, otherIndex, n, graph);
                    int newOtherNeighbors = countNeighbors(i, j, otherN, graph);
                    int diffA = newNeighbors - currentNeighbors;
                    int diffB = newOtherNeighbors - otherNeighbors;
                    if (diffA + diffB <= 0) {
                        row.swap(j, otherIndex);
                    } else {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    private <E> int refineInternal(Graph<N, E> graph) {
        int x = wideSwap(graph, rows);
        int y = wideSwap(graph, getColumns());

        int a = refineInternal(rows, graph);
        int b = refineInternal(getColumns(), graph);
        return b + a + x + y;
    }

    private <E> boolean isConnected(Graph<N, E> graph, N a, N b) {
        return graph.isNeighbor(a, b);
    }

    private Iteration<N> getIfPresent(List<? extends Iteration<N>> l, int ix) {
        if (ix < l.size() && ix >= 0) {
            Iteration<N> result = l.get(ix);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return null;
    }

    private N getIfPresent(Iteration<N> iter, int ix) {
        if (ix < 0) {
            return null;
        }
        if (ix >= iter.size()) {
            return null;
        }
        return iter.get(ix);
    }

    private <E> Grid getNeighborsIn(Iteration<N> iter, int around, Graph<N, E> graph, Set<? super N> result) {
        if (iter == null) {
            return this;
        }
        N prev = getIfPresent(iter, around - 1);
        N next = getIfPresent(iter, around + 1);
        if (prev != null) {
            result.add(prev);
        }
        if (next != null) {
            result.add(next);
        }
        return this;
    }

    private <E> int countNeighbors(int row, int column, N node, Graph<N, E> graph) {
        Iteration<N> rowAbove = getIfPresent(rows, row - 1);
        Iteration<N> rowBelow = getIfPresent(rows, row + 1);
        Iteration<N> columnLeft = getIfPresent(getColumns(), column - 1);
        Iteration<N> columnRight = getIfPresent(getColumns(), column + 1);

        Set<N> foundNeighbors = new HashSet<>();
        getNeighborsIn(rowAbove, column, graph, foundNeighbors);
        getNeighborsIn(rowBelow, column, graph, foundNeighbors);
        getNeighborsIn(columnLeft, row, graph, foundNeighbors);
        getNeighborsIn(columnRight, row, graph, foundNeighbors);
        if (!foundNeighbors.isEmpty()) {
            Set<N> actualNeighbors = new HashSet<>();
            Collection<N> neighbs = graph.getNeighbors(node);
            if (neighbs != null) {
                actualNeighbors.addAll(neighbs);
            }
            foundNeighbors.retainAll(actualNeighbors);
        }
        return foundNeighbors.size();
    }

    private <E> int refineInternal(List<? extends Iteration<N>> rows, Graph<N, E> graph) {
        int result = 0;
        int rowCount = rows.size();
        for (Iteration<N> row : rows) {
            if (row.size() == 0) {
                break;
            }
            int sz = row.size();
            for (int i = 1; i < sz; i++) {
                N prev = row.get(i - 1);
                N curr = row.get(i);
                if (i != sz - 1) {
                    N next = row.get(i + 1);
                    if (isConnected(graph, prev, next) && !isConnected(graph, prev, curr)) {
                        result++;
                        row.swap(i, i + 1);
                    }
                }
            }
        }
        return result;
    }

    public List<Row<N>> getRows() {
        return rows;
    }

    private List<Iteration<N>> columns;

    public List<Iteration<N>> getColumns() {
        if (columns == null) {
            List<Iteration<N>> result = new LinkedList<>();
            int ix = 0;
            for (Row<N> row : rows) {
                if (row.isEmpty()) {
                    break;
                }
                result.add(new Column(ix++));
            }
            return columns = result;
        }
        return columns;
    }

    private class Column extends Iteration<N> {

        private int index;

        public Column(int index) {
            this.index = index;
        }

        @Override
        public int size() {
            int result = 0;
            for (Row<N> row : rows) {
                if (row.size() < index) {
                    result++;
                }
            }
            return result;
        }

        @Override
        public N get(int ix) {
            Row<N> row = rows.get(ix);
            return row.get(index);
        }

        @Override
        public void set(int ix, N n) {
            Row<N> row = rows.get(ix);
            row.set(index, n);
        }
    }
}
