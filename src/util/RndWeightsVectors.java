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
package util;

import java.util.Random;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class RndWeightsVectors {

    public static double[] randSum(int n, double m, double powerPareto) {
        Random rand = new Random(1);
        double randNums[] = new double[n], sum = 0;

        for (int i = 0; i < randNums.length; i++) {
            randNums[i] = rand.nextDouble();
                  //  1.0/Math.pow(rand.nextDouble() ,powerPareto); // let's creat a real long tail dist.
            sum += randNums[i];
        }

        for (int i = 0; i < randNums.length; i++) {
            randNums[i] = randNums[i] / sum * m;
        }

        return randNums;
    }

   
}
