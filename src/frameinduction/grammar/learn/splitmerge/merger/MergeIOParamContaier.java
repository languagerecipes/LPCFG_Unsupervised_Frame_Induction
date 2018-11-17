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

import frameinduction.grammar.learn.IOParam;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MergeIOParamContaier {
    private IOParam ioParam;
    private String symbol;

    public MergeIOParamContaier(IOParam ioParam, String symbol) {
        this.ioParam = ioParam;
        this.symbol = symbol;
    }

    public IOParam getIoParam() {
        return ioParam;
    }

    public String getSymbol() {
        return symbol;
    }
    
}
