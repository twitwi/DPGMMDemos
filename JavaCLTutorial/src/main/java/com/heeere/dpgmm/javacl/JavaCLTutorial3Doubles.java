package com.heeere.dpgmm.javacl;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import org.bridj.Pointer;
import java.io.IOException;

public class JavaCLTutorial3Doubles {

    public static void main(String[] args) throws IOException {
        CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();

        int n = 1024;

        for (CLDevice d : context.getDevices()) {
            System.err.println(d.getName());
            System.err.println(d.getLocalMemSize());
            System.err.println(d.getGlobalMemSize());
            System.err.println(d.getGlobalMemCacheSize());
            System.err.println(d.getMaxWorkGroupSize());
            System.err.println(d.getMaxWorkItemDimensions());
            System.err.println(d.getMaxWorkItemSizes());
        }

        // Create OpenCL input and output buffers
        CLBuffer<Double> a = context.createDoubleBuffer(Usage.InputOutput, n), // a and b and read AND written to
                b = context.createDoubleBuffer(Usage.InputOutput, n),
                out = context.createDoubleBuffer(Usage.Output, n);

        TutorialKernelsDouble kernels = new TutorialKernelsDouble(context);
        int[] globalSizes = new int[]{n};
        CLEvent fillEvt = kernels.fill_in_doubles(queue, a, b, n, globalSizes, null);
        CLEvent addEvt = kernels.add_doubles(queue, a, b, out, n, globalSizes, null, fillEvt);

        Pointer<Double> outPtr = out.read(queue, addEvt); // blocks until add_floats finished

        // Print the first 10 output values :
        for (int i = 0; i < 10 && i < n; i++) {
            System.out.println("out[" + i + "] = " + outPtr.get(i));
        }

    }
}
