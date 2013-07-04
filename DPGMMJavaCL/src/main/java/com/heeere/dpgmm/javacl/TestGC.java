/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.JavaCL;

/**
 *
 * @author twilight
 */
public class TestGC {
    
    public static void main(String[] args) {
        CLContext context = JavaCL.createBestContext();
        CLDevice[] devices = context.getDevices();
        for (int i = 0; i < devices.length; i++) {
            System.err.println(i+": "+devices[i]);
        }
        System.err.println("Releasing context");
        context.release();
        System.err.println("Now GC'ing");
        System.gc();
        System.err.println("GC'ed");
    }
    
}
