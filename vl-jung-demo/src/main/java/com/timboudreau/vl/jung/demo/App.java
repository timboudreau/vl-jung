package com.timboudreau.vl.jung.demo;

import com.timboudreau.vl.jung.extensions.SimpleJungScene;
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
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Context;
import static edu.uci.ics.jung.graph.util.TestGraphs.pairs;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
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
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Exceptions;

public class App {

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

    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
        final JFrame jf = new JFrame("Demo");
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        ObservableGraph g = new ObservableGraph(getDemoGraph());
//        ObservableGraph g = new ObservableGraph(createDirectedAcyclicGraph(2, 9, 0.75D));
        Forest<String, Integer> forest = new DelegateForest<>();
        ObservableGraph g = new ObservableGraph(new BalloonLayoutDemo().createTree(forest));

        Layout layout = new TreeLayout(forest, 70, 70);
        final SimpleJungScene scene = new SceneImpl(g, layout);
        jf.setLayout(new BorderLayout());
        jf.add(new JScrollPane(scene.createView()), BorderLayout.CENTER);

        JToolBar bar = new JToolBar();
        bar.setMargin(new Insets(5,5,5,5));
        bar.setLayout(new FlowLayout(5));
        bar.add(new AbstractAction("Layout") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                scene.relayout();
                scene.validate();
                scene.repaint();
            }
        });
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
                scene.setGraphLayout((Layout) layouts.getSelectedItem());
            }
        });

        bar.add(new JLabel(" Connection Shape"));
        DefaultComboBoxModel<Transformer<Context<Graph<String, Number>, Number>, Shape>> shapes = new DefaultComboBoxModel<>();
        final JComboBox<Transformer<Context<Graph<String, Number>, Number>, Shape>> shapesBox = new JComboBox<>(shapes);
        shapes.addElement(new EdgeShape.QuadCurve<String, Number>());
        shapes.addElement(new EdgeShape.BentLine<String, Number>());
        shapes.addElement(new EdgeShape.CubicCurve<String, Number>());
        shapes.addElement(new EdgeShape.Line<String, Number>());
        shapes.addElement(new EdgeShape.Box<String, Number>());
        shapes.addElement(new EdgeShape.Orthogonal<String, Number>());
        shapes.addElement(new EdgeShape.Wedge<String, Number>(10));

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

//        final JDialog dlg = new JDialog(jf);
//        dlg.setLocationRelativeTo(jf);
//        dlg.setContentPane(scene.createSatelliteView());
        bar.add(new MinSizePanel(scene.createSatelliteView()));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        jf.setSize(jf.getGraphicsConfiguration().getBounds().width - 120, 700);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent we) {
                scene.relayout();
                scene.validate();
//                dlg.setLocationRelativeTo(jf);
//                dlg.pack();
//                Rectangle r = jf.getBounds();
//                dlg.setLocation(new Point(r.x + r.width, r.y));
//                dlg.setVisible(true);
            }
        });
        jf.setVisible(true);
    }

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
    
    private static class SceneImpl extends SimpleJungScene<String,Number> {

        public SceneImpl(ObservableGraph<String, Number> graph, Layout layout) throws IOException {
            super(graph, layout);
        }

        @Override
        protected Widget createNodeWidget(String node) {
            DemoWidget<String,Number> w = new DemoWidget<String,Number>(this);
            w.setLabel(node + "");
            w.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return w;
        }
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
}
