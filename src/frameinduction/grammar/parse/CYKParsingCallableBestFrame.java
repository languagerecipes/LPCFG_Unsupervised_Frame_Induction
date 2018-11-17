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

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParsingCallableBestFrame implements Callable<Collection<String>> {

    private final Collection<Fragment> fc;
    private final RuleMaps theRuleMap;

    public CYKParsingCallableBestFrame(Collection<Fragment> fcp, RuleMaps theRuleMap) {
        this.fc = fcp;
        this.theRuleMap = new RuleMaps(theRuleMap);
    }

    @Override
    public Collection<String> call() throws Exception {
        Collection<String> parseCharQueue = new ConcurrentLinkedQueue<>();
        Logger.getGlobal().log(Level.FINER, "Running a cyk parser for f# {0}", fc.size());
        int startSymbolID = theRuleMap.getStartSymbolID();
        for (Fragment frame : fc) {
            try {
                CYKParser cykP = new CYKParser();
                ParseChart chart = cykP.parse(new FragmentCompact(frame, theRuleMap), theRuleMap);
                if (chart != null) {
                    CNFRule mostProbableParse = chart.getMostProbableParse(startSymbolID);

                    ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(chart.getIdentifier(),mostProbableParse, theRuleMap);
                    //ParseFrame parseTreeToFrameRoleSyntax = HelperParseChartIO.parseTreeToFrameRoleSyntax(mostProbableParse, theRuleMap);
                    String fid = frame.getSentID();
                    String retValue = fid
                            + " " + parseTreeToFrame.toStringStd();
                    parseCharQueue.add(retValue);

                } else {
                    Logger.getGlobal().log(Level.WARNING, "Warning; no parse for frame {0}", frame.toStringPosition());

                }

            } catch (Exception ex) {
                Logger.getLogger(CYKParsingCallableBestFrame.class.getName()).log(Level.SEVERE, "Exception in CYKParsingCallableBestFrame {0}", ex);
            }

        }
        return parseCharQueue;
    }

}
