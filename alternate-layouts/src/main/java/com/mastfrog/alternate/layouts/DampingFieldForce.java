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
import java.awt.Shape;
import com.mastfrog.geometry.EqLine;

/**
 *
 * @author Tim Boudreau
 */
 class DampingFieldForce implements Force {

    private final com.mastfrog.geometry.Circle circle;
    private final EqLine line = new EqLine();
    private final EqLine ln = new EqLine();
    private final Strength strength;
    private Force delegate;
    private int ticks = 0;

    DampingFieldForce(Force delegate) {
        this(Strength.INVERSE_SQUARE.squareroot(), delegate);
    }

    DampingFieldForce(Strength strength, Force delegate) {
        circle = new com.mastfrog.geometry.Circle().setRadius(0);
        this.strength = strength;
        this.delegate = delegate;
    }

    DampingFieldForce setDelegate(Force del) {
        this.delegate = del;
        return this;
    }

    Shape circle() {
        return circle;
    }

    @Override
    public void accept(double a, double b, DoubleBiConsumer transformed) {
        switch (ticks++) {
            case 0:
                circle.setCenter(a, b);
                delegate.accept(a, b, transformed);
                break;
            case 1:
                line.setLine(circle.centerX(), circle.centerY(), a, b);
                circle.setRadius(line.length());
                circle.setCenter(line.midPoint());
                delegate.accept(a, b, transformed);
                break;
            default:
                boolean handled = false;
                line.setLine(circle.centerX(), circle.centerY(), a, b);
                if (circle.contains(a, b)) {
                    double str = strength.computeStrength(circle.centerX(), circle.centerY(), a, b, 25);
                    handled = true;
                    delegate.accept(a, b, (a1, b1) -> {
                        ln.setLine(a, b, a1, b1);
                        ln.setLength(ln.length() * str);
                        transformed.accept(ln.x2, ln.y2);
                    });
                    circle.setRadius(Math.max(20, circle.radius() * 0.9825));
                    line.setLength(line.length() * 0.5, true);
                    circle.setCenter(line.x1, line.y1);
                    return;
                }
                double origLen = line.length();
                if (line.length() > 0) {
                    if (!handled) {
                        circle.setCenter(line.midPoint());
                    }
                    if (!handled || circle.radius() < 1) {
                        circle.setRadius(Math.max(20, Math.max(circle.radius() * 1.1, origLen * 0.75)));
                    }
                }
                delegate.accept(a, b, transformed);
        }
    }

}
