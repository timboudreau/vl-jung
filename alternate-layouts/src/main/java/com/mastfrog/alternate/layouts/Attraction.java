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

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
class Attraction implements Force {

    private static final double threshold = 0.001;
    private final Circle circle;
    private final double radius;
    private final double relatedness;
    private final double minRel;

    Attraction(double x, double y, double radius, double relatedness, double minRel) {
        circle = new Circle(x, y);
        this.radius = radius;
        this.relatedness = relatedness;
        this.minRel = minRel;
    }
    Strength repulsion = Strength.INVERSE_SQUARE.negate();
    Strength attraction = Strength.INVERSE_SQUARE.multiply(0.01);

    @Override
    public void accept(double a, double b, DoubleBiConsumer transformed) {
        double dist = circle.distanceToCenter(a, b);
        double ang = circle.angleOf(a, b);
        Strength str;
        if (Math.abs(dist - radius) < threshold) {
            transformed.accept(a, b);
            return;
        } else if (dist < radius) {
            //                System.out.println("repel " + dist + " vs " + radius);
            str = repulsion;
            //                transformed.accept(a, b);
            //                return;
        } else {
            //                System.out.println("attr " + dist + " vs " + radius);
            str = attraction;
        }
        Strength s = str;
        // compute a circle from the real center to dist
        circle.positionOf(ang, (a1, b1) -> {
            double diff = -Math.abs(dist - radius);
            //                double strength = s.computeStrength(a1, b1, a, b, diff) * 0.1 * relatedness;
            //                double strength = s.computeStrength(a1, b1, a, b, diff) * 0.01 * relatedness;
            double strength = s.multiply(relatedness + (relatedness * .025)).computeStrength(a1, b1, a, b, diff);
            //                System.out.println("str " + strength);
            if (relatedness > 0 && relatedness > minRel && strength >= 0.01) {
                circle.positionOf(ang, dist + (strength * diff), transformed);
            } else {
                transformed.accept(a, b);
            }
        });
    }

}
