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


import frameinduction.grammar.RuleMaps;
import frameinduction.evaluation.EvaluationResultAndProcessContainer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import frameinduction.settings.ExperimentPathBuilder;
import java.io.File;
import java.io.FileWriter;

/**
 *
 * @author Behrang Qasemizadeh <zadeh at phil.hhu.de>
 */
public class PrintStatistics {

    final private boolean printDetailEval; // each step in the split
    final private boolean printIterationSummary; // overal changes after all the split and merge splits // once after slpit, one after merge
    private String tabbingForPrint;
    private PrintWriter pwEMPlotReport;
    private PrintWriter pwSummaryForTest;
    EvaluationResultAndProcessContainer evalContainer;
    final AtomicInteger orderCounter = new AtomicInteger();
    private double TOP_N_PARSE_PORTION_TO_CHOOSE = .01;

    public PrintStatistics(
            boolean printDetailEval,
            boolean printIterationSummary,
            ExperimentPathBuilder path,
            // String tabbingForPrint,
            EvaluationResultAndProcessContainer evalContainer) throws IOException {
        
        this.printDetailEval = printDetailEval;
        this.printIterationSummary = printIterationSummary;
        
        this.tabbingForPrint = "";
        
        this.evalContainer = evalContainer;
        pwEMPlotReport = new PrintWriter(new FileWriter(path.getPerformanceLogFile()));
        pwSummaryForTest = new PrintWriter(new FileWriter(new File(path.getExperimentRootFolder() + "short-summary.txt")));
        
    }

    public void setTabbingForPrint(String tabbingForPrint) {
        this.tabbingForPrint = tabbingForPrint;
    }
     
    
 

    public void printCurentEvaluationAtThisStage(int level, RuleMaps rm) throws IOException, Exception {
        String additionalData = "";
        logCurrentState(level, rm,additionalData);

    }
    
    
     /**
     * -999 for only summary
     *
     * @param level
     * @param rm
     * @param additionalInformation
     * @throws IOException
     * @throws Exception
     */
    public void logCurrentState(int level, RuleMaps rm, String additionalInformation) throws IOException, Exception {

        
        
        if (level == -999 && (printIterationSummary||printDetailEval)) {
            evalContainer.addEvaluationPointMemWise(rm, true, orderCounter.incrementAndGet(), TOP_N_PARSE_PORTION_TO_CHOOSE);
            String toWebChartFile = evalContainer.toWebChartFile();
            String toWebChartFileRole = evalContainer.toWebChartRoleFile();
            System.err.println("XML results are written in \n\t" + toWebChartFile + "\n\t " + toWebChartFileRole);
            String toWrite = tabbingForPrint + level + "\t:" + evalContainer.getCurrentIterationEvalSummary() + " " + evalContainer.getEvaluationGoldFile();
            System.err.println("> " + toWrite);
            pwEMPlotReport.println(toWrite);
            pwEMPlotReport.flush();
            pwSummaryForTest.println(
                    tabbingForPrint + "\t"
                    + evalContainer.getCurrentIterationEvalSummary()
                    + "\t\t"
                    + evalContainer.getEvaluationGoldFile()
                    + " " + additionalInformation
            ); //+ " " + getCurrentParams());
            pwSummaryForTest.flush();
            
        }

    }

    public void close() {
        if (this.pwEMPlotReport != null) {
            pwEMPlotReport.close();
        }
        if (this.pwSummaryForTest != null) {
            pwSummaryForTest.close();
        }
    }

    public EvaluationResultAndProcessContainer getEvalContainer() {
        return evalContainer;
    }
    
    

}
