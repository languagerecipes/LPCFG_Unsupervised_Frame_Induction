/* 
 * Copyright 2018 Behrang Qasemizadeh (zadeh@phil.hhu.de).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package embedding.sim;


import util.embedding.ISimMethod;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SimJenShanDiv implements ISimMethod {

    public static final double log2 = Math.log(2);

    @Override
    public double measureSim(double[] vec1, double[] vec2) {
        return -jensenShannonDivergence(vec1, vec2);
    }

    public double jensenShannonDivergence(double[] p1, double[] p2) {
        assert (p1.length == p2.length);
        double[] average = new double[p1.length];
        for (int i = 0; i < p1.length; ++i) {
            average[i] += (p1[i] + p2[i]) / 2;
        }
        return (klDivergence(p1, average) + klDivergence(p2, average)) / 2;
    }

    public double klDivergence(double[] p1, double[] p2) {

        double klDiv = 0.0;

        for (int i = 0; i < p1.length; ++i) {
            if (p1[i] == 0) {
                continue;
            }
            if (p2[i] == 0.0) {
                continue;
            } // Limin

            klDiv += p1[i] * Math.log(p1[i] / p2[i]);
        }

        return klDiv / log2; // moved this division out of the loop -DM
    }

   
}
