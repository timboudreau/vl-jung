/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package com.timboudreau.maven.dependency.graph.fork;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JScrollPane;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.widget.Widget;


/**
 * Layout instance implementing the FruchtermanReingold algorithm 
 * http://mtc.epfl.ch/~beyer/CCVisu/manual/main005.html
 * 
 * Inspired by implementations at JUNG and Prefuse.
 */
public class FruchtermanReingoldLayout extends SceneLayout {

    private double forceConstant;
    private double temp;
    private int iterations = 700;
    private final int magicSizeMultiplier = 10;
    private final int magicSizeConstant = 200;
    private Rectangle bounds;
    protected int m_fidx;
    
    private static final double MIN = 0.000001D;
    private static final double ALPHA = 0.1;
    private DependencyGraphScene scene;
    private JScrollPane panel;
    
    public FruchtermanReingoldLayout(DependencyGraphScene scene, JScrollPane panel) {
        super(scene);
        iterations = 700;
        this.scene = scene;
        init();
        this.panel = panel;
    }
    
    public @Override void performLayout() {
        performLayout(true);
        scene.validate();
        Rectangle rectangle = new Rectangle (0, 0, 1, 1);
        for (Widget widget : scene.getChildren()) {
            Rectangle childBounds = widget.getBounds();
            if (childBounds == null) {
                continue;
            }
            rectangle = rectangle.union(widget.convertLocalToScene(childBounds));
        }
        Dimension dim = rectangle.getSize ();
        Dimension viewDim = panel.getViewportBorderBounds ().getSize ();
        double zoom = Math.min ((float) viewDim.width / dim.width, (float) viewDim.height / dim.height);
        scene.setZoomFactor (Math.min(zoom, 1));
        scene.validate();
    }
    
    private void performLayout(boolean finish) {
        for (int i=0; i < iterations; i++ ) {
            int repeats = 0;
            while (true) {
                
            for (ArtifactGraphNode n : scene.getNodes()) {
                if (n.isFixed()) {
                    continue;
                }
//                if (i < iterations / 5) {
//                    if (scene.findNodeEdges(n, false, true).size() == 1 
//                       && scene.findNodeEdges(n, true, false).size() == 0) {
//                        //for leaves, ignore repulsion first, to cause closer location to parent...
//                        System.out.println("continue.." + n.getArtifact().getId());
//                        continue;
//                    }
//                }
                calcRepulsion(n);
            }
            for (ArtifactGraphEdge e : scene.getEdges()) {
                calcAttraction(e);
            }
            for (ArtifactGraphNode n : scene.getNodes()) {
                if (n.isFixed()) {
                    continue;
                }
                calcPositions(n);
            }
            if (areAllFixed() || repeats > 2) {
                doRelayoutNonFixed();
                resetFixed();
                cool(i);
                break;
            }
            repeats = repeats + 1;
            }
        }
        if (finish) {
            finish();
        }
    }
    
    public void rePerformLayout(int iters) {
        int nds = scene.getNodes().size();
        iterations = iters;
        bounds = scene.getBounds();
        if (bounds == null) {
            return;
        }
//        System.out.println("scene bounds are =" + bounds);
        temp = bounds.getWidth() / 1000;
//        forceConstant = 0.75 * Math.sqrt(bounds.getHeight() * bounds.getWidth() / nds);
        forceConstant = 0.25 * Math.sqrt(bounds.getHeight() * bounds.getWidth() / nds);
//        System.out.println("force constant2=" + forceConstant);
        performLayout(false);
    }
    
    
    
    
    private void init() {
        int nds = scene.getNodes().size();
        bounds = new Rectangle(magicSizeConstant  + (magicSizeMultiplier * nds), 
                               magicSizeConstant  + (magicSizeMultiplier * nds)); //g.getMaximumBounds();
        temp = bounds.getWidth() / 10;
        forceConstant = 0.75 * Math.sqrt(bounds.getHeight() * bounds.getWidth() / nds);
        
        ArtifactGraphNode r = scene.getRootGraphNode();
        r.locX = bounds.getCenterX();
        r.locY = bounds.getCenterY();
        r.setFixed(true);
        layoutCirculary(scene.getNodes(), r);
    }
    
