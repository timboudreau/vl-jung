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
package com.timboudreau.vl.jungrapht.extensions;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Simple implementation of GraphTheme which uses base colors and modifies
 * the hue, saturation and brightness to derive theme colors.
 *
 * @author Tim Boudreau
 */
public class GraphThemeImpl implements GraphTheme {

    private final Color bgBase;
    private final Color fgBase;

    public GraphThemeImpl() {
        this(new Color(80, 110, 240), new Color(130, 140, 230));
    }

    public GraphThemeImpl(Color bgBase) {
        this.bgBase = bgBase;
        this.fgBase = new Color(
                255 - bgBase.getRed(),
                255 - bgBase.getGreen(),
                255 - bgBase.getBlue());
    }

    public GraphThemeImpl(Color bgBase, Color fgBase) {
        this.bgBase = bgBase;
        this.fgBase = fgBase;
    }

    @Override
    public Color getBackground(States... states) {
        List<States> l = Arrays.asList(states);
        if (l.size() == 2 && l.contains(States.UNRELATED_TO_SELECTION)
                && l.contains(States.HOVERED)) {
            return Color.ORANGE.darker();
        }
        if (states.length == 0) {
            return Color.GRAY;
        }
        return adjust(bgBase, 1, states);
    }

    @Override
    public Color getForeground(States... states) {
        List<States> l = Arrays.asList(states);
        if (l.size() == 2 && l.contains(States.UNRELATED_TO_SELECTION)
                && l.contains(States.HOVERED)) {
            return Color.BLACK;
        }
        return adjust(fgBase, -1, states);
    }

    @Override
    public Color getEdgeColor(States... states) {
        List<States> l = Arrays.asList(states);
        if (l.size() == 2 && l.contains(States.UNRELATED_TO_SELECTION)
                && l.contains(States.HOVERED)) {
            return new Color(240, 240, 120);
        }
        return adjust(fgBase, 1, states);
    }

    private Color adjust(Color color, int direction, States... states) {
        float dir = direction;
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        if (states.length == 0) {
            hsb[1] = 0.1F;
        } else {
            for (States s : states) {
                s.adjust(dir, hsb);
            }
        }
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }
}
