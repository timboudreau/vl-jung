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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package com.timboudreau.maven.dependency.graph.fork;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.LevelOfDetailsWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import static com.timboudreau.maven.dependency.graph.fork.Bundle.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author mkleint
 */
class ArtifactWidget extends Widget implements ActionListener, SelectProvider {

    static final Color ROOT = new Color(178, 228, 255);
    static final Color DIRECTS = new Color(178, 228, 255);
    static final Color DIRECTS_CONFLICT = new Color(235, 88, 194);
    static final Color DISABLE_HIGHTLIGHT = new Color(255, 255, 194);
    static final Color HIGHTLIGHT = new Color(255, 255, 129);
    static final Color DISABLE_CONFLICT = new Color(219, 155, 153);
    static final Color CONFLICT = new Color(219, 11, 5);
    static final Color MANAGED = new Color(30, 255, 150);
    static final Color WARNING = new Color(255, 150, 20);
    static final Color DISABLE_WARNING = EdgeWidget.deriveColor(WARNING, 0.7f);

    static final Color PROVIDED = new Color(191, 255, 255);
    static final Color COMPILE = new Color(191, 191, 255);
    static final Color RUNTIME = new Color(191, 255, 191);
    static final Color TEST = new Color(202, 151, 151);

    private static final int LEFT_TOP = 1;
    private static final int LEFT_BOTTOM = 2;
    private static final int RIGHT_TOP = 3;
    private static final int RIGHT_BOTTOM = 4;

    private static final @StaticResource String LOCK_ICON = "com/timboudreau/maven/dependency/graph/fork/lock.png";
    private static final @StaticResource String LOCK_BROKEN_ICON = "com/timboudreau/maven/dependency/graph/fork/lock-broken.png";
    private static final @StaticResource String BULB_ICON = "com/timboudreau/maven/dependency/graph/fork/bulb.gif";
    private static final @StaticResource String BULB_HIGHLIGHT_ICON = "com/timboudreau/maven/dependency/graph/fork/bulb-highlight.gif";

    private ArtifactGraphNode node;
    private List<String> scopes;
    private boolean readable = false;
    private boolean enlargedFromHover = false;

    private Timer hoverTimer;
    private Color hoverBorderC;

    private LabelWidget artifactW, versionW;
    private Widget contentW;
    private ImageWidget lockW, fixHintW;

    private int paintState = EdgeWidget.REGULAR;

    private Font origFont;
    private Color origForeground;

    private String tooltipText;

    ArtifactWidget(DependencyGraphScene scene, ArtifactGraphNode node) {
        super(scene);
        this.node = node;

        Artifact artifact = node.getArtifact().getArtifact();
        setLayout(LayoutFactory.createVerticalFlowLayout());

        updateTooltip();
        initContent(scene, artifact);

        hoverTimer = new Timer(500, this);
        hoverTimer.setRepeats(false);

        hoverBorderC = UIManager.getColor("TextPane.selectionBackground");
        if (hoverBorderC == null) {
            hoverBorderC = Color.GRAY;
        }
    }

