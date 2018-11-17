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

/**
 *
 * @author behra
 */
public class PairKeySparseSymetric {

    private int row = 0;
    private int col = 0;
    private int hasKey = 23;

    public PairKeySparseSymetric(PairKeySparseSymetric psk) {
        this(psk.row, psk.col);
    }

    
    public PairKeySparseSymetric(final int x, final int y) {
        if (x > y) {
            this.row = x;
            this.col = y;
        } else {
            this.row = y;
            this.col = x;
        }
        hasKey = hasKey * 37 + row;
        hasKey = hasKey * 31 + col;

    }

    public int getIndexR() {
        return row;
    }

    public int getIndexC() {
        return col;
    }
    
    

    public boolean equals(final Object obj) {
        if (obj instanceof PairKeySparseSymetric) {
            PairKeySparseSymetric index = (PairKeySparseSymetric) obj;
            return ((row == index.row) && (col == index.col));
        } else {
            return false;
        }
    }

    public int hashCode() {
        return hasKey;
    }

    @Override
    public String toString() {
        return this.col +" "+this.row;
    }

    
}
