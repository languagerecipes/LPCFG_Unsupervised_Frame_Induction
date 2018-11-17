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
public class SimGoodmanKruskalGamma implements ISimMethod {

	/**
	 * Computes Goodman and Kruskal's gamma between the two sense rankings.
	 * 
	 * @param goldSensePerceptions
	 * @param testSensePerceptions
	 * @param numSenses
	 * @return
	 */
	private static double goodmanKruskalGamma(double[] a, double[] b) {
		int length = a.length;
		//double numerator = 0;

		int concordant = 0;
		int discordant = 0;

		// For all pairs, track how many pairs satisfy the ordering
		for (int i = 0; i < length; ++i) {
			for (int j = i + 1; j < length; ++j) {
				// NOTE: this value will be 1 if there exists an match or
				// "concordance" in the ordering of the two pairs. Otherwise
				// it, will be a -1 of the pairs are not matched or are
				// "discordant.
				double ai = a[i];
				double aj = a[j];
				double bi = b[i];
				double bj = b[j];

				// If there was a tied rank, don't count the comparisons towards
				// the concordance totals
				if (ai != aj && bi != bj) {
					if ((ai < aj && bi < bj) || (ai > aj && bi > bj))
						concordant++;
					else
						discordant++;
				}
			}
		}

		int cd = concordant + discordant;
		return (cd == 0) ? 0 : ((double) (concordant - discordant)) / cd;
	}

	

    @Override
    public double measureSim(double[] vec1, double[] vec2) {
        return goodmanKruskalGamma(vec2, vec1);
    }
}