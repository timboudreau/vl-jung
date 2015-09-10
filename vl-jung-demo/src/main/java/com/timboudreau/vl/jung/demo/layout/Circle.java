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

import java.awt.geom.Point2D;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Iterator;

/**
 *
 * @author Tim Boudreau
 */
class Circle {

    double centerX;
    double centerY;
    double radius = 10;
    private double rotation;

    public Circle() {
        this(new Point2D.Double());
    }

    public Circle(Point2D center) {
        centerX = center.getX();
        centerY = center.getY();
    }
    
    public Circle(double centerX, double centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }
    
    void setCenter(double x, double y) {
        this.centerX = x;
        this.centerY = y;
    }

    void setRadius(double radius) {
        this.radius = radius;
    }

    void setRotation(double angle) {
        this.rotation = angle;
    }

    public double angleOf(double x, double y) {
        double angle = rotation + ((Math.toDegrees(Math.atan2(x - centerX, centerY - y)) + 360.0) % 360.0);
        return angle;
    }

    public double distanceToCenter(double x, double y) {
        double distX = x - centerX;
        double distY = y - centerY;
        double len = Math.sqrt((distX * distX) + (distY * distY));
        return len;
    }

    double factor = 1D;

    void setUsablePercentage(double factor) {
        this.factor = Math.max(0.001D, Math.min(1D, factor));
        System.out.println("Factor to " + factor);
    }

    public double[] positionOf(double angle) {
        return positionOf(angle, this.radius, new double[2]);
    }
    
    public double[] positionOf(double angle, double radius, double[] into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        into[0] = radius * cos(angle) + centerX;
        into[1] = radius * sin(angle) + centerY;
        return into;
    }    

    public Iterator<double[]> positions(final int count) {
        return new Iterator<double[]>() {
            int ix = -1;

            @Override
            public boolean hasNext() {
                boolean result = count == 0 ? false : ix <= count;
                return result;
            }

            @Override
            public double[] next() {
                ix++;
                if (ix == 0) {
                    return new double[]{centerX, centerY - radius};
                } else {
                    double stepSize = (360D / count) * factor;
                    double angle = ((((double) ix) * stepSize) + rotation) % 360D;
                    System.out.println(ix + " angle is " + angle + " step size " + stepSize);
                    return new double[]{centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle)};
                }
            }
        };
    }

    public Iterable<double[]> iterable(final int count) {
        return new Iterable<double[]>() {
            @Override
            public Iterator<double[]> iterator() {
                return positions(count);
            }
        };
    }

}
