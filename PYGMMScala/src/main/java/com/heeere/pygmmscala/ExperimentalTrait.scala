/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.pygmmscala

import com.heeere.dpgmm.utilities.Renderable
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import scala.util.Random


trait ExperimentalTrait {

  
  type Observations = Array[Array[Double]]
  
  class Component(mean: Array[Double], diagStddev: Array[Double]) {
    def drawInto(out: Array[Double]): Array[Double] = {
      var i = 0
      while (i < out.size) {
        out(i) = mean(i) + initializingRandom.nextGaussian() * diagStddev(i)
        i += 1
      }
      out
    }
  }
  object Component {
    def random(dim: Int) = new Component(
      Array.tabulate(dim)((i) => initializingRandom.nextDouble * 1. + 0.),
      Array.tabulate(dim)((i) => initializingRandom.nextDouble * .03 + .02)      
    )
  }
  
  var random = new Random()
  var initializingRandom = new Random()
  var observations: Observations = null
  
  def generatedData(dim: Int, nTopics: Int, nPoints: Int) = {
    val components = Array.tabulate(nTopics)((i) => Component.random(dim))

    observations = Array.ofDim(nPoints, dim)
    var i = 0
    while (i < nPoints) {
      components(initializingRandom.nextInt(components.size)).drawInto(observations(i))
      i += 1
    }
  }
  
  def displayWeightedTopics(theName: String, observations: Observations, stats: Seq[(PerComponentTabling, Double)]) = new Renderable.Abstract {
    override def render(go: Graphics2D, width: Int, height: Int) = {
      val g = go.create().asInstanceOf[Graphics2D]
      val to01: Double = math.max(width, height)
      val toImage = 1. / to01
      val margin = .15
      g.scale(width, height);
      g.translate(margin, margin)
      g.scale(1. - 2*margin, 1. - 2*margin)
      g.setStroke(new BasicStroke(4.f / (width + height)));
      g.setColor(Color.GREEN.darker().darker());
      var i = 0
      while (i < observations.size) {
        val o = observations(i)
        g.translate(o(0), o(1))
        val w = 1. / width
        val h = 1. / height
        g.fill(new Ellipse2D.Double(-w, -h, 2 * w, 2 * h))
        g.translate(-o(0), -o(1))
        i += 1
      }
      i = 0
      while (i < stats.size) {
        val (c, weight) = stats(i)
        if (weight > 0) {
          g.setColor(if (weight <= 0.009) Color.GRAY else Color.RED)
          g.translate(c.mean(0), c.mean(1))
          val w = 2 * c.stddev(0)
          val h = 2 * c.stddev(1)
          g.draw(new Ellipse2D.Double(-w, -h, 2 * w, 2 * h))
          g.scale(2 * toImage, 2 * toImage)
          g.drawString("%.2f".format(weight), 0.f, 0.f)
          g.scale(to01 / 2, to01 / 2)
          g.translate(-c.mean(0), -c.mean(1))
        }
        i += 1
      }
    }
    name(theName)
  }
  
}
