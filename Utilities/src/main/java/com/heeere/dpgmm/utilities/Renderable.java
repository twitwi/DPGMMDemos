/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.dpgmm.utilities;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author remonet
 */
public interface Renderable {

    public void render(Graphics2D g, int width, int height);

    public String getName();
    public Renderable name(String newName);

    public Renderable addRenderableEventListener(RenderableEventListener l);
    public Renderable removeRenderableEventListener(RenderableEventListener l);

    public Point2D clickPosition(Point2D originalAt);

    public static abstract class Abstract implements Renderable {
        private String name; // private to avoid name clashes
        private List<RenderableEventListener> listeners = new ArrayList<RenderableEventListener>(); // same here
        // add protected accessors if you access to these fields (you already have them for name)

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Renderable name(String newName) {
            this.name = newName;
            return this;
        }

        @Override
        public Renderable addRenderableEventListener(RenderableEventListener l) {
            listeners.add(l);
            return this;
        }

        @Override
        public Renderable removeRenderableEventListener(RenderableEventListener l) {
            listeners.remove(l);
            return this;
        }

        public Point2D clickPosition(Point2D originalAt) {
            return (Point2D) originalAt.clone();
        }

    }
}
