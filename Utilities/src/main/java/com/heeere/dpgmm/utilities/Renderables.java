/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.utilities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author remonet
 */
public class Renderables {

    public static Renderable autoscale(final Image im, final Float maxScaleUp) {
        return new Renderable.Abstract() {

            public void render(Graphics2D g, int width, int height) {
                float scaleUp = Math.min(width / (float) im.getWidth(null), height / (float) im.getHeight(null));
                if (maxScaleUp != null) {
                    scaleUp = Math.min(scaleUp, maxScaleUp);
                }
                AffineTransform t = g.getTransform();
                g.scale(scaleUp, scaleUp);
                g.drawImage(im, null, null);
                g.setTransform(t);
            }
        };
    }

    public static Renderable renderable(final Image im, final float scaleUp) {
        return new Renderable.Abstract() {

            public void render(Graphics2D g, int width, int height) {
                AffineTransform t = g.getTransform();
                g.scale(scaleUp, scaleUp);
                g.drawImage(im, null, null);
                g.setTransform(t);;
            }
        };
    }

    public static Renderable renderable(final Image im) {
        return new Renderable.Abstract() {

            public void render(Graphics2D g, int width, int height) {
                g.drawImage(im, null, null);
            }
        };
    }

    public static Renderable displayStackedRenderables(final String name, final Renderable... renderables) {
        return displayStackedRenderables(name, null, null, false, renderables);
    }

    public static Renderable layered(final boolean firstIsOnTop, final Renderable... renderables) {
        return new Renderable.Abstract() {

            public void render(Graphics2D g, int width, int height) {
                for (int ir = 0; ir < renderables.length; ir++) {
                    int index = firstIsOnTop ? renderables.length - 1 - ir : ir;
                    Graphics2D gm = (Graphics2D) g.create();
                    renderables[index].render(gm, width, height);
                    gm.dispose();
                }
            }
        };

    }

    public static Renderable displayStackedRenderables(final String name, Integer marginInPixelsParam, Color separatorColorParam, final boolean isVertical, final Renderable... renderables) {
        final Integer marginInPixels = defaultsTo(marginInPixelsParam, 3);
        final Color separatorColor = defaultsTo(separatorColorParam, Color.RED);
        return new Renderable.Abstract() {

            {
                name(name);
            }

            public void render(Graphics2D g, int width, int height) {
                int count = renderables.length;
                int dimOfInterest = isVertical ? height : width;
                int elementSize = (dimOfInterest - (count - 1) * marginInPixels) / count;
                int ver = isVertical ? 1 : 0;
                int hor = 1 - ver;
                for (int ir = 0; ir < renderables.length; ir++) {
                    Graphics2D gm = (Graphics2D) g.create();
                    gm.translate(hor * ir * (elementSize + marginInPixels), ver * ir * (elementSize + marginInPixels));
                    gm.setColor(separatorColor);
                    gm.fill(new Rectangle2D.Float(hor * -marginInPixels, ver * -marginInPixels, ver * width + hor * marginInPixels, ver * marginInPixels + hor * height));
                    renderables[ir].render(gm, ver * width + hor * elementSize, ver * elementSize + hor * height);
                    gm.dispose();
                }
            }
        };
    }

    private static <T> T defaultsTo(T v, T def) {
        return v == null ? def : v;
    }
}