    private void finish() {
        for (ArtifactGraphNode n : scene.getNodes()) {
            Widget wid = scene.findWidget(n);
            Point point = new Point();
            point.setLocation(n.locX, n.locY);
            wid.setPreferredLocation(point);
        }
    }
    
    public void calcPositions(ArtifactGraphNode n) {
        double deltaLength = Math.max(MIN,
                Math.sqrt(n.dispX * n.dispX + n.dispY * n.dispY));
        
        double xDisp = n.dispX/deltaLength * Math.min(deltaLength, temp);

        double yDisp = n.dispY/deltaLength * Math.min(deltaLength, temp);
        
        n.locX += xDisp;
        n.locY += yDisp;
        if (isThereFreeSpaceNonFixedSpace(n)) {
            n.setFixed(true);
        }
//        double x = n.locX;
//        double y = n.locY;
//        // don't let nodes leave the display
//        double borderWidth = bounds.getWidth() / 50.0;
//        if (x < bounds.getMinX() + borderWidth) {
//            x = bounds.getMinX() + borderWidth + Math.random() * borderWidth * 2.0;
//        } else if (x > (bounds.getMaxX() - borderWidth)) {
//            x = bounds.getMaxX() - borderWidth - Math.random() * borderWidth * 2.0;
//        }
//
//        if (y < bounds.getMinY() + borderWidth) {
//            y = bounds.getMinY() + borderWidth + Math.random() * borderWidth * 2.0;
//        } else if (y > (bounds.getMaxY() - borderWidth)) {
//            y = bounds.getMaxY() - borderWidth - Math.random() * borderWidth * 2.0;
//        }

//        n.locX = x;
//        n.locY = y;
    }

    public void calcAttraction(ArtifactGraphEdge e) {
        ArtifactGraphNode n1 = scene.getEdgeSource(e);
        ArtifactGraphNode n2 = scene.getEdgeTarget(e);
        assert (n1 != null && n2 != null) : "wrong edge=" + e;
//        Widget wid1 = scene.findWidget(n1);
//        Rectangle rect1 = wid1.getBounds();
//        Widget wid2 = scene.findWidget(n2);
//        Rectangle rect2 = wid2.getBounds();
        
        double xDelta = n1.locX - n2.locX;
        double yDelta = n1.locX - n2.locY;

        double deltaLength = Math.max(MIN, Math.sqrt(xDelta*xDelta + yDelta*yDelta));
        double force =  (deltaLength * deltaLength) / forceConstant;

        double xDisp = (xDelta / deltaLength) * force;
        double yDisp = (yDelta / deltaLength) * force;
        
        n1.dispX -= xDisp; 
        n1.dispY -= yDisp;
        n2.dispX += xDisp; 
        n2.dispY += yDisp;
    }

    public void calcRepulsion(ArtifactGraphNode n1) {
        n1.dispX = 0.0; 
        n1.dispY = 0.0;
//        Widget wid1 = scene.findWidget(n1);
//        Rectangle rect1 = wid1.getBounds();

        for (ArtifactGraphNode n2 : scene.getNodes()) {
//            Widget wid2 = scene.findWidget(n2);
//            Rectangle rect2 = wid1.getBounds();
            //TODO..
//            if (n2.isFixed()) continue;
            if (n1 != n2) {
                double xDelta = n1.locX - n2.locX;
                double yDelta = n1.locY - n2.locY;
                double deltaLength = Math.max(MIN, Math.sqrt(xDelta*xDelta + yDelta*yDelta));
                double force = (forceConstant * forceConstant) / deltaLength;
                n1.dispX += (xDelta / deltaLength) * force;
                n1.dispY += (yDelta / deltaLength) * force;
            }
        }
    }
    
