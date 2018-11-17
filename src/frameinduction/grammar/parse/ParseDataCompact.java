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

import frameinduction.grammar.RulesCounts;
import frameinduction.grammar.parse.io.ParseFrame;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ParseDataCompact {

    //private final String retFrameValueStr;
    private final int countAllValidParses;
    private final double llForTheTopNParses;
    private final double sumProbForBestParse;
    private final RulesCounts rcForTopNParses;
    private final int countChosenParses;
    private final double llForAll;
    private final ParseFrame bestFrame;

//    public ParseDataCompact(ParseFrame bestFrame,
//            int countAllValidParses,
//            int countChosenParses,
//            double llForTheTopNParses,
//            double sumProbForBestParse,
//            RulesCounts rcForTopNParses) {
//        this.bestFrame = bestFrame;
//        this.countAllValidParses = countAllValidParses;
//        this.countChosenParses = countChosenParses;
//        this.llForTheTopNParses = llForTheTopNParses;
//        this.sumProbForBestParse = sumProbForBestParse;
//        this.rcForTopNParses = rcForTopNParses;
//       this.llForAll = 0.0;
//    }

    public ParseDataCompact(ParseFrame bestFrame,
            int countAllValidParses,
            int countChosenParses,
            double llForTheTopNParses,
            double sumProbForBestParse,
            RulesCounts rcForTopNParses, double llForAll) {
        this.llForAll = llForAll;
        this.bestFrame = bestFrame;
        this.countAllValidParses = countAllValidParses;
        this.countChosenParses = countChosenParses;
        this.llForTheTopNParses = llForTheTopNParses;
        this.sumProbForBestParse = sumProbForBestParse;
        this.rcForTopNParses = rcForTopNParses;
        //System.err.println(">>> lll > " +llForAll);
    }

    public int getCountChosenParses() {
        return countChosenParses;
    }

    public int getCountAllValidParses() {
        return countAllValidParses;
    }

    public double getLlForTheTopNParses() {
        return llForTheTopNParses;
    }

    public RulesCounts getRcForTopNParses() {
        return rcForTopNParses;
    }

//    public String getRetFrameValueStr() {
//        return retFrameValueStr;
//    }
    public ParseFrame getBestFrame() {
        return bestFrame;
    }

    public double getSumProbForBestParse() {
        return sumProbForBestParse;
    }

    public double getLlForAll() {
        return llForAll;
    }
    
    

}
