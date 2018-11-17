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
package frameinduction.grammar.learn.splitmerge.merger;

import frameinduction.grammar.learn.MathUtil;

/**
 *
 * @author behra
 */


     class StatTuple {

        double prob;
        int count;

        public StatTuple(double prob, int count) {
            this.prob = prob;
            this.count = count;
        }

    public StatTuple(StatTuple st) {
        this.prob= st.prob;
        this.count= st.count;
    }
        
        

        public int getCount() {
            return count;
        }

        public double getProb() {
            return prob;
        }

        public void multiplySum(double prob, int count) {
            this.prob += prob;
            this.count += count;
        }
         public void logSumSum(double prob, int count) {
           this.prob = MathUtil.logSumExp(this.prob, prob);
            this.count += count;
        }
           public void addSum(double prob, int count) {
           this.prob = MathUtil.logSumExp(this.prob, prob);
            this.count += count;
        }
            public void logSumSum(StatTuple st) {
           this.prob = MathUtil.logSumExp(this.prob, st.prob);
            this.count += st.count;
        }
     
         public void multiplySum(StatTuple st) {
             this.prob += st.prob;
             this.count += st.count;
         }
    }    

