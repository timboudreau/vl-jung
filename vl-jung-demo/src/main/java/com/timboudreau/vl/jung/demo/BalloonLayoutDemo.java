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
import org.apache.commons.collections15.Factory;

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

    Factory<Integer> edgeFactory = new Factory<Integer>() {
        int i = 0;
        public Integer create() {
            return i++;
        }
    };

    Forest<String, Integer> createTree(Forest<String, Integer> graph) {

        graph.addVertex("A0");
        graph.addEdge(edgeFactory.create(), "A0", "B0");
        graph.addEdge(edgeFactory.create(), "A0", "B1");
        graph.addEdge(edgeFactory.create(), "A0", "B2");

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

        graph.addEdge(edgeFactory.create(), "A0", "HA1");
        graph.addEdge(edgeFactory.create(), "A0", "HA2");
        graph.addEdge(edgeFactory.create(), "A0", "HA3");
       	graph.addEdge(edgeFactory.create(), "HA3", "I1");
       	graph.addEdge(edgeFactory.create(), "HA3", "I2");
        
       	graph.addEdge(edgeFactory.create(), "I2", "J1");
       	graph.addVertex("K0");
       	graph.addEdge(edgeFactory.create(), "K0", "K1");
       	graph.addEdge(edgeFactory.create(), "K0", "K2");
       	graph.addEdge(edgeFactory.create(), "K0", "K3");
       	
       	graph.addVertex("J0");
    	graph.addEdge(edgeFactory.create(), "J0", "J1");
    	graph.addEdge(edgeFactory.create(), "J0", "J2");
    	graph.addEdge(edgeFactory.create(), "J1", "J4");
    	graph.addEdge(edgeFactory.create(), "J2", "J3");
        
    	graph.addEdge(edgeFactory.create(), "J2", "J5");
    	graph.addEdge(edgeFactory.create(), "J4", "J6");
    	graph.addEdge(edgeFactory.create(), "J4", "J7");
    	graph.addEdge(edgeFactory.create(), "J3", "J8");
    	graph.addEdge(edgeFactory.create(), "J6", "B9");
        return graph;
    }
}
