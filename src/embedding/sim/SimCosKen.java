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

import org.apache.commons.math3.analysis.function.Cos;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import util.embedding.ISimMethod;

/**
 *
 * @author Zadeh
 */
public class SimCosKen implements ISimMethod{

    @Override
    public double measureSim(double[] vec1, double[] vec2) {
        KendallsCorrelation kc = new KendallsCorrelation();
        double correlation = kc.correlation(vec1, vec2);
        SimCosine simc  = new SimCosine();
        double cosine = simc.cosine(vec1, vec2);
        return 2*cosine*correlation/(cosine+correlation);
    }
    
}
