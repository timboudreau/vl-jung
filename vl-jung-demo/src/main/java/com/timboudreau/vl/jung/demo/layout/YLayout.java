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

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author Tim Boudreau
 */
public class YLayout<N, E> extends AbstractLayout implements IterativeContext {

    private final Arranger arranger;
    private final Arranger initial;
    private boolean initialized;

    public YLayout(Graph<N, E> graph, Arranger initial, Arranger... arrangers) {
        super(graph);
        if (arrangers.length == 1) {
            arranger = arrangers[0];
        } else if (arrangers.length == 0) {
//            throw new IllegalArgumentException("No arrangers");
            this.arranger = new Arranger(){

                @Override
                protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
                    return true;
                }
            };
        } else {
            arranger = new MetaArranger(arrangers);
        }
        this.initial = initial;
    }

    @SuppressWarnings("unchecked")
    public Graph<N, E> graph() {
        return (Graph<N, E>) graph;
    }

    @Override
    public void initialize() {
        if (!initialized) {
            initialized = true;
            if (initial != null) {
                boolean done;
                initial.onStart(graph, size);
                do {
                    done = initial.step(graph, this);
                } while (!done);
            }
        }
    }

    @Override
    public void reset() {
        initial.reset();
        arranger.reset();;
        initialized = false;
        done = false;
    }

    boolean done;

    @Override
    public void step() {
        done = arranger.step(graph, this);
    }

    @Override
    public boolean done() {
        return done;
    }

    public static abstract class Arranger {

        /**
         * return true if done
         */
        protected <N, E> void onStart(Graph<N, E> graph, Dimension size) {

        }

        protected void onDone() {

        }

        protected abstract <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout);

        protected void reset() {

        }
    }

    private static final class MetaArranger extends Arranger {

        private Iterator<Arranger> arrangers;
        private final Arranger[] orig;

        public MetaArranger(Arranger... arrangers) {
            this.orig = arrangers;
            this.arrangers = Arrays.asList(arrangers).iterator();
        }

        @Override
        protected void reset() {
            for (Arranger a : orig) {
                a.reset();
            }
            this.arrangers = Arrays.asList(orig).iterator();
        }

        @Override
        protected void onDone() {
            if (curr != null) {
                curr.onDone();
            }
        }
        private Arranger curr;

        private <N, E> Arranger arr(Graph<N, E> graph, Dimension size) {
            if (curr != null) {
                return curr;
            }
            if (arrangers.hasNext()) {
                Arranger result = curr = arrangers.next();
                if (result != null) {
                    result.onStart(graph, size);
                }
                return result;
            }
            return null;
        }

        @Override
        protected <N, E> boolean step(Graph<N, E> graph, AbstractLayout layout) {
            Arranger a = arr(graph, layout.getSize());
            if (a == null) {
                return true;
            }
            boolean done = a.step(graph, layout);
            if (done) {
                curr.onDone();
                curr = null;
                done = !arrangers.hasNext();
            }
            return done;
        }
    }
}
