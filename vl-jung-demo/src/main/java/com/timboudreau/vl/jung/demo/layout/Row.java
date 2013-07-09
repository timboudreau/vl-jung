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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
class Row<N> extends Iteration<N> {
    public List<N> items = new ArrayList<>();

    public void refine(Graph<N, ?> graph) {
        refineInternal(graph);
    }

    private <E> void refineInternal(Graph<N, E> graph) {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                continue;
            }
            N prev = items.get(i - 1);
            N curr = items.get(i);
        }
    }

    public void add(N item) {
        items.remove(item);
        items.add(item);
    }

    public void swap(N a, N b) {
        if (a == b) {
            return;
        }
        int ax = items.indexOf(a);
        int bx = items.indexOf(b);
        if (ax == -1) {
            throw new IllegalArgumentException("Does not contain " + a);
        }
        if (bx == -1) {
            throw new IllegalArgumentException("Does not contain " + b);
        }
        items.set(ax, b);
        items.set(bx, a);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public N get(int ix) {
        if (ix >= items.size()) {
            return null;
        }
        return items.get(ix);
    }

    @Override
    public void set(int ix, N n) {
        items.set(ix, n);
    }
}
