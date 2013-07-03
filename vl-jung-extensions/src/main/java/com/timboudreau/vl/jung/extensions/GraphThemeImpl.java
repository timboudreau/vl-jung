package com.timboudreau.vl.jung.extensions;

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
        this(new Color(80, 90, 220), new Color(130, 140, 230));
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
