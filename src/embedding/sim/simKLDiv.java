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
public class simKLDiv implements ISimMethod {

	public final static double LOG2 = Math.log(2);

	public double AvgKLDivergence(double[] p1, double[] p2) {

		double klDiv = 0.0;
		for (int i = 0; i < p1.length; ++i) {
			if (p1[i] == 0) {
				continue;
			}
			if (p2[i] == 0.0) {
				continue;
			} // Limin
			double pi_1 = p1[i] / (p1[i] + p2[i]);
			double pi_2 = p2[i] / (p1[i] + p2[i]);
			double w_t = (pi_1 * p1[i]) + (pi_2 * p2[i]);

			klDiv += (pi_1 * dkl(p1[i], w_t)) + (pi_2 * dkl(p2[i], w_t));

		}

		return klDiv;
	}

	// public double klDivergence(double[] p1, double[] p2) {
	//
	// double klDiv = 0.0;
	// for (int i = 0; i < p1.length; ++i) {
	// if (p1[i] == 0) {
	// continue;
	// }
	// if (p2[i] == 0.0) {
	// continue;
	// } // Limin
	//
	// klDiv += p1[i] * Math.log(p1[i] / p2[i])
	// ;
	// }
	//
	// return klDiv / LOG2;
	// }

	private double dkl(double p, double q) {
		return p * Math.log(p / q);
	}

	
		
	

    @Override
    public double measureSim(double[] dense1, double[] dense2) {
      return AvgKLDivergence(dense1, dense2);
    }
}
