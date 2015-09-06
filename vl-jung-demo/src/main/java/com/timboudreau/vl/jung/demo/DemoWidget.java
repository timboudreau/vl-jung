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
package com.timboudreau.vl.jung.demo;

import com.timboudreau.vl.jung.JungScene;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public class DemoWidget<N, E> extends Widget {

    private Stroke stroke = new BasicStroke(2);
    private String label = "";
    final String node;
    private final Lookup lkp;

    public DemoWidget(JungScene<N, E> scene, String node) {
        super(scene);
        lkp = Lookups.fixed(node);
        this.node = node;
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

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    private JungScene<N, E> scene() {
        return (JungScene<N, E>) getScene();
    }

    private Shape getShape() {
        // Get the node
        N node = (N) scene().findObject(this);
        // Grow the shape based on the number of connections
        int ix = scene().graph().getNeighborCount(node);
        // Minimum size 18 pixels, grow by 3 for each connection
        double w, h;
        w = h = 18 + (ix * 3);
        
        double labelWidth = getGraphics().getFontMetrics().stringWidth(label);
        w = Math.max(w, labelWidth + 18);
        
//        Ellipse2D.Double e = new Ellipse2D.Double(0, 0, w, h);
        RoundRectangle2D.Double e = new RoundRectangle2D.Double(-w / 2D, -h / 2D, w, h, 8, 8);
        return e;
    }

    @Override
    protected Rectangle calculateClientArea() {
        // Stroke the shape to make sure we include the line width in our
        // bounding box
        return stroke.createStrokedShape(getShape()).getBounds();
    }

    private Paint getPaint() {
        Color start = (Color) getBackground();
        Color end = start.brighter();
        Rectangle r = getClientArea();
        float x = r.x;
        float y = r.y;
        float x1 = r.x + r.width;
        float y1 = r.y + r.height;
        GradientPaint result;
        if (getScene().getSceneAnimator().isAnimatingBackgroundColor(this)) {
            // Don't cache transient gradient paints, just ones that will
            // be used repeatedly
            result = new GradientPaint(x, y, start, x1, y1, end);
        } else {
            result = CACHE.getPaint(x, y, start, x1, y1, end);
        }
        return result;
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();

        g.setFont(getFont());
        g.setStroke(stroke);
        // First fill the shape
        Shape shape = getShape();
        // Set up our gradient
        g.setPaint(getPaint());
        g.fill(shape);

        // Now draw the outline
        g.setPaint(getForeground());
        g.draw(shape);

        // Now draw toString() on the node
        float ht = g.getFontMetrics().getHeight();
        float w = g.getFontMetrics().stringWidth(label);
        Rectangle r = getClientArea();
        g.setColor(getTextColor());
        float y = (float) r.getCenterY() - (ht / 2F);
        y += g.getFontMetrics().getMaxAscent();
        float x = (float) r.getCenterX() - (w / 2F);
        g.drawString(label, x, y);

        // Draw a highlight shape if it is selected
        if (scene().getSelection().isSelected(scene().findObject(this))) {
            g.setColor(new Color(150, 150, 250));
            AffineTransform scale = AffineTransform.getScaleInstance(0.8, 0.8);
            double width = (shape.getBounds().getWidth() * 0.0125D);
            scale.concatenate(AffineTransform.getTranslateInstance(width, width));
            shape = scale.createTransformedShape(shape);
            g.draw(shape);
        }
    }

    private static GPCache CACHE = new GPCache();

    private Color getTextColor() {
        // Convoluted but works.
        Color c = (Color) getBackground();
        // Get the grascale version of the color, so that we handle perceptual
        // differences in dark/light - a highly saturated blue is bright according
        // to HSB a dark color is not readable against it
        ColorSpace grayscale = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        float[] rgb = new float[]{(float) c.getRed() / 255F,
            (float) c.getGreen() / 255F, (float) c.getBlue() / 255F};
//        rgb = grayscale.fromRGB(rgb);
        // Invert the grayscale version of all color components
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = 1F - rgb[i];
        }
        // Convert it back to RGB values between 0.0F and 1.0F
        rgb = grayscale.toRGB(rgb);
        // Convert it back to a color, doing some additional computation - 
        // we want values very close to 0.5F - neutral gray - not to be
        // indistinguishable, so have values between 0.3 and 0.7 repel
        // the value
        return new Color(toByteValue(rgb[0]), toByteValue(rgb[1]), toByteValue(rgb[2]));
    }

    private int toByteValue(float f) {
        float dist = 0.5F - f;
        // bounce the value away from 0.5
        if (Math.abs(dist) < 0.4) {
            f *= 1F - dist;
        }
        // multiply it back into a Color value and constrain it within 0-255
        float result = Math.min(255F, Math.max(0F, f * 255F));
        return (int) result;
    }

    /**
     * GradientPaint allocates a fairly large byte[] raster; since the number of
     * these needed is finite, we use a cache to reduce memory pressure.
     */
    private static class GPCache {

        private final Map<String, GradientPaint> paints = new HashMap<>();

        public GradientPaint getPaint(float x, float y, Color start, float x1, float y1, Color end) {
            // A key that will be unique by the passed parameters
            String key = (int) x + "-" + (int) y + "-" + (int) x1 + "-" + (int) y1 + ':' + c2s(start) + ',' + c2s(end);
            GradientPaint p = paints.get(key);
            if (p == null) {
                if (paints.size() > 50) {
                    // Don't let the cache get huge
                    paints.clear();
                }
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
