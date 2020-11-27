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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Timer;
import com.mastfrog.geometry.EnhRectangle2D;

/**
 *
 * @author Tim Boudreau
 */
final class Gui extends JComponent implements ActionListener, MouseListener, MouseMotionListener {

    private final ForceDirected d;
    private final Ellipse2D.Double ell;
    private final Timer timer = new Timer(10, this);
    private final int size;
    private int zoom = 0;

    Gui(ForceDirected d) {
        this(d, 24);
    }

    Gui(ForceDirected d, int size) {
        this.d = d;
        setFocusable(true);
        Font font = new Font("monofur", Font.BOLD, 15);
        setFont(font);
        int half = size / 2;
        ell = new Ellipse2D.Double(-half, -half, size, size);
        setBackground(Color.WHITE);
        setForeground(Color.ORANGE);
        setDoubleBuffered(false);
        this.size = size;
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        if (timer.isRunning()) {
                            timer.stop();
                        } else {
                            timer.start();
                        }   break;
                    case KeyEvent.VK_ESCAPE:
                        setZoom(0);
                        setPan(0, 0);
                        break;
                    case KeyEvent.VK_EQUALS:
                        setZoom(zoom+1);
                        break;
                    case KeyEvent.VK_MINUS:
                        setZoom(zoom-1);
                        break;
                    case KeyEvent.VK_LEFT:
                        setPan(panX-5, panY);
                        break;
                    case KeyEvent.VK_RIGHT:
                        setPan(panX + 5, panY);
                        break;
                    case KeyEvent.VK_UP:
                        setPan(panX, (int) (panY-(5 * zoomFactor())));
                        break;
                    case KeyEvent.VK_DOWN:
                        setPan(panX, (int) (panY - (5 * zoomFactor())));
                        break;
                    case KeyEvent.VK_ENTER :
                        home();
                        break;
                    default:
                        break;
                }
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int i = e.getWheelRotation();
                setZoom(zoom + i);
                repaint();
            }
        });
    }

    private void home() {
        EnhRectangle2D r = new EnhRectangle2D();
        for (int i = 0; i < d.xs.length; i++) {
            r.add(d.xs[i], d.ys[i]);
        }
        r.grow(size);
        int panX = (int) Math.floor(r.x);
        int panY = (int) Math.floor(r.y);
        EnhRectangle2D bds = new EnhRectangle2D(getBounds());

        AffineTransform xf = AffineTransform.getScaleInstance(bds.width / r.width, bds.height / r.height);
        xf.concatenate(AffineTransform.getTranslateInstance(panX, panY));
        xform = xf;
        repaint();
    }

    private void setPan(int panX, int panY) {
        this.panX = panX;
        this.panY = panY;
        transformChanged();
        repaint();
    }

    private void setZoom(int zoom) {
        this.zoom = zoom;
        transformChanged();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(d.maxX, d.maxY);
    }

    @Override
    public void invalidate() {
    }

    @Override
    public void revalidate() {
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocus();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    private static final double zoomStep = 0.075;

    private double rawZoom() {
        double z = zoom;
        if (z > 0) {
            return 1D + (z * zoomStep);
        } else {
            return (double) zoom * zoomStep;
        }
    }

    private void transformChanged() {
        xform = null;
        inv = null;
    }

    AffineTransform inverseTransform() {
        if (inv == null) {
            try {
                inv = transform().createInverse();
            } catch (NoninvertibleTransformException ex) {
                inv = AffineTransform.getTranslateInstance(0, 0);
                Logger.getLogger(Gui.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return inv;
    }

    AffineTransform xform;
    AffineTransform inv;

    AffineTransform transform() {
        if (xform == null) {
            if (zoom != 0 || panX != 0 || panY != 0) {
                if (zoom != 0) {
                    double zf = zoomFactor();
                    xform = AffineTransform.getScaleInstance(zf, zf);
                } else {
                    xform = AffineTransform.getTranslateInstance(0, 0);
                }
                if (panX != 0 || panY != 0) {
                    xform.concatenate(AffineTransform.getTranslateInstance(panX, panY));
                }
            } else {
                xform = new AffineTransform();
                xform.setToIdentity();
            }
        }
        return xform;
    }

    private double zoomFactor() {
        if (zoom != 0) {
            double zoom = rawZoom();
            double zoomFactor;
            if (zoom < 0) {
                double nz = (-zoom) + 1;
                zoomFactor = 1d / (nz * nz);
            } else {
                zoomFactor = zoom;
            }
            return zoomFactor;
        }
        return 1;
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g2.transform(transform());
        if (!timer.isRunning()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        g.setColor(Color.RED);
        g2.setStroke(new BasicStroke(2.5F));;
        g2.draw(ForceDirected.damping.circle());
        g2.setStroke(new BasicStroke(1.5F));;
        g2.setFont(getFont());
        //            g.translate(512, 512);
        g.setColor(Color.LIGHT_GRAY);
        d.edges((ix, xa, ya, xb, yb) -> {
            g2.drawLine((int) xa, (int) ya, (int) xb, (int) yb);
        });
        d.positions((index, x, y) -> {
            g.setColor(getForeground());
            g2.translate(x, y);
            g2.fill(ell);
            g2.setColor(Color.BLACK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD));
            String s = Integer.toString(index);
            g2.drawString(Integer.toString(index), g2.getFontMetrics().stringWidth(s) / -2,
                    g2.getFontMetrics().getMaxAscent() / 2);
            g2.draw(ell);
            g2.translate(-(x), -(y));
        });

    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        //            d.iterate();
        if (dragIndex < 0) {
            d.iterate();
            repaint();
        }
    }

    int dragIndex = -1;
    private void beginDrag(MouseEvent e, int index) {
        e.consume();
        dragIndex = index;
    }

    private void endDrag(MouseEvent e) {
        dragIndex = -1;
        repaint();
    }

    Point panPoint;

    private void beginPan(MouseEvent e) {
        panPoint = e.getPoint();
    }

    private int panX = 0;
    private int panY = 0;

    private void pan(MouseEvent e) {
        int localPanX = this.panX;
        int localPanY = this.panY;
        localPanX -= (panPoint.x - e.getX());
        localPanY -= (panPoint.y - e.getY());
        panPoint = e.getPoint();
        setPan(localPanX, localPanY);
    }

    private void endPan(MouseEvent e) {
        panPoint = null;
    }

    private void transformPoint(Point p) {
        inverseTransform().transform(p, p);
    }

    private int indexFor(MouseEvent e) {
        Point p = e.getPoint();
        transformPoint(p);
        double sz = size;
        double half = (sz * zoomFactor()) / 2D;
        int dragIndex = d.testPositions((ix, x, y) -> {
            x -= half;
            y -= half;
            if (p.x > x && p.y > y) {
                if (p.x <= x + size && p.y <= y + size) {
                    return true;
                }
            }
            return false;
        });
        return dragIndex;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int ix = indexFor(e);
        if (ix >= 0) {
            beginDrag(e, ix);
        } else {
            beginPan(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        endDrag(e);
        endPan(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragIndex >= 0) {
            Point p = e.getPoint();
            transformPoint(p);
            d.setPosition(dragIndex, p.x, p.y);
            repaint();
        } else if (panPoint != null) {
            pan(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

}
