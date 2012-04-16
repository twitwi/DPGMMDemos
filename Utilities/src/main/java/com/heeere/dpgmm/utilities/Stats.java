/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.utilities;

import java.util.Arrays;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.util.FastMath;

/**
 *
 * @author twilight
 */
public final class Stats {

    private static final double gaussDenominator = Math.sqrt(2 * Math.PI);

    public static double evaluateGaussianWithVariance(double x, double mu, double variance) {
        return new NormalDistributionImpl(mu, FastMath.sqrt(variance)).density(x);
    }

    public static int drawFromProportionalMultinomial(double[] pObservationComesFromTopic, double sum) {
        if (sum == 0) {
            System.err.println("WARNING: should use log/exp thing as the non-log sum is 0\n  " + Arrays.toString(pObservationComesFromTopic));
            System.err.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        }
        double u = Math.random() * sum;
        int i = 0;
        while (u > pObservationComesFromTopic[i]) {
            u -= pObservationComesFromTopic[i];
            i++;
            if (i == pObservationComesFromTopic.length) {
                System.err.println(Arrays.toString(pObservationComesFromTopic));
                System.err.println(sum);
                System.err.println(u);
            }
        }
        return i;
    }

    public static int drawFromLogProportionalMultinomialInPlaceWithMax(double[] logProportionnal, double max) {
        double sum = 0;
        for (int i = 0; i < logProportionnal.length; i++) {
            logProportionnal[i] = Math.exp(logProportionnal[i] - max);
            sum += logProportionnal[i];
        }
        return Stats.drawFromProportionalMultinomial(logProportionnal, sum);
    }

    public static int drawFromLogProportionalMultinomialInPlace(double[] logProportionnal) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logProportionnal.length; i++) {
            max = Math.max(max, logProportionnal[i]);
        }
        return drawFromLogProportionalMultinomialInPlaceWithMax(logProportionnal, max);
    }

    public static int[] drawFromProportionalMultinomial(double[][] probOccEtc, double sum) {
        if (sum == 0) {
            System.err.println("WARNING: should use log/exp thing as the non-log sum is 0\n  " + Arrays.deepToString(probOccEtc));
            System.err.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        }
        double u = Math.random() * sum;
        for (int i1 = 0; i1 < probOccEtc.length; i1++) {
            double[] subprobs = probOccEtc[i1];
            for (int i2 = 0; i2 < subprobs.length; i2++) {
                u -= subprobs[i2];
                if (u < 0) {
                    return new int[]{i1, i2};
                }
            }
        }
        // this is an error or limit case
        System.err.println(Arrays.deepToString(probOccEtc));
        System.err.println(sum);
        System.err.println(u);
        return new int[]{probOccEtc.length - 1, probOccEtc[probOccEtc.length - 1].length - 1};
    }

    public static int drawFromGeometric(double p) {
        double u = Math.random();
        return (int) (Math.log(u) / Math.log(1 - p));
    }
}
