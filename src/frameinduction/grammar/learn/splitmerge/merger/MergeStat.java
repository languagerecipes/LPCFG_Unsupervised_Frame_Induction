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
 * @author behra
 */
public class MergeStat implements Comparable<MergeStat> {

    private int symbolIDOrIndex1;
    private int symbolIDOrIndex2;
    private double psumWT;
    private double pMnWT;

    public MergeStat(int symbolIDOrIndex1, int symbolIDOrIndex2, double psumWT, double pMnWT) {

        if (symbolIDOrIndex1 > symbolIDOrIndex2) {
            this.symbolIDOrIndex1 = symbolIDOrIndex1;
            this.symbolIDOrIndex2 = symbolIDOrIndex2;
        } else {
            this.symbolIDOrIndex1 = symbolIDOrIndex2;
            this.symbolIDOrIndex2 = symbolIDOrIndex1;
        }
        this.psumWT = psumWT;
        this.pMnWT = pMnWT;
    }

    public int getSymbolIDOrIndex1() {
        return symbolIDOrIndex1;
    }

    public int getSymbolIDOrIndex2() {
        return symbolIDOrIndex2;
    }

    public double getPsumWT() {
        return psumWT;
    }

    public double getpMnWT() {
        return pMnWT;
    }

    public String getDummyKey(){
        
    return this.symbolIDOrIndex1+"-"+symbolIDOrIndex2;
    }
    @Override
    public int compareTo(MergeStat t) {
        assert (t.symbolIDOrIndex1 > t.symbolIDOrIndex2);
        assert (this.symbolIDOrIndex1 > this.symbolIDOrIndex2);

        int compare = Integer.compare(this.symbolIDOrIndex1, t.symbolIDOrIndex1);
        if (compare != 0) {
            return compare;
        } else {
            int compare2 = Integer.compare(this.symbolIDOrIndex2, t.symbolIDOrIndex2);
            return compare2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MergeStat) {
            MergeStat mst = (MergeStat) o;
            return mst.compareTo(this) == 0;

        } else {
            return false;
        }
    }

    
    

}
