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


import org.jgrapht.Graph;

import java.util.function.Supplier;

/**
 * Demonstrates the visualization of a Tree using TreeLayout and BalloonLayout.
 * An examiner lens performing a hyperbolic transformation of the view is also
 * included.
 *
 * @author Tom Nelson
 */
@SuppressWarnings("serial")
public class BalloonLayoutDemo {

    // Code borrowed from JUNG's demos

    public static Supplier<String> edgeFactory = new Supplier<>() {
        int i = 0;
        public String get() {
            return Integer.toString(i++);
        }
    };

    Graph<String, String> createTree(Graph<String, String> graph) {
        String base = "Base Node";
        graph.addVertex(base);
        graph.addVertex("B0");
        graph.addEdge(base, "B0");
        graph.addVertex("B1");
        graph.addEdge(base, "B1");
        graph.addVertex("B2");
        graph.addEdge(base, "B2");

        graph.addVertex("C0");
        graph.addEdge("B0", "C0");
        graph.addVertex("C1");
        graph.addEdge("B0", "C1");
        graph.addVertex("C2");
        graph.addEdge("B0", "C2");
        graph.addVertex("C3");
        graph.addEdge("B0", "C3");

        graph.addVertex("H0");
        graph.addEdge("C2", "H0");
        graph.addVertex("H1");
        graph.addEdge("C2", "H1");

        graph.addVertex("D0");
        graph.addEdge("B1", "D0");
        graph.addVertex("D1");
        graph.addEdge("B1", "D1");
        graph.addVertex("D2");
        graph.addEdge("B1", "D2");

        graph.addEdge(base, "D2");
        graph.addEdge(base, "C3");

        graph.addVertex("E0");
        graph.addEdge("B2", "E0");
        graph.addVertex("E1");
        graph.addEdge("B2", "E1");
        graph.addVertex("E2");
        graph.addEdge("B2", "E2");

        graph.addVertex("F0");
        graph.addEdge("D0", "F0");
        graph.addVertex("F1");
        graph.addEdge("D0", "F1");
        graph.addVertex("F2");
        graph.addEdge("D0", "F2");

        graph.addVertex("G0");
        graph.addEdge("D1", "G0");
        graph.addVertex("G1");
        graph.addEdge("D1", "G1");
        graph.addVertex("G2");
        graph.addEdge("D1", "G2");
        graph.addVertex("G3");
        graph.addEdge("D1", "G3");
        graph.addVertex("G4");
        graph.addEdge("D1", "G4");
        graph.addVertex("G5");
        graph.addEdge("D1", "G5");
        graph.addVertex("G6");
        graph.addEdge("D1", "G6");
        graph.addVertex("G7");
        graph.addEdge("D1", "G7");

        graph.addVertex("HA1");
        graph.addEdge(base, "HA1");
        graph.addVertex("HA2");
        graph.addEdge(base, "HA2");
        graph.addVertex("HA3");
        graph.addEdge(base, "HA3");
        graph.addVertex("I1");
       	graph.addEdge("HA3", "I1");
        graph.addVertex("I2");
       	graph.addEdge("HA3", "I2");

        graph.addVertex("J1");
       	graph.addEdge("I2", "J1");
       	graph.addVertex("K0");
        graph.addVertex("K1");
       	graph.addEdge("K0", "K1");
        graph.addVertex("K2");
       	graph.addEdge("K0", "K2");
        graph.addVertex("K3");
       	graph.addEdge("K0", "K3");

        graph.addVertex("J0");
        graph.addVertex("J1");
    	graph.addEdge("J0", "J1");
        graph.addVertex("J2");
    	graph.addEdge("J0", "J2");
        graph.addVertex("J4");
    	graph.addEdge("J1", "J4");
        graph.addVertex("J3");
    	graph.addEdge("J2", "J3");

    	graph.addEdge(base, "J4");

        graph.addVertex("J5");
    	graph.addEdge("J2", "J5");
        graph.addVertex("J6");
    	graph.addEdge("J4", "J6");
        graph.addVertex("J7");
    	graph.addEdge("J4", "J7");
        graph.addVertex("J8");
    	graph.addEdge("J3", "J8");
        graph.addVertex("B9");
    	graph.addEdge("J6", "B9");
        return graph;
    }
}
