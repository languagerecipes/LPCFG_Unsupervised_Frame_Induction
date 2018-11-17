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
package mhutil;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.parse.CNFRule;
import frameinduction.grammar.parse.CYKAllParses;
import frameinduction.grammar.parse.ParseChart;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.ParseGrammarUtils;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperGrammarLearnMethods {

    /**
     * Method to collect rules that are used and to remove the orphan ones.
     *
     * @param theRuleMap
     * @param fragmentsAll
     * @param topParse
     * @return
     * @throws Exception
     */
    public static RulesCounts collectUsedRulesInValidParses(RuleMaps theRuleMap, Collection<Fragment> fragmentsAll) throws Exception {
        //  AtomicInteger count = new AtomicInteger(0);

        // DoubleAdder probSum = new DoubleAdder();
        //DoubleAdder lnBest = new DoubleAdder();
        CYKAllParses cypAll = new CYKAllParses(fragmentsAll, theRuleMap, Settings.DEF_THREAD_NUM_TO_USE);
        //System.out.println("Now running the parser in " + threadNum + " threads");
        Collection<ParseChart> gotParses = cypAll.run();

        //for(Future<Collection<ParseChart>> parseColelction:gotParses){
        RulesCounts rc = new RulesCounts();
        gotParses.forEach(parse -> {
            try {
                //   parse.getRules(rc, theRuleMap, true);
                //  for(ParseChart parse: parseColelction.get()){
                List<CNFRule> mostProbableParse = parse.getValidParses(theRuleMap.getStartSymbolID());
                if(mostProbableParse!=null){
                for (CNFRule probableParse : mostProbableParse) {
                    ParseGrammarUtils.getRulesInChain(probableParse, rc);
                    //lnBest.add(probableParse.getProb());
                    //break;
                }}
            } catch (Exception ex) {
                Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return rc;//

    }

    public static RulesCounts collectUsedRulesFromTheBestNParses(
            RuleMaps theRuleMap,
            Collection<Fragment> fragmentsAll, int topN) throws Exception {

        CYKAllParses cypAll = new CYKAllParses(fragmentsAll, theRuleMap, Settings.DEF_THREAD_NUM_TO_USE);
        Collection<ParseChart> gotParses = cypAll.run();

        RulesCounts rc = new RulesCounts();
        gotParses.forEach(parse -> {

            try {
                List<CNFRule> mostProbableParse = parse.getValidParses(theRuleMap.getStartSymbolID());
                int coutToBreak = 0;
                for (CNFRule probableParse : mostProbableParse) {

                    ParseGrammarUtils.getRulesInChain(probableParse, rc);
                    //lnBest.add(probableParse.getProb());
                    if (++coutToBreak >= topN) {
                        break;
                    }

                }
            } catch (Exception ex) {
                Logger.getLogger(HelperLearnerMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return rc;//

    }
}
