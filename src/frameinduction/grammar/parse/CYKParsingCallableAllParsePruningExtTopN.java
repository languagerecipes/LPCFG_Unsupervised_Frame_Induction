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
import frameinduction.grammar.RulesCounts;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.ParseGrammarUtils;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParsingCallableAllParsePruningExtTopN implements Callable<Collection<ParseDataCompact>> {

    final private Collection<Fragment> fcp;
    final private RuleMaps theRuleMap;
    final private int tgtNBest;
    final private RulesCounts rcForTopNParses;
    //int id;

    public CYKParsingCallableAllParsePruningExtTopN(Collection<Fragment> fcp, RuleMaps theRuleMap, int tgtNBest) {
        this.fcp = fcp;
        this.theRuleMap = theRuleMap;
        this.tgtNBest = tgtNBest;
        this.rcForTopNParses = new RulesCounts();
        //this.id = id;

    }

    @Override
    public Collection<ParseDataCompact> call() {

        Collection<ParseDataCompact> parseCharQueue = new ConcurrentLinkedQueue<>();

        for (Fragment fc : fcp) {

            try {
                //try {
                CYKParser cyk = new CYKParser();
                ParseChart parse = cyk.parse(new FragmentCompact(fc, theRuleMap), theRuleMap);

                parse.setIdentifier(fc.getSentID());

                List<CNFRule> validParsesSortByProb = parse.getSortedValidParses(theRuleMap.getStartSymbolID());
                if (validParsesSortByProb.size() > 0) { // this must be the case otherwise there are some erorrs
                    int totalParseForThis = validParsesSortByProb.size();
                    int targetTopNParsesX = tgtNBest;
                    if(totalParseForThis<targetTopNParsesX){
                        targetTopNParsesX=totalParseForThis;
                    }
//                    if (totalParseForThis > 1) {
//                        int s = (int) Math.floor(totalParseForThis * tgtNBest);
//                        targetTopNParsesX = 1 + s;
//                        
//                    }
//                    if(targetTopNParsesX>1){
//                        System.err.println("Target parse to keep is set to "  + targetTopNParsesX);
//                    }

                    int topNF = Math.min(validParsesSortByProb.size(), targetTopNParsesX);
                    int countAllValidParses = validParsesSortByProb.size();
                    double llForTheTopNParses = 0.0;
                    double llForBestParse = 0.0;
                    llForBestParse = validParsesSortByProb.get(0).getProb();
                    for (int i = 0; i < topNF; i++) {
                        // add things rulecounts
                        ParseGrammarUtils.getRulesInChain(validParsesSortByProb.get(i), rcForTopNParses);
                        llForTheTopNParses += validParsesSortByProb.get(i).getProb();
                       
                    }
                    double llForall = llForTheTopNParses;
                     for (int i = topNF; i < validParsesSortByProb.size(); i++) {
                        ParseGrammarUtils.getRulesInChain(validParsesSortByProb.get(i), rcForTopNParses);
                        llForall += validParsesSortByProb.get(i).getProb();
                    }
                    // for evaluatuion purpose--> the computed number nust eb the same as the reported LL in the EM process.
                    //double log = Math.log(parse.sumProbValidParses(ruleMap.getStartSymbolID()));
                    //probSum.add(log);
                    // String retFrameValueStr = null;
                    ParseFrame bestParseFrame = null;

                    // get the prob
                   
                    // get the string format we use for evaluation and put it in a queue
                    bestParseFrame = HelperParseChartIO.parseTreeToFrame(
                            parse.getIdentifier(), validParsesSortByProb.get(0), theRuleMap);
                    //retFrameValueStr = parseTreeToFrame.toStringStdEvaluation();

                    bestParseFrame.setLikelihood(llForTheTopNParses);

                    ParseDataCompact pd = new ParseDataCompact(
                            bestParseFrame, countAllValidParses, topNF,
                            llForTheTopNParses, llForBestParse, rcForTopNParses,llForall);
                    parseCharQueue.add(pd);
                }
//            } catch (Exception ex) {
//                Logger.getLogger(CYKParsingCallableAllParsePruning.class.getName()).log(Level.SEVERE, null, ex);
//            }
            } catch (Exception ex) {
                Logger.getLogger(CYKParsingCallableAllParsePruningExtTopN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return parseCharQueue;
    }

}
