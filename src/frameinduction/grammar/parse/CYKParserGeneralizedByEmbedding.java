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

import mhutil.HelperRuleMapsMethods;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenGrammarRoleFixedIndeOneHPClus;
import frameinduction.grammar.learn.MathUtil;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.ExperimentPathBuilder;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mhutil.HelperFragmentMethods;
import util.embedding.WordPairSimilarityContainer;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CYKParserGeneralizedByEmbedding implements PCFGParser {

    //  private final WordPairSimilaritySmallMemoryFixedID wf;
    private final RuleMaps rm;
    private final WordPairSimilarityContainer wsContainer;
    //private final Set<Integer> idsUsedInUnification;
    private final double threasholdOnSimilarity = 0.7;//0.75;// 0.3;//1e-7;
    //private final double thresholdOnMinSim =0.480;

    public CYKParserGeneralizedByEmbedding(WordPairSimilarityContainer wfd, RuleMaps rm) throws Exception {

        wsContainer = wfd;
        this.rm = rm;
        //idsUsedInUnification=new HashSet<>();
    }

    private void innerWeigtedUnification(Fragment f, ParseChart pc, FragmentCompact fc, int k, int tID) throws Exception {
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        boolean toContinue = true;
        int count=0;
        double thre = threasholdOnSimilarity;
        while (toContinue) {

            for (int wordsID : reversUnaryRuleMap.keySet()) {

                Double measureSim = this.wsContainer.getSimValue(tID, wordsID);
                if (measureSim > Math.max(0, thre)) {
                    count++;
                    Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(wordsID);
                    Set<Map.Entry<Integer, Double>> entrySet = lhsToUnary.entrySet();
                    for (Map.Entry<Integer, Double> e : entrySet) {
                        Integer lhsSymbol = e.getKey();
                        Double param = e.getValue();
                        // \theta^log(sim(v,t)) gices a good result
                        Double multiply
                                = // MathUtil.logSumExp(
                               param
                                        +
                              Math.log(
                                    measureSim     
                               ) ;
//,
                                //                            + Math.log(
                                //                            measureSim),
                                //    param) // )
                                ;
                        // System.err.println(multiply);
                        if (Double.isFinite(multiply)) {
                            CNFRule cnf = new CNFRule(lhsSymbol, tID, k - 1, k, multiply);
                            String positionInSent = fc.getPositionsInSentence()[k - 1];
                            cnf.setPositionInSent(positionInSent);

                            pc.addValue(k - 1, k, cnf);
//                        System.err.println("HERE EMBD: " + multiply + " " + measureSim + " " + param + " " + lhsSymbol + " " + rm.getSymbolFromID(lhsSymbol));
//                        pc.addValue(start, end, null); .incInsideParam(k - 1, k, lhsSymbol, param);

                        } else{
                            System.err.println("Warning .... in embedding based parsing  ");
                        }
                    }
                }
             
            }
            if (count > 6) {
               // if (thre < thresholdOnMinSim) {
                    toContinue = false;
               // }
                // toContinue = false;
            }
            if (thre <= 0) {
                
                // then there is not way to get a parse for this input
                System.err.println("** ---> 2524 --> " + tID+" "+ rm.getSymbolFromID(tID) +" " + f.toStringPosition() +" "  );
                toContinue = false;
            }
            thre -= 0.02;
        }

    }
//    

    public ParseChart parse(Fragment f,FragmentCompact fc, RuleMaps rm) throws Exception {
        int sentLength = fc.getTerminals().length;
        ParseChart pc = new ParseChart(sentLength);
        for (int k = 1; k <= sentLength; k++) {

            if (k % 2 == 1) {
                //if (k == 1) {
                int tID = fc.getTerminals()[k - 1];
                // innerWeigtedUnification12More(k, tID);
                //innerWeigtedUnificationPoP(k, tID,dimensionTemp);
                innerWeigtedUnification(f,pc, fc, k, tID);

//                dynamic threasholding gave worst results
//                double threasholdOnSimilarityMax = threasholdOnSimilarity;
//                while (true) {
//                    boolean innerWeigtedUnification = innerWeigtedUnification(pc, fc, k, tID, threasholdOnSimilarityMax);
//                    if (innerWeigtedUnification) {
//                        break;
//                    } else {
//
//                        threasholdOnSimilarityMax = Math.max(0, threasholdOnSimilarityMax - .05);
//                    }
//                }
            } else {
                int rhsSymbol = fc.getTerminals()[k - 1];
                String positionInSent = fc.getPositionsInSentence()[k - 1];
                Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(rhsSymbol);
                Set<Map.Entry<Integer, Double>> iteratorUnary = lhsToUnary.entrySet();
                for (Map.Entry<Integer, Double> e : iteratorUnary) {
                    int lhsSymbol = e.getKey();
                    double param = e.getValue();
                    CNFRule cnf = new CNFRule(lhsSymbol, rhsSymbol, k - 1, k, param);
                    cnf.setPositionInSent(positionInSent);
                    pc.addValue(k - 1, k, cnf);

                }
            }
        }
        for (int width = 2; width <= sentLength; width++) {
            for (int i = 0; i <= sentLength - width; i++) {
                int k = i + width;
                for (int j = i + 1; j <= k - 1; j++) {
                    Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap = rm.getReverseBinaryRuleMap();
                    Set<Map.Entry<Integer, Map<Integer, Map<Integer, Double>>>> iteratorLhs = reverseBinaryRuleMap.entrySet();
                    for (Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> e : iteratorLhs) {
                        int rhsB1 = e.getKey();
                        Set<Map.Entry<Integer, Map<Integer, Double>>> iteratorRHSBC = e.getValue().entrySet();
                        for (Map.Entry<Integer, Map<Integer, Double>> e1 : iteratorRHSBC) {
                            int rhsC2 = e1.getKey();
                            Map<Integer, Double> lhsAndParamvalue = e1.getValue();
                            Set<Map.Entry<Integer, Double>> iteratorLHSParam = lhsAndParamvalue.entrySet();
                            for (Map.Entry<Integer, Double> e2 : iteratorLHSParam) {
                                int lhsA = e2.getKey();
                                double lhsToRHS1RhS2Param = e2.getValue();
                                //List<CNFRule> cellLeft = pc.getCell(i, j, rhsB1);
                                List<CNFRule> cellLeft = pc.getCell(i, j);
                                if (cellLeft != null) {
                                    //List<CNFRule> cellRight = pc.getCell(j, k, rhsC2);
                                    List<CNFRule> cellRight = pc.getCell(j, k);
                                    if (cellRight != null) {
                                        for (CNFRule cnfleft : cellLeft) {
                                            if (cnfleft.getSymbolLhs() == rhsB1) {
                                                for (CNFRule cnfright : cellRight) {
                                                    if (cnfright.getSymbolLhs() == rhsC2) {
                                                        double thisProb = lhsToRHS1RhS2Param + cnfleft.getProb() + cnfright.getProb();
                                                        //if (thisProb > 0) {
                                                        CNFRule cnfForThis = new CNFRule(lhsA, cnfleft, cnfright, thisProb);
                                                        pc.addValue(i, k, cnfForThis);
                                                        //}
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }

                            }
                        }

                    }

                }

            }

        }

        return pc;
//        this.ZParam = ioChart.getInsideValueFor(0, sentLength, this.rm.getStartSymbolID());
//        //System.out.println("Z param " + ZParam);
//        
//        if (ZParam == 0.0) {
//            System.out.println(Arrays.toString(this.fc.getTerminals()));
//        }
    }

    private boolean innerWeigtedUnification(ParseChart pc, FragmentCompact fc, int k, int tID, double threshold) throws Exception {
        Map<Integer, Map<Integer, Double>> reversUnaryRuleMap = rm.getReversUnaryRuleMap();
        boolean returnValue = false;
        for (int wordsID : reversUnaryRuleMap.keySet()) {

            Double measureSim = this.wsContainer.getSimValue(tID, wordsID);
            if (measureSim > threshold) {
                returnValue = true;
                //this.idsUsedInUnification.add(wordsID);
                Map<Integer, Double> lhsToUnary = rm.getLhsToUnary(wordsID);
                Set<Map.Entry<Integer, Double>> entrySet = lhsToUnary.entrySet();
                for (Map.Entry<Integer, Double> e : entrySet) {
                    Integer lhsSymbol = e.getKey();
                    Double param = e.getValue();
                    // \theta^log(sim(v,t)) gices a good result
                    Double multiply
                            = param
                            + Math.log(
                                    measureSim);
                    // System.err.println(multiply);
                    if (Double.isFinite(multiply)) {
                        CNFRule cnf = new CNFRule(lhsSymbol, tID, k - 1, k, multiply);
                        String positionInSent = fc.getPositionsInSentence()[k - 1];
                        cnf.setPositionInSent(positionInSent);

                        pc.addValue(k - 1, k, cnf);
//                        System.err.println("HERE EMBD: " + multiply + " " + measureSim + " " + param + " " + lhsSymbol + " " + rm.getSymbolFromID(lhsSymbol));
//                        pc.addValue(start, end, null); .incInsideParam(k - 1, k, lhsSymbol, param);

                    }
                }
            }
        }
        return returnValue;
    }

    public static void main(String[] args) throws IOException, Exception {

        String masterInputParsedFilePath = "../lr_other/stanford-parse.txt";
        List<String> gFiles = new ArrayList<>();

        //   gFiles.add("../golddata/annotations/frame-filter-15-125.txt");
        //gFiles.add("../golddata/annotations/framenet-ambig.txt");
        gFiles.add("../golddata/annotations/framenet-eval4.txt");
        gFiles.add("../golddata/annotations/dev1.txt");

        ExperimentPathBuilder exp = new ExperimentPathBuilder("temp/embdPars/", gFiles.get(0));
        Settings settings1 = new Settings();
        List<Fragment> inputFragmentToBuildModel = HelperFragmentMethods.parseTreeToFragments(
                masterInputParsedFilePath, null, null, settings1,
                -1,
                11, exp.getGoldData());

        GenGrammarRoleFixedIndeOneHPClus gph = new GenGrammarRoleFixedIndeOneHPClus(settings1.getActiveDepToIntMap());
        gph.genRules(inputFragmentToBuildModel);
        RuleMaps theRuleMap = gph.getTheRuleMap(inputFragmentToBuildModel);
        EvaluationResultAndProcessContainer evcGold1 = new EvaluationResultAndProcessContainer(gFiles.get(1), inputFragmentToBuildModel, exp);
        evcGold1.buildDeafaultBaselines(theRuleMap);
        String baselinesToString = evcGold1.baselinesToString();
        System.err.println(baselinesToString);

        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(inputFragmentToBuildModel, theRuleMap);
        evcGold1.evaluationFromParsed(0, doParsingToBestFrames);
        String currentIterationEvalSummary = evcGold1.getCurrentIterationEvalSummary();
        System.err.println("--> \n" + currentIterationEvalSummary);
        // make sure both settings have the same dependency map

//
//        
        List<Fragment> inputFragmentsToParse = HelperFragmentMethods.parseTreeToFragments(
                masterInputParsedFilePath, null, null, settings1,
                -1,
                11,
                gFiles.get(1));

        HelperRuleMapsMethods.addNewTerminals(inputFragmentsToParse, theRuleMap);

        //evcGold1.evaluationFromParsed(0, doParsingToBestFrames);
        System.err.println("--> \n" + currentIterationEvalSummary);

        WordPairSimilaritySmallMemoryFixedID wsmFi = new WordPairSimilaritySmallMemoryFixedID(exp.getPathToVector(), inputFragmentToBuildModel, 999);
        WordPairSimilarityContainer wsp = new WordPairSimilarityContainer(theRuleMap, wsmFi);
        CYKParserGeneralizedByEmbedding cykEbm = new CYKParserGeneralizedByEmbedding(wsp, theRuleMap);
        for (Fragment f : inputFragmentToBuildModel) {
            FragmentCompact fragmentCompact = new FragmentCompact(f, theRuleMap);
            ParseChart parse = cykEbm.parse(f,fragmentCompact, theRuleMap);
            if (parse != null && parse.hasParse(theRuleMap.getStartSymbolID())) {
                CNFRule mostProbableParse = parse.getMostProbableParse(theRuleMap.getStartSymbolID());
                ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame("a", mostProbableParse, theRuleMap);
                System.err.println(parseTreeToFrame.toString());
            }
        }
//        write the main for test

    }
}
