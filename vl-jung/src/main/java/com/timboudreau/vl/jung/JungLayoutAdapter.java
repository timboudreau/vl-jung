/*
 * Copyright (c) 2018, Tim Boudreau
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
package com.timboudreau.vl.jung;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.Graph;
import java.util.function.Consumer;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.graph.GraphPinScene;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.layout.SceneLayout;

/**
 *
 * @author Tim Boudreau
 */
public interface JungLayoutAdapter {

    MoveProvider moveProvider();

    MoveProvider moveProvider(MoveProvider delegate);

    void setGraphLayout(Layout layout, boolean animate);

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Layout<N, E> layout, NodeMovedNotifier notifier, Consumer<AnimationController> controllerRececiver) {
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Layout<N, E> layout, NodeMovedNotifier notifier, AnimationController animator) {
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Layout<N, E> layout, AnimationController animator) {
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Layout<N, E> layout, Consumer<AnimationController> controllerRececiver) {
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Layout<N, E> layout, NodeMovedNotifier notifier, Consumer<AnimationController> controllerRececiver) {
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Layout<N, E> layout, NodeMovedNotifier notifier, AnimationController animator) {
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Layout<N, E> layout, AnimationController animator) {
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Layout<N, E> layout, Consumer<AnimationController> controllerRececiver) {
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }


    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Graph<N, E> graph, NodeMovedNotifier notifier, Consumer<AnimationController> controllerRececiver) {
        Layout<N,E> layout = new SpringLayout(graph);
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Graph<N, E> graph, NodeMovedNotifier notifier, AnimationController animator) {
        Layout<N,E> layout = new SpringLayout(graph);
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Graph<N, E> graph, AnimationController animator) {
        Layout<N,E> layout = new SpringLayout(graph);
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        return adapter;
    }

    public static <N, E> SceneLayout create(GraphScene<N, E> scene, Graph<N, E> graph, Consumer<AnimationController> controllerRececiver) {
        Layout<N,E> layout = new SpringLayout(graph);
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Graph<N, E> graph, NodeMovedNotifier notifier, Consumer<AnimationController> controllerRececiver) {
        Layout<N,E> layout = new SpringLayout(graph);
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Graph<N, E> graph, NodeMovedNotifier notifier, AnimationController animator) {
        Layout<N,E> layout = new SpringLayout(graph);
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, notifier);
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Graph<N, E> graph, AnimationController animator) {
        Layout<N,E> layout = new SpringLayout(graph);
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        return adapter;
    }

    public static <N, E, P> SceneLayout create(GraphPinScene<N, E, P> scene, Graph<N, E> graph, Consumer<AnimationController> controllerRececiver) {
        Layout<N,E> layout = new SpringLayout(graph);
        AnimationController animator = new AnimationController();
        LayoutAdapter<N, E> adapter = new LayoutAdapter<>(scene, layout, animator, null);
        if (controllerRececiver != null) {
            controllerRececiver.accept(animator);
        }
        return adapter;
    }
}
