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

    public double[] apply(double[] into) {
        if (into == null) {
            into = new double[2];
        }
        double angle = circle.angleOf(into[0], into[1]);
        double distance = circle.distanceToCenter(into[0], into[1]);
        double str = strength.computeStrength(circle.centerX, circle.centerY, into[0], into[1], distance);
        if (str == 0D) {
            return into;
        }
        str *= 6;
//        str = 1D / str;
        double newRad;
        double rad;
        if (str < 0D) {
//            rad = distance + (distance * (1D / -str));
            newRad = distance + -str;
        } else {
            newRad = distance - str;
//            rad = distance * str;
        }
//        System.out.println("STRENGTH: " + str + " dist " + distance + " radius: " + rad);
        
//        circle.radius = rad;
        circle.positionOf(angle, newRad, into);
        circle.centerX = into[0];
        circle.centerY = into[1];
        return into;
    }
    
    private static class Multiplier implements Strength {
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

    public interface Strength {

        public double computeStrength(double centerX, double centerY, double x, double y, double distance);
        
        default Strength multiply(double val) {
            return new Multiplier(this, val);
        }
        
        default Strength bound(double maxDistance) {
            return bounded(this, maxDistance);
        }
        
        default Strength negate() {
            return Force.negate(this);
        }
    }

    public static final Strength NO_DROPOFF = new NoDropoffStrength();

    public static final Strength INVERSE_SQUARE = new InverseSquareLaw();
    public static final Strength LINEAR = new Linear();
    
    public static Strength inverseSquareScaled(double multiplier) {
        return new InverseSquareLaw(multiplier);
    }
    
    public static Strength negate(final Strength strength) {
        return new NegatingStrength(strength);
    }
    
    private static class Linear implements Strength {

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D / Math.max(distance, 1);
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
            distance = Math.max(distance, 1);
            distance *= multiplier;
            System.out.println("DIST " + distance + " result " + (1D / (distance * distance)) );
            return (1D / (distance * distance));
        }
    }

    public static Strength bounded(final Strength strength, final double maxDistance) {
        return new BoundedStrength(maxDistance, strength);
    }

    private static class BoundedStrength implements Strength {

        private final double maxDistance;
        private final Strength strength;

        public BoundedStrength(double maxDistance, Strength strength) {
            this.maxDistance = maxDistance;
            this.strength = strength;
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            if (distance > maxDistance) {
                return 0;
            }
            return strength.computeStrength(centerX, centerY, x, y, distance);
        }
    }

    private static class NegatingStrength implements Strength {

        private final Strength strength;

        public NegatingStrength(Strength strength) {
            this.strength = strength;
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return strength.computeStrength(centerX, centerY, x, y, distance) * -1;
        }
    }

    private static class NoDropoffStrength implements Strength {

        public NoDropoffStrength() {
        }

        @Override
        public double computeStrength(double centerX, double centerY, double x, double y, double distance) {
            return 1D;
        }
    }
}
