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
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class CircleTest {
    
    @Test
    public void testCircle() {
        Circle circ = new Circle(new Point2D.Double(100, 100));
        circ.radius = 100;
        double zeroAngle = circ.angleOf(100, 0);
        assertEquals(0D, zeroAngle, 0.000001D);
        
        double ninetyAngle = circ.angleOf(200, 100);
        assertEquals(90D, ninetyAngle, 0.000001D);
        
        double oneEighty = circ.angleOf(100, 200);
        assertEquals(180D, oneEighty, 0.000001D);
        
        double twoSeventy = circ.angleOf(0, 100);
        assertEquals(270D, twoSeventy, 0.000001D);
        
        double dist = circ.distanceToCenter(99, 100);
        assertEquals(1, dist, 0.000001D);
        
        dist = circ.distanceToCenter(200, 100);
        assertEquals(100D, dist, 0.000001D);
        
        double[] d = circ.positionOf(90);
        assertEquals(200D, d[0], 0.00000001D);
        assertEquals(100D, d[1], 0.00000001D);
        
        d = circ.positionOf(180);
        assertEquals(100D, d[0], 0.00000001D);
        assertEquals(200D, d[1], 0.00000001D);
        d = circ.positionOf(270);
        assertEquals(0D, d[0], 0.00000001D);
        assertEquals(100D, d[1], 0.00000001D);
        d = circ.positionOf(0);
        assertEquals(100D, d[0], 0.00000001D);
        assertEquals(0D, d[1], 0.00000001D);
        d = circ.positionOf(360);
        assertEquals(100D, d[0], 0.00000001D);
        assertEquals(0D, d[1], 0.00000001D);
    }
}
