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
import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.layout.algorithms.BalloonLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.CircleLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.DAGLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.EiglspergerLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.FRLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.ForceAtlas2LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.GEMLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.ISOMLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.KKLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.RadialTreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.SpringLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.TidierRadialTreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.TidierTreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.TreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.repulsion.BarnesHutFA2Repulsion;
import org.jungrapht.visualization.layout.algorithms.repulsion.BarnesHutFRRepulsion;
import org.jungrapht.visualization.layout.algorithms.repulsion.BarnesHutSpringRepulsion;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Layering;
import org.jungrapht.visualization.layout.algorithms.util.EdgeShapeFunctionSupplier;
import org.jungrapht.visualization.util.Context;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    private static final Pattern QUOTES = Pattern.compile("\"(.*?)\"\\s+\"(.*?)\"\\s+");
    private static final Pattern NO_QUOTES = Pattern.compile("(.*?)\\s+(.*)");

    private static class GraphAndForest {

        private final ListenableGraph<String, String> graph;
        private final Graph<String, String> forest;

        public GraphAndForest(ListenableGraph<String, String> graph, Graph<String, String> forest) {
            this.graph = graph;
            this.forest = forest;
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }


        final JFrame jf = new JFrame("Visual Library + JUNG Demo");
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        GraphAndForest gf = loadGraph(args);

        LayoutAlgorithm<String> layout;

        try {
            layout = gf.forest == null ? new KKLayoutAlgorithm<>() : new TreeLayoutAlgorithm<>();
        } catch (IllegalArgumentException ex) {
            layout = new KKLayoutAlgorithm<>();
        }
        final BaseJungScene scene = new SceneImpl(gf.graph, layout);
        jf.setLayout(new BorderLayout());
        jf.add(new JScrollPane(scene.createView()), BorderLayout.CENTER);

        JToolBar bar = new JToolBar();
        bar.setMargin(new Insets(5, 5, 5, 5));
        bar.setLayout(new FlowLayout(5));
        DefaultComboBoxModel<LayoutAlgorithm> mdl = new DefaultComboBoxModel<>();
        mdl.addElement(new KKLayoutAlgorithm());
        mdl.addElement(layout);
        if (gf.forest != null) {
            mdl.addElement(new BalloonLayoutAlgorithm());
            mdl.addElement(new RadialTreeLayoutAlgorithm());
        }
        mdl.addElement(TidierTreeLayoutAlgorithm.edgeAwareBuilder()
                .vertexShapeFunction(v -> new Ellipse2D.Double(-10, -10, 20, 20)).build());
        mdl.addElement(TidierRadialTreeLayoutAlgorithm.edgeAwareBuilder()
                .vertexShapeFunction(v -> new Ellipse2D.Double(-10, -10, 20, 20)).build());
        mdl.addElement(new CircleLayoutAlgorithm());
        mdl.addElement(FRLayoutAlgorithm.builder().repulsionContractBuilder(BarnesHutFRRepulsion.barnesHutBuilder()).build());
        mdl.addElement(new ISOMLayoutAlgorithm());
        mdl.addElement(SpringLayoutAlgorithm.builder().repulsionContractBuilder(BarnesHutSpringRepulsion.barnesHutBuilder()).build());
        mdl.addElement(new DAGLayoutAlgorithm());
        mdl.addElement(GEMLayoutAlgorithm.edgeAwareBuilder().build());
        mdl.addElement(ForceAtlas2LayoutAlgorithm.builder()
                .repulsionContractBuilder(BarnesHutFA2Repulsion.builder().repulsionK(300))
        .build());
        mdl.addElement(EiglspergerLayoutAlgorithm.edgeAwareBuilder()
                .edgeShapeFunctionConsumer(context -> scene.setConnectionEdgeShape(context))
                .layering(Layering.COFFMAN_GRAHAM)
                .build());

        mdl.setSelectedItem(layout);
        final JCheckBox checkbox = new JCheckBox("Animate iterative layouts");

        scene.setLayoutAnimationFramesPerSecond(48);

        final JComboBox<LayoutAlgorithm> layouts = new JComboBox(mdl);
        layouts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1) {
                o = o.getClass().getSimpleName();
                return super.getListCellRendererComponent(jlist, o, i, bln, bln1); //To change body of generated methods, choose Tools | Templates.
            }
        });
        bar.add(new JLabel(" Layout Type"));
        bar.add(layouts);

        bar.add(new JLabel(" Connection Shape"));
        DefaultComboBoxModel<Function<Context<Graph<String, String>, String>, Shape>> shapes = new DefaultComboBoxModel<>();
        shapes.addElement(new EdgeShape.QuadCurve<String, String>());
        shapes.addElement(new EdgeShape.CubicCurve<String, String>());
        shapes.addElement(new EdgeShape.Line<String, String>());
        shapes.addElement(new EdgeShape.Box<String, String>());
        shapes.addElement(new EdgeShape.ArticulatedLine<>());

        final JComboBox<Function<Context<Graph<String, String>, String>, Shape>> shapesBox = new JComboBox<>(shapes);
        shapesBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                Function<Context<Graph<String, String>, String>, Shape> xform =
                        (Function<Context<Graph<String, String>, String>, Shape>) shapesBox.getSelectedItem();
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


        layouts.addActionListener(ae -> {
            LayoutAlgorithm layout1 = (LayoutAlgorithm) layouts.getSelectedItem();
            if (!(layout1 instanceof EdgeShapeFunctionSupplier)) {
                Function<Context<Graph<String, String>, String>, Shape> xform =
                        (Function<Context<Graph<String, String>, String>, Shape>) shapesBox.getSelectedItem();
                scene.setConnectionEdgeShape( xform);
            }
            // These two layouts implement IterativeContext, but they do
            // not evolve toward anything, they just randomly rearrange
            // themselves.  So disable animation for these.
            if (layout1 instanceof ISOMLayoutAlgorithm || layout1 instanceof DAGLayoutAlgorithm) {
                checkbox.setSelected(false);
            }
            scene.setGraphLayout(layout1, true);
        });

        jf.add(bar, BorderLayout.NORTH);
        bar.add(new MinSizePanel(scene.createSatelliteView()));
        bar.setFloatable(false);
        bar.setRollover(true);

        final JLabel selectionLabel = new JLabel("<html>&nbsp;</html>");
        Lookup.Result<String> selectedNodes = scene.getLookup().lookupResult(String.class);
        selectedNodes.allInstances();
        LookupListener listener = new LookupListener() {
            @Override
            public void resultChanged(LookupEvent le) {
                Lookup.Result<String> res = (Lookup.Result<String>) le.getSource();
                StringBuilder sb = new StringBuilder("<html>");
                List<String> l = new ArrayList<>(res.allInstances());
                Collections.sort(l);
                for (String s : l) {
                    if (sb.length() != 6) {
                        sb.append(", ");
                    }
                    sb.append(s);
                }
                sb.append("</html>");
                selectionLabel.setText(sb.toString());
            }
        };
        selectionLabel.putClientProperty("ll", listener); // ensure it's not garbage collected
        selectionLabel.putClientProperty("lr", selectedNodes); // ensure it's not garbage collected
        selectedNodes.addLookupListener(listener);
        selectedNodes.allInstances();


        checkbox.setSelected(true);
        checkbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                scene.setAnimateIterativeLayouts(checkbox.isSelected());
            }
        });
        bar.add(checkbox);
        bar.add(selectionLabel);
        selectionLabel.setHorizontalAlignment(SwingConstants.TRAILING);

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

    private static String[] elements(String line, String[] into) {
        if (line.startsWith("#")) {
            return null;
        }
        Matcher m = QUOTES.matcher(line);
        if (m.find()) {
            into[0] = m.group(1);
            into[1] = m.group(2);
            return into;
        } else {
            m = NO_QUOTES.matcher(line);
            if (m.find()) {
                into[0] = m.group(1);
                into[1] = m.group(2);
                return into;
            }
        }
        return null;
    }

    private static GraphAndForest loadGraph(String[] args) throws FileNotFoundException, IOException {
        if (args.length > 0) {
            File f = new File(args[0]);
            if (!f.exists() || !f.isFile() || !f.canRead()) {
                System.err.println("File does not exist, is not readable, or is not a file: " + f);
                System.exit(1);
            }
            try {
                Graph<String, String> forest = GraphTypeBuilder.<String,String>directed()
                        .buildGraph();
                String[] arr = new String[2];
                Set<String> pairs = new HashSet<>();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    int ix = 0;
                    for (String line; (line = br.readLine()) != null;) {
                        line = line.trim();
                        String[] items = elements(line, arr);
                        if (items != null) {
                            if (items[0].equals(items[1])) {
                                continue;
                            }
                            String[] x = new String[]{items[0], items[1]};
                            Arrays.sort(x);
                            String key = x[0] + "::" + x[1];
                            if (!pairs.contains(key)) {
                                String edge = Integer.toString(ix);
                                forest.addEdge(items[0], items[1], edge);
                                pairs.add(key);
                            } else {
                                System.out.println("DUP: " + key);
                            }
                        }
                        ix++;
                    }
                }
                ListenableGraph<String, String> g = new DefaultListenableGraph<>(forest);
                return new GraphAndForest(g, forest);
            } catch (Exception e) {
                // Graph has cycles - try undirected
                e.printStackTrace();
                Graph<String, String> graph = GraphTypeBuilder.<String,String>undirected()
                        .allowingSelfLoops(true)
                        .allowingMultipleEdges(true).buildGraph();

                String[] arr = new String[2];
                Set<String> pairs = new HashSet<>();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    int ix = 0;
                    for (String line; (line = br.readLine()) != null;) {
                        line = line.trim();
                        String[] items = elements(line, arr);
                        if (items != null) {
                            if (items[0].equals(items[1])) {
                                continue;
                            }
                            String edge = Integer.toString(ix);
                            try {
                                String[] x = new String[]{items[0], items[1]};
                                Arrays.sort(x);
                                String key = x[0] + "::" + x[1];
                                if (!pairs.contains(key)) {
                                    graph.addEdge(items[0], items[1], edge);
                                    pairs.add(key);
                                } else {
                                    System.out.println("DUP: " + key);
                                }
                            } catch (IllegalArgumentException ex) {
                                System.err.println(ex.getMessage());
                            }
                        }
                        ix++;
                    }
                }
                ListenableGraph<String, String> g = (ListenableGraph<String,String>)graph;
                return new GraphAndForest(g, null);
            }
        } else {
            Graph<String, String> forest = GraphTypeBuilder.<String,String>directed()
                    .edgeSupplier(BalloonLayoutDemo.edgeFactory).buildGraph();

            ListenableGraph<String, String> g = new DefaultListenableGraph(new BalloonLayoutDemo().createTree(forest));
            return new GraphAndForest(g, forest);
        }
    }
}
