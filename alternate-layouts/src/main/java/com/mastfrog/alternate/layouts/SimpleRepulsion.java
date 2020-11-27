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
class SimpleRepulsion implements Force {

    private final double threshold;
    private final Circle circle;

    public SimpleRepulsion(double cx, double cy, double threshold) {
        this.threshold = threshold;
        circle = new Circle(cx, cy);
    }

    static double adjAngle(double a, double by) {
        a += by;
        if (a < 0) {
            a = 360D + a;
        } else if (a > 360) {
            a -= 360D;
        }
        return a;
    }

    @Override
    public void accept(double a, double b, DoubleBiConsumer transformed) {
        double dist = circle.distanceToCenter(a, b);
        if (dist < threshold) {
            double ang = circle.angleOf(a, b);
            //                double tan = Math.toDegrees(Math.tan(Math.toRadians(ang)));
            //                circle.positionOf(tan, transformed);
            //                double newDist = dist + 3D;
            //                double newDist = dist + 3D;
            double newDist = dist + 1D;
            double newAng = ang + 2; //adjAngle(ang, 2);
            circle.positionOf(newAng, newDist, transformed);
        } else {
            transformed.accept(a, b);
        }
    }

}
