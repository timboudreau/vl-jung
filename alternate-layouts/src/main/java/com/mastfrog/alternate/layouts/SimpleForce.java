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

import static com.mastfrog.alternate.layouts.Strength.NO_DROPOFF;
import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
class SimpleForce implements Force {

    private final Circle circle;
    private final Strength strength;

    public SimpleForce(double towardX, double towardY) {
        this(towardY, towardY, NO_DROPOFF);
    }

    public SimpleForce(double towardX, double towardY, Strength strength) {
        circle = new Circle(towardX, towardY);
        this.strength = strength;
    }

    @Override
    public void accept(double a, double b, DoubleBiConsumer transformed) {
        double angle = circle.angleOf(a, b);
        double distance = circle.distanceToCenter(a, b);
        double str = strength.computeStrength(circle.centerX(), circle.centerY(),
                a, b, distance);
        if (str == 0D) {
            transformed.accept(a, b);
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
        circle.positionOf(angle, newRad, (a1, b1) -> {
            circle.setCenter(a1, b1);
            transformed.accept(a1, b1);
        });

    }
}
