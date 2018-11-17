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
public class MTInsideOutsideCallable implements Callable<RulesCounts> {

    private static final Logger LOGGER = Logger.getLogger(MTInsideOutsideCallable.class.getName());
    Collection<Fragment> fcp;
    RuleMaps theRuleMap;
    int id;
    private static final int BUFFER_SIZE = 300;

    public MTInsideOutsideCallable(Collection<Fragment> fcp, RuleMaps theRuleMap, int id) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;//new RuleMaps(theRuleMap);
        this.id = id;
    }

    @Override
    public RulesCounts call() throws Exception {
        RulesCounts rc = new RulesCounts();
         for (Fragment fc : fcp) {
            try {
                // change FragmentCompact to be done one in main method
                //RulesCounts rc = new RulesCounts();
                InsideOutside cis = new InsideOutside(new FragmentCompact(fc, theRuleMap), theRuleMap //rmr
                        , rc); // <--
                cis.inside();
                cis.outsideAlgorithm();
                
            } catch (Exception ex) {
                
                LOGGER.log(Level.SEVERE, null, ex);
            }
        };
        
        return rc;
    }

}
