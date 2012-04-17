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
        int componentCount = 10; // count before any change // TODO find it ...
        final int nUpdates = 1000;
        
        CLBuffer<Integer> clUpdates = context.createIntBuffer(CLMem.Usage.InputOutput, nUpdates);
        CLEvent draw = null;//kernels.compute_updates();
        CLEvent apply = kernels.apply_updates(queue, clObs, clUpdates, clStats, clZ, componentCount, nUpdates, dimension, only(componentCount + 1), null, draw);
        //TODO reduce/repack
        //queue.flush();
        queue.finish();
        // java state is now outdated
        javaIsUptodate = false;

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
        final int sizeOfStatsTable = (2 * dimension + 1) * observationCount;


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
            for (PerTopicTabling t : stats) {
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
