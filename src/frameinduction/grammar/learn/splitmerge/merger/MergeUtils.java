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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MergeUtils {
    
    /** 
     * Sum all the io-params from the second map into the first one.
     * @param ioParamMapSummary
     * @param ioParamMapPartial 
     */
    public static void summarizeSymbolsInsideOutsides(Map<Integer, IOParam> ioParamMapSummary, Map<Integer, IOParam> ioParamMapPartial) {

        for (Entry<Integer, IOParam> entry : ioParamMapPartial.entrySet()) {
            int symbolKey = entry.getKey();
            IOParam valueIO = entry.getValue();
            
            if (ioParamMapSummary.containsKey(symbolKey)) {
                IOParam get = ioParamMapSummary.get(symbolKey);
                get.addThisParamForMerge(valueIO);
                get.incFrequency(valueIO.getFrequency());
                
            } else {
                IOParam ioP = new IOParam(valueIO);
                ioP.incFrequency(valueIO.getFrequency());
                IOParam put = ioParamMapSummary.put(symbolKey, ioP);
                if (put != null) {
                    put.addThisParamForMerge(ioP);
                    put.incFrequency(ioP.getFrequency());
                }
            }

        }

    }
    
    public static int getSumAllRulesUsed(Map<Integer, IOParam> ioParamMapSummary) {

        AtomicInteger ai = new AtomicInteger();
        ioParamMapSummary.entrySet().parallelStream().forEach(entry->{
            int intValue = entry.getValue().getFrequency();
            
            ai.addAndGet(intValue);
        });
        
           return ai.intValue();

        }

    

}
