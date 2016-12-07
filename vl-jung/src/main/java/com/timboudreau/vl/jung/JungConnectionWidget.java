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

import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.CubicCurve;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.Orthogonal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import org.netbeans.api.visual.widget.Widget;

/**
 * Uses the Transformers defined in EdgeShape to paint edges using various kinds
 * of lines and curves
 *
 * @author Tim Boudreau
 */
public class JungConnectionWidget<V, E> extends Widget {
    private Stroke stroke = new BasicStroke(2);
    private final E edge;
    private Function<E, Shape> transformer;
    
    public static <V, E> JungConnectionWidget<V, E> createQuadratic(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget(scene, EdgeShape.quadCurve(scene.graph()), edge);
    }

    public static <V,E> JungConnectionWidget<V,E> createCubic(JungScene<V,E> scene, E edge) {
        CubicCurve curve = new EdgeShape<>(scene.graph).new CubicCurve();
        return new JungConnectionWidget<>(scene, curve, edge);
    }

    public static <V,E>  JungConnectionWidget<V,E>  createOrthogonal(JungScene<V,E>  scene, E edge) {
        Orthogonal orth = EdgeShape.orthogonal(scene.graph);
        return new JungConnectionWidget<> (scene, orth, edge);
    }
    
    static class ES<V,E> extends EdgeShape<V,E> {

        public ES(Graph<V, E> g) {
            super(g);
        }
        
        BentLine createBentLine() {
            return new BentLine();
        }
        
        Loop createLoop() {
            return new Loop();
        }
        
        Box createBox() {
            return new Box();
        }
    }

    public static <V,E>  JungConnectionWidget createBent(JungScene scene, E edge) {
        EdgeShape.BentLine bentLine = new ES<>(scene.graph).createBentLine();
        Function<E,Shape> f = bentLine;
        return new JungConnectionWidget<>(scene, f, edge);
    }

    public static <V,E>  JungConnectionWidget createLoop(JungScene scene, E edge) {
        Function<E,Shape> loop = new ES<>(scene.graph).createLoop();
        return new JungConnectionWidget<>(scene, loop, edge);
    }

    public static <V,E>  JungConnectionWidget createBox(JungScene scene, E edge) {
        Function<E,Shape> box = new ES<>(scene.graph).createBox();
        return new JungConnectionWidget<>(scene, box, edge);
    }
    
    public JungConnectionWidget(JungScene<V, E> scene, E edge) {
        this(scene, defaultCurve(scene, edge), edge);
    }
    
    static <V,E> Function<E,Shape> defaultCurve(JungScene<V,E> scene, E edge) {
        Function<E,Shape> quadCurve = new ES<>(scene.graph).new QuadCurve();
        return quadCurve;
    }

    public JungConnectionWidget(JungScene<V, E> scene, Function<E, Shape> transformer, E edge) {
        super(scene);
        this.edge = edge;
        this.transformer = transformer;
        setForeground(new Color(190, 190, 255));
        setOpaque(false);
    }

    public void setTransformer(Function<E, Shape> transformer) {
        this.transformer = transformer;
        Function<Context<Graph<V, E>, E>, Shape> f =  null;
        
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
        JungScene<V, E> scene = (JungScene<V, E>) getScene();
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
        JungScene<V,E> scene = (JungScene<V,E>) getScene();
        
        Graph<V, E> graph = getGraph();
        Context<Graph<V, E>, E> c = Context.getInstance(graph, edge);
        
        Shape edgeShape = transformer.apply(edge);

        Pair<V> nodes = graph.getEndpoints(edge);
        Layout<V, E> layout = scene.layout;

        Widget w1 = scene.findWidget(nodes.getFirst());
//        Rectangle r1 = w1.getClientArea();
        Widget w2 = scene.findWidget(nodes.getSecond());
//        Rectangle r2 = w2.getClientArea();

        Point2D firstLoc = layout.apply(nodes.getFirst());
        Point2D secondLoc = layout.apply(nodes.getSecond());
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

        float x1 = (float) firstLoc.getX();
        float y1 = (float) firstLoc.getY();
        float x2 = (float) secondLoc.getX();
        float y2 = (float) secondLoc.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(firstLoc.getX(), firstLoc.getY());

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        xform.scale(dist, 1.0);

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
