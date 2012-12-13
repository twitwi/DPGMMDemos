
package com.heeere.pygmmscala
import com.heeere.dpgmm.utilities.RenderableStackViewer
import scala.util.Random

object Main {

  def square(x:Double) = x*x
  
  def main(args: Array[String]):Unit = {
    
    /*
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
     */

    val totalIter = 1000
    val dim = 2
    val nGauss = 10
    val nSamples = 10000
    //val (pyAlpha, pyD) = (.1, 0.)
    //val (pyAlpha, pyD) = (.01, 0.1)
    //val (pyAlpha, pyD) = (.001, 0.2)
    //val (pyAlpha, pyD) = (.0001, 0.3)
    //val (pyAlpha, pyD) = (.00001, 0.4)
    val (pyAlpha, pyD) = (0, 0.5)
    
    val hMu0 = Array.fill(dim)(.5)
    val hSigma0Diag = Array.fill(dim)(.15).map(square)
    val fixedSigmaDiag = Array.fill(dim)(.045).map(square)
    
    val fixedRandom = Option(new Random(0xFEED))
    
    val g = new Sampler()
    for (i <- fixedRandom) g.random = i
    for (i <- fixedRandom) g.initializingRandom = i
    
    g.generatedData(dim, nGauss, nSamples)
    g.initWithObservations(g.observations)

    // todo
    
    
    val w = new RenderableStackViewer()
    w.title("Scala PYGMM").exitOnClose.show
    w.addRenderable(g.displayWeightedTopics("Initial Points", g.observations, Seq.empty))
    w.title("Done");
    
    var iter = 0
    while (iter < totalIter) {
      iter += 1
      println("Doing iteration "+iter)
      g.doGibbsSweep(pyAlpha, pyD, fixedSigmaDiag, hMu0, hSigma0Diag)
      w.addRenderable(g.displayWeightedTopics("After " +iter+" Iter", g.observations, g.estimateWeightedComponents()))
    }
  }
}
