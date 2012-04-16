/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.dpgmm.utilities;

import java.awt.geom.Point2D;

/**
 *
 * @author remonet
 */
public interface RenderableEventListener {
    
    public void mousePressed(Point2D atDesign, Point2D atScreen);
    public void genericEventFired(String name, Object... params);

    public static class Abstract implements RenderableEventListener {
        @Override
        public void mousePressed(Point2D atDesign, Point2D atScreen) {
        }
        @Override
        public void genericEventFired(String name, Object... params) {
        }
    }
}