    /**
     * this "cools" down the forces causing smaller movements..
     */
    private void cool(int iter) {
        temp *= (1.0 - iter / (double) iterations);
    }
    
    
    private void layoutCirculary(Collection<ArtifactGraphNode> nodes, ArtifactGraphNode master) {
        Point masterPoint = new Point();
        masterPoint.setLocation(master.locX, master.locY);
        double r;
        double theta;
        double thetaStep = Math.PI / 5;
        r = 150;
        theta = 0;
        Iterator<ArtifactGraphNode> it = nodes.iterator();
        ArtifactGraphNode nd = it.next();
        while (true) {
            AffineTransform tr = AffineTransform.getRotateInstance(theta);
            Point2D d2point = tr.transform(new Point2D.Double(0, r), null);
            Point point = new Point((int)d2point.getX() + masterPoint.x, (int)d2point.getY() + masterPoint.y);
            if (isThereFreeSpace(point, nd)) {
                nd.locX = point.getX();
                nd.locY = point.getY();
                nd.dispX = 0;
                nd.dispY = 0;
                if (it.hasNext()) {
                    nd = it.next();
                } else {
                    return;
                }
            }
            theta = theta + thetaStep;
            if (theta > (Math.PI * 2 - Math.PI / 10)) {
                r = r + 90;
                theta = theta - Math.PI * 2;
                thetaStep = thetaStep * 3 / 4; 
            }
        }
        
    }
    
    private boolean isThereFreeSpace(Point pnt, ArtifactGraphNode node) {
        Rectangle bnds = scene.findWidget(node).getBounds();
        if (bnds == null) {
            return true;
        }
        bnds = new Rectangle(pnt.x, pnt.y, bnds.width, bnds.height);
        for (ArtifactGraphNode nd : scene.getNodes()) {
            Rectangle bnds2 = scene.findWidget(nd).getBounds();
            if (bnds2 == null) {
                return true;
            }
            Point point = new Point();
            point.setLocation(nd.locX, nd.locY);
            bnds2 = new Rectangle(point, bnds2.getSize());
            if (bnds.intersects((bnds2))) {
                return false;
            }
        }
        return true;
    }

    private boolean areAllFixed() {
        for (ArtifactGraphNode nd : scene.getNodes()) {
            if (!nd.isFixed()) {
                return false;
            }
        }
        return true;
    }
    
    private void resetFixed() {
        for (ArtifactGraphNode nd : scene.getNodes()) {
            nd.setFixed(false);
        }
        scene.getRootGraphNode().setFixed(true);
    }
    
    private boolean isThereFreeSpaceNonFixedSpace(ArtifactGraphNode node) {
        Rectangle bnds = scene.findWidget(node).getBounds();
        if (bnds == null) {
            return true;
        }
        Point pnt = new Point();
        pnt.setLocation(node.locX, node.locY);
        bnds = new Rectangle(pnt, bnds.getSize());
        for (ArtifactGraphNode nd : scene.getNodes()) {
            Rectangle bnds2 = scene.findWidget(nd).getBounds();
            if (bnds2 == null) {
                return true;
            }
            Point point = new Point();
            point.setLocation(nd.locX, nd.locY);
            bnds2 = new Rectangle(point, bnds2.getSize());
            if (nd.isFixed() && bnds.intersects((bnds2))) {
                return false;
            }
        }
        return true;
    }
    
    private void doRelayoutNonFixed() {
        for (ArtifactGraphNode node : scene.getNodes()) {
            if (!node.isFixed()) {
                relayoutNonFixed(node);
            }
        }
    }
    
    private void relayoutNonFixed(ArtifactGraphNode node) {
        Point masterPoint = new Point();
        masterPoint.setLocation(node.locX, node.locY);
        double r;
        double theta;
        double thetaStep = Math.PI / 5;
        r = 30;
        theta = 0;
        node.setFixed(false);
        while (true) {
            AffineTransform tr = AffineTransform.getRotateInstance(theta);
            Point2D d2point = tr.transform(new Point2D.Double(0, r), null);
            Point point = new Point((int)d2point.getX() + masterPoint.x, (int)d2point.getY() + masterPoint.y);
            node.locX = point.getX();
            node.locY = point.getY();
            if (isThereFreeSpaceNonFixedSpace(node)) {
                node.setFixed(true);
                return;
            }
            theta = theta + thetaStep;
            if (theta > (Math.PI * 2 - Math.PI / 10)) {
                r = r + 30;
                theta = theta - Math.PI * 2;
                thetaStep = thetaStep * 3 / 4; 
            }
        }
        
    }
    
    
} 
