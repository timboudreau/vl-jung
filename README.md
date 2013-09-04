JUNG + NetBeans' Visual Library Integration
===========================================

This project integrates [JUNG](http://jung.sourceforge.net/) - a library for drawing edge/node graphs, and [Visual Library](http://graph.netbeans.org/) - a component library which is good at layered animated widgets which can host Swing components.

The result gains new features for users of either library.  Visual Library users get a collection of good, usable graph layouts backed by research in graph theory.  JUNG users get interactivity - such as animation, using real UI components to represent graph contents, and the ability to incorporate non-graph elements into a graph UI to provide a richer user experience.

Watch [this video](http://timboudreau.com/blog/NetBeans_Visual_Library_Meets_JUNG/read) for a demo and overview of the project.

Visual Library is a standalone library which is part of NetBeans but can be used in standalone Java applications.  You don't have to be writing a NetBeans plugin to use this library!

Builds and a Maven repository containing the JARs can be found [on timboudreau.com](http://timboudreau.com/builds/job/vl-jung).  [Read the Javadoc]([Javadoc here](http://timboudreau.com/builds/job/vl-jung/lastSuccessfulBuild/artifact/vl-jung/target/site/apidocs/index.html) and also [here](http://timboudreau.com/builds/job/vl-jung/lastSuccessfulBuild/artifact/vl-jung-extensions/target/site/apidocs/index.html) (there are two libraries).

Features
--------

  * ``JungScene`` &mdash; provides an abstract Visual Library scene implementation which wrappers a JUNG layout as a visual library ``SceneLayout``
  * ``JungConnectionWidget`` &mdash; embeds JUNG's shape-based edge painting logic in a visual library widget
  * ``BaseJungScene`` &mdash; provides commonly used functionality - manages layer widgets and adding and removing of widgets as the graph is modified


Project Layout
--------------

The project consists of a Maven parent project with four child projects:

  * _Visual Library + Jung_ &mdash; does the basic integration of JUNG with Visual Library and provides ``JungScene`` and ``JungConnectionWidget``
    * Supports JUNG's animated layouts either using a timer or by pre-rolling
    * Wrappers any JUNG layout in a Visual Library ``SceneLayout`` so that it can be used to lay out a scene with actual widgets
  * _Visual Library Jung Base Classes_ &mdash; adds in convenience classes such as ``BaseJungScene`` and its supporting cast
  * _Visual Library + Jung Demo_ &mdash; is a standalone (non-NetBeans) demo application
  * _Visual Library + JUNG NetBeans Module Wrapper_ &mdash; is a NetBeans [Library Wrapper Module](http://wiki.netbeans.org/DevFaqWrapperModules) which embeds JUNG and these libraries and exposes their packages as its public API (NetBeans modules use classloader partitioning to restrict package access).


Build & Run
-----------

The easy way to use these libraries is via Maven, using [this Maven repository](http://timboudreau.com/builds) - follow the link for a ``&lt;repository&gt;`` section to add to your POM.

To build the projects, simply check the sources out from Git and build with Maven using JDK 7 or greater.


License
-------

BSD 2-clause license for compatibility with JUNG (and most anything else).


More Info
------------
  * [NetBeans' Visual Library](http://graph.netbeans.org) 
  * [JUNG - Java Universal Graph framework](http://jung.sourceforge.net)
  * [Demo video on YouTube](http://www.youtube.com/watch?v=pdkxnDZRJLM)
  * [Builds + Maven repo](http://timboudreau.com/builds/job/vl-jung)
  * [Javadoc](http://timboudreau.com/builds/job/vl-jung/lastSuccessfulBuild/artifact/vl-jung/target/site/apidocs/index.html) and [here](http://timboudreau.com/builds/job/vl-jung/lastSuccessfulBuild/artifact/vl-jung-extensions/target/site/apidocs/index.html)

