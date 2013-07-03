/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.timboudreau.vl.jung.extensions;

import java.awt.Color;

/**
 *
 * @author Tim Boudreau
 */
public interface GraphTheme {

    Color getBackground(States... states);

    Color getEdgeColor(States... states);

    Color getForeground(States... states);

}
