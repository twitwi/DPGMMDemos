/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.java;

/**
 *
 * For one document.
 * There are #getSize observations in the document.
 * Each has the same dimension, getData(0).length.
 * 
 */
public class Observations {

    private double[][] data;

    public Observations(int number, int dimensionOfSpace) {
        data = new double[number][dimensionOfSpace];
    }

    public double[] getData(int i) {
        return data[i];
    }

    public int getSize() {
        return data.length;
    }
}
