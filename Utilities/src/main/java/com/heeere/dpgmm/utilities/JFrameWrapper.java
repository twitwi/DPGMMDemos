/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.dpgmm.utilities;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author remonet
 */
public class JFrameWrapper {

    public static JFrameWrapper create() {
        return new JFrameWrapper(new JFrame());
    }
    public static JFrameWrapper wrap(JFrame f) {
        return new JFrameWrapper(f);
    }

    protected JFrame f;

    protected JFrameWrapper() {
        this(new JFrame());
    }
    private JFrameWrapper(JFrame jFrame) {
        this.f = jFrame;
    }
    public JFrameWrapper content(JComponent content) {
        f.getContentPane().removeAll();
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(content);
        f.pack();
        return this;
    }
    public JFrameWrapper exitOnClose() {
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return this;
    }
    public JFrameWrapper nothingOnClose() {
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        return this;
    }
    public JFrameWrapper disposeOnClose() {
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return this;
    }
    public JFrameWrapper show() {
        if (SwingUtilities.isEventDispatchThread()) {
            f.pack();
            f.setVisible(true);
            return this;
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        show();
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(JFrameWrapper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(JFrameWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return this;
        }
    }
    public JFrameWrapper showWithoutPacking() {
        f.setVisible(true);
        return this;
    }

    public JFrameWrapper title(String title) {
        f.setTitle(title);
        return this;
    }

    public JFrame accessFrame() {
        return f;
    }

}
