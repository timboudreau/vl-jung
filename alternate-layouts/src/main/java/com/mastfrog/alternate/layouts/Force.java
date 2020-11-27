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

/**
 *
 * @author Tim Boudreau
 */
 interface Force {

    public static final Force NONE = (a, b, c) -> {
        c.accept(a, b);
    };

    void accept(double a, double b, DoubleBiConsumer transformed);

    default Force and(Force f) {
        return (a, b, xf) -> {
            accept(a, b, (a1, b1) -> {
                f.accept(a1, b1, xf);
            });
        };
    }

    default Force or(Force f) {
        return (a, b, xf) -> {
            accept(a, b, (a1, b1) -> {
                if (a == a1 && b == b1) {
                    f.accept(a, b, xf);
                } else {
                    double deltaA = Math.abs(a1 - 1);
                    double deltaB = Math.abs(b1 - b);
                    if (deltaA <= 0.1 && deltaB <= 0.1) {
                        f.accept(a, b, xf);
                    } else {
                        xf.accept(a1, b1);
                    }
                }
            });
        };
    }
}
