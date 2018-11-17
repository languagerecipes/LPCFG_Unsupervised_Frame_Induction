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
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MTInsideOutsideGauPCallable implements Callable<RulesCounts> {

    private final Collection<Fragment> fcp;
    private final RuleMaps theRuleMap;
    
    private static final int BUFFER_SIZE = 300;
    final private int dimensionality ;
    private final int nonZeroElement;
    public MTInsideOutsideGauPCallable(Collection<Fragment> fcp, RuleMaps theRuleMap, int dimensionality,  int nonZeroElement) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;//new RuleMaps(theRuleMap);
        this.dimensionality= dimensionality;
        this.nonZeroElement = nonZeroElement;
        
    }

    @Override
    public RulesCounts call() throws Exception {
       // RulesCounts rcSum = new RulesCounts();
      //  RuleMapsReader rmr = new RuleMapsReader(theRuleMap);
       // ConcurrentLinkedDeque<RulesCounts> waitingList = new ConcurrentLinkedDeque<>();
   
        // System.out.println("Running me " + id + " for f# " + fcp.size());
       // AtomicInteger count = new AtomicInteger(0);
        RulesCounts rc = new RulesCounts();
        // for (Fragment fc : fcp) {
        fcp. //parallelStream().
                forEach(fc -> {
            try {
                // change FragmentCompact to be done one in main method
                //RulesCounts rc = new RulesCounts();
                InsideOutsideGauP cis = new InsideOutsideGauP(new FragmentCompact(fc, theRuleMap), theRuleMap //rmr
                        , rc,dimensionality,nonZeroElement); // <--
                cis.inside();
                cis.outsideAlgorithm();
//                if (!Double.isFinite(cis.getZParam())) {
//                    System.out.println("* " + fc.toString());
//                }
            
//                waitingList.add(rc);
//                if (countVal % BUFFER_SIZE == 0) {
//                    for (int i = 0; i < BUFFER_SIZE; i++) {
//                        RulesCounts remove = waitingList.remove();
//                        rcSum.addRuleCounts(remove,0);
//
//                    }
////                //System.out.println("Parser " + id + " -> parsed " + count);
////                //util.MemoryUtils.getMemoryReport();
//                }


                //
            } catch (Exception ex) {
                
                Logger.getLogger(MTInsideOutsideGauPCallable.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        // }
        //System.out.println("Parser " + id + " -> parsed " + count);
//        waitingList.stream().forEach(remove -> {
//            rcSum.addRuleCounts(remove,0);
//        });

        //return rcSum;
        return rc;
    }

}
