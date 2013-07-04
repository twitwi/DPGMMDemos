/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.nativelibs4java.opencl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteOrder;
import java.util.Formatter;
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
    CLBuffer<Float> clStats = null;
    CLBuffer<Integer> clZ = null;
    int clComponentCount = -111;
    
    private int updateBlockSize = 10000;

    public void setUpdateBlockSize(int updateBlockSize) {
        this.updateBlockSize = updateBlockSize;
    }
    

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
        final int nUpdates = updateBlockSize; // TODO avoid the need for the z size to be a multiple of this
        CLBuffer<Integer> clUpdates = context.createIntBuffer(CLMem.Usage.InputOutput, nUpdates * 3);
        //CLBuffer<Float> clDBG = context.createFloatBuffer(CLMem.Usage.InputOutput, nUpdates * GMMKernels.MAXTOPIC); // unfreed, just for testing
        CLBuffer<Float> clFixedSigmaDiag = cl(fixedSigmaDiag);
        CLBuffer<Float> clHMu0 = cl(hMu0);
        CLBuffer<Float> clHSigma0Diag = cl(hSigma0Diag);
        for (int fromObservationIndex = 0; fromObservationIndex < observationCount; fromObservationIndex += nUpdates) {
            //queue.finish();
            CLBuffer<Float> clRand;
            { // send some random value to the gpu
                Pointer<Float> rand = Pointer.allocateFloats(nUpdates).order(context.getByteOrder());
                int c = 0;
                for (int i = 0; i < nUpdates; i++) {
                    rand.set(c++, (float) Math.random());
                }
                clRand = context.createFloatBuffer(CLMem.Usage.Input, rand);
            }
            CLEvent draw = kernels.compute_updates(queue, clObs, clStats, clZ, clUpdates, clRand, clFixedSigmaDiag, clHMu0, clHSigma0Diag, dimension, (float) alpha, clComponentCount, fromObservationIndex, observationCount, only(nUpdates), null);
            // clDBG goes just after clRand
            CLEvent apply = kernels.apply_updates(queue, clObs, clUpdates, clStats, clZ, clComponentCount, nUpdates, dimension, only(clComponentCount + 1), null, draw);
            /*{
                PrintStream out = null;
                try {
                    Pointer<Integer> up = clUpdates.read(queue, draw);
                    out = new PrintStream("/tmp/update-" + nIter);
                    for (int i = 0; i < nUpdates; i++) {
                        out.println(up.get(3 * i + 0) + " " + up.get(3 * i + 1) + " " + up.get(3 * i + 2));
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(GibbsSamplerWithCL.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    out.close();
                }
                try {
                    Pointer<Float> dbg = clDBG.read(queue, draw);
                    out = new PrintStream("/tmp/draw-" + nIter);
                    for (int i = 0; i < nUpdates; i++) {
                        for (int k = 0; k < GMMKernels.MAXTOPIC; k++) {
                            out.print(dbg.get(i * GMMKernels.MAXTOPIC + k) + " ");
                        }
                        out.println();
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(GibbsSamplerWithCL.class.getName()).log(Level.SEVERE, null, ex);
                }
                nIter++;
            }*/

            Pointer<Integer> tmp = clZ.read(queue, apply);
            // refind the highest used z
            clComponentCount = -111;
            for (int i = 0; i < observationCount; i++) {
                clComponentCount = Math.max(clComponentCount, tmp.get(i) + 1);
            }
            tmp.release();
            clRand.release();
            //System.err.println("   "+clComponentCount);
            //TODO reduce/repack
        }
        clFixedSigmaDiag.release();
        clHMu0.release();
        clHSigma0Diag.release();
        clUpdates.release();
        // java state is now outdated
        javaIsUptodate = false;

    }
    int nIter = 0;

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
        { // read back the z
            Pointer<Integer> tmp = clZ.read(queue);
            for (int i = 0; i < observationCount; i++) {
                z[i] = tmp.get(i);
            }
        }
        { // read back the stats
            Pointer<Float> tmp = clStats.read(queue);
            int c = 0;
            stats.clear();
            for (int k = 0; k < clComponentCount; k++) {
                PerTopicTabling t = new PerTopicTabling(dimension);
                t.nObs = (int) (float) tmp.get(c++);
                for (int i = 0; i < dimension; i++) {
                    t.sum[i] = tmp.get(c++);
                }
                for (int i = 0; i < dimension; i++) {
                    t.sumSquares[i] = tmp.get(c++);
                }
                stats.add(t);
            }
        }

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
            if (clStats != null) {
                clStats.release();
                clStats = null;
            }
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
            if (clZ != null) {
                clZ.release();
                clZ = null;
            }
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
