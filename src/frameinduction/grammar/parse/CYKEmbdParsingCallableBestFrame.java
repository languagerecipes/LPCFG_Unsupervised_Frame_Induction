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
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.embedding.WordPairSimilarityContainer;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKEmbdParsingCallableBestFrame implements Callable<Collection<ParseFrame>> {

    private final Collection<Fragment> fc;
    private final RuleMaps theRuleMap;

    private final CYKParserGeneralizedByEmbedding cykPEmbedding;

    public CYKEmbdParsingCallableBestFrame(Collection<Fragment> fcp, WordPairSimilarityContainer wordPairSim, RuleMaps theRuleMap) throws Exception {
        this.fc = fcp;
        this.theRuleMap = new RuleMaps(theRuleMap);

        cykPEmbedding = new CYKParserGeneralizedByEmbedding(wordPairSim, theRuleMap);

    }

    @Override
    public Collection<ParseFrame> call() throws Exception {
        Collection<ParseFrame> parseCharQueue = new ConcurrentLinkedQueue<>();
        Logger.getGlobal().log(Level.FINER, "Running a cyk parser for f# {0}", fc.size());
        int startSymbolID = theRuleMap.getStartSymbolID();
        for (Fragment frame : fc) {
            try {

                ParseChart chart = cykPEmbedding.parse(frame, new FragmentCompact(frame, theRuleMap), theRuleMap);
                chart.setIdentifier(frame.getSentID());
                if (chart != null) {
                    CNFRule mostProbableParse = chart.getMostProbableParse(startSymbolID);
                    if(mostProbableParse!=null){
                    ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(
                            chart.getIdentifier(), mostProbableParse, theRuleMap);
                    parseTreeToFrame.setHeadPositionInSentence(frame.getHeadPosition());
                    //ParseFrame parseTreeToFrameRoleSyntax = HelperParseChartIO.parseTreeToFrameRoleSyntax(mostProbableParse, theRuleMap);
                    //String fid = frame.getSentID();
                    //parseTreeToFrame.setFrameNumber(fid);
//                    String retValue = fid
//                            + " " + parseTreeToFrame.toStringStd();
                    parseCharQueue.add(parseTreeToFrame);
                    }else{
                        Logger.getGlobal().log(Level.WARNING, "No parse found for " + frame.getEvaluationID()+" " + frame.toStringPosition());
                    }
                } else {
                    Logger.getGlobal().log(Level.WARNING, "Warning; no parse for frame {0}", frame.toStringPosition());

                }

            } catch (Exception ex) {
                Logger.getLogger(CYKEmbdParsingCallableBestFrame.class.getName()).log(Level.SEVERE, "Exception in CYKParsingCallableBestFrame {0}", ex);
            }

        }
        return parseCharQueue;
    }

}
