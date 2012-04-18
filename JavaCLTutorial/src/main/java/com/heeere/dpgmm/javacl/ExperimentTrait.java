/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heeere.dpgmm.javacl;

import com.heeere.dpgmm.javacl.GibbsSampler.PerTopicTabling;
import com.heeere.dpgmm.utilities.Renderable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

/**
 *
 * @author twilight
 */
public class ExperimentTrait {

    protected Observations observations = null;
    protected Random random = new Random();

    public void setRandom(Random random) {
        this.random = random;
    }
    
    public void generateData(int dim, int nTopics, int nPoints) {
        Topic[] dataTopics = new Topic[nTopics];
        for (int i = 0; i < dataTopics.length; i++) {
            /*
             * double stdev = Math.max(0.01, .02 + random.nextDouble() * .04); stdev = 0.05;
             */
            double[] mean = new double[dim];
            double[] stdev = new double[dim];
            for (int j = 0; j < stdev.length; j++) {
                mean[j] = random.nextDouble();
                stdev[j] = 0.02 + random.nextDouble() * .03;
            }
            dataTopics[i] = new Topic(mean, stdev);
        }
        observations = new Observations(nPoints, dim);
        for (int i = 0; i < nPoints; i++) {
            dataTopics[random.nextInt(dataTopics.length)].drawInto(observations.getData(i));
        }
    }

    public Renderable displayWeigtedTopics(final String name, final Observations obs, final double[] weights, final List<PerTopicTabling> stats) {
        return new Renderable.Abstract() {

            {
                name(name);
            }

            public void render(Graphics2D g, int width, int height) {
                g = (Graphics2D) g.create();
                double to01 = Math.max(width, height);
                double toImage = 1. / to01;
                g.scale(width, height);
                double margin = .15;
                g.translate(margin, margin);
                g.scale(1 - 2 * margin, 1 - 2 * margin);
                g.setStroke(new BasicStroke(4.f / (width + height)));
                g.setColor(Color.GREEN.darker().darker());
                for (int i = 0; i < obs.getSize(); i++) {
                    double[] o = obs.getData(i);
                    g.translate(o[0], o[1]);
                    double w = 1. / width;
                    double h = 1. / height;
                    g.fill(new Ellipse2D.Double(-w, -h, 2 * w, 2 * h));
                    g.translate(-o[0], -o[1]);
                }
                for (int i = 0; i < stats.size(); i++) {
                    PerTopicTabling topic = stats.get(i);
                    double weight = weights[i];
                    g.setColor(weight <= 0.009 ? Color.GRAY : Color.RED);
                    g.translate(topic.mean(0),topic.mean(1));
                    double w = 2 *  .045;//topic.stddev(0);
                    double h = 2 * .045;//topic.stddev(1);
                    g.draw(new Ellipse2D.Double(-w, -h, 2 * w, 2 * h));
                    g.scale(2 * toImage, 2 * toImage);
                    g.drawString(new Formatter().format("%.2f", weight).toString(), 0.f, 0.f);
                    g.scale(to01 / 2, to01 / 2);
                    g.translate(-topic.mean(0),-topic.mean(1));
                }
            }
        };
    }
    
        protected double[] copy(double[] t) {
        return Arrays.copyOf(t, t.length);
    }

    protected <T> T[] copy(T[] t) {
        return Arrays.copyOf(t, t.length);
    }

    protected double sum(double[] t) {
        double res = 0;
        for (double d : t) {
            res += d;
        }
        return res;
    }

}
