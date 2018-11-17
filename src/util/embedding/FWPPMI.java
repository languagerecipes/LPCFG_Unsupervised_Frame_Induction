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
package util.embedding;


import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class FWPPMI  {
    

   
    public Map<Integer, double[]> weightingProcessI(Map<Integer, double[]> wordMap, int dim){
        Map<Integer, double[]> wordMapWeighted = new ConcurrentHashMap<>();
        Collection<double[]> values = wordMap.values();
        //Iterator<double[]> iterator = wordMap.values().iterator();
        double sumAllComponents = 0.0;
        //int dim = 900;
        double[] vectorOfSumAllVectors = new double[dim];
        
        for (double[] vec : values) {
            for (int i = 0; i < dim; i++) {
                vectorOfSumAllVectors[i] += vec[i];

            }
        }
        for (int i = 0; i < dim; i++) {
            sumAllComponents += vectorOfSumAllVectors[i];

        }
//        if (iterator.hasNext()) {
//            vectorOfSumAllVectors= iterator.next();
//            
//            while (iterator.hasNext()) {
//                double[] next = iterator.next();
//                for (int i = 0; i < next.length; i++) {
//                    vectorOfSumAllVectors[i]+=next[i];
//                }
//            }
//            
//            for (int i = 0; i < vectorOfSumAllVectors.length; i++) {
//                sumAllComponents+= vectorOfSumAllVectors[i];
//
//            }
//        }
            
        if(!Double.isFinite(sumAllComponents)){
            System.err.println("Unseen ERROR in wsc weighting");
        }
        final double[] sumVec = vectorOfSumAllVectors;
        final double sumF = sumAllComponents;
//        System.err.println("sum f: " + sum);
//        System.err.println("sum vec " + Arrays.toString(sumVec));
        wordMap.keySet() //.parallelStream()
                .forEach(word -> {
            double[] rawVector = wordMap.get(word);
            double[] convertToPPMIDouble = convertToPPMIDouble(rawVector,  sumF,sumVec);
            double[] putIfAbsent = wordMapWeighted.putIfAbsent(word, convertToPPMIDouble);
            if(putIfAbsent!=null){
                throw new RuntimeException("Unseen situation ...");
            }
        });
           
        
        return wordMapWeighted;
    }
  
    public double[] convertToPPMIDouble(double[] vec,  double sumAll,double[] ssAll) {

        double[] vecWeighted = new double[vec.length];
        double sumThisRow = 0.0;
        for (int i = 0; i < vec.length; i++) {
            sumThisRow += vec[i];
        }
        for (int idx = 0; idx < vec.length; idx++) {
            double pmi = Math.log((vec[idx] * sumAll))-Math.log((sumThisRow * ssAll[idx]));
            vecWeighted[idx] = Double.max(0, pmi);
            if (!Double.isFinite(vecWeighted[idx])) {
               // System.err.println("WARNING: Inifinite value in vector weighting. ");
                vecWeighted[idx] = 0;
            }

        }

        return vecWeighted;

    }
    
//    static double getVariance(float[] v, double mean)
//    {
//        
//        double temp = 0;
//        for(double a :v.getVaules())
//            temp += (a-mean)*(a-mean);
//        return temp/v.getDimensionality();
//    }
}
