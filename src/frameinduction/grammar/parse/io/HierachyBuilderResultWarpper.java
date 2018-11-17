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
package frameinduction.grammar.parse.io;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author behra
 */
public class HierachyBuilderResultWarpper {
    Map<Integer, Collection<Fragment>> partitionFragmentsByClusters;
    Map<Integer, Collection<ParseFrame>> parseFrameMap;
    RuleMaps ruleMap;

    public HierachyBuilderResultWarpper( HierachyBuilderResultWarpper hcw) {
        partitionFragmentsByClusters = new ConcurrentHashMap<>(hcw.partitionFragmentsByClusters);
        parseFrameMap = new ConcurrentHashMap<>(hcw.parseFrameMap);
        ruleMap = new RuleMaps(hcw.ruleMap);
    }
    
    
    
    

    public HierachyBuilderResultWarpper(Map<Integer, Collection<Fragment>> partitionFragmentsByClusters,
            Map<Integer, Collection<ParseFrame>> parseFrameMap, RuleMaps rulemaps) {
        this.partitionFragmentsByClusters = partitionFragmentsByClusters;
        this.parseFrameMap = parseFrameMap;
        this.ruleMap = rulemaps;
    }

    public Map<Integer, Collection<ParseFrame>> getParseFrameMap() {
        return parseFrameMap;
    }

    public Map<Integer, Collection<Fragment>> getPartitionFragmentsByClusters() {
        return partitionFragmentsByClusters;
    }

    public RuleMaps getRuleMap() {
        return ruleMap;
    }
    
    public int getFragmentSize(){
        AtomicInteger ai = new AtomicInteger();
        parseFrameMap.values().forEach(col->{
         ai.addAndGet(col.size());
    });
        return ai.intValue();
    }
    
    public static List<HierachyBuilderResultWarpper> flattenHiearchial(Collection<Collection<HierachyBuilderResultWarpper>> listOfList){
        
        List<HierachyBuilderResultWarpper> listFlatten = new ArrayList<>();
        listOfList.forEach(list->{
            listFlatten.addAll(list);
                });
        return listFlatten;
    }
    
}
