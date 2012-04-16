/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.nativelibs4java.opencl.*;
import java.nio.ByteOrder;
import org.bridj.Pointer;

/**
 *
 * @author twilight
 */
public class GibbsSamplerWithCL extends GibbsSampler {

    CLContext context = null;
    CLQueue queue = null;
    CLBuffer<Float> clObs;
    CLBuffer<Float> clStats;
    CLBuffer<Integer> clZ;
    CLBuffer<Integer> clUpdates;
    /*
     * = context.createFloatBuffer(CLMem.Usage.InputOutput, n), // a and b and
     * read AND written to b =
     * context.createFloatBuffer(CLMem.Usage.InputOutput, n), out =
     * context.createFloatBuffer(CLMem.Usage.Output, n);
     */

    private void lazyInitOpenCL() {
        if (context == null) {
            context = JavaCL.createBestContext();
            queue = context.createDefaultQueue();
        }
    }

    public void switchToOpenCL() {
        lazyInitOpenCL();
        final ByteOrder byteOrder = context.getByteOrder();
        final int sizeOfObservationTable = dimension * observationCount;
        final int sizeOfStatsTable = (2 * dimension + 1) * observationCount;


        { // pack and transfer observations
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
    }
}
