/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.java;

import com.heeere.dpgmm.utilities.Renderable;
import com.heeere.dpgmm.utilities.Stats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.util.FastMath;

/**
 *
 * @author twilight
 */
public class GibbsSampler extends ExperimentTrait {

    int observationCount;
    int dimension;
    // sampling state
    int[] z;
    //
    // tabling
    List<PerTopicTabling> stats;

    private void cleanupEmptyComponents() {
        int from = stats.size();
        int[] remap = new int[stats.size()];
        int remapNextIndex = 0;
        for (int i = 0; i < remap.length; i++) {
            if (stats.get(i).nObs == 0) {
                remap[i] = -123456;
            } else {
                remap[i] = remapNextIndex;
                stats.set(remap[i], stats.get(i));
                remapNextIndex++;
            }
        }
        while (stats.size() > remapNextIndex) {
            stats.remove(stats.size() - 1);
        }
        for (int i = 0; i < z.length; i++) {
            if (z[i] != -1) {
                z[i] = remap[z[i]];
            }
        }
        int to = stats.size();
        System.err.println("=========== CLEANUP DONE: " + from + " => " + to);
    }

    void killRandomComponent() {
        //int k = new Random().nextInt(stats.size()); // equi
        int k = z[new Random().nextInt(z.length)]; // based on weight
        PerTopicTabling s = stats.get(k);
        s.nObs = 0;
        for (int i = 0; i < z.length; i++) {
            if (z[i] == k) {
                z[i] = -1;
            }
       }
        cleanupEmptyComponents();
    }

    //
    public static class PerTopicTabling implements Cloneable {

        public PerTopicTabling(int dimension) {
            nObs = 0;
            sum = new double[dimension];
            sumSquares = new double[dimension];
        }

        private PerTopicTabling(PerTopicTabling o) {
            this.nObs = o.nObs;
            this.sum = Arrays.copyOf(o.sum, o.sum.length);
            this.sumSquares = Arrays.copyOf(o.sumSquares, o.sumSquares.length);
        }
        int nObs;
        double[] sum;
        double[] sumSquares;

        public double mean(int i) {
            return sum[i] / nObs;
        }

        public double stddev(int i) {
            return Math.sqrt(sumSquares[i] / nObs - sum[i] / nObs * sum[i] / nObs);
        }

        public void contribute(double[] data) {
            nObs++;
            for (int i = 0; i < data.length; i++) {
                sum[i] += data[i];
                sumSquares[i] += data[i] * data[i];
            }
        }

        public void uncontribute(double[] data) {
            nObs--;
            for (int i = 0; i < data.length; i++) {
                sum[i] -= data[i];
                sumSquares[i] -= data[i] * data[i];
            }
        }

        public double posteriorPredictive(double[] data, double[] fixedSigmaDiag, double[] hMu0, double[] hSigma0Diag) {
            double res = 1;
            for (int c = 0; c < data.length; c++) {
                double x = data[c];
                double sigma0Prime = 1. / (1. / hSigma0Diag[c] + nObs * 1. / fixedSigmaDiag[c]);
                double mu0Prime = sigma0Prime * (1. / hSigma0Diag[c] * hMu0[c] + nObs * 1. / fixedSigmaDiag[c] * sum[c] / nObs);
                // 
                double mu = mu0Prime;
                double sigma = sigma0Prime + fixedSigmaDiag[c];
                //res *= Stats.evaluateGaussianWithVariance(x, mu, sigma);
                res *= new NormalDistributionImpl(mu, FastMath.sqrt(sigma)).density(x);
            }
            return res;
        }
    }

    private double averageProbaFromPrior(double[] data, double[] fixedSigmaDiag, double[] hMu0, double[] hSigma0Diag) {
        double res = 1;
        for (int c = 0; c < data.length; c++) {
            double x = data[c];
            double mu = hMu0[c];
            double sigma = hSigma0Diag[c] + fixedSigmaDiag[c];
            res *= Stats.evaluateGaussianWithVariance(x, mu, sigma);
        }
        return res;
    }

    public void init(Observations obs) {
        this.observations = obs;
        this.dimension = observations.getData(0).length;
        this.observationCount = observations.getSize();
        this.z = new int[observationCount];
        Arrays.fill(z, -1);
        this.stats = new ArrayList<PerTopicTabling>();
    }

    public void doDirichletProcessEstimation(double alpha, double[] fixedSigmaDiag, double[] hMu0, double[] hSigma0Diag) {
        int countToNextCleanup = -1;
        // full swipe over all variables
        for (int i = 0; i < observationCount; i++) {
            if (countToNextCleanup > 0) {
                countToNextCleanup--;
                if (countToNextCleanup == 0) {
                    cleanupEmptyComponents();
                }
            }
            int oldZ = z[i];
            if (oldZ != -1) {
                stats.get(oldZ).uncontribute(observations.getData(i));
            }
            double[] drawTable = new double[stats.size() + 1];
            // the part on DP proba
            for (int k = 0; k < drawTable.length - 1; k++) {
                drawTable[k] = stats.get(k).nObs;
            }
            drawTable[drawTable.length - 1] = alpha;
            // the part on the observation likelihood
            for (int k = 0; k < drawTable.length - 1; k++) {
                drawTable[k] *= stats.get(k).posteriorPredictive(observations.getData(i), fixedSigmaDiag, hMu0, hSigma0Diag);
            }
            drawTable[drawTable.length - 1] *= averageProbaFromPrior(observations.getData(i), fixedSigmaDiag, hMu0, hSigma0Diag);
            // now draw from the table
            double sum = sum(drawTable);
            int newZ = Stats.drawFromProportionalMultinomial(drawTable, sum);
            z[i] = newZ;
            if (newZ == drawTable.length - 1) {
                // new component
                stats.add(new PerTopicTabling(dimension));
            }
            stats.get(newZ).contribute(observations.getData(i));
            if (oldZ != -1 && countToNextCleanup <= 0 && stats.get(oldZ).nObs == 0) {
                countToNextCleanup = 10;
            }
        }
    }

    public Renderable getWeigtedTopicsDisplay() {
        double[] weights = new double[stats.size()];
        double incr = 1. / observations.getSize();
        for (int oi = 0; oi < observations.getSize(); oi++) {
            if (z[oi] != -1) {
                weights[z[oi]] += incr;
            }
        }
        return displayWeigtedTopics(this.getClass().getName(), observations, weights, copy(stats));
    }

    private List<PerTopicTabling> copy(List<PerTopicTabling> t) {
        ArrayList<PerTopicTabling> res = new ArrayList<PerTopicTabling>(t.size());
        for (PerTopicTabling o : t) {
            res.add(new PerTopicTabling(o));
        }
        return res;
    }

}
