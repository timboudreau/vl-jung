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

import com.timboudreau.vl.jung.extensions.BaseJungScene;
import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import org.apache.commons.collections15.Transformer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

public class App {

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

        final JFrame jf = new JFrame("Visual Library + JUNG Demo");
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        ObservableGraph g = new ObservableGraph(getDemoGraph());
//        ObservableGraph g = new ObservableGraph(createDirectedAcyclicGraph(2, 9, 0.75D));
        Forest<String, Integer> forest = new DelegateForest<>();
        ObservableGraph g = new ObservableGraph(new BalloonLayoutDemo().createTree(forest));

        Layout layout = new TreeLayout(forest, 70, 70);
        final BaseJungScene scene = new SceneImpl(g, layout);
        jf.setLayout(new BorderLayout());
        jf.add(new JScrollPane(scene.createView()), BorderLayout.CENTER);

        JToolBar bar = new JToolBar();
        bar.setMargin(new Insets(5, 5, 5, 5));
        bar.setLayout(new FlowLayout(5));
        DefaultComboBoxModel<Layout> mdl = new DefaultComboBoxModel<>();
        mdl.addElement(new KKLayout(g));
        mdl.addElement(layout);
        mdl.addElement(new BalloonLayout(forest));
        mdl.addElement(new FRLayout(g));
        mdl.addElement(new FRLayout2(g));
        mdl.addElement(new ISOMLayout(g));
        mdl.addElement(new SpringLayout(g));
        mdl.addElement(new SpringLayout2(g));
        mdl.addElement(new DAGLayout(g));
        mdl.addElement(new CircleLayout(g));
        mdl.setSelectedItem(layout);
        final JComboBox<Layout> layouts = new JComboBox(mdl);
        layouts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                o = o.getClass().getSimpleName();
                return super.getListCellRendererComponent(jlist, o, i, bln, bln1); //To change body of generated methods, choose Tools | Templates.
            }
        });
        bar.add(new JLabel(" Layout Type"));
        bar.add(layouts);
        layouts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                scene.setGraphLayout((Layout) layouts.getSelectedItem(), true);
            }
        });

        bar.add(new JLabel(" Connection Shape"));
        DefaultComboBoxModel<Transformer<Context<Graph<String, Number>, Number>, Shape>> shapes = new DefaultComboBoxModel<>();
        shapes.addElement(new EdgeShape.QuadCurve<String, Number>());
        shapes.addElement(new EdgeShape.BentLine<String, Number>());
        shapes.addElement(new EdgeShape.CubicCurve<String, Number>());
        shapes.addElement(new EdgeShape.Line<String, Number>());
        shapes.addElement(new EdgeShape.Box<String, Number>());
        shapes.addElement(new EdgeShape.Orthogonal<String, Number>());
        shapes.addElement(new EdgeShape.Wedge<String, Number>(10));

        final JComboBox<Transformer<Context<Graph<String, Number>, Number>, Shape>> shapesBox = new JComboBox<>(shapes);
        shapesBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                Transformer<Context<Graph<String, Number>, Number>, Shape> xform = (Transformer<Context<Graph<String, Number>, Number>, Shape>) shapesBox.getSelectedItem();
                scene.setConnectionEdgeShape(xform);
            }
        });
        shapesBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                o = o.getClass().getSimpleName();
                return super.getListCellRendererComponent(jlist, o, i, bln, bln1); //To change body of generated methods, choose Tools | Templates.
            }
        });
        shapesBox.setSelectedItem(new EdgeShape.QuadCurve<>());
        bar.add(shapesBox);
        jf.add(bar, BorderLayout.NORTH);
        bar.add(new MinSizePanel(scene.createSatelliteView()));

        final JLabel selectionLabel = new JLabel("<html>&nbsp;</html>");
        System.out.println("LOOKUP IS " + scene.getLookup());
        Lookup.Result<String> selectedNodes = scene.getLookup().lookupResult(String.class);
        LookupListener listener = new LookupListener() {
            @Override
            public void resultChanged(LookupEvent le) {
                System.out.println("RES CHANGED");
                Lookup.Result<String> res = (Lookup.Result<String>) le.getSource();
                StringBuilder sb = new StringBuilder("<html>");
                List<String> l = new ArrayList<>(res.allInstances());
                Collections.sort(l);
                for (String s : l) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(s);
                }
                sb.append("</html>");
                selectionLabel.setText(sb.toString());
                System.out.println("LOOKUP EVENT " + sb);
            }
        };
        selectedNodes.addLookupListener(listener);
        selectedNodes.allInstances();
        
        bar.add(selectionLabel);

//        jf.setSize(jf.getGraphicsConfiguration().getBounds().width - 120, 700);
        jf.setSize(new Dimension(1280, 720));
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent we) {
                scene.relayout(true);
                scene.validate();
            }
        });
        jf.setVisible(true);
    }

    private static class MinSizePanel extends JPanel {

        MinSizePanel(JComponent inner) {
            setLayout(new BorderLayout());
            add(inner, BorderLayout.CENTER);
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }

        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            result.height = 40;
            return result;
        }
    }

    /*
     public static Graph<String, Number> createDirectedAcyclicGraph(int layers,
     int maxNodesPerLayer,
     double linkprob) {

     DirectedGraph<String, Number> dag = new DirectedSparseMultigraph<String, Number>();
     Set<String> previousLayers = new HashSet<String>();
     Set<String> inThisLayer = new HashSet<String>();
     for (int i = 0; i < layers; i++) {

     int nodesThisLayer = (int) (Math.random() * maxNodesPerLayer) + 1;
     for (int j = 0; j < nodesThisLayer; j++) {
     String v = i + ":" + j;
     dag.addVertex(v);
     inThisLayer.add(v);
     // for each previous node...
     for (String v2 : previousLayers) {
     if (Math.random() < linkprob) {
     Double de = new Double(Math.random());
     dag.addEdge(de, v, v2);
     }
     }
     }

     previousLayers.addAll(inThisLayer);
     inThisLayer.clear();
     }
     return dag;
     }

     public static Graph<String, Number> getDemoGraph() {
     UndirectedGraph<String, Number> g
     = new UndirectedSparseMultigraph<>();

     for (int i = 0; i < pairs.length; i++) {
     String[] pair = pairs[i];
     createEdge(g, pair[0], pair[1], Integer.parseInt(pair[2]));
     }

     // let's throw in a clique, too
     for (int i = 1; i <= 10; i++) {
     for (int j = i + 1; j <= 10; j++) {
     String i1 = "c" + i;
     String i2 = "c" + j;
     g.addEdge(Math.pow(i + 2, j), i1, i2);
     }
     }

     // and, last, a partial clique
     for (int i = 11; i <= 20; i++) {
     for (int j = i + 1; j <= 20; j++) {
     if (Math.random() > 0.6) {
     continue;
     }
     String i1 = "p" + i;
     String i2 = "p" + j;
     g.addEdge(Math.pow(i + 2, j), i1, i2);
     }
     }
     return g;
     }

     private static void createEdge(Graph<String, Number> g,
     String v1Label,
     String v2Label,
     int weight) {
     g.addEdge(new Double(Math.random()), v1Label, v2Label);
     }
     */
}
