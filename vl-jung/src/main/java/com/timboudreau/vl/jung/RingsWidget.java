/* 
 * Copyright (c) 2013, Tim Boudreau
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
package com.timboudreau.vl.jung;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import org.netbeans.api.visual.widget.Widget;

/**
 * LayerWidget which displays rings, for use with BalloonLayout
 *
 * @author Tim Boudreau
 */
public final class RingsWidget<N, E> extends Widget {
    private Stroke stroke = new BasicStroke(1);

    public RingsWidget(JungScene<N, E> scene) {
        super(scene);
        setForeground(Color.ORANGE);
    }

    public void setStroke(Stroke stroke) {
        assert stroke != null : "Stroke null";
        if (!this.stroke.equals(stroke)) {
            this.stroke = stroke;
            revalidate();
        }
    }

    @Override
    public void paintWidget() {
        JungScene<N, E> scene = (JungScene<N, E>) getScene();
        Layout<N, E> l = scene.layout;
        if (l instanceof BalloonLayout) {
            BalloonLayout<N, E> layout = (BalloonLayout<N, E>) l;

            Graphics2D g2d = getGraphics();
            g2d.setColor(getForeground());

            Ellipse2D ellipse = new Ellipse2D.Double();
            for (N v : layout.getGraph().getVertices()) {
                Double radius = layout.getRadii().get(v);
                if (radius == null) {
                    continue;
                }
                Point2D p = layout.apply(v);
                ellipse.setFrame(-radius, -radius, 2 * radius, 2 * radius);
                AffineTransform at = AffineTransform.getTranslateInstance(p.getX(), p.getY());
                // Transform it to the center of the widget
//                Widget w = scene.findWidget(v);
//                if (w != null) {
//                    Rectangle r = w.getClientArea();
//                    if (r != null) {
//                        at.concatenate(AffineTransform.getTranslateInstance(r.width / 2, r.height / 2));
//                    }
//                }

                Shape shape = at.createTransformedShape(ellipse);
                g2d.draw(shape);
            }
        }
    }

    @Override
    protected Rectangle calculateClientArea() {
        Rectangle result = new Rectangle();
        JungScene<N, E> scene = (JungScene<N, E>) getScene();
        Layout<N, E> l = scene.layout;
        if (l instanceof BalloonLayout) {
            BalloonLayout<N, E> layout = (BalloonLayout<N, E>) l;
            Ellipse2D ellipse = new Ellipse2D.Double();
            for (N v : layout.getGraph().getVertices()) {
                Double radius = layout.getRadii().get(v);
                if (radius == null) {
                    continue;
                }
                Point2D p = layout.apply(v);
                ellipse.setFrame(-radius, -radius, 2 * radius, 2 * radius);
                AffineTransform at = AffineTransform.getTranslateInstance(p.getX(), p.getY());

                // Transform it to the center of the widget
//                Widget w = scene.findWidget(v);
//                if (w != null) {
//                    Rectangle r = w.getClientArea();
//                    if (r != null) {
//                        at.concatenate(AffineTransform.getTranslateInstance(r.width / 2, r.height / 2));
//                    }
//                }
                Shape shape = at.createTransformedShape(ellipse);
                shape = stroke.createStrokedShape(shape);
                result.add(shape.getBounds());
            }
//            result = convertLocalToScene(result);
        }
        return result;
    }
}
