/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.java;

import com.heeere.dpgmm.utilities.RenderableStackViewer;
import java.util.Random;

/**
 *
 * @author twilight
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int dim = 2; // the GMM can be in higher dimension (than 2) but the display will display the first 2 dimensions
        int nGauss = 10;
        int nSamples = 10000;
        double alpha = .1;
        double[] hMu0 = new double[]{.5, .5, .5, .5}; // prior on mean: centered in the middle of the space
        double[] hSigma0Diag = new double[]{.15, .15, .15, .15}; // prior on mean: broad variance
        double size = .045; // the real ones are actually between 0.02 and 0.05
        double[] fixedSigmaDiag = new double[]{size, size, size, size};
        squareEachOf(hSigma0Diag);
        squareEachOf(fixedSigmaDiag);
        
        Random fixedRandomOrNull = null; //new Random(0xFEED); // <- you can use a fixed random to fix the generated datapoints

        if (args.length > 0) {
            if (args.length != 3) {
                System.err.println("... nGauss nSamplesInTotal alpha");
                System.err.printf("default to %d %d %f%n", nGauss, nSamples, alpha);
                System.exit(1);
            }
            nGauss = Integer.parseInt(args[0]);
            nSamples = Integer.parseInt(args[1]);
            alpha = Double.parseDouble(args[2]);
        }

        GibbsSampler g = new GibbsSampler();
        if (fixedRandomOrNull != null) {
            g.setRandom(fixedRandomOrNull);
        }

        g.generateData(dim, nGauss, nSamples);
        g.init(g.observations);

        //TopicDistribution h = new TopicDistribution();

        long start = System.currentTimeMillis();
        RenderableStackViewer w = new RenderableStackViewer();
        w.title("Estimating topics").exitOnClose().show();

        int iter = 1;
        g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
        w.addRenderable(g.getWeigtedTopicsDisplay().name("(" + iter + ")"));
        System.err.println(iter + " " + (System.currentTimeMillis() - start));

        for (int i = 0; i < 400; i++) {
            int dIter = 1;
            iter += dIter;
            for (int j = 0; j < dIter; j++) {
                g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
            }
            w.addRenderable(g.getWeigtedTopicsDisplay().name("(" + iter + ")"));
            System.err.println(iter + " " + (System.currentTimeMillis() - start));
        }
        for (int i = 0; i < 100; i++) {
            int dIter = 20;
            iter += dIter;
            for (int j = 0; j < dIter; j++) {
                g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
            }
            w.addRenderable(g.getWeigtedTopicsDisplay().name("Fast (" + iter + ")"));
            System.err.println(iter + " " + (System.currentTimeMillis() - start));
        }
        w.title("Done");

    }

    private static void squareEachOf(double[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] *= data[i];
        }
    }
}
