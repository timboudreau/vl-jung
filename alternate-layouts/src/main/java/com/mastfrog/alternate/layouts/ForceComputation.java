/* 
 * Copyright (c) 2020, Tim Boudreau
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
package com.mastfrog.alternate.layouts;

/**
 *
 * @author Tim Boudreau
 */
interface ForceComputation {

    double apply(double position, double otherPosition, double affinity);

    default ForceComputation plus(ForceComputation other) {
        return (p, o, a) -> {
            return apply(p, o, a) + other.apply(p, o, a);
        };
    }

    default ForceComputation times(ForceComputation other) {
        return (p, o, a) -> {
            return apply(p, o, a) * other.apply(p, o, a);
        };
    }

    public static class ReplusionForce implements ForceComputation {

        double repulsionHorizon = 600;

        @Override
        public double apply(double position, double otherPosition, double affinity) {
            double diff = position - otherPosition;
            double dist = Math.abs(diff);
            double dir = diff < 0 ? -1 : 1;
            if (dist < repulsionHorizon) {
                return dir * Math.cbrt(dist);
            }
            return 0;
        }
    }

    public static class AttractionForce implements ForceComputation {

        @Override
        public double apply(double position, double otherPosition, double affinity) {
            double diff = position - otherPosition;
            double dist = Math.abs(diff);
            double dir = diff < 0 ? affinity : -affinity;
            return Math.cbrt(diff) * 1D / -dir;
        }
    }

    public static class ForceImpl implements ForceComputation {

        double repulsionHorizon = 850;

        public double apply(double position, double otherPosition, double affinity) {
            double diff = position - otherPosition;
            double dist = Math.abs(diff);
            if (System.nanoTime() % 35 == 0) {
                return diff < 0 ? -12 : 12;
            }
            if (dist < 0.01D) {
                return 0;
            }
            double distToReplusionHorizion = dist - repulsionHorizon;
            double deg = distToReplusionHorizion / repulsionHorizon;
            double repulsionFactor = (deg * -affinity) * 0.525;
            if (distToReplusionHorizion < 0) {
                //                return -affinity * dist;
                //                return (1D/-affinity) * 10D;
                return Math.sqrt(300D / -affinity) * 1D / dist;
            }
            //            double dir = distToReplusionHorizion < 0 ? -1 : 1;
            double dir = diff < 0 ? -1 : 1;
            //            if (affinity == 0D) {
            //                return -dir * 0.25;
            //            }
            //            if (affinity == 0D) {
            //                if (dist < repulsionHorizon) {
            //                    return position - dir;
            ////                    return position > otherPosition ? position - 1 : position + 1;
            //                }
            //                return position;
            //            }
            //            return dir + Math.sqrt(distToReplusionHorizion * affinity);
            //            return dir * affinity *  (2D / dist);
            return repulsionFactor + (affinity * ((distToReplusionHorizion * dir) / 50D)) + ((-dist * affinity) / 1500D);
            //            return position + (diff < 0 ? affinity : -affinity);
            //            double adj = dir * affinity * (1D / (distToReplusionHorizion / 2D));
            //
            //            return position + adj;
            //            double m = dir * ((distToReplusionHorizion - Math.abs(otherPosition)) / 3);
            //
            //            double factor = dir * (distToReplusionHorizion * affinity);
            //
            //            return position + (m * affinity);
        }

        private int quadraticEase(double elapsed, double start, double end, double total) {
            double result;
            if ((elapsed /= total / 2) < 1) {
                result = end / 2 * elapsed * elapsed + start;
            } else {
                result = -end / 2 * ((--elapsed) * (elapsed - 2) - 1) + elapsed;
            }
            return (int) result;
        }
    }

}
