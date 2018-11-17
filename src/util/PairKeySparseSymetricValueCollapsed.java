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

import java.util.Comparator;

/**
 *
 * @author behra
 */
public class PairKeySparseSymetricValueCollapsed extends PairKeySparseSymetric{

    final private double value;

    public PairKeySparseSymetricValueCollapsed(PairKeySparseSymetric psk, double value) {

        super(psk);
        this.value = value;
    }

    final public static Comparator<PairKeySparseSymetricValueCollapsed> CMPR_BY_VALUE = new Comparator<PairKeySparseSymetricValueCollapsed>(){
        @Override
        public int compare(PairKeySparseSymetricValueCollapsed t, PairKeySparseSymetricValueCollapsed t1) {
            return Double.compare(t.value,t1.value);
        }
    };

    public double getValue() {
        return value;
    }
    
    

    
}
