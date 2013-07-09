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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.api.NbMavenProject;
import static com.timboudreau.maven.dependency.graph.fork.Bundle.*;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import org.netbeans.modules.maven.indexer.api.ui.ArtifactViewer;
import org.netbeans.modules.maven.indexer.spi.ui.ArtifactViewerFactory;
import org.netbeans.modules.maven.model.Utilities;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.spi.nodes.NodeUtils;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * component showing graph of dependencies for project.
 * @author Milos Kleint 
 */
public class DependencyGraphTopComponent extends TopComponent implements LookupListener, MultiViewElement, ActionListener {

    private static final @StaticResource String ZOOM_IN_ICON = "com/timboudreau/maven/dependency/graph/fork/zoomin.gif";
    private static final @StaticResource String ZOOM_OUT_ICON = "com/timboudreau/maven/dependency/graph/fork/zoomout.gif";
//    public static final String ATTRIBUTE_DEPENDENCIES_LAYOUT = "MavenProjectDependenciesLayout"; //NOI18N
    private static final Logger LOG = Logger.getLogger(DependencyGraphTopComponent.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(DependencyGraphTopComponent.class);
    private final RequestProcessor.Task task_reload = RP.create(new Runnable() {
        @Override
        public void run() {
            createScene();
        }
    });
    
    @MultiViewElement.Registration(
        displayName="#TAB_Graph",
        iconBase=NodeUtils.ICON_DEPENDENCY_JAR,
        persistenceType=TopComponent.PERSISTENCE_NEVER,
        preferredID=ArtifactViewer.HINT_GRAPH,
        mimeType=Constants.POM_MIME_TYPE,
        position=100
    )
    @Messages("TAB_Graph=JUNG Graph")
    public static MultiViewElement forPOM(final Lookup editor) {
        class L extends ProxyLookup implements PropertyChangeListener {
            Project p;
            L() {
                FileObject pom = editor.lookup(FileObject.class);
                if (pom != null) {
                    p = FileOwnerQuery.getOwner(pom);
                    if (p != null) {
                        NbMavenProject nbmp = p.getLookup().lookup(NbMavenProject.class);
                        if (nbmp != null) {
                            nbmp.addPropertyChangeListener(WeakListeners.propertyChange(this, nbmp));
                            reset();
                        } else {
                            LOG.log(Level.WARNING, "not a Maven project: {0}", p);
                        }
                    } else {
                        LOG.log(Level.WARNING, "no owner of {0}", pom);
                    }
                } else {
                    LOG.log(Level.WARNING, "no FileObject in {0}", editor);
                }
            }
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (NbMavenProject.PROP_PROJECT.equals(evt.getPropertyName())) {
                    reset();
                }
            }
            private void reset() {
                ArtifactViewerFactory avf = Lookup.getDefault().lookup(ArtifactViewerFactory.class);
                if (avf != null) {
                    Lookup l = avf.createLookup(p);
                    if (l != null) {
                        setLookups(l);
                    } else {
                        LOG.log(Level.WARNING, "no artifact lookup for {0}", p);
                    }
                } else {
                    LOG.warning("no ArtifactViewerFactory found");
                }
            }
        }
        return new DependencyGraphTopComponent(new L());
    }

//    private Project project;
    private Lookup.Result<DependencyNode> result;
    private Lookup.Result<MavenProject> result2;
    private Lookup.Result<POMModel> result3;

    private DependencyGraphScene scene;
    private MultiViewElementCallback callback;
    final JScrollPane pane = new JScrollPane();
    
    private HighlightVisitor highlightV;
    
    private Timer timer = new Timer(500, new ActionListener() {
        @Override public void actionPerformed(ActionEvent arg0) {
            checkFindValue();
        }
    });
    private JToolBar toolbar;
    
    @Messages({
        "LBL_Scope_All=All",
        "LBL_Scope_Compile=Compile",
        "LBL_Scope_Runtime=Runtime",
        "LBL_Scope_Test=Test"
    })
    public DependencyGraphTopComponent(Lookup lookup) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, hashCode() + " created: " + lookup, new Exception());
        }
        associateLookup(lookup);
        initComponents();
        animateLayouts.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (scene != null) {
                    scene.setAnimateIterativeLayouts(animateLayouts.isSelected());
                }
            }
        });
        layouts.addActionListener(this);
