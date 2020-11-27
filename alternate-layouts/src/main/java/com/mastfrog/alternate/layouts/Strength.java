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

import com.mastfrog.geometry.Circle;
import static java.lang.Math.log;

/**
 *
 * @author Tim Boudreau
 */
interface Strength {

    public double computeStrength(double centerX, double centerY, double x, double y, double distance);

    public static final Strength INVERSE_SQUARE = new InverseSquareLaw();
    public static final Strength LINEAR = new Linear();

    default Strength multiply(double val) {
        return new Multiplier(this, val);
    }

    default Strength bound(double maxDistance) {
        return bounded(this, maxDistance);
    }

    default Strength negate() {
        return negate(this);
    }

    default Strength logarithmic() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return Math.log(this.computeStrength(centerX, centerY, x, y, distance));
        };
    }

    default Strength cuberoot() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return Math.cbrt(this.computeStrength(centerX, centerY, x, y, distance));
        };
    }

    default Strength squareroot() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return Math.cbrt(this.computeStrength(centerX, centerY, x, y, distance));
        };
    }

    default Strength sin() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return Math.sin(this.computeStrength(centerX, centerY, x, y, distance));
        };
    }

    default Strength cos() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return Math.sin(this.computeStrength(centerX, centerY, x, y, distance));
        };
    }

    static Strength fixed(double val) {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return val;
        };
    }

    default Strength internal() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            if (new Circle(centerX, centerY).distanceToCenter(x, y) < distance) {
                return Strength.this.computeStrength(centerX, centerY, x, y, distance);
            } else {
                return 0;
            }
        };
    }

    default Strength external() {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            if (new Circle(centerX, centerY).distanceToCenter(x, y) >= distance) {
                return Strength.this.computeStrength(centerX, centerY, x, y, distance);
            } else {
                return 0;
            }
        };
    }

    static Strength LOG = (double centerX, double centerY, double x, double y, double distance) -> {
        return log(distance);
    };

    default Strength dividedInto(double d) {
        return (double centerX, double centerY, double x, double y, double distance) -> {
            return d / Strength.this.computeStrength(centerX, centerY, x, y, distance);
        };
    }

    public static class Multiplier implements Strength {

        private final Strength orig;
        private final double multiplyBy;

        public Multiplier(Strength orig, double multiplyBy) {
            this.orig = orig;
            this.multiplyBy = multiplyBy;
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return multiplyBy * orig.computeStrength(centerX, centerY, x, y, distance);
        }
    }
    public static final Strength NO_DROPOFF = new Strength() {
        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D;
        }
    };

    public static Strength inverseSquareScaled(double multiplier) {
        return new InverseSquareLaw(multiplier);
    }

    public static Strength negate(final Strength strength) {
        return new Strength() {
            @Override
            public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
                return strength.computeStrength(centerX, centerY, x, y, distance) * -1;
            }
        };
    }

    public static class Linear implements Strength {

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D / Math.max(distance, 1);
        }
    }

    public static class InverseSquareLaw implements Strength {

        private final double multiplier;

        InverseSquareLaw(double multiplier) {
            this.multiplier = multiplier;
        }

        InverseSquareLaw() {
            this(1D);
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            distance = Math.max(distance, 1);
            distance *= multiplier;
//            System.out.println("DIST " + distance + " result " + (1D / (distance * distance)));
            return 1D / (distance * distance);
        }
    }

    public static Strength bounded(final Strength strength, final double maxDistance) {
        return new Strength() {
            @Override
            public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
                if (distance > maxDistance) {
                    return 0;
                }
                return strength.computeStrength(centerX, centerY, x, y, distance);
            }
        };
    }

}
