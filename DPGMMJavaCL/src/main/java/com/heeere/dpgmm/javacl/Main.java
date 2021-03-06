/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.heeere.dpgmm.utilities.RenderableStackViewer;
import java.util.Arrays;
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
        int dim = 10; // the GMM can be in higher dimension (than 2) but the display will display the first 2 dimensions
        int nGauss = 20;
        int nSamples = 100000;
        double alpha = .01;
        double[] hMu0 = repeat(.5, dim); // prior on mean: centered in the middle of the space
        double[] hSigma0Diag = repeat(.15,dim); // prior on mean: broad variance
        double size = .045; // the real ones are actually between 0.02 and 0.05
        double[] fixedSigmaDiag = repeat(size, dim);
        squareEachOf(hSigma0Diag);
        squareEachOf(fixedSigmaDiag);

        Random fixedRandomOrNull = new Random(0xFEED); // <- you can use a fixed random to fix the generated datapoints

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

        //GibbsSampler g = new GibbsSampler();
        GibbsSamplerWithCL g = new GibbsSamplerWithCL();
        if (fixedRandomOrNull != null) {
            g.setRandom(fixedRandomOrNull);
        }

        g.generateData(dim, nGauss, nSamples);
        g.init(g.observations);

        //TopicDistribution h = new TopicDistribution();
        boolean display = true;

        long start = System.currentTimeMillis();
        RenderableStackViewer w = null;
        if (display) {
            w = new RenderableStackViewer();
            w.title("Estimating topics").exitOnClose().show();
        }

        int iter = 1;
        g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
        if (display) w.addRenderable(g.getWeigtedTopicsDisplay().name("(" + iter + ")"));
        System.err.println(iter + " " + (System.currentTimeMillis() - start));

        boolean switchBack = true; // switch back to java every time for display?
        g.switchToOpenCL();

        for (int i = 0; i < 200; i++) {
            int dIter = 1;
            iter += dIter;
            for (int j = 0; j < dIter; j++) {
                g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
            }
            if (display) {
                if (switchBack) {
                    g.switchBackToJava();
                    g.switchToOpenCL();
                }
                if (display) w.addRenderable(g.getWeigtedTopicsDisplay().name("(" + iter + ")"));
            }
            System.err.println(iter + " " + (System.currentTimeMillis() - start));
        }
        for (int i = 0; i < 100; i++) {
            int dIter = 20;
            iter += dIter;
            for (int j = 0; j < dIter; j++) {
                g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
            }
            if (display) {
                if (switchBack) {
                    g.switchBackToJava();
                    g.switchToOpenCL();
                }
                w.addRenderable(g.getWeigtedTopicsDisplay().name("Fast (" + iter + ")"));
            }
            System.err.println(iter + " " + (System.currentTimeMillis() - start));
        }
        if (display) w.title("Done");

    }

    private static void squareEachOf(double[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] *= data[i];
        }
    }

    private static double[] repeat(double d, int dim) {
        double[]res=new double[dim];
        Arrays.fill(res, d);
        return res;
    }
}
