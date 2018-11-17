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
package frameinduction.grammar.parse;

import frameinduction.grammar.RuleMaps;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParsingCallableBestParseCNF implements Callable<Collection<CNFRule>> {

    private List<Fragment> fcp;
    private RuleMaps theRuleMap;
    private int id;

    public CYKParsingCallableBestParseCNF(List<Fragment> fcp, RuleMaps theRuleMap, int id) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;
        this.id = id;
    }

    @Override
    public Collection<CNFRule> call() {

        //System.out.println("Running me " + id + " for f# " + fcp.size());
        // int count = 0;
        //ParseChart parseChart = null;
        Collection<CNFRule> parseCharQueue = new ConcurrentLinkedQueue<>();

        for (Fragment fc : fcp) {
            try {
                // = null;
                //System.out.println("Here");
                // change FragmentCompact to be done one in main method
                CYKParser cyk = new CYKParser();
                FragmentCompact fragmentCompact = new FragmentCompact(fc, theRuleMap);
                ParseChart parseChart = cyk.parse(fragmentCompact, theRuleMap);
              CNFRule mostProbableParse   = parseChart.getMostProbableParse(this.theRuleMap.getStartSymbolID());
                parseCharQueue.add(mostProbableParse);
                //       count +=parseChart.countValidParse(theRuleMap.getStartSymbolID());

//            if (++count % 1000 == 0) {
//                
//                //util.MemoryUtils.getMemoryReport();
//            }
            } catch (Exception ex) {
                System.err.println(ex);
                Logger.getLogger(CYKParsingCallableBestParseCNF.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

//        System.out.println("Parser " + id + " -> parsed " + count);
//        System.out.println("Parser " + id + " -> parsed " + count);
        return parseCharQueue;
    }

}
