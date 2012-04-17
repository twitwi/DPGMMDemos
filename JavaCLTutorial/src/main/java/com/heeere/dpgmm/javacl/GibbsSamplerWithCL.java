/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.nativelibs4java.opencl.*;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.Pointer;

/**
 *
 * @author twilight
 */
public class GibbsSamplerWithCL extends GibbsSampler {

    boolean currentlyInJava = true;
    boolean javaIsUptodate = true;
    boolean openclIsUptodate = false;
    CLContext context = null;
    CLQueue queue = null;
    GMMKernels kernels = null;
    CLBuffer<Float> clObs = null;
    CLBuffer<Float> clStats;
    CLBuffer<Integer> clZ;
    int clComponentCount = -111;

    /*
     * = context.createFloatBuffer(CLMem.Usage.InputOutput, n), // a and b and
     * read AND written to b =
     * context.createFloatBuffer(CLMem.Usage.InputOutput, n), out =
     * context.createFloatBuffer(CLMem.Usage.Output, n);
     */
    @Override
    public void doDirichletProcessEstimation(double alpha, double[] fixedSigmaDiag, double[] hMu0, double[] hSigma0Diag) {
        if (currentlyInJava) {
            if (!javaIsUptodate) {
                throw new IllegalStateException("In java mode but java not up to date...");
            }
            super.doDirichletProcessEstimation(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
            openclIsUptodate = false;
        } else {
            doDirichletProcessEstimationCL(alpha, fixedSigmaDiag, hMu0, hSigma0Diag);
        }
    }

    public void doDirichletProcessEstimationCL(double alpha, double[] fixedSigmaDiag, double[] hMu0, double[] hSigma0Diag) {
        if (!openclIsUptodate) {
            throw new IllegalStateException("In opencl mode but opencl not up to date...");
        }
        // do the iteration on the OpenCL device
        final int nUpdates = 1000;
        CLBuffer<Integer> clUpdates = context.createIntBuffer(CLMem.Usage.InputOutput, nUpdates);
        for (int fromObservationIndex = 0; fromObservationIndex < observationCount; fromObservationIndex += nUpdates) {
            CLBuffer<Float> clRand;
            { // send some random value to the gpu
                Pointer<Float> tmp = Pointer.allocateFloats(nUpdates).order(context.getByteOrder());
                int c = 0;
                for (int i = 0; i < nUpdates; i++) {
                    tmp.set(c++, (float) Math.random());
                }
                clRand = context.createFloatBuffer(CLMem.Usage.Input, tmp);
            }
            CLEvent draw = kernels.compute_updates(queue, clObs, clStats, clZ, clUpdates, clRand, cl(fixedSigmaDiag), cl(hMu0), cl(hSigma0Diag), dimension, (float) alpha, clComponentCount, fromObservationIndex, observationCount, only(nUpdates), null);
            CLEvent apply = kernels.apply_updates(queue, clObs, clUpdates, clStats, clZ, clComponentCount, nUpdates, dimension, only(clComponentCount + 1), null, draw);
            //queue.flush();
            //queue.finish();
            Pointer<Integer> outPtr = clZ.read(queue, apply);
            // refind the highest used z
            clComponentCount = -111;
            for (int k = 0; k < GMMKernels.MAXTOPIC; k++) {
                clComponentCount = Math.max(clComponentCount, outPtr.get(k)+1);
            }
            //TODO reduce/repack
        }
        // java state is now outdated
        javaIsUptodate = false;

    }

    private CLBuffer<Float> cl(double[] arr) {
        Pointer<Float> tmp = Pointer.allocateFloats(arr.length).order(context.getByteOrder());
        for (int i = 0; i < arr.length; i++) {
            tmp.set(i, (float) arr[i]);
        }
        return context.createFloatBuffer(CLMem.Usage.Input, tmp);
    }

    private int[] only(int i) {
        return new int[]{i};
    }

    private void lazyInitOpenCL() {
        if (context == null) {
            try {
                context = JavaCL.createBestContext();
                queue = context.createDefaultQueue();
                kernels = new GMMKernels(context);
            } catch (IOException ex) {
                Logger.getLogger(GibbsSamplerWithCL.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void switchBackToJava() {
        if (currentlyInJava) {
            throw new IllegalStateException("Already switched back to Java");
        }
        currentlyInJava = true;
        if (javaIsUptodate) {
            return;
        }
        // TODO read back the data (z and stats)

        javaIsUptodate = true;
    }

    public void switchToOpenCL() {
        if (!currentlyInJava) {
            throw new IllegalStateException("Already switched to OpenCL");
        }
        currentlyInJava = false;
        if (openclIsUptodate) {
            return;
        }
        lazyInitOpenCL();
        final ByteOrder byteOrder = context.getByteOrder();
        final int sizeOfObservationTable = dimension * observationCount;
        final int sizeOfStatsTable = (2 * dimension + 1) * GMMKernels.MAXTOPIC; //;(2 * dimension + 1) * observationCount;


        if (clObs == null) { // pack and transfer observations
            Pointer<Float> obsPtr = Pointer.allocateFloats(sizeOfObservationTable).order(byteOrder);
            int c = 0;
            for (int i = 0; i < observationCount; i++) {
                for (double d : observations.getData(i)) {
                    obsPtr.set(c++, (float) d);
                }
            }
            clObs = context.createFloatBuffer(CLMem.Usage.Input, obsPtr);
        }
        { // same for the stats
            Pointer<Float> statsPtr = Pointer.allocateFloats(sizeOfStatsTable).order(byteOrder);
            int c = 0;
            clComponentCount = 0;
            for (PerTopicTabling t : stats) {
                clComponentCount++;
                statsPtr.set(c++, (float) t.nObs);
                for (double d : t.sum) {
                    statsPtr.set(c++, (float) d);
                }
                for (double d : t.sumSquares) {
                    statsPtr.set(c++, (float) d);
                }
            }
            clStats = context.createFloatBuffer(CLMem.Usage.InputOutput, statsPtr);
        }
        { // same for z[] (it is int though)
            Pointer<Integer> zPtr = Pointer.allocateInts(observationCount).order(byteOrder);
            int c = 0;
            for (int topic : z) {
                zPtr.set(c++, topic);
            }
            clZ = context.createIntBuffer(CLMem.Usage.InputOutput, zPtr);
        }
        openclIsUptodate = true;
    }
}
