package com.timboudreau.vl.jung.extensions;

/**
 *
 * @author Tim Boudreau
 */
public enum States {
    SELECTED, HOVERED, CONNECTED_TO_SELECTION, INDIRECTLY_CONNECTED_TO_SELECTION, UNRELATED_TO_SELECTION;

    private float clamp(float flt) {
        return Math.max(0F, Math.min(1.0F, flt));
    }

    private float rotate(float flt, float adj, float dir) {
        adj *= dir;
        flt += adj;
        if (flt < 0F) {
            flt = 1F - flt;
        } else if (flt > 1F) {
            flt -= 1F;
        }
        return clamp(flt);
    }

    void adjust(float dir, float[] hsb) {
        switch (this) {
            case SELECTED:
                hsb[1] = dir >= 1 ? 0.75F : 0.25F;
                break;
            case HOVERED:
                hsb[0] = rotate(hsb[0], 0.10F, -dir);
                hsb[1] = clamp(hsb[1] + 0.1F);
                break;
            case CONNECTED_TO_SELECTION:
                hsb[0] = rotate(hsb[0], 0.05F, dir);
                hsb[1] = 0.35F;
                hsb[2] = clamp(hsb[2] + (-dir * 0.25F));
                break;
            case INDIRECTLY_CONNECTED_TO_SELECTION:
                hsb[0] = rotate(hsb[0], 0.05F, dir);
                hsb[1] = 0.25F;
                hsb[2] = clamp(hsb[2] + (dir * 0.1F));
                break;
            case UNRELATED_TO_SELECTION:
                hsb[1] = 0.1F;
                hsb[2] = clamp(hsb[2] + 0.25F);
                break;
        }
    }
    
}
