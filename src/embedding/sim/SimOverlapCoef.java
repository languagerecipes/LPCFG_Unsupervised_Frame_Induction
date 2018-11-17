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


import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import util.embedding.ISimMethod;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SimOverlapCoef implements ISimMethod {

	public static double dot(double[] a, double[] b) {
            
		double y = 0;

		for (int x = 0; x < a.length; x++) {
			y += a[x] * b[x];
		}

		return y;
	}

	public static double inner(double[] a, double[] b) {
            
		double y = 0;

		y = dot(a, b) / Math.min(dot(a, a), dot(b, b));

                SpearmansCorrelation pc = new SpearmansCorrelation();
            //PearsonsCorrelation pc = new PearsonsCorrelation();
            
            double x = pc.correlation(a, b);
		return y*x;
	}

	
    @Override
    public double measureSim(double[] vec1, double[] vec2) {
        return inner(vec2, vec1);
    }

}
