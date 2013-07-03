package com.timboudreau.vl.jung.demo;

import com.timboudreau.vl.demo.JungScene;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class DemoWidget<N, E> extends Widget {

    private String label = "";

    public DemoWidget(JungScene<N, E> scene) {
        super(scene);
        setBackground(new Color(240, 240, 255));
        setForeground(Color.gray);
        getActions().addAction(ActionFactory.createInplaceEditorAction(new TextFieldInplaceEditor() {

            @Override
            public boolean isEnabled(Widget widget) {
                return true;
            }

            @Override
            public String getText(Widget widget) {
                return label;
            }

            @Override
            public void setText(Widget widget, String text) {
                label = text;
                revalidate();
            }
        }));
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private Stroke stroke = new BasicStroke(2);

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    private JungScene<N, E> scene() {
        return (JungScene<N, E>) getScene();
    }

    private Shape getShape() {
        N node = (N) scene().findObject(this);
        int ix = scene().graph().getNeighborCount(node);
        int w, h;
        w = h = 18 + (ix * 5);
        Ellipse2D.Double e = new Ellipse2D.Double(0, 0, w, h);
//        RoundRectangle2D.Double e = new RoundRectangle2D.Double(0, 0, w, h, 8, 8);
        return e;
    }

    @Override
    protected Rectangle calculateClientArea() {
        Shape shape = getShape();
        shape = stroke.createStrokedShape(shape);
        Rectangle result = shape.getBounds();
//        result.x -= result.width / 2;
//        result.y -= result.height / 2;
        result.x -= 2;
        result.y -= 2;
        result.width += 4;
        result.height += 4;
        return result;
    }

    private Paint getPaint() {
        Color start = (Color) getBackground();
        Color end = start.brighter();
        Rectangle r = getClientArea();
        float x = 0;
        float y = 0;
        float x1 = r.width;
        float y1 = r.height;
        return CACHE.getPaint(x, y, start, x1, y1, end);
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();

        g.setFont(getFont());
        g.setStroke(stroke);
        Shape shape = getShape();
        g.setPaint(getPaint());
        g.fill(shape);
        g.setPaint(getForeground());
        g.draw(shape);
        float ht = g.getFontMetrics().getHeight();
        float w = g.getFontMetrics().stringWidth(label);
        Rectangle r = getClientArea();
        g.setColor(Color.WHITE);
        float y = (float) r.getCenterY() - (ht / 2F);
        y += g.getFontMetrics().getMaxAscent();
        float x = (float) r.getCenterX() - (w / 2F);
        
        g.drawString(label, x, y);
        
        if (scene().getSelection().isSelected(scene().findObject(this))) {
            g.setColor(new Color(150, 150, 250));
            AffineTransform scale = AffineTransform.getScaleInstance(0.8, 0.8);
            double width = (shape.getBounds().getWidth() * 0.125D);
            scale.concatenate(AffineTransform.getTranslateInstance(width, width));
            shape = scale.createTransformedShape(shape);
            g.draw(shape);
        }
        
    }

    private static GPCache CACHE = new GPCache();
    /**
     * GradientPaint allocates a fairly large byte[] raster;  since the
     * number of these needed is finite, we use a cache to reduce memory
     * pressure.
     */
    private static class GPCache {

        private final Map<String, GradientPaint> paints = new HashMap<>();

        public GradientPaint getPaint(float x, float y, Color start, float x1, float y1, Color end) {
            String key = x + "" + y + "" + x1 + "" + y1 + c2s(start) + c2s(end);
            GradientPaint p = paints.get(key);
            if (p == null) {
                p = new GradientPaint(x, y, start, x1, y1, end);
                paints.put(key, p);
            }
            return p;
        }

        private String c2s(Color color) {
            return Integer.toHexString(color.getRGB());
        }
    }
}
