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
package frameinduction.grammar.learn.splitmerge.split;

import frameinduction.grammar.RuleMaps;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SplitFrameHeadsExtendedToSubstates implements ISplit{

    /**
     * 
     * @param rm
     * @return 
     * @throws Exception 
     */
    @Override
    public RuleMaps split(RuleMaps rm) throws Exception {
        Set<Integer> frameIndexSet = new HashSet<>(rm.getFrameIndexSet());
       // System.out.println(frameIndexSet);
        //RuleMaps copyRM = new RuleMaps(rm);
        for (int i:frameIndexSet) {
           
          //  System.out.println(copyRM.getFrameHeadLargestIndex());
           // System.out.println("Remove spit " + i);
            SplitHeadsWithSubstates sp = new SplitHeadsWithSubstates(i, rm);
            RuleMaps performSplit = sp.performSplit();
            
            rm=new RuleMaps(performSplit);
           // System.out.println(copyRM.getFrameIndexSet());
            //System.out.println("-----");
            
        }
        return rm;
        
    }
    
  

    @Override
    public RuleMaps split(RuleMaps rm, int limitOnNumberOfCategories) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RuleMaps split(RuleMaps rm, double porpotionToSplit) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
