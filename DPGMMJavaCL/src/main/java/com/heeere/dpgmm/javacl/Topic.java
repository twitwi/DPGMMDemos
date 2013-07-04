/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A gaussian.
 *
 * @author twilight
 */
public class Topic implements Cloneable {

    double[] mean;
    double[] diagStddev;

    public Topic(double[] mean, double[] diagStddev) {
        this.mean = mean;
        this.diagStddev = diagStddev;
    }
    
    Random r = new Random();

    public void drawInto(double[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = mean[i]  + r.nextGaussian() * diagStddev[i];
        }
    }

    public Topic getCopy() {
        try {
            return (Topic) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Topic.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
