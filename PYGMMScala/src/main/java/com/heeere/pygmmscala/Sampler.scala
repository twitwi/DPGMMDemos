
package com.heeere.pygmmscala
import com.heeere.dpgmm.utilities.Stats

class Sampler extends ExperimentalTrait {
  
  var dimension = -1
  def observationCount = observations.size
  var z: Array[Int] = null
  var stats: scala.collection.mutable.IndexedSeq[PerComponentTabling] = null
  def componentCount = stats.size // todo might need to take into account the not-yet-cleaned ones
  
  def initWithObservations(obs: Observations) = {
    observations = obs
    dimension = observations(0).size
    z = Array.fill(observationCount)(-1)
    stats = scala.collection.mutable.IndexedSeq.empty
  }
  
  def doGibbsSweep(pyAlpha_ : Double, pyD: Double, fixedSigmaDiag: Array[Double], hMu0: Array[Double], hSigma0Diag: Array[Double]) = {
    var countToNextCleanup = -1 // ensuring we don't clean too often
    var i = 0
    while (i < observationCount) {
      val pyAlpha = if (pyAlpha_ == 0 && componentCount == 0) 1. else pyAlpha_
      if (countToNextCleanup > 0) { // cleanup
        countToNextCleanup -= 1
        if (countToNextCleanup == 0) {
          cleanupEmptyComponents()
        }
      }
      val oldZ = z(i)
      if (oldZ != -1) {
        stats(oldZ).uncontribute(observations(i))
      }
      val drawTable = Array.ofDim[Double](stats.size + 1)
      
      // the part on Pitman Yor proba
      var k = 0
      def tr(x:Double) = x // math.log10(10 + x) / math.log10(1. + pyAlpha)
      
      while (k < drawTable.length - 1) {
        drawTable(k) = tr(stats(k).count - pyD)
        k += 1
      }
      drawTable(drawTable.length - 1) = pyAlpha + pyD * componentCount
      // the part on the observation likelihood
      k = 0
      while (k < drawTable.length - 1) {
        if (drawTable(k) > 0) // with the pyD it can be (0 - pyD)
          drawTable(k) *= stats(k).posteriorPredictive(observations(i), fixedSigmaDiag, hMu0, hSigma0Diag)
        k += 1
      }
      drawTable(drawTable.length - 1) *= averageProbaFromPrior(observations(i), fixedSigmaDiag, hMu0, hSigma0Diag)
      // now draw from the table
      val newZ = Stats.drawFromProportionalMultinomial(drawTable, drawTable.sum)
      z(i) = newZ
      if (newZ == drawTable.length - 1) {
        // new component
        stats = stats :+ new PerComponentTabling(dimension)
      }
      stats(newZ).contribute(observations(i))
      if (oldZ != -1 && countToNextCleanup <= 0 && stats(oldZ).count == 0) {
        countToNextCleanup = 10
      }
      i += 1
    }
  }

  def cleanupEmptyComponents() = {
    val from = componentCount
    val remap = Array.ofDim[Int](from)
    var remapNextIndex = 0
    var i = 0
    while (i < from) {
      if (stats(i).count == 0) {
        remap(i) = -123456
      } else {
        remap(i) = remapNextIndex
        stats(remap(i)) = stats(i)
        remapNextIndex += 1
      }
      i += 1
    }
    stats = stats.take(remapNextIndex)
    i = 0
    while (i < observationCount) {
      if (z(i) != -1) z(i) = remap(z(i))
      i += 1
    }
  }
  
  def averageProbaFromPrior(data: Array[Double], fixedSigmaDiag: Array[Double], hMu0: Array[Double], hSigma0Diag: Array[Double]) = {
    var res = 1.
    var c = 0
    while (c < data.length) {
      val x = data(c)
      val mu = hMu0(c)
      val sigma = hSigma0Diag(c) + fixedSigmaDiag(c)
      res *= Stats.evaluateGaussianWithVariance(x, mu, sigma)
      c += 1
    }
    res
  }
  
  def estimateWeightedComponents(): Seq[(PerComponentTabling, Double)] = {
    val w = Array.fill(componentCount)(0.)
    val incr = 1. / observationCount
    var i = 0
    while (i < observationCount) {
      w(z(i)) += incr
      i += 1
    }
    stats.zipWithIndex.map{ case (s, i) => (s.copy(), w(i)) }
  }
}


class PerComponentTabling(dim: Int) {
  private var nObs = 0.
  private val sum = Array.fill(dim)(0.)
  private val sumSquares = Array.fill(dim)(0.)
  
  def count() = nObs
  def mean(i: Int) = sum(i) / nObs
  def stddev(i: Int) = math.sqrt(sumSquares(i) / nObs - sum(i) / nObs * sum(i) / nObs)
  def update(factor: Double)(data: Array[Double]) = {
    nObs += factor
    var i = 0
    while (i < dim) {
      sum(i) += data(i) * factor
      sumSquares(i) += data(i)*data(i) * factor
      i += 1
    }
  }
  
  val contribute = update(1.)_
  val uncontribute = update(-1.)_
  
  def posteriorPredictive(data: Array[Double], fixedSigmaDiag: Array[Double], hMu0: Array[Double], hSigma0Diag: Array[Double]): Double = {
    var res = 1.
    var c = 0
    while (c < dim) {
      val sigma0Prime = 1. / (1. / hSigma0Diag(c) + nObs * 1. / fixedSigmaDiag(c))
      val mu0Prime = sigma0Prime * (1. / hSigma0Diag(c) * hMu0(c) + nObs * 1. / fixedSigmaDiag(c) * sum(c) / nObs)
      // 
      val mu = mu0Prime
      val sigma = sigma0Prime + fixedSigmaDiag(c)
      val x = data(c)
      res *= Stats.evaluateGaussianWithVariance(x, mu, sigma)
      c += 1
    }
    res
  }
  
  def copy() = {
    val res = new PerComponentTabling(dim)
    System.arraycopy(sum, 0, res.sum, 0, sum.length)
    System.arraycopy(sumSquares, 0, res.sumSquares, 0, sumSquares.length)
    res.nObs = nObs
    res
  }
}