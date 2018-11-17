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
package frameinduction.grammar.learn;

import java.util.Random;
import java.util.concurrent.atomic.DoubleAccumulator;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MathUtil {
    
    public static double logSumExp(double u, double v) {
        // we assume v is always bigger than u otherwise we replace their position to avoid under or overflow
        // max(u, v) + log(exp(u - max(u, v)) + exp(v - max(u, v))) 
        if (v >= u) {
            
            double res = v + FastMath.log1p(FastMath.exp(u - v));
            return res;
        } else {
            return u + FastMath.log1p(FastMath.exp(v - u));
        }

    }
//    public static void main(String[] args) {
//        System.out.println(logSumExp(-Double.MAX_VALUE, -.2d));
//        DoubleAccumulator doubleLogAcc = getDoubleLogAcc();
//        doubleLogAcc.accumulate(-.2);
//        System.out.println(doubleLogAcc.doubleValue());
//        System.out.println(getDoubleLogAcc(.2));
//        DoubleAccumulator doubleMaxAcc = getDoubleMaxAcc(0);
//        int max = 0;
//        for (int i = 0; i < 1000; i++) {
//           Random r = new Random();
//            int nextInt = r.nextInt();
//           doubleMaxAcc.accumulate(nextInt);
//            if(max<nextInt){
//                max=nextInt;
//            }
//        }
//        System.out.println(max);
//        System.out.println(doubleMaxAcc.doubleValue());
//    }
    public static DoubleAccumulator getDoubleLogAcc(){
        DoubleAccumulator dac = new DoubleAccumulator((x,y)->logSumExp(x, y), -Double.MAX_VALUE);
        return dac;
    }
    
     public static DoubleAccumulator getDoubleMaxAcc(double min){
        DoubleAccumulator dac = new DoubleAccumulator((x,y)->Math.max(x, y),min);
        return dac;
    }
       public static DoubleAccumulator getDoubleMinAcc(double min){
        DoubleAccumulator dac = new DoubleAccumulator((x,y)->Math.min(x, y),min);
        return dac;
    }
    
    public static DoubleAccumulator getDoubleLogAcc(double d){
        DoubleAccumulator dac = getDoubleLogAcc();
        dac.accumulate(d);
        return dac;
    }

    public static double getSum(double[] vector2) {
        Double sum=0.0;
        for(double d: vector2){
            sum+=d;
        }
       
       return sum;
    }
    
}
