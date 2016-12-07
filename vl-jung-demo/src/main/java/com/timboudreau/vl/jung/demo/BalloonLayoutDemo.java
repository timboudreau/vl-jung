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
package com.timboudreau.vl.jung.demo;

import edu.uci.ics.jung.graph.Forest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Demonstrates the visualization of a Tree using TreeLayout and BalloonLayout.
 * An examiner lens performing a hyperbolic transformation of the view is also
 * included.
 *
 * @author Tom Nelson
 */
@SuppressWarnings("serial")
public class BalloonLayoutDemo {
    
    interface Factory<T> {
        public T create();
    }

    // Code borrowed from JUNG's demos
    Factory<String> edgeFactory = new Factory<String> () {
        int i = 0;

        public String create() {
            return Integer.toString(i++);
        }
    };

    String ri(List<String> l) {
        return l.get(r.nextInt(l.size()));
    }

    Random r = new Random(230432);
    int last = 0;
    char ch = 'a';

    List<String> xgroup(int count, Forest<String, String> graph) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            l.add(ch + "" + (i + last));
        }
        last += count;
        Set<String> used = new HashSet<>();
        Map<String, String> m = new HashMap<>();
        Set<Pair> pairs = new HashSet<>();
        loop:
        for (int i = 0; i < count * 15; i++) {
            String a;
            String b;
            a = ri(l);
            int ct = 0;
            do {
                if (ct++ > count * 5) {
                    break loop;
                }
                b = ri(l);
            } while (used.contains(b) || a.equals(b) || Objects.equals(m.get(b), a) || Objects.equals(m.get(a), b));
            used.add(b);
            pairs.add(new Pair(a, b));
            m.put(b, a);
        }
        List<Pair> pairsSorted = new LinkedList<>(pairs);
        Collections.sort(pairsSorted);
        for (Pair p : pairsSorted) {
            System.out.println(p.a + " -> " + p.b);
            graph.addEdge(edgeFactory.create(), p.a, p.b);
        }
        ch++;
        return l;
    }

    List<String> group(int count, Forest<String, String> graph) {
        String base = ch++ + "";
        List<String> l = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String a = base + i;
            l.add(a);
            for (int j = 0; j < count; j++) {
                String b = base + i + "-" + j;
                graph.addEdge(edgeFactory.create(), a, b);
                for (int k = j - 1; k >= 0; k--) {
                    String c = base + i + "-" + k;
                    graph.addEdge(edgeFactory.create(), a, c);
                    graph.addEdge(edgeFactory.create(), b, c);
                    break;
                }
                l.add(b);
            }
        }
        return l;
    }

    Forest<String, String> xcreateTree(Forest<String, String> graph) {
        List<String> a = group(12, graph);
        List<String> b = group(13, graph);
        List<String> c = group(11, graph);
//        String ria = ri(a);
//        String rib = ri(b);
//        String ric = ri(c);
//        graph.addEdge(edgeFactory.create(), ria, rib);
//        graph.addEdge(edgeFactory.create(), ric, rib);
//        graph.addEdge(edgeFactory.create(), ric, ria);
        return graph;
    }

    Forest<String, String> createTree(Forest<String, String> graph) {
        String base = "X0";
        graph.addEdge(edgeFactory.create(), base, "B0");
        graph.addEdge(edgeFactory.create(), base, "B1");
        graph.addEdge(edgeFactory.create(), base, "B2");

        graph.addEdge(edgeFactory.create(), "B0", "C0");
        graph.addEdge(edgeFactory.create(), "B0", "C1");
        graph.addEdge(edgeFactory.create(), "B0", "C2");
        graph.addEdge(edgeFactory.create(), "B0", "C3");

        graph.addEdge(edgeFactory.create(), "C2", "H0");
        graph.addEdge(edgeFactory.create(), "C2", "H1");

        graph.addEdge(edgeFactory.create(), "B1", "D0");
        graph.addEdge(edgeFactory.create(), "B1", "D1");
        graph.addEdge(edgeFactory.create(), "B1", "D2");

        graph.addEdge(edgeFactory.create(), "B2", "E0");
        graph.addEdge(edgeFactory.create(), "B2", "E1");
        graph.addEdge(edgeFactory.create(), "B2", "E2");

        graph.addEdge(edgeFactory.create(), "D0", "F0");
        graph.addEdge(edgeFactory.create(), "D0", "F1");
        graph.addEdge(edgeFactory.create(), "D0", "F2");

        graph.addEdge(edgeFactory.create(), "D1", "G0");
        graph.addEdge(edgeFactory.create(), "D1", "G1");
        graph.addEdge(edgeFactory.create(), "D1", "G2");
        graph.addEdge(edgeFactory.create(), "D1", "G3");
        graph.addEdge(edgeFactory.create(), "D1", "G4");
        graph.addEdge(edgeFactory.create(), "D1", "G5");
        graph.addEdge(edgeFactory.create(), "D1", "G6");
        graph.addEdge(edgeFactory.create(), "D1", "G7");

        graph.addEdge(edgeFactory.create(), base, "HA1");
        graph.addEdge(edgeFactory.create(), base, "HA2");
        graph.addEdge(edgeFactory.create(), base, "HA3");
        graph.addEdge(edgeFactory.create(), "HA3", "I1");
        graph.addEdge(edgeFactory.create(), "HA3", "I2");

        graph.addEdge(edgeFactory.create(), "I2", "J1");
        graph.addEdge(edgeFactory.create(), "K0", "K1");
        graph.addEdge(edgeFactory.create(), "K0", "K2");
        graph.addEdge(edgeFactory.create(), "K0", "K3");

        graph.addEdge(edgeFactory.create(), "J0", "J1");
        graph.addEdge(edgeFactory.create(), "J0", "J2");
        graph.addEdge(edgeFactory.create(), "J1", "J4");
        graph.addEdge(edgeFactory.create(), "J2", "J3");

        graph.addEdge(edgeFactory.create(), "J2", "J5");
        graph.addEdge(edgeFactory.create(), "J4", "J6");
        graph.addEdge(edgeFactory.create(), "J4", "J7");
        graph.addEdge(edgeFactory.create(), "J3", "J8");
        graph.addEdge(edgeFactory.create(), "J6", "B9");
        
        graph.addEdge(edgeFactory.create(), base, "K3");
        graph.addEdge(edgeFactory.create(), "K3", "L1");
        graph.addEdge(edgeFactory.create(), "K3", "L2");
        graph.addEdge(edgeFactory.create(), "K3", "L3");
        graph.addEdge(edgeFactory.create(), "K3", "L4");
        graph.addEdge(edgeFactory.create(), "K3", "L5");
        graph.addEdge(edgeFactory.create(), "K3", "L6");
        
        graph.addEdge(edgeFactory.create(), "L3", "M1");
        graph.addEdge(edgeFactory.create(), "L3", "M2");
        graph.addEdge(edgeFactory.create(), "L2", "M3");
        graph.addEdge(edgeFactory.create(), "L2", "M4");
        graph.addEdge(edgeFactory.create(), "L2", "M5");
        graph.addEdge(edgeFactory.create(), "M1", "N1");
        return graph;
    }

    static class Pair implements Comparable<Pair> {

        public final String a;
        public final String b;

        public Pair(String a, String b) {
            this.a = a;
            this.b = b;
        }

        public String toString() {
            return a + ":" + b;
        }

        @Override
        public int compareTo(Pair o) {
            return toString().compareTo(o.toString());
        }
    }
}