//        project = proj;
        //sldDepth.getLabelTable().put(0, new JLabel(LBL_All())); LBL_All=All
        timer.setDelay(500);
        timer.setRepeats(false);
        txtFind.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent arg0) {
                timer.restart();
            }

            @Override public void removeUpdate(DocumentEvent arg0) {
                timer.restart();
            }

            @Override public void changedUpdate(DocumentEvent arg0) {
                timer.restart();
            }
        });
        comScopes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                int scopesSize = ((List<?>) value).size();
                String msg;
                if (scopesSize == 0) {
                    msg = LBL_Scope_All();
                } else if (scopesSize == 2) {
                    msg = LBL_Scope_Compile();
                } else if (scopesSize == 3) {
                    msg = LBL_Scope_Runtime();
                } else {
                    msg = LBL_Scope_Test();
                }
                return super.getListCellRendererComponent(list, msg, index, isSelected, cellHasFocus);
            }
        });
        DefaultComboBoxModel mdl = new DefaultComboBoxModel();
        mdl.addElement(Arrays.asList(new String[0]));
        mdl.addElement(Arrays.asList(new String[] {
            Artifact.SCOPE_PROVIDED,
            Artifact.SCOPE_COMPILE
        }));
        mdl.addElement(Arrays.asList(new String[] {
            Artifact.SCOPE_PROVIDED,
            Artifact.SCOPE_COMPILE,
            Artifact.SCOPE_RUNTIME
        }));
        mdl.addElement(Arrays.asList(new String[] {
            Artifact.SCOPE_PROVIDED,
            Artifact.SCOPE_COMPILE,
            Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_TEST
        }));
        comScopes.setModel(mdl);
        comScopes.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scene != null) {
                    @SuppressWarnings("unchecked")
                    List<String> selected = (List<String>) comScopes.getSelectedItem();
                    ScopesVisitor vis = new ScopesVisitor(scene, selected);
                    scene.getRootGraphNode().getArtifact().accept(vis);
                    scene.validate();
                    scene.repaint();
                    revalidate();
                    repaint();
                }
            }
        });
        if( "Aqua".equals(UIManager.getLookAndFeel().getID()) ) { //NOI18N
            setBackground(UIManager.getColor("NbExplorerView.background")); //NOI18N
        }
    }
    
    private void checkFindValue() {
        String val = txtFind.getText().trim();
        if ("".equals(val)) { //NOI18N
            val = null;
        }
        SearchVisitor visitor = new SearchVisitor(scene);
        visitor.setSearchString(val);
        DependencyNode node = scene.getRootGraphNode().getArtifact();
        node.accept(visitor);
        scene.validate();
        scene.repaint();
        revalidate();
        repaint();

    }
    
    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }
    
    @Messages("LBL_Loading=Loading and constructing graph:")
    @Override public void componentOpened() {
        super.componentOpened();
        pane.setWheelScrollingEnabled(true);
        maxPathSpinner.setEnabled(false);
        maxPathSpinner.setVisible(false);
        lblPath.setVisible(false);
        txtFind.setEnabled(false);
        btnBigger.setEnabled(false);
        btnSmaller.setEnabled(false);
        comScopes.setEnabled(false);
        add(pane, BorderLayout.CENTER);
        setPaneText(LBL_Loading(), true);
        result = getLookup().lookupResult(DependencyNode.class);
        result.addLookupListener(this);
        result2 = getLookup().lookupResult(MavenProject.class);
        result2.addLookupListener(this);
        result3 = getLookup().lookupResult(POMModel.class);
        result3.addLookupListener(this);
        createScene();
    }
    
    private boolean animate;
    @Override
    public void componentActivated() {
        super.componentActivated();
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
    }

    @Override
    public void componentDeactivated() {
        super.componentDeactivated();
    }

    @Override
    public void componentHidden() {
        if (scene != null && animateLayouts.isSelected()) {
            animate = scene.isAnimateIterativeLayouts();
            scene.setAnimateIterativeLayouts(false);
        }
        super.componentHidden();
    }

    @Override
    public void componentShowing() {
        if (scene != null && animateLayouts.isSelected()) {
            scene.setAnimateIterativeLayouts(animate && animateLayouts.isSelected());
        }
        super.componentShowing();
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        btnBigger = new javax.swing.JButton();
        btnSmaller = new javax.swing.JButton();
        lblFind = new javax.swing.JLabel();
        txtFind = new javax.swing.JTextField();
        lblPath = new javax.swing.JLabel();
        maxPathSpinner = new javax.swing.JSpinner();
        lblScopes = new javax.swing.JLabel();
        comScopes = new javax.swing.JComboBox();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnBigger.setIcon(ImageUtilities.loadImageIcon(ZOOM_IN_ICON, true));
        btnBigger.setFocusable(false);
        btnBigger.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBigger.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBigger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBiggerActionPerformed(evt);
            }
        });
        jToolBar1.add(btnBigger);

        btnSmaller.setIcon(ImageUtilities.loadImageIcon(ZOOM_OUT_ICON, true));
        btnSmaller.setFocusable(false);
        btnSmaller.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSmaller.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSmaller.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSmallerActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSmaller);

        org.openide.awt.Mnemonics.setLocalizedText(lblFind, org.openide.util.NbBundle.getMessage(DependencyGraphTopComponent.class, "DependencyGraphTopComponent.lblFind.text")); // NOI18N
        jToolBar1.add(lblFind);

        txtFind.setMaximumSize(new java.awt.Dimension(200, 19));
        txtFind.setMinimumSize(new java.awt.Dimension(50, 19));
        txtFind.setPreferredSize(new java.awt.Dimension(150, 19));
        jToolBar1.add(txtFind);

        jPanel1.add(jToolBar1);

        lblPath.setLabelFor(maxPathSpinner);
        org.openide.awt.Mnemonics.setLocalizedText(lblPath, org.openide.util.NbBundle.getMessage(DependencyGraphTopComponent.class, "DependencyGraphTopComponent.lblPath.text")); // NOI18N
        lblPath.setToolTipText(org.openide.util.NbBundle.getMessage(DependencyGraphTopComponent.class, "DependencyGraphTopComponent.maxPathSpinner.toolTipText")); // NOI18N
        jPanel1.add(lblPath);

        maxPathSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 5, 1));
        maxPathSpinner.setToolTipText(org.openide.util.NbBundle.getMessage(DependencyGraphTopComponent.class, "DependencyGraphTopComponent.maxPathSpinner.toolTipText")); // NOI18N
        maxPathSpinner.setRequestFocusEnabled(false);
        maxPathSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maxPathSpinnerStateChanged(evt);
            }
        });
        jPanel1.add(maxPathSpinner);

        org.openide.awt.Mnemonics.setLocalizedText(lblScopes, org.openide.util.NbBundle.getMessage(DependencyGraphTopComponent.class, "DependencyGraphTopComponent.lblScopes.text")); // NOI18N
        jPanel1.add(lblScopes);
        jPanel1.add(comScopes);

        add(jPanel1, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents
    
    private void btnSmallerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSmallerActionPerformed
        scene.setZoomFactor(scene.getZoomFactor() * 0.8);
        scene.validate();
        scene.repaint();
        if (!pane.getHorizontalScrollBar().isVisible() && 
            !pane.getVerticalScrollBar().isVisible()) {
            revalidate();
            repaint();
        }
        
    }//GEN-LAST:event_btnSmallerActionPerformed
    
    private void btnBiggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBiggerActionPerformed
        scene.setZoomFactor(scene.getZoomFactor() * 1.2);
        scene.validate();
        scene.repaint();
        if (pane.getHorizontalScrollBar().isVisible() || 
            pane.getVerticalScrollBar().isVisible()) {
            revalidate();
            repaint();
        }
        
    }//GEN-LAST:event_btnBiggerActionPerformed

    private void maxPathSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maxPathSpinnerStateChanged
        depthHighlight();
    }//GEN-LAST:event_maxPathSpinnerStateChanged

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBigger;
    private javax.swing.JButton btnSmaller;
    private javax.swing.JComboBox comScopes;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JLabel lblFind;
    private javax.swing.JLabel lblPath;
    private javax.swing.JLabel lblScopes;
    private javax.swing.JSpinner maxPathSpinner;
    private javax.swing.JTextField txtFind;
    // End of variables declaration//GEN-END:variables

    private boolean expectingChanges;
    void saveChanges(POMModel model) throws IOException {
        LOG.log(Level.FINE, "{0} saveChanges...", hashCode());
        assert !expectingChanges;
        expectingChanges = true;
        try {
            Utilities.saveChanges(model);
        } finally {
            expectingChanges = false;
            LOG.log(Level.FINE, "{0} saveChanges...done", hashCode());
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        if (expectingChanges) {
            LOG.log(Level.FINE, "{0} expecting change", hashCode());
            return;
        }
        LOG.log(Level.FINE, hashCode() + " not expecting change", new Exception());
        task_reload.schedule(200); // aggregate the events, multiple will be often coming close one by another..
    }

    /** Highlights/diminishes graph nodes and edges based on path from root depth */
    public void depthHighlight () {
        if (highlightV == null) {
            highlightV = new HighlightVisitor(scene);
        }
        //int value = sldDepth.getValue();
        int value = ((SpinnerNumberModel)maxPathSpinner.getModel()).getNumber().intValue();
        highlightV.setMaxDepth(value);
        DependencyNode node = scene.getRootGraphNode().getArtifact();
        node.accept(highlightV);
        scene.validate();
        scene.repaint();
    }

    JScrollPane getScrollPane () {
        return pane;
    }

    @Messages("Err_CannotLoad=Cannot display Artifact's dependency tree.")
    private void createScene() {
        Iterator<? extends DependencyNode> it1 = result.allInstances().iterator();
        Iterator<? extends MavenProject> it2 = result2.allInstances().iterator();
        Iterator<? extends POMModel> it3 = result3.allInstances().iterator();
        final MavenProject prj = it2.hasNext() ? it2.next() : null;
        if (prj != null && NbMavenProject.isErrorPlaceholder(prj)) {
            setPaneText(Err_CannotLoad(), false);
        }
        final Project nbProj = getLookup().lookup(Project.class);
        if (prj != null && it1.hasNext()) {
            final DependencyNode root = it1.next();
            final POMModel model = it3.hasNext() ? it3.next() : null;
            RP.post(new Runnable() {
                @Override public void run() {
                    final DependencyGraphScene scene2 = new DependencyGraphScene(prj, nbProj, DependencyGraphTopComponent.this, model);
                    scene2.setAnimateIterativeLayouts(animateLayouts.isSelected());
                    GraphConstructor constr = new GraphConstructor(scene2);
                    root.accept(constr);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            scene = scene2;
                            layouts.setRenderer(scene.createRenderer());
                            layouts.setModel(scene.getLayoutModel());
                            JComponent sceneView = scene.getView();
                            if (sceneView == null) {
                                sceneView = scene.createView();
                                // vlv: print
                                sceneView.putClientProperty("print.printable", true); // NOI18N
                            }
                            pane.setViewportView(sceneView);
                            scene.cleanLayout(pane);
                            scene.setSelectedObjects(Collections.singleton(scene.getRootGraphNode()));
                            txtFind.setEnabled(true);
                            btnBigger.setEnabled(true);
                            btnSmaller.setEnabled(true);
                            comScopes.setEnabled(true);
                            if (scene.getMaxNodeDepth() > 1) {
                                lblPath.setVisible(true);
                                ((SpinnerNumberModel)maxPathSpinner.getModel()).
                                        setMaximum(Integer.valueOf(scene.getMaxNodeDepth()));
                                maxPathSpinner.setEnabled(true);
                                maxPathSpinner.setVisible(true);
                            }
                            depthHighlight();
                        }
                    });
                }
            });
        } else {
            LOG.log(Level.WARNING, "{0} missing DependencyNode and/or Project", hashCode());
        }
    }

    @Override
    public JComponent getVisualRepresentation() {
        jPanel1.removeAll();
        jToolBar1.removeAll();
        return this;
    }

    public static class EditorToolbar extends org.openide.awt.Toolbar {
        public EditorToolbar() {
            Border b = UIManager.getBorder("Nb.Editor.Toolbar.border"); //NOI18N
            setBorder(b);
            if( "Aqua".equals(UIManager.getLookAndFeel().getID()) ) { //NOI18N
                setBackground(UIManager.getColor("NbExplorerView.background")); //NOI18N
            }
        }

        @Override
        public String getUIClassID() {
            if( UIManager.get("Nb.Toolbar.ui") != null ) { //NOI18N
                return "Nb.Toolbar.ui"; //NOI18N
            }
            return super.getUIClassID();
        }

        @Override
        public String getName() {
            return "editorToolbar"; //NOI18N
        }
    }

    private final JComboBox<edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge>> layouts = new JComboBox<>();
    @Messages("LBL_AnimateLayout=Animate")
    private final JCheckBox animateLayouts = new JCheckBox(LBL_AnimateLayout());

    @Override
    public JComponent getToolbarRepresentation() {
        if (toolbar == null) {
            toolbar = new EditorToolbar();
            toolbar.setFloatable(false);
            toolbar.setRollover(true);
//            Action[] a = new Action[1];
//            Action[] actions = getLookup().lookup(a.getClass());
//            for (Action act : actions) {
//                JButton btn = new JButton();
//                Actions.connect(btn, act);
//                toolbar.add(btn);
//            }
            Dimension space = new Dimension(2, 0);
            toolbar.addSeparator(space);
            toolbar.add(btnBigger);
            toolbar.addSeparator(space);
            toolbar.add(btnSmaller);
            toolbar.addSeparator(space);
            
            toolbar.add(animateLayouts);
            toolbar.addSeparator(space);

            toolbar.add(layouts);

            toolbar.addSeparator(space);
            toolbar.add(lblFind);
            toolbar.add(txtFind);
            toolbar.addSeparator(space);
            toolbar.add(lblPath);
            toolbar.add(maxPathSpinner);
            toolbar.addSeparator(space);
            toolbar.add(lblScopes);
            toolbar.add(comScopes);
        }
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    @Override public void actionPerformed(ActionEvent ae) {
        edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge> old = scene.layout();
        JComboBox<edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge>> box = (JComboBox<edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge>>) ae.getSource();
        edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge> layout = 
                (edu.uci.ics.jung.algorithms.layout.Layout<ArtifactGraphNode, ArtifactGraphEdge>) 
                box.getSelectedItem();
        boolean wasSelected = animateLayouts.isSelected();
        try {
            if (wasSelected && layout instanceof ISOMLayout<?,?> || layout instanceof DAGLayout<?,?>) {
                animateLayouts.setSelected(false);
            }
            scene.setGraphLayout(layout, true);
        } catch (Exception e) {
            Logger.getLogger(DependencyGraphTopComponent.class.getName()).log(Level.INFO, 
                    "Could not use " + layout.getClass().getName(), e);
            // Some layouts only work for trees with no inter-dependencies
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), e.getMessage());
            box.setSelectedItem(old);
            animateLayouts.setSelected(wasSelected);
            scene.setGraphLayout(old, false);
        }
    }

    @Override public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;
    }

    @Override public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    private void setPaneText(String text, boolean progress)  {
        JComponent vView;
        if (progress) {
            JPanel panel = new JPanel();
            JProgressBar pb = new JProgressBar();
            JLabel lbl = new JLabel();

            panel.setLayout(new java.awt.GridBagLayout());
            panel.setOpaque(false);

            pb.setIndeterminate(true);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            panel.add(pb, gridBagConstraints);

            Mnemonics.setLocalizedText(lbl, text);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
            panel.add(lbl, gridBagConstraints);
            vView = panel;
        } else {
            JLabel lbl = new JLabel(text);
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setVerticalAlignment(JLabel.CENTER);
            vView = lbl;
        }

        pane.setViewportView(vView);
    }
}
