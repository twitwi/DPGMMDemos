/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.dpgmm.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author remonet
 */
public class RenderableStackViewer extends JFrameWrapper {

    @Override
    public JFrameWrapper title(String title) {
        setBaseTitle(title);
        return this;
    }

    public RenderableStackViewer() {
        f.setLayout(new BorderLayout());
        slots = new RenderDelta[1];
        contentSlider = new JSlider(JSlider.VERTICAL);
        f.getContentPane().add(contentSlider, BorderLayout.BEFORE_LINE_BEGINS);
        for (int i = 0; i < slots.length; i++) {
            final int fI = i;
            slots[i] = new RenderDelta(i);
            slots[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point2D originalAt = e.getPoint();
                    Point2D at = content.get(slots[fI].index + contentSlider.getValue()).clickPosition(originalAt);
                    fireRenderableClicked(fI, at, originalAt);
                }

            });
            slots[i].addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getWheelRotation() < 0) {
                        switchToIndex(Math.min(contentSlider.getValue() + 1, contentSlider.getMaximum() - 1));
                    } else {
                        switchToIndex(Math.max(contentSlider.getValue() - 1, contentSlider.getMinimum()));
                    }
                }
            });
        }
        f.getContentPane().add(slots[0], BorderLayout.CENTER);
        contentSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                switchToIndex(contentSlider.getValue());
            }
        });
        updateSliderRange();
        contentSlider.requestFocusInWindow();
    }
    
    public void addRenderable(Renderable renderable) {
        content.add(renderable);
        updateSliderRange();
    }

    public void replaceRenderable(int i, Renderable renderable) {
        content.set(i, renderable);
        updateSliderRange();
    }



    /**
     * 
     * @deprecated use Renderables# instead
     */
    @Deprecated
    public static Renderable renderable(final Image im, final float scaleUp) {
        return Renderables.renderable(im, scaleUp);
    }
    /**
     * 
     * @deprecated use Renderables# instead
     */
    @Deprecated
    public static Renderable renderable(final Image im) {
        return Renderables.renderable(im);
    }





    List<Renderable> content = new ArrayList<Renderable>();

    JSlider contentSlider;
    RenderDelta[] slots;
    String baseTitle = "";

    private void setBaseTitle(String title) {
        this.baseTitle = title;
        String contentTitle = " ";
        if (contentSlider.isEnabled()) {
            contentTitle += content.get(contentSlider.getValue()).getName();
        }
        f.setTitle(baseTitle + contentTitle);
    }

    private void switchToIndex(int value) {
        contentSlider.setValue(value);
        setBaseTitle(baseTitle);
        for (int i = 0; i < slots.length; i++) {
            slots[i].reload();
        }
    }

    private void updateSliderRange() {
        if (content.isEmpty()) {
            contentSlider.setEnabled(false);
            return;
        }
        int value = contentSlider.getValue();
        int max = content.size() - 1;
        value = Math.min(value, max);
        contentSlider.getModel().setRangeProperties(value, 1, 0, max+1, false);
        switchToIndex(value);
        contentSlider.setEnabled(true);
    }

    public static interface ClickListener {
        void renderableClicked(int imageIndex, Point2D at, Point2D originalAt);
    }

    private List<ClickListener> clickListeners = new ArrayList<ClickListener>();
    public void addClickListener(ClickListener l) {
        clickListeners.add(l);
    }

    public void clearClickListeners() {
        clickListeners.clear();
    }
    private void fireRenderableClicked(int index, Point2D at, Point2D originalAt) {
        for (ClickListener clickListener : new ArrayList<ClickListener>(clickListeners)) {
            clickListener.renderableClicked(index, at, originalAt);
        }
    }

    private class RenderDelta extends JPanel {

        int index;

        public RenderDelta() {
            setBorder(new LineBorder(Color.RED, 10));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 800);
        }
        
        public RenderDelta(int i) {
            this.index = i;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!contentSlider.isEnabled() || index + contentSlider.getValue() >= contentSlider.getMaximum()) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                return;
            }
            g.setColor(Color.DARK_GRAY.darker().darker());
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2 = (Graphics2D) g.create();
            content.get(index + contentSlider.getValue()).render(g2, getWidth(), getHeight());
        }

        private void reload() {
            repaint();
        }

    }


    
    
}
