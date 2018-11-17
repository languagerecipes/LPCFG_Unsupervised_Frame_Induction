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



/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MergeTuple implements Comparable<MergeTuple>{
    private final String symbol1;
    private final String sybmol2;
    private final double lossEsitmated;

    public MergeTuple(String symbol1, String sybmol2, double lossEsitmated) {
        this.symbol1 = symbol1;
        this.sybmol2 = sybmol2;
        this.lossEsitmated = lossEsitmated;
    }

    @Override
    public int compareTo(MergeTuple t) {
        return Double.compare(lossEsitmated, t.lossEsitmated);
    }

    @Override
    public String toString() {
        return "("+this.symbol1+" and "+ this.sybmol2 + " gives loss " +lossEsitmated+")";
    }

    public String getSybmol2() {
        return sybmol2;
    }

    public String getSymbol1() {
        return symbol1;
    }

    public double getLossEsitmated() {
        return lossEsitmated;
    }

   

   
    
    

    
    
    
    
    
    
    
}
