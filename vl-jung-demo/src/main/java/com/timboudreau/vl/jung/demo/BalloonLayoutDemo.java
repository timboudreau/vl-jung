/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package com.timboudreau.vl.jung.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.transform.LensSupport;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformerDecorator;
import edu.uci.ics.jung.visualization.transform.shape.HyperbolicShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.ViewLensSupport;
import edu.uci.ics.jung.visualization.util.Animator;

/**
 * Demonstrates the visualization of a Tree using TreeLayout
 * and BalloonLayout. An examiner lens performing a hyperbolic
 * transformation of the view is also included.
 * 
 * @author Tom Nelson
 * 
 */
@SuppressWarnings("serial")
public class BalloonLayoutDemo extends JApplet {

	Factory<DirectedGraph<String,Integer>> graphFactory = 
		new Factory<DirectedGraph<String,Integer>>() {

		public DirectedGraph<String, Integer> create() {
			return new DirectedSparseMultigraph<String,Integer>();
		}
	};

	Factory<Tree<String,Integer>> treeFactory =
		new Factory<Tree<String,Integer>> () {

		public Tree<String, Integer> create() {
			return new DelegateTree<String,Integer>(graphFactory);
		}
	};

	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i=0;
		public Integer create() {
			return i++;
		}};
    
    Factory<String> vertexFactory = new Factory<String>() {
    	int i=0;
		public String create() {
			return "V"+i++;
		}};
    
    Forest<String,Integer> createTree(Forest<String,Integer> graph) {
    	
       	graph.addVertex("A0");
       	graph.addEdge(edgeFactory.create(), "A0", "B0");
       	graph.addEdge(edgeFactory.create(), "A0", "B1");
       	graph.addEdge(edgeFactory.create(), "A0", "B2");
       	
       	graph.addEdge(edgeFactory.create(), "B0", "C0");
       	graph.addEdge(edgeFactory.create(), "B0", "C1");
       	graph.addEdge(edgeFactory.create(), "B0", "C2");
       	graph.addEdge(edgeFactory.create(), "B0", "C3");

       	graph.addEdge(edgeFactory.create(), "C2", "H0");
       	graph.addEdge(edgeFactory.create(), "C2", "H1");

       	graph.addEdge(edgeFactory.create(), "B1", "D0");
       	graph.addEdge(edgeFactory.create(), "B1", "D1");
       	graph.addEdge(edgeFactory.create(), "B1", "D2");

       	graph.addEdge(edgeFactory.create(), "B2", "E0");
       	graph.addEdge(edgeFactory.create(), "B2", "E1");
       	graph.addEdge(edgeFactory.create(), "B2", "E2");

       	graph.addEdge(edgeFactory.create(), "D0", "F0");
       	graph.addEdge(edgeFactory.create(), "D0", "F1");
       	graph.addEdge(edgeFactory.create(), "D0", "F2");
       	
       	graph.addEdge(edgeFactory.create(), "D1", "G0");
       	graph.addEdge(edgeFactory.create(), "D1", "G1");
       	graph.addEdge(edgeFactory.create(), "D1", "G2");
       	graph.addEdge(edgeFactory.create(), "D1", "G3");
       	graph.addEdge(edgeFactory.create(), "D1", "G4");
       	graph.addEdge(edgeFactory.create(), "D1", "G5");
       	graph.addEdge(edgeFactory.create(), "D1", "G6");
       	graph.addEdge(edgeFactory.create(), "D1", "G7");
        
       	graph.addEdge(edgeFactory.create(), "A0", "HA1");
       	graph.addEdge(edgeFactory.create(), "A0", "HA2");
       	graph.addEdge(edgeFactory.create(), "A0", "HA3");
        
       	graph.addEdge(edgeFactory.create(), "HA3", "I1");
       	graph.addEdge(edgeFactory.create(), "HA3", "I2");
        
       	graph.addEdge(edgeFactory.create(), "I2", "J1");
        
        
       	
       	// uncomment this to make it a Forest:
       	graph.addVertex("K0");
       	graph.addEdge(edgeFactory.create(), "K0", "K1");
       	graph.addEdge(edgeFactory.create(), "K0", "K2");
       	graph.addEdge(edgeFactory.create(), "K0", "K3");
       	
       	graph.addVertex("J0");
    	graph.addEdge(edgeFactory.create(), "J0", "J1");
    	graph.addEdge(edgeFactory.create(), "J0", "J2");
    	graph.addEdge(edgeFactory.create(), "J1", "J4");
    	graph.addEdge(edgeFactory.create(), "J2", "J3");
        
    	graph.addEdge(edgeFactory.create(), "J2", "J5");
    	graph.addEdge(edgeFactory.create(), "J4", "J6");
    	graph.addEdge(edgeFactory.create(), "J4", "J7");
    	graph.addEdge(edgeFactory.create(), "J3", "J8");
    	graph.addEdge(edgeFactory.create(), "J6", "B9");

       	return graph;
    }

    /**
     * a driver for this demo
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content.add(new BalloonLayoutDemo());
        frame.pack();
        frame.setVisible(true);
    }
}