    @Messages({
        "TIP_SingleConflict=Conflict with <b>{0}</b> version required by <b>{1}</b>",
        "TIP_SingleWarning=Warning, older version <b>{0}</b> requested by <b>{1}</b>",
        "TIP_MultipleConflict=Conflicts with:<table><thead><tr><th>Version</th><th>Artifact</th></tr></thead><tbody>",
        "TIP_MultipleWarning=Warning, older versions requested:<table><thead><tr><th>Version</th><th>Artifact</th></tr></thead><tbody>",
        "TIP_Artifact=<html><i>GroupId:</i><b> {0}</b><br><i>ArtifactId:</i><b> {1} </b><br><i>Version:</i><b> {2}</b><br><i>Scope:</i><b> {3}</b><br><i>Type:</i><b> {4}</b><br>{5}</html>"
    })
    private void updateTooltip () {
        StringBuilder tooltip = new StringBuilder();
        int conflictCount = 0;
        DependencyNode firstConflict = null;
        int conflictType = node.getConflictType();
        if (conflictType != ArtifactGraphNode.NO_CONFLICT) {
            for (DependencyNode nd : node.getDuplicatesOrConflicts()) {
                if (nd.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
                    conflictCount++;
                    if (firstConflict == null) {
                        firstConflict = nd;
                    }
                }
            }
        }

        if (conflictCount == 1) {
            DependencyNode parent = firstConflict.getParent();
            String version = firstConflict.getArtifact().getVersion();
            String requester = parent != null ? parent.getArtifact().getArtifactId() : "???";
            tooltip.append(conflictType == ArtifactGraphNode.CONFLICT ? TIP_SingleConflict(version, requester) : TIP_SingleWarning(version, requester));
        } else if (conflictCount > 1) {
            tooltip.append(conflictType == ArtifactGraphNode.CONFLICT ? TIP_MultipleConflict() : TIP_MultipleWarning());
            for (DependencyNode nd : node.getDuplicatesOrConflicts()) {
                if (nd.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
                    tooltip.append("<tr><td>");
                    tooltip.append(nd.getArtifact().getVersion());
                    tooltip.append("</td>");
                    tooltip.append("<td>");
                    DependencyNode parent = nd.getParent();
                    if (parent != null) {
                        Artifact artifact = parent.getArtifact();
                        assert artifact != null;
                        tooltip.append(artifact.getArtifactId());
                    }
                    tooltip.append("</td></tr>");
                }
            }
            tooltip.append("</tbody></table>");
        }

        Artifact artifact = node.getArtifact().getArtifact();
        final String scope = (artifact.getScope() != null ? artifact.getScope() : "");
        tooltipText = TIP_Artifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), scope, artifact.getType(), tooltip.toString());
        setToolTipText(tooltipText);
    }

    void highlightText(String searchTerm) {
        if (searchTerm != null && node.getArtifact().getArtifact().getArtifactId().contains(searchTerm)) {
            artifactW.setBackground(HIGHTLIGHT);
            artifactW.setOpaque(true);
            setPaintState(EdgeWidget.REGULAR);
            setReadable(true);
        } else {
            //reset
            artifactW.setBackground(Color.WHITE);
            artifactW.setOpaque(false);
            setPaintState(EdgeWidget.GRAYED);
            setReadable(false);
        }
    }

    void hightlightScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    private Color colorForScope(String scope) {
        if (Artifact.SCOPE_COMPILE.equals(scope)) {
            return COMPILE;
        }
        if (Artifact.SCOPE_PROVIDED.equals(scope)) {
            return PROVIDED;
        }
        if (Artifact.SCOPE_RUNTIME.equals(scope)) {
            return RUNTIME;
        }
        if (Artifact.SCOPE_TEST.equals(scope)) {
            return TEST;
        }
        return Color.BLACK;
    }

    void setPaintState (int state) {
        if (this.paintState == state) {
            return;
        }
        this.paintState = state;

        updatePaintContent();
    }

    int getPaintState () {
        return paintState;
    }

    private void updatePaintContent() {
        if (origForeground == null) {
            origForeground = getForeground();
        }

        boolean isDisabled = paintState == EdgeWidget.DISABLED;

        Color foreC = origForeground;
        if (paintState == EdgeWidget.GRAYED || isDisabled) {
            foreC = UIManager.getColor("textInactiveText");
            if (foreC == null) {
                foreC = Color.LIGHT_GRAY;
            }
            if (isDisabled) {
                foreC = new Color ((int)(foreC.getAlpha() / 1.3f), foreC.getRed(),
                        foreC.getGreen(), foreC.getBlue());
            }
        }

        contentW.setBorder(BorderFactory.createLineBorder(10, foreC));
        artifactW.setForeground(foreC);
        versionW.setForeground(foreC);
        if (lockW != null) {
            lockW.setPaintAsDisabled(paintState == EdgeWidget.GRAYED);
            lockW.setVisible(!isDisabled);
        }

        setToolTipText(paintState != EdgeWidget.DISABLED ? tooltipText : null);

        contentW.repaint();
    }

    @Messages("ACT_FixVersionConflict=Fix Version Conflict...")
    private void initContent (DependencyGraphScene scene, Artifact artifact) {
        contentW = new LevelOfDetailsWidget(scene, 0.05, 0.1, Double.MAX_VALUE, Double.MAX_VALUE);
        contentW.setBorder(BorderFactory.createLineBorder(10));
        contentW.setLayout(LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.JUSTIFY, 1));
        artifactW = new LabelWidget(scene);
        artifactW.setLabel(artifact.getArtifactId() + "  ");
        if (node.isRoot()) {
            Font defF = scene.getDefaultFont();
            artifactW.setFont(defF.deriveFont(Font.BOLD, defF.getSize() + 3f));
        }
        contentW.addChild(artifactW);
        Widget versionDetW = new LevelOfDetailsWidget(scene, 0.5, 0.7, Double.MAX_VALUE, Double.MAX_VALUE);
        versionDetW.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 2));
        contentW.addChild(versionDetW);
        versionW = new LabelWidget(scene);
        versionW.setLabel(artifact.getVersion());
        int mngState = node.getManagedState();
        if (mngState != ArtifactGraphNode.UNMANAGED) {
             lockW = new ImageWidget(scene,
                    mngState == ArtifactGraphNode.MANAGED ? ImageUtilities.loadImage(LOCK_ICON) : ImageUtilities.loadImage(LOCK_BROKEN_ICON));
        }
        versionDetW.addChild(versionW);
        if (lockW != null) {
            versionDetW.addChild(lockW);
        }

        // fix hint
        if (scene.isEditable() && DependencyGraphScene.isFixCandidate(node)) {
            Widget rootW = new Widget(scene);
            rootW.setLayout(LayoutFactory.createOverlayLayout());
            fixHintW = new ImageWidget(scene, ImageUtilities.loadImage(BULB_ICON));
            fixHintW.setVisible(false);
            fixHintW.setToolTipText(ACT_FixVersionConflict());
            fixHintW.getActions().addAction(scene.hoverAction);
            fixHintW.getActions().addAction(ActionFactory.createSelectAction(this));
            Widget panelW = new Widget(scene);
            panelW.setLayout(LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.LEFT_TOP, 0));
            panelW.setBorder(BorderFactory.createEmptyBorder(0, 3));
            panelW.addChild(fixHintW);
            rootW.addChild(panelW);
            rootW.addChild(contentW);
            addChild(rootW);
        } else {
            addChild(contentW);
        }

    }

    void modelChanged () {
        versionW.setLabel(node.getArtifact().getArtifact().getVersion());
        if (!DependencyGraphScene.isFixCandidate(node) && fixHintW != null) {
            fixHintW.setVisible(false);
            fixHintW = null;
        }
        updateTooltip();
        
        repaint();
    }

    @Override
    protected void paintBackground() {
        super.paintBackground();

        if (paintState == EdgeWidget.DISABLED) {
            return;
        }

        Graphics2D g = getScene().getGraphics();
        Rectangle bounds = getClientArea();

        if (node.isRoot()) {
            paintBottom(g, bounds, ROOT, Color.WHITE, bounds.height / 2);
        } else {
            if (scopes != null && scopes.size() > 0 && scopes.contains(node.getArtifact().getArtifact().getScope())) {
                Color scopeC = colorForScope(node.getArtifact().getArtifact().getScope());
                paintCorner(RIGHT_BOTTOM, g, bounds, scopeC, Color.WHITE, bounds.width / 2, bounds.height / 2);
            }
            int conflictType = node.getConflictType();
            Color leftTopC = null;
            if (conflictType != ArtifactGraphNode.NO_CONFLICT) {
                leftTopC = conflictType == ArtifactGraphNode.CONFLICT
                        ? (paintState == EdgeWidget.GRAYED ? DISABLE_CONFLICT : CONFLICT)
                        : (paintState == EdgeWidget.GRAYED ? DISABLE_WARNING : WARNING);
            } else {
                int state = node.getManagedState();
                if (ArtifactGraphNode.OVERRIDES_MANAGED == state) {
                    leftTopC = WARNING;
                }
            }
            if (leftTopC != null) {
                paintCorner(LEFT_TOP, g, bounds, leftTopC, Color.WHITE, bounds.width, bounds.height / 2);
            }

            if (node.getPrimaryLevel() == 1) {
                paintBottom(g, bounds, DIRECTS, Color.WHITE, bounds.height / 6);
            }
        }

        if (getState().isHovered() || getState().isSelected()) {
            paintHover(g, bounds, hoverBorderC, getState().isSelected());
        }
    }

    private static void paintCorner (int corner, Graphics2D g, Rectangle bounds,
            Color c1, Color c2, int x, int y) {
        double h = y*y + x*x;
        int gradX = (int)(y*y*x / h);
        int gradY = (int)(y*x*x / h);

        Point startPoint = new Point();
        Point direction = new Point();
        switch (corner) {
            case LEFT_TOP:
                startPoint.x = bounds.x;
                startPoint.y = bounds.y;
                direction.x = 1;
                direction.y = 1;
            break;
            case LEFT_BOTTOM:
                startPoint.x = bounds.x;
                startPoint.y = bounds.y + bounds.height;
                direction.x = 1;
                direction.y = -1;
            break;
            case RIGHT_TOP:
                startPoint.x = bounds.x + bounds.width;
                startPoint.y = bounds.y;
                direction.x = -1;
                direction.y = 1;
            break;
            case RIGHT_BOTTOM:
                startPoint.x = bounds.x + bounds.width;
                startPoint.y = bounds.y + bounds.height;
                direction.x = -1;
                direction.y = -1;
            break;
            default:
                throw new IllegalArgumentException("Corner id not valid"); //NOI18N
        }
        
        g.setPaint(new GradientPaint(startPoint.x, startPoint.y, c1,
                startPoint.x + direction.x * gradX,
                startPoint.y + direction.y * gradY, c2));
        g.fillRect(
                Math.min(startPoint.x, startPoint.x + direction.x * x),
                Math.min(startPoint.y, startPoint.y + direction.y * y),
                x, y);
    }

    private static void paintBottom (Graphics2D g, Rectangle bounds, Color c1, Color c2, int thickness) {
        g.setPaint(new GradientPaint(bounds.x, bounds.y + bounds.height, c1,
                bounds.x, bounds.y + bounds.height - thickness, c2));
        g.fillRect(bounds.x, bounds.y + bounds.height - thickness, bounds.width, thickness);
    }

    private static void paintHover (Graphics2D g, Rectangle bounds, Color c, boolean selected) {
        g.setColor(c);
        g.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2);
        if (!selected) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        }
        g.drawRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
        if (selected) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        } else {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 75));
        }
        g.drawRect(bounds.x + 3, bounds.y + 3, bounds.width - 6, bounds.height - 6);
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        super.notifyStateChanged(previousState, state);

        boolean repaintNeeded = false;
        boolean updateNeeded = false;

        if (paintState != EdgeWidget.DISABLED) {
            if (!previousState.isHovered() && state.isHovered()) {
                hoverTimer.restart();
                repaintNeeded = true;
            }

            if (previousState.isHovered() && !state.isHovered()) {
                hoverTimer.stop();
                repaintNeeded = true;
                updateNeeded = enlargedFromHover;
                enlargedFromHover = false;
            }
        }
        
        if (previousState.isSelected() != state.isSelected()) {
            updateNeeded = true;
        }

        if (updateNeeded) {
            updateContent();
        } else if (repaintNeeded) {
            repaint();
        }

    }

    @Override public void actionPerformed(ActionEvent e) {
        enlargedFromHover = true;
        updateContent();
    }

    public void setReadable (boolean readable) {
        if (this.readable == readable) {
            return;
        }
        this.readable = readable;
        updateContent();
    }

    public boolean isReadable () {
        return readable;
    }

    public ArtifactGraphNode getNode () {
        return node;
    }

    private void updateContent () {
        boolean isAnimated = ((DependencyGraphScene)getScene()).isAnimated();

        if (isAnimated) {
            artifactW.setPreferredBounds(artifactW.getPreferredBounds());
        }

        boolean makeReadable = getState().isSelected() || enlargedFromHover || readable;

        Font origF = getOrigFont();
        Font newF = origF;
        if (makeReadable) {
            bringToFront();
            // enlarge fonts so that content is readable
            newF = getReadable(getScene(), origF);
        }

        artifactW.setFont(newF);
        versionW.setFont(newF);

        if (isAnimated) {
            getScene().getSceneAnimator().animatePreferredBounds(artifactW, null);
        }

        if (fixHintW != null) {
            fixHintW.setVisible(makeReadable);
        }
    }

    private Font getOrigFont () {
        if (origFont == null) {
            origFont = artifactW.getFont();
            if (origFont == null) {
                origFont = getScene().getDefaultFont();
            }
        }
        return origFont;
    }

    public static Font getReadable (Scene scene, Font original) {
        float fSizeRatio = scene.getDefaultFont().getSize() / (float)original.getSize();
        float ratio = (float) Math.max (1, fSizeRatio / Math.max(0.0001f, scene.getZoomFactor()));
        if (ratio != 1.0f) {
            return original.deriveFont(original.getSize() * ratio);
        }
        return original;
    }

    @Override public boolean isAimingAllowed(Widget widget, Point localLocation, boolean invertSelection) {
        return false;
    }

    @Override public boolean isSelectionAllowed(Widget widget, Point localLocation, boolean invertSelection) {
        return true;
    }

    @Override public void select(Widget widget, Point localLocation, boolean invertSelection) {
        ((DependencyGraphScene)getScene()).invokeFixConflict(node);
    }

    void bulbHovered () {
        if (fixHintW != null) {
            fixHintW.setImage(ImageUtilities.loadImage(BULB_HIGHLIGHT_ICON));
        }
    }

    void bulbUnhovered () {
        if (fixHintW != null) {
            fixHintW.setImage(ImageUtilities.loadImage(BULB_ICON));
        }
    }

}
