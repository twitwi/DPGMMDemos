/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.heeere.dpgmm.utilities.RenderableStackViewer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author twilight
 */
public class Benchmark1 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] argsArray) {
        
        List<String> args = new ArrayList<String>(Arrays.asList(argsArray));
        //TopicDistribution h = new TopicDistribution();
        boolean display = args.contains("--display");
        args.remove("--display");
        boolean switchBack = true; // switch back to java every time for display?
        boolean switchToOpencl = true;
        if (args.contains("--java")) {
            switchToOpencl = false;
            switchBack = false;
            args.remove("--java");
        }
        
        int iarg = 0;
        final int expect = 6;
        if (args.size() != expect) {
            System.err.println("Expected "+expect+" parameters.");
            System.exit(1);
        }
        int nGauss = i(args.get(iarg++)); // 10
        int nSamples = i(args.get(iarg++)); // 100000
        int dim = i(args.get(iarg++)); // 100
        double alpha = d(args.get(iarg++)); // 0.01
        int duration = i(args.get(iarg++)); // 30000
        int updateClBlockSize = i(args.get(iarg++)); // 10000
        
        double[] hMu0 = repeat(.5, dim); // prior on mean: centered in the middle of the space
        double[] hSigma0Diag = repeat(.15,dim); // prior on mean: broad variance
        double size = .045; // the real ones are actually between 0.02 and 0.05
        double[] fixedSigmaDiag = repeat(size, dim);
        squareEachOf(hSigma0Diag);
        squareEachOf(fixedSigmaDiag);

        Random fixedRandomOrNull = new Random(0xFEED); // <- you can use a fixed random to fix the generated datapoints

        //GibbsSampler g = new GibbsSampler();
        GibbsSamplerWithCL g = new GibbsSamplerWithCL();
        if (fixedRandomOrNull != null) {
            g.setRandom(fixedRandomOrNull);
        }

        g.generateData(dim, nGauss, nSamples);
        g.init(g.observations);
        g.setUpdateBlockSize(updateClBlockSize);

        long start = System.currentTimeMillis();
        RenderableStackViewer w = null;
        if (display) {
            w = new RenderableStackViewer();
            w.title("Estimating topics").exitOnClose().show();
        }

        int iter = 1;
        g.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
        if (display) w.addRenderable(g.getWeigtedTopicsDisplay().name("(" + iter + ")"));
        System.out.println(iter + " " + (System.currentTimeMillis() - start));

        if (switchToOpencl) g.switchToOpenCL();

        for (int i = 0; i < 1000; i++) {
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
            System.out.println(iter + " " + (System.currentTimeMillis() - start));
            if (System.currentTimeMillis() - start > duration) {
                break;
            }
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

    private static int i(String s) {
        return Integer.parseInt(s);
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }
}
