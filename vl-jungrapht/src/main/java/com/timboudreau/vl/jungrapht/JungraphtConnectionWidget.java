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
package com.timboudreau.vl.jungrapht;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.function.BiFunction;

import org.jgrapht.Graph;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.netbeans.api.visual.widget.Widget;

/**
 * Uses the Transformers defined in EdgeShape to paint edges using various kinds
 * of lines and curves
 *
 * @author Tim Boudreau
 */
public class JungraphtConnectionWidget<V, E> extends Widget {
    private Stroke stroke = new BasicStroke(2);
    private final E edge;
    private BiFunction<Graph<V, E>, E, Shape> transformer;

    public void setEdgeShapeFunction(BiFunction<Graph<V, E>, E, Shape> transformer) {
        this.transformer = transformer;
    }
    public static <V, E> JungraphtConnectionWidget<V, E> createQuadratic(JungraphtScene<V, E> scene, E edge) {
        return new JungraphtConnectionWidget<>(scene, new EdgeShape.QuadCurve<V, E>(), edge);
    }

    public static <V, E> JungraphtConnectionWidget<V, E> createCubic(JungraphtScene<V, E> scene, E edge) {
        return new JungraphtConnectionWidget<>(scene, new EdgeShape.CubicCurve<V, E>(), edge);
    }

    public static <V, E> JungraphtConnectionWidget<V, E> createOrthogonal(JungraphtScene<V, E> scene, E edge) {
        return new JungraphtConnectionWidget<>(scene, new EdgeShape.Orthogonal<V, E>(), edge);
    }

    public static <V, E> JungraphtConnectionWidget<V, E> createLoop(JungraphtScene<V, E> scene, E edge) {
        return new JungraphtConnectionWidget<>(scene, new EdgeShape.Loop<V, E>(), edge);
    }

    public static <V, E> JungraphtConnectionWidget<V, E> createBox(JungraphtScene<V, E> scene, E edge) {
        return new JungraphtConnectionWidget<>(scene, new EdgeShape.Box<V, E>(), edge);
    }

    public JungraphtConnectionWidget(JungraphtScene<V, E> scene, E edge) {
        this(scene, new EdgeShape.QuadCurve<V, E>(), edge);
    }

    public JungraphtConnectionWidget(JungraphtScene<V, E> scene, BiFunction<Graph<V, E>, E, Shape> transformer, E edge) {
        super(scene);
        this.edge = edge;
        this.transformer = transformer;
        setForeground(new Color(190, 190, 255));
        setOpaque(false);
    }

    public void setFunction(BiFunction<Graph<V, E>, E, Shape> transformer) {
        this.transformer = transformer;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return true;
    }

    public void setStroke(Stroke stroke) {
        assert stroke != null : "Stroke null";
        this.stroke = stroke;
    }
    
    public Stroke getStroke() {
        return stroke;
    }

    private Graph<V, E> getGraph() {
        JungraphtScene<V, E> scene = (JungraphtScene<V, E>) getScene();
        return scene.graph;
    }

    @Override
    protected Rectangle calculateClientArea() {
        Shape shape = getShape().getBounds2D();
        Rectangle result = new Rectangle(stroke.createStrokedShape(shape).getBounds());
        result = convertLocalToScene(result);
        return result;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        Stroke stk = new BasicStroke(5);
        Shape shape = stk.createStrokedShape(getShape());
        return shape.contains(convertLocalToScene(localLocation));
    }

    private Shape getShape() {
        JungraphtScene<V,E> scene = (JungraphtScene<V,E>) getScene();
        
        Graph<V, E> graph = getGraph();
        Shape edgeShape = transformer.apply(graph, edge);

        V source = graph.getEdgeSource(edge);
        V target = graph.getEdgeTarget(edge);
        LayoutModel<V> layoutModel = scene.model;

        Widget w1 = scene.findWidget(source);
        Rectangle r1 = w1.getClientArea();
        Widget w2 = scene.findWidget(target);
        Rectangle r2 = w2.getClientArea();

        org.jungrapht.visualization.layout.model.Point firstLoc = layoutModel.apply(source);
        org.jungrapht.visualization.layout.model.Point secondLoc = layoutModel.apply(target);
//        if (r1 != null) {
//            r1.x = 0;
//            r1.y = 0;
//            firstLoc = new Point2D.Double(firstLoc.getX() + (double) r1.getCenterX(),
//                    firstLoc.getY() + (double) r1.getCenterY());
//        }
//        if (r2 != null) {
//            r2.x = 0;
//            r2.y = 0;
//            secondLoc = new Point2D.Double(secondLoc.getX() + (double) r2.getCenterX(),
//                    secondLoc.getY() + (double) r2.getCenterY());
//        }

        float x1 = (float) firstLoc.x;
        float y1 = (float) firstLoc.y;
        float x2 = (float) secondLoc.x;
        float y2 = (float) secondLoc.y;

        AffineTransform xform = AffineTransform.getTranslateInstance(firstLoc.x, firstLoc.y);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (edgeShape instanceof Path2D) {
            xform.scale(dist, dist);
        } else {
            xform.scale(dist, 1.0);
        }

        edgeShape = xform.createTransformedShape(edgeShape);
        return edgeShape;
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        g.setPaint(getForeground());
        g.setStroke(stroke);
        Shape edgeShape = getShape();
        g.draw(edgeShape);
    }
}
