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
package frameinduction.grammar.learn;

import frameinduction.grammar.RuleMaps;
//import frameinduction.grammar.RuleMapsReader;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.learn.splitmerge.merger.MergeUtils;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MTInsideOutsideCallableWithIOParamSummary implements Callable<IOProcessCompleteDataContainer> {

    private final List<Fragment> fcp;
    private final  RuleMaps theRuleMap;
    private final int thisWorkerID;
    //private static final int BUFFER_SIZE = 300;

    public MTInsideOutsideCallableWithIOParamSummary(List<Fragment> fcp, RuleMaps theRuleMap, int id) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;//new RuleMaps(theRuleMap);
        this.thisWorkerID = id;
    }

    @Override
    public IOProcessCompleteDataContainer call() throws Exception {
       // RulesCounts rcSum = new RulesCounts();
      //  RuleMapsReader rmr = new RuleMapsReader(theRuleMap);
        //ConcurrentLinkedDeque<RulesCounts> waitingList = new ConcurrentLinkedDeque<>();
   
        // System.out.println("Running me " + id + " for f# " + fcp.size());
        //AtomicInteger count = new AtomicInteger(0);
        RulesCounts rc = new RulesCounts();
        //Set<Integer> ssTest = ConcurrentHashMap.newKeySet();
        Map<Integer, IOParam> batchIOStatSummary  = new ConcurrentHashMap<>();
        // for (Fragment fc : fcp) {
        fcp. //parallelStream().
                forEach(fc -> {
                    
            try {
                // change FragmentCompact to be done one in main method
                //RulesCounts rc = new RulesCounts();
                InsideOutside cis = new InsideOutside(new FragmentCompact(fc, theRuleMap), theRuleMap //rmr
                        , rc); // <--
                cis.inside();
                cis.outsideAlgorithm();
                InOutParameterChart ioChart = cis.getIoChart();
                Map<Integer, IOParam> sentenceSymbolIOStatSummary = ioChart.getSentenceSymbolIOStatSumSummary();
                //ssTest.addAll(sentenceSymbolIOStatSummary.keySet());
                MergeUtils.summarizeSymbolsInsideOutsides(batchIOStatSummary, sentenceSymbolIOStatSummary);


                //
            } catch (Exception ex) {
                
                Logger.getLogger(MTInsideOutsideCallableWithIOParamSummary.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        // }
        //System.out.println("Parser " + id + " -> parsed " + count);
//        waitingList.stream().forEach(remove -> {
//            rcSum.addRuleCounts(remove,0);
//        });

        //return rcSum;
        //System.err.println("symbol size is " + batchIOStatSummary.size());
        return new IOProcessCompleteDataContainer(rc, batchIOStatSummary);
    }

}
