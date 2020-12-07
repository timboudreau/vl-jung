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
