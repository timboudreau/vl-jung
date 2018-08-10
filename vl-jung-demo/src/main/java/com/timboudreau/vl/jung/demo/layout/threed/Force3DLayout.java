/*
 * Copyright (c) 2017, Tim Boudreau
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

package com.timboudreau.vl.jung.demo.layout.threed;

import edu.uci.ics.jung.graph.Graph;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Tim Boudreau
 */
public class Force3DLayout<N> {

    private final Graph<N, ?> graph;
    private int iteration = 0;
    private double temperature = 1;
    private final CircumferenceProvider circ;
    private final double minDistance;

    Force3DLayout(Graph<N, ?> graph, CircumferenceProvider circ, double minDistance) {
        this.graph = graph;
        this.circ = circ;
        this.minDistance = minDistance;
    }
    
    interface CircumferenceProvider<N> {
        double circumference(N n);
    }

    private Map<N, Coordinate> last;
    public final Map<N, Coordinate> tick() {
        Map<N, Coordinate> result = new HashMap<>();
        if (iteration++ == 0) {
            Random r = new Random(230404);
            double count = graph.getVertexCount();
            for (N n : graph.getVertices()) {
                double x = r.nextDouble() * (minDistance * count);
                double y = r.nextDouble() * (minDistance * count);
                double z = r.nextDouble() * (minDistance * count);
                result.put(n, new Coordinate(x,y,z));
            }
        } else {
            for (N n : graph.getVertices()) {
                Coordinate curr = last.get(n);
                for (Map.Entry<N, Coordinate> others : last.entrySet()) {
                    if (n.equals(others.getKey())) {
                        continue;
                    }
                    
                }
            }
        }
        return last = result;
    }
    
    final class Coordinate {
        public final double x;
        public final double y;
        public final double z;

        public Coordinate(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Coordinate plus(Coordinate other) {
            return new Coordinate(x + other.x, y + other.y, z + other.z);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.z) ^ (Double.doubleToLongBits(this.z) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Coordinate other = (Coordinate) obj;
            if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
                return false;
            }
            if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
                return false;
            }
            if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
                return false;
            }
            return true;
        }
        
    }
}
