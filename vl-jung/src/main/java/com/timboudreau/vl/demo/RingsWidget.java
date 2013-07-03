package com.timboudreau.vl.demo;

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
public class RingsWidget<N, E> extends Widget {

    private Stroke stroke = new BasicStroke(1);

    public RingsWidget(JungScene<N, E> scene) {
        super(scene);
        setForeground(new Color(220, 220, 80));
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
                Point2D p = layout.transform(v);
                ellipse.setFrame(-radius, -radius, 2 * radius, 2 * radius);
                AffineTransform at = AffineTransform.getTranslateInstance(p.getX(), p.getY());

                // Transform it to the center of the widget
                Widget w = scene.findWidget(v);
                if (w != null) {
                    Rectangle r = w.getBounds();
                    if (r != null) {
                        at.concatenate(AffineTransform.getTranslateInstance(r.width / 2, r.height / 2));
                    }
                }

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
                Point2D p = layout.transform(v);
                ellipse.setFrame(-radius, -radius, 2 * radius, 2 * radius);
                AffineTransform at = AffineTransform.getTranslateInstance(p.getX(), p.getY());

                // Transform it to the center of the widget
                Widget w = scene.findWidget(v);
                if (w != null) {
                    Rectangle r = w.getBounds();
                    if (r != null) {
                        at.concatenate(AffineTransform.getTranslateInstance(r.width / 2, r.height / 2));
                    }
                }
                Shape shape = at.createTransformedShape(ellipse);
                shape = stroke.createStrokedShape(shape);
                result.add(shape.getBounds());
            }
//            result = convertLocalToScene(result);
        }
        return result;
    }
}
