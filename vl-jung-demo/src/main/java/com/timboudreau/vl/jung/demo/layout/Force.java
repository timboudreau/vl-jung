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

/**
 *
 * @author Tim Boudreau
 */
public class Force {

    private final Circle circle;
    private final Strength strength;

    public Force(double towardX, double towardY) {
        this(towardY, towardY, NO_DROPOFF);
    }

    public Force(double towardX, double towardY, Strength strength) {
        circle = new Circle(towardX, towardY);
        this.strength = strength;
    }

    public void apply(double[] into) {
        double angle = circle.angleOf(into[0], into[1]);
        double distance = circle.distanceToCenter(into[0], into[1]);
        double str = strength.computeStrength(circle.centerX, circle.centerY, into[0], into[1], distance);
        if (str == 0D) {
            return;
        }
        double rad;
        if (str < 0D) {
            rad = distance + (distance * -str);
        } else {
            rad = distance * str;
        }
        circle.radius = rad;
        circle.positionOf(angle, distance * str, into);
    }

    public interface Strength {

        public double computeStrength(double centerX, double centerY, double x, double y, double distance);
    }

    public static final Strength NO_DROPOFF = new Strength() {

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D;
        }
    };

    public static final Strength INVERSE_SQUARE = new InverseSquareLaw();
    public static final Strength LINEAR = new Linear();
    
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
    
    private static class Linear implements Strength {

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D / distance;
        }
    }

    private static class InverseSquareLaw implements Strength {

        private final double multiplier;

        InverseSquareLaw(double multiplier) {
            this.multiplier = multiplier;
        }

        InverseSquareLaw() {
            this(1D);
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            distance *= multiplier;
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
