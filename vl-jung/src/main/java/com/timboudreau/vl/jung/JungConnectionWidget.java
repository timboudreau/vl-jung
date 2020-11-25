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
import edu.uci.ics.jung.visualization.decorators.ParallelEdgeShapeTransformer;
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

    private Stroke stroke = new BasicStroke(4);
    private final E edge;
    private Function<E, Shape> transformer;

    static final class ES<V, E> extends EdgeShape<V, E> {

        public ES(Graph<V, E> g) {
            super(g);
            Object o = super.box;
        }

        public ParallelEdgeShapeTransformer<V, E> box() {
            return super.box;
        }

        public BentLine bent() {
            return bent;
        }

        public Loop loop() {
            return super.loop;
        }

        public Line line() {
            return line;
        }

        public Orthogonal orthag() {
            return orthag;
        }

        public SimpleLoop simpleLoop() {
            return simpleLoop;
        }

        private final BentLine bent = new BentLine();
        private final Line line = new Line();
        private final Orthogonal orthag = new EdgeShape.Orthogonal();
        private final SimpleLoop simpleLoop = new SimpleLoop();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> quadratic(Graph<V, E> graph) {
        return EdgeShape.quadCurve(graph);
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> cubic(Graph<V, E> graph) {
        return EdgeShape.cubicCurve(graph);
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> orthagonal(Graph<V, E> graph) {
        return new ES<>(graph).orthag();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> bent(Graph<V, E> graph) {
        return new ES<>(graph).bent();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> loop(Graph<V, E> graph) {
        return new ES<>(graph).loop();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> box(Graph<V, E> graph) {
        return new ES<>(graph).box();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> simpleLoop(Graph<V, E> graph) {
        return new ES<>(graph).simpleLoop();
    }

    public static <V, E> ParallelEdgeShapeTransformer<V, E> wedge(Graph<V, E> graph) {
        return EdgeShape.wedge(graph, 2);
    }

    public static <E> Function<E, Shape> line(Graph<?, E> graph) {
        return new ES<>(graph).line();
    }

    public static <V, E> JungConnectionWidget<V, E> createQuadratic(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, EdgeShape.quadCurve(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createCubic(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, EdgeShape.cubicCurve(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createOrthogonal(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, EdgeShape.orthogonal(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createBent(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, wedge(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createLoop(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, loop(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createSimpleLoop(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, simpleLoop(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createWegde(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, wedge(scene.graph), edge);
    }

    public static <V, E> JungConnectionWidget<V, E> createBox(JungScene<V, E> scene, E edge) {
        return new JungConnectionWidget<>(scene, new ES<>(scene.graph).box(), edge);
    }

    public JungConnectionWidget(JungScene<V, E> scene, E edge) {
        this(scene, EdgeShape.quadCurve(scene.graph), edge);
    }

    public JungConnectionWidget(JungScene<V, E> scene, Function<E, Shape> transformer, E edge) {
        super(scene);
        this.edge = edge;
        this.transformer = transformer;
        setForeground(new Color(0, 0, 180));
        setOpaque(false);
    }

    public void setTransformer(Function<E,Shape> transformer) {
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
        JungScene<V, E> scene = (JungScene<V, E>) getScene();

        Graph<V, E> graph = getGraph();
        Context<Graph<V, E>, E> c = Context.getInstance(graph, edge);
        Shape edgeShape = transformer.apply(edge);

        Pair<V> nodes = graph.getEndpoints(edge);
        Layout<V, E> layout = scene.layout;

        Widget w1 = scene.findWidget(nodes.getFirst());
        Rectangle r1 = w1.getClientArea();
        Widget w2 = scene.findWidget(nodes.getSecond());
        Rectangle r2 = w2.getClientArea();

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
