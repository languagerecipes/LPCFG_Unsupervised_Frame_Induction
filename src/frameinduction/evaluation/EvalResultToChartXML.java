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
package frameinduction.evaluation;


import java.text.DecimalFormat;
import java.util.List;
import semeval.utils.EvaluationResult;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class EvalResultToChartXML {

    public EvalResultToChartXML() {
        iterationPrecisionTik =1;
    }
    
 public EvalResultToChartXML(int iterationTik) {
        iterationPrecisionTik =iterationTik;
    }
    
    
    private int coulrCounter = 0;
    private int iterationPrecisionTik =1;
    private static String colors = "#d6a090\n"
            + "#fe3b1e\n"
            + "#a12c32\n"
            + "#fa2f7a\n"
            + "#fb9fda\n"
            + "#e61cf7\n"
            + "#992f7c\n"
            + "#47011f\n"
            + "#051155\n"
            + "#4f02ec\n"
            + "#2d69cb\n"
            + "#00a6ee\n"
            + "#6febff\n"
            + "#08a29a\n"
            + "#2a666a\n"
            + "#063619\n"
            + "#000000\n"
            + "#4a4957\n"
            + "#8e7ba4\n"
            + "#b7c0ff\n"
            + "#fff777\n"
            + "#acbe9c\n"
            + "#827c70\n"
            + "#5a3b1c\n"
            + "#ae6507\n"
            + "#f7aa30\n"
            + "#f4ea5c\n"
            + "#9b9500\n"
            + "#566204\n"
            + "#11963b\n"
            + "#51e113\n"
            + "#08fdcc";
    private static String[] colurList = colors.split("\n");

    private static String numberFormat(double number) {
        DecimalFormat df = new DecimalFormat("####0.00");
        return df.format(number);
    }

    /**
     *
     * @param suufixIdentifierMain
     * @param evList
     * @param baselines
     * @param baselineLabels
     * @param rndBaselines
     */
    public StringBuilder process(
            String suufixIdentifierMain, List<EvaluationResult> evList, List<EvaluationResult> baselines, List<String> baselineLabels, List<EvaluationResult> rndBaselines) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        sb.append(makeCategories(evList)).append("\n");
        coulrCounter = 0;
        StringBuilder processMain = processMain(evList, suufixIdentifierMain);
        sb.append(processMain).append("\n");
        StringBuilder rnadomPerIteration = processRandomBaselinePerIteration(rndBaselines);

        sb.append(rnadomPerIteration).append("\n");
        int count = 0;
        for (EvaluationResult baseline : baselines) {
            StringBuilder processBaseline = processBaseline(evList.size(), baseline, baselineLabels.get(count++));
            sb.append(processBaseline).append("\n");
        }

        sb.append("</chart>\n");
        return sb;

    }

    public void process(List<EvaluationResult> evList, List<EvaluationResult> baselines, List<String> baselineLabels) {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        sb.append(makeCategories(baselines));
        StringBuilder processMain = processMain(evList, "m");
        sb.append(processMain).append("\n");
        int count = 0;
        for (EvaluationResult baseline : baselines) {
            StringBuilder processBaseline = processBaseline(evList.size(), baseline, baselineLabels.get(count++));
            sb.append(processBaseline).append("\n");
        }

        sb.append("</chart>\n");
        System.out.println(sb.toString());
    }

    /**
     *
     * @param evList
     * @param suffixForTitle
     * @return
     */
    private StringBuilder processMain(List<EvaluationResult> evList, String suffixForTitle) {
        StringBuilder sb = new StringBuilder();
        Double[] purity = new Double[evList.size()], inversePurity = new Double[evList.size()], puIpuF = new Double[evList.size()],
                bCubedPrecision;
        bCubedPrecision = new Double[evList.size()];
        Double[] bCubedRecall = new Double[evList.size()], bCubedF1 = new Double[evList.size()],
                pairCountPrecision = new Double[evList.size()],
                pairCountRecall;
        pairCountRecall = new Double[evList.size()];
        Double[] pairCountF1 = new Double[evList.size()], pairCountRandIndex = new Double[evList.size()], pairCountAdjustedRandIndex = new Double[evList.size()],
                entropyNormalizedVariOfInfo;
        entropyNormalizedVariOfInfo = new Double[evList.size()];
        Double[] entropyVariOfInfo = new Double[evList.size()], entropyVMeasure = new Double[evList.size()];

        Double[] sysClusterNum = new Double[evList.size()];
        Double[] countValidParses = new Double[evList.size()];
        Double[] countUnaryRules = new Double[evList.size()];
        Double[] countBinRules = new Double[evList.size()];
        Double[] likelihood = new Double[evList.size()];
        Double[] changeLikelihood = new Double[evList.size()];
        Double[] bestParseLikelihood = new Double[evList.size()];
        Double[] bestTopNParseLikelihood = new Double[evList.size()];

        for (int i = 0; i < evList.size(); i+=iterationPrecisionTik) {
            EvaluationResult ev = evList.get(i);
            // group 1
            purity[i] = ev.getPurity();
            inversePurity[i] = ev.getInversePurity();
            puIpuF[i] = ev.getPuIpuf1();
            // group2
            bCubedPrecision[i] = ev.getBCubedPrecision();
            bCubedRecall[i] = ev.getBCubedRecall();
            bCubedF1[i] = ev.getbCubedF1();

            // group3
            pairCountPrecision[i] = ev.getPairCountPrecision();
            pairCountRecall[i] = ev.getPairCountRecall();
            pairCountF1[i] = ev.getPairCountF1();

            //group4
            pairCountRandIndex[i] = ev.getPairCountRandIndex();

            // group 5
            pairCountAdjustedRandIndex[i] = ev.getPairCountAdjustedRandIndex();

            // group 6
            entropyNormalizedVariOfInfo[i] = ev.getEntropyNormalizedVariOfInfo();

            // g7
            entropyVariOfInfo[i] = ev.getEntropyVariOfInfo();

            // group 8
            entropyVMeasure[i] = ev.getEntropyVMeasure();

            //g 9
            sysClusterNum[i] = (double) ev.getSysClusterNum();

            countValidParses[i] = (double)ev.getCountValidParses();

            likelihood[i] = ev.getLikelihood();

            changeLikelihood[i] = ev.getChangeInLikelihood();

            bestParseLikelihood[i] = ev.getLikelihoodForBestParses();
            bestTopNParseLikelihood[i] = ev.getLikelihoodForChosenNBestParses();
            countUnaryRules[i] = (double)ev.getNumberOFUnaryRules();
            countBinRules[i] = (double)ev.getNumberOfBinRules();

        }

        String[] puritySet = new String[3];
        puritySet[0] = "Pu" + suffixForTitle;
        puritySet[1] = "iPu" + suffixForTitle;
        puritySet[2] = "FPuiPu" + suffixForTitle;
        Double[][] puipf = {purity, inversePurity, puIpuF};
        String axisPurity = getAxisMain("Set-Match" + suffixForTitle, 0, puipf, puritySet, "right", "0", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPurity);

        String[] bCubedSet = new String[3];
        bCubedSet[0] = "bCubdPr" + suffixForTitle;
        bCubedSet[1] = "bCubdRe" + suffixForTitle;
        bCubedSet[2] = "bcubedF" + suffixForTitle;
        Double[][] bCubed = {bCubedPrecision, bCubedRecall, bCubedF1};
        String axisBcubed = getAxisMain("B-Cubed" + suffixForTitle, 0, bCubed, bCubedSet, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisBcubed);

        String[] pairCountingSetTile = new String[3];
        pairCountingSetTile[0] = "pcPr" + suffixForTitle;
        pairCountingSetTile[1] = "pcRe" + suffixForTitle;
        pairCountingSetTile[2] = "pcF" + suffixForTitle;
        Double[][] pcSet = {pairCountPrecision, pairCountRecall, pairCountF1};
        String axisPairCounting = getAxisMain("PairCnt" + suffixForTitle, 0, pcSet, pairCountingSetTile, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPairCounting);

        String rndIndex = getAxisMain("RndIdx" + suffixForTitle, 0, pairCountRandIndex, "RndIdx" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(rndIndex);

        String adjRndIndex = getAxisMain("AdRndIdx" + suffixForTitle, 0, pairCountAdjustedRandIndex, "AdRndIdx" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(adjRndIndex);

        String vMeasure = getAxisMain("Vmsr" + suffixForTitle, 0, entropyVMeasure, "Vmsr" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(vMeasure);

        String entropyNormalizedVariOfInfoString = getAxisMain("nlzVIn" + suffixForTitle, 0, entropyNormalizedVariOfInfo, "nlzVIn" + suffixForTitle,
                "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(entropyNormalizedVariOfInfoString);

        //  String entropyVariOfInfoString = getAxis("varInfo", 0, entropyVariOfInfo, "varInfo", "right", "", "1");
        //  sb.append(entropyVariOfInfoString);
        String sysClusterNumString = getAxisMain("|Clusts.|" + suffixForTitle, 0, sysClusterNum, "|CL|" + suffixForTitle, "left", "axisonleft=\"0\" showAxis=\"0\" minvalue=\"0\"", "1");
        sb.append(sysClusterNumString);

        String sysValidParseCount = getAxisMain("|VdPrs|" + suffixForTitle, 0, countValidParses, "|VdPrs|" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(sysValidParseCount);
        String logLikelihoodStr = getAxisMain("lgLkhd" + suffixForTitle, 0, likelihood, "lgLkhd" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(logLikelihoodStr);
        String difLogLikelihoodStr = getAxisMain("dif-LL" + suffixForTitle, 0, changeLikelihood, "dif-LL" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(difLogLikelihoodStr);
        String bestParseLikelihoodStr = getAxisMain("bPrsLL" + suffixForTitle, 0, bestParseLikelihood, "bPrsLL" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(bestParseLikelihoodStr);

        String bestTopNParseLikelihoodStr = getAxisBaseline("bNpsLL" + suffixForTitle, 0, bestTopNParseLikelihood, "bNpsLL" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(bestTopNParseLikelihoodStr);
        
        String unrayRuleCountStr = getAxisMain("|R-U|" + suffixForTitle, 0, countUnaryRules, "|R-U|" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(unrayRuleCountStr);
        String countBinRulesStr = getAxisMain("|R-B|" + suffixForTitle, 0, countBinRules, "|R-B|" + suffixForTitle, "left", "axisonleft=\"0\"", "1");
        sb.append(countBinRulesStr);

        return sb;
    }

    private StringBuilder processRandomBaselinePerIteration(List<EvaluationResult> evRandomList) {
        coulrCounter = 0;
        String suffixForTitle = "-R";
        StringBuilder sb = new StringBuilder();
        Double[] purity = new Double[evRandomList.size()], inversePurity = new Double[evRandomList.size()], puIpuF = new Double[evRandomList.size()],
                bCubedPrecision;
        bCubedPrecision = new Double[evRandomList.size()];
        Double[] bCubedRecall = new Double[evRandomList.size()], bCubedF1 = new Double[evRandomList.size()],
                pairCountPrecision = new Double[evRandomList.size()],
                pairCountRecall;
        pairCountRecall = new Double[evRandomList.size()];
        Double[] pairCountF1 = new Double[evRandomList.size()], pairCountRandIndex = new Double[evRandomList.size()], pairCountAdjustedRandIndex = new Double[evRandomList.size()],
                entropyNormalizedVariOfInfo;
        entropyNormalizedVariOfInfo = new Double[evRandomList.size()];
        Double[] entropyVariOfInfo = new Double[evRandomList.size()], entropyVMeasure = new Double[evRandomList.size()];

        Double[] sysClusterNum = new Double[evRandomList.size()];
        Double[] countValidParses = new Double[evRandomList.size()];
        Double[] countUnaryRules = new Double[evRandomList.size()];
        Double[] countBinRules = new Double[evRandomList.size()];
        Double[] likelihood = new Double[evRandomList.size()];
        Double[] changeLikelihood = new Double[evRandomList.size()];
        Double[] bestParseLikelihood = new Double[evRandomList.size()];
        Double[] bestTopNParseLikelihood = new Double[evRandomList.size()];
        for (int i = 0; i < evRandomList.size(); i++) {
            EvaluationResult ev = evRandomList.get(i);
            // group 1
            purity[i] = ev.getPurity();
            inversePurity[i] = ev.getInversePurity();
            puIpuF[i] = ev.getPuIpuf1();
            // group2
            bCubedPrecision[i] = ev.getBCubedPrecision();
            bCubedRecall[i] = ev.getBCubedRecall();
            bCubedF1[i] = ev.getbCubedF1();

            // group3
            pairCountPrecision[i] = ev.getPairCountPrecision();
            pairCountRecall[i] = ev.getPairCountRecall();
            pairCountF1[i] = ev.getPairCountF1();

            //group4
            pairCountRandIndex[i] = ev.getPairCountRandIndex();

            // group 5
            pairCountAdjustedRandIndex[i] = ev.getPairCountAdjustedRandIndex();

            // group 6
            entropyNormalizedVariOfInfo[i] = ev.getEntropyNormalizedVariOfInfo();

            // g7
            entropyVariOfInfo[i] = ev.getEntropyVariOfInfo();

            // group 8
            entropyVMeasure[i] = ev.getEntropyVMeasure();

            //g 9
            sysClusterNum[i] = (double)ev.getSysClusterNum();

            countValidParses[i] = (double)ev.getCountValidParses();

            likelihood[i] = ev.getLikelihood();

            changeLikelihood[i] = ev.getChangeInLikelihood();

            bestParseLikelihood[i] = ev.getLikelihoodForBestParses();
            bestTopNParseLikelihood[i]= ev.getLikelihoodForChosenNBestParses();
            countUnaryRules[i] = (double)ev.getNumberOFUnaryRules();
            countBinRules[i] = (double)ev.getNumberOfBinRules();

        }

        String[] puritySet = new String[3];
        puritySet[0] = "Pu" + suffixForTitle;
        puritySet[1] = "iPu" + suffixForTitle;
        puritySet[2] = "FPuiPu" + suffixForTitle;
        Double[][] puipf = {purity, inversePurity, puIpuF};
        String axisPurity = getAxisBaseline("Set-Match" + suffixForTitle, 0, puipf, puritySet, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPurity);

        String[] bCubedSet = new String[3];
        bCubedSet[0] = "bCubdPr" + suffixForTitle;
        bCubedSet[1] = "bCubdRe" + suffixForTitle;
        bCubedSet[2] = "bcubedF" + suffixForTitle;
        Double[][] bCubed = {bCubedPrecision, bCubedRecall, bCubedF1};
        String axisBcubed = getAxisBaseline("B-Cubed" + suffixForTitle, 0, bCubed, bCubedSet, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisBcubed);

        String[] pairCountingSetTile = new String[3];
        pairCountingSetTile[0] = "pcPr" + suffixForTitle;
        pairCountingSetTile[1] = "pcRe" + suffixForTitle;
        pairCountingSetTile[2] = "pcF" + suffixForTitle;
        Double[][] pcSet = {pairCountPrecision, pairCountRecall, pairCountF1};
        String axisPairCounting = getAxisBaseline("PairCnt" + suffixForTitle, 0, pcSet, pairCountingSetTile, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPairCounting);

        String rndIndex = getAxisBaseline("RndIdx" + suffixForTitle, 0, pairCountRandIndex, "RndIdx" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(rndIndex);

        String adjRndIndex = getAxisBaseline("AdRndIdx" + suffixForTitle, 0, pairCountAdjustedRandIndex, "AdRndIdx" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(adjRndIndex);

        String vMeasure = getAxisBaseline("Vmsr" + suffixForTitle, 0, entropyVMeasure, "Vmsr" + suffixForTitle, "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(vMeasure);

        String entropyNormalizedVariOfInfoString = getAxisBaseline("nlzVIn" + suffixForTitle, 0, entropyNormalizedVariOfInfo, "nlzVIn" + suffixForTitle,
                "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(entropyNormalizedVariOfInfoString);

//        String entropyVariOfInfoString = getAxisBaseline("vInf", 0, entropyVariOfInfo, "vInf", "right", "", "1");
//        sb.append(entropyVariOfInfoString);
        
        String sysClusterNumString = getAxisBaseline("|Clusts.|" + suffixForTitle, 0, sysClusterNum, "|CL|" + suffixForTitle, "left", "axisonleft=\"0\" showAxis=\"0\" minvalue=\"0\"", "1");
        sb.append(sysClusterNumString);

        String sysValidParseCount = getAxisBaseline("|VdPrs|" + suffixForTitle, 0, countValidParses, "|VdPrs|" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(sysValidParseCount);
        String logLikelihoodStr = getAxisBaseline("lgLkhd" + suffixForTitle, 0, likelihood, "lgLkhd" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(logLikelihoodStr);
        String difLogLikelihoodStr = getAxisBaseline("dif-LL" + suffixForTitle, 0, changeLikelihood, "dif-LL" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(difLogLikelihoodStr);
        String bestParseLikelihoodStr = getAxisBaseline("bPrsLL" + suffixForTitle, 0, bestParseLikelihood, "bPrsLL" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(bestParseLikelihoodStr);

        String bestTopNParseLikelihoodStr = getAxisBaseline("bNpsLL" + suffixForTitle, 0, bestTopNParseLikelihood, "bNpsLL" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(bestTopNParseLikelihoodStr);
        
        String unrayRuleCountStr = getAxisBaseline("|R-U|" + suffixForTitle, 0, countUnaryRules, "|R-U|" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(unrayRuleCountStr);
        String countBinRulesStr = getAxisBaseline("|R-B|" + suffixForTitle, 0, countBinRules, "|R-B|" + suffixForTitle, "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(countBinRulesStr);

        return sb;
    }

    public StringBuilder processBaseline(int iterations, EvaluationResult evBaseline, String baselineName) {
        coulrCounter = 0;
        StringBuilder sb = new StringBuilder();
        Double[] purity = new Double[iterations], inversePurity = new Double[iterations], puIpuF = new Double[iterations],
                bCubedPrecision;
        bCubedPrecision = new Double[iterations];
        Double[] bCubedRecall = new Double[iterations], bCubedF1 = new Double[iterations],
                pairCountPrecision = new Double[iterations],
                pairCountRecall;
        pairCountRecall = new Double[iterations];
        Double[] pairCountF1 = new Double[iterations], pairCountRandIndex = new Double[iterations], pairCountAdjustedRandIndex = new Double[iterations],
                entropyNormalizedVariOfInfo;
        entropyNormalizedVariOfInfo = new Double[iterations];
        Double[] entropyVariOfInfo = new Double[iterations], entropyVMeasure = new Double[iterations];

        Double[] sysClusterNum = new Double[iterations];
        Double[] countValidParses = new Double[iterations];
        Double[] countUnaryRules = new Double[iterations];
        Double[] countBinRules = new Double[iterations];
        Double[] likelihood = new Double[iterations];
        Double[] changeLikelihood = new Double[iterations];
        Double[] bestTopNParseLikelihood = new Double[iterations];
        Double[] bestParseLikelihood = new Double[iterations];

        for (int i = 0; i < iterations; i+=iterationPrecisionTik) {
            //EvaluationResult evBaseline = evList.get(i);
            // group 1
            purity[i] = evBaseline.getPurity();
            inversePurity[i] = evBaseline.getInversePurity();
            puIpuF[i] = evBaseline.getPuIpuf1();
            // group2
            bCubedPrecision[i] = evBaseline.getBCubedPrecision();
            bCubedRecall[i] = evBaseline.getBCubedRecall();
            bCubedF1[i] = evBaseline.getbCubedF1();

            // group3
            pairCountPrecision[i] = evBaseline.getPairCountPrecision();
            pairCountRecall[i] = evBaseline.getPairCountRecall();
            pairCountF1[i] = evBaseline.getPairCountF1();

            //group4
            pairCountRandIndex[i] = evBaseline.getPairCountRandIndex();

            // group 5
            pairCountAdjustedRandIndex[i] = evBaseline.getPairCountAdjustedRandIndex();

            // group 6
            entropyNormalizedVariOfInfo[i] = evBaseline.getEntropyNormalizedVariOfInfo();

            // g7
            entropyVariOfInfo[i] = evBaseline.getEntropyVariOfInfo();

            // group 8
            entropyVMeasure[i] = evBaseline.getEntropyVMeasure();

            //g 9
            sysClusterNum[i] = (double)evBaseline.getSysClusterNum();

            countValidParses[i] = (double)evBaseline.getCountValidParses();

            likelihood[i] = evBaseline.getLikelihood();

            changeLikelihood[i] = evBaseline.getChangeInLikelihood();
            bestTopNParseLikelihood[i] = evBaseline.getLikelihoodForChosenNBestParses();
            bestParseLikelihood[i] = evBaseline.getLikelihoodForBestParses();
            countUnaryRules[i] =(double) evBaseline.getNumberOFUnaryRules();
            countBinRules[i] =(double) evBaseline.getNumberOfBinRules();

        }

        String[] puritySet = new String[3];
        puritySet[0] = baselineName + "Pu";
        puritySet[1] = baselineName + "iPu";
        puritySet[2] = baselineName + "FPuiPu";
        Double[][] puipf = {purity, inversePurity, puIpuF};
        String axisPurity = getAxisBaseline(baselineName + "Set-Match", 0, puipf, puritySet, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPurity);

        String[] bCubedSet = new String[3];
        bCubedSet[0] = baselineName + "bCubdPr";
        bCubedSet[1] = baselineName + "bCubdRe";
        bCubedSet[2] = baselineName + "bcubedF";
        Double[][] bCubed = {bCubedPrecision, bCubedRecall, bCubedF1};
        String axisBcubed = getAxisBaseline(baselineName + "B-Cubed", 0, bCubed, bCubedSet, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisBcubed);

        String[] pairCountingSetTile = new String[3];
        pairCountingSetTile[0] = baselineName + "pcPr";
        pairCountingSetTile[1] = baselineName + "pcRe";
        pairCountingSetTile[2] = baselineName + "pcF";
        Double[][] pcSet = {pairCountPrecision, pairCountRecall, pairCountF1};
        String axisPairCounting = getAxisBaseline(baselineName + "PairCnt", 0, pcSet, pairCountingSetTile, "right", "1", " minvalue=\"0\" maxvalue=\"100\" ", 0);
        sb.append(axisPairCounting);

        String rndIndex = getAxisBaseline(baselineName + "RndIdx", 0, pairCountRandIndex, baselineName + "RndIdx", "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(rndIndex);

        String adjRndIndex = getAxisBaseline(baselineName + "AdRndIdx", 0, pairCountAdjustedRandIndex, baselineName + "AdRndIdx", "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(adjRndIndex);

        String vMeasure = getAxisBaseline(baselineName + "Vmsr", 0, entropyVMeasure, baselineName + "Vmsr", "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(vMeasure);

        String entropyNormalizedVariOfInfoString = getAxisBaseline(baselineName + "nlzVIn", 0,
                entropyNormalizedVariOfInfo, baselineName + "nlzVIn", "right", " showAxis=\"0\" minvalue=\"0\" maxvalue=\"100\" ", "1");
        sb.append(entropyNormalizedVariOfInfoString);

        //String entropyVariOfInfoString = getAxis("varInfo", 0, entropyVariOfInfo, "varInfo", "right", "", "1");
        //sb.append(entropyVariOfInfoString);
        String sysClusterNumString = getAxisBaseline(baselineName + "|Clusts.|", 0, sysClusterNum, baselineName + "|CL|", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(sysClusterNumString);

        String sysValidParseCount = getAxisBaseline(baselineName + "|VdPrs|", 0, countValidParses, baselineName + "|VdPrs|", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(sysValidParseCount);
        String logLikelihoodStr = getAxisBaseline(baselineName + "lgLkhd", 0, likelihood, baselineName + "lgLkhd", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(logLikelihoodStr);
        String difLogLikelihoodStr = getAxisBaseline(baselineName + "dif-LL", 0, changeLikelihood, baselineName + "dif-LL", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(difLogLikelihoodStr);
        String bestParseLikelihoodStr = getAxisBaseline(baselineName + "bPrsLL", 0, bestParseLikelihood, baselineName + "bPrsLL", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(bestParseLikelihoodStr);

        String bestTopNParseLikelihoodStr = getAxisBaseline(baselineName+"bNpsLL" , 0, bestTopNParseLikelihood, baselineName+ "bNpsLL" , "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(bestTopNParseLikelihoodStr);
        
        String unrayRuleCountStr = getAxisBaseline("|R-U|", 0, countUnaryRules, baselineName + "|R-U|", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(unrayRuleCountStr);
        String countBinRulesStr = getAxisBaseline("|R-B|", 0, countBinRules, baselineName + "|R-B|", "left", " showAxis=\"0\" axisonleft=\"0\"", "1");
        sb.append(countBinRulesStr);
        return sb;

    }
//

    private String getAxisBaseline(String axisTitle, int tickWidth, Double[][] dataseries, String[] titleForSeries, String position, String initialyHidden,
            String otherOptions, int showAxis) {
        tickWidth = 1;
        StringBuilder sb = new StringBuilder("<axis title=\"" + axisTitle + "\" tickwidth=\"" + tickWidth + "\" divlineisdashed=\"1\" allowSelection=\"0\""
                + " color=\"" + colurList[coulrCounter] + "\" " + otherOptions
                + "showAxis=\"" + showAxis + "\""
                + ">\n"); // assign the color of the first series in this serries

        for (int i = 0; i < titleForSeries.length; i++) {
            String titleForSery = titleForSeries[i];
            String couler = colurList[coulrCounter++];
            sb.append(getDatasetOpeningBaseline(titleForSery, position, 1, couler, initialyHidden));
            Double[] data = dataseries[i];

            for (double d : data) {
                sb.append("\t<set value=\"" + numberFormat(d) + "\" />\n");
            }
            sb.append("</dataset>\n");
        }
        sb.append("</axis>\n");
        return sb.toString();
    }

    private String getAxisBaseline(String axisTitle, int tickWidth, Object[] dataseries, String titleForSeries, String position,
            String otherOptions, String initialyHidden) {
        tickWidth = 1;
        StringBuilder sb = new StringBuilder("<axis title=\"" + axisTitle
                + "\"  color=\"" + colurList[coulrCounter] + "\" tickwidth=\"" + tickWidth + "\" divlineisdashed=\"1\" allowSelection=\"0\" "
                + otherOptions //+ " dashed=\"1\""
                + ">\n");
        String couler = colurList[coulrCounter++];
        sb.append(getDatasetOpeningBaseline(titleForSeries, position, 1, couler, initialyHidden));

        for (Object d : dataseries) {
            String value = "";
            if (d instanceof Double) {
                value = numberFormat((Double) d);
            } else {
                value = d.toString();
            }
            sb.append("\t<set value=\"").append(value).append("\" />\n");
        }
        sb.append("</dataset>\n");
        sb.append("</axis>\n");
        return sb.toString();
    }

    private String getAxisMain(String axisTitle, int tickWidth, Double[][] dataseries, String[] titleForSeries, String position, String initialyHidden,
            String otherOptions, int showAxis) {
        StringBuilder sb = new StringBuilder("<axis title=\"" + axisTitle + "\" tickwidth=\"" + tickWidth + "\" divlineisdashed=\"1\" allowSelection=\"0\""
                + " color=\"" + colurList[coulrCounter] + "\" " + otherOptions
                + "showAxis=\"" + showAxis + "\""
                + ">\n"); // assign the color of the first series in this serries

        for (int i = 0; i < titleForSeries.length; i++) {
            String titleForSery = titleForSeries[i];
            String couler = colurList[coulrCounter++];
            sb.append(getDatasetOpening(titleForSery, position, 4, couler, initialyHidden));
            Double[] data = dataseries[i];

            for (double d : data) {
                sb.append("\t<set value=\"" + numberFormat(d) + "\" />\n");
            }
            sb.append("</dataset>\n");
        }
        sb.append("</axis>");
        return sb.toString();
    }

    private String getAxisMain(String axisTitle, int tickWidth, Object[] dataseries, String titleForSeries, String position,
            String otherOptions, String initialyHidden) {

        StringBuilder sb = new StringBuilder("<axis title=\"" + axisTitle
                + "\"  color=\"" + colurList[coulrCounter] + "\" tickwidth=\"" + tickWidth + "\" divlineisdashed=\"1\" allowSelection=\"0\" "
                + otherOptions + " >\n");
        String couler = colurList[coulrCounter++];
        sb.append(getDatasetOpening(titleForSeries, position, 4, couler, initialyHidden));

        for (Object d : dataseries) {
            String value = "";
            if (d instanceof Double) {
                value = numberFormat((Double) d);
            } else {
                value = d.toString();
            }
            sb.append("\t<set value=\"").append(value).append("\" />\n");
        }
        sb.append("</dataset>\n");
        sb.append("</axis>");
        return sb.toString();
    }

    private String getDatasetOpening(String title, String position, int linethickness, String color, String initialHidden) {
        return "<dataset titlepos=\"" + position + "\" seriesname=\"" + title + "\" linethickness=\"" + linethickness + "\" color=\"" + color + "\" "
                + " initiallyhidden=\"" + initialHidden + "\""
                + ">\n";

    }

    private String getDatasetOpeningBaseline(String title, String position, int linethickness, String color, String initialHidden) {
        return "<dataset titlepos=\"" + position + "\" seriesname=\"" + title + "\" linethickness=\"" + linethickness + "\" color=\"" + color + "\" "
                + " initiallyhidden=\"" + initialHidden + "\""
                + " dashed=\"1\">\n";

    }

    // convert to a method later
    private String header = "<chart palette=\"0\" caption=\"Frame Induction\" subcaption=\"Perforamnce Changes\" \n"
            + "xaxisname=\"Iteration\" showvalues=\"0\" divlinealpha=\"5010\" \n"
            + "numvdivlines=\"2\" vdivlinealpha=\"1\" showalternatevgridcolor=\"1\" \n"
            + "alternatevgridalpha=\"5\" canvaspadding=\"0\" labeldisplay=\"ROTATE\" showborder=\"0\" legendIconScale=\".5\" legendAllowDrag=\"1\""
            + " legendNumColumns=\"7\" setAdaptiveYMin='1'>\n";

    private String makeCategories(List<EvaluationResult> evResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("<categories>");
        for (int i = 0; i < evResult.size(); i++) {
            double iterationLabel = evResult.get(i).getLearningIteration();

            sb.append("<category label=\"").append(iterationLabel).append("\" stepskipped=\"flase\" appliedsmartlabel=\"true\" labeltooltext=\"Iteration " + iterationLabel + "\" />\n");
        }
        sb.append("</categories>\n");
        return sb.toString();
    }
}
