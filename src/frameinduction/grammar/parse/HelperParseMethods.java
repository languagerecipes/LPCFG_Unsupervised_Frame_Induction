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
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.FragmentCompact;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import util.embedding.WordPairSimilarityContainer;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author behra
 */
public class HelperParseMethods {

    
     /**
     * Parse the input fragment file given the grammar grmModel and dump the result in the form of std evaluation to outputParses file.
     * @param grmModel
     * @param fragmentFile
     * @param outputFile
     * @return
     * @throws IOException
     * @throws Exception
     */
    public static Collection<ParseFrame> parseDumpStdEval(
            String grmModel,
            String fragmentFile, // use for semantic role labeling
            //String gold, 
            String outputFile) throws IOException, Exception {

        List<Fragment> fragments = HelperFragmentMethods.loadFragments(fragmentFile);
        RuleMaps fromFile = RuleMaps.fromArchivedFile(grmModel);
        HelperRuleMapsMethods.addNewTerminals(fragments, fromFile);
        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fragments, fromFile);
        dumpParseCollToFile(outputFile, doParsingToBestFrames);
        return doParsingToBestFrames;
    }
    
    
    
    /**
     * Parse input with the given grammar file and dumps results in which the first element in the ll for the frame
     * @param grmModel
     * @param fragmentFile
     * @param outputFile
     * @return
     * @throws IOException
     * @throws Exception 
     */
    
      public static Collection<ParseFrame> parseDumpStdEvalWithLLData(
            String grmModel,
            String fragmentFile, // use for semantic role labeling
            //String gold, 
            String outputFile) throws IOException, Exception {

        List<Fragment> fragments = HelperFragmentMethods.loadFragments(fragmentFile);
        RuleMaps fromFile = RuleMaps.fromArchivedFile(grmModel);
        HelperRuleMapsMethods.addNewTerminals(fragments, fromFile);
        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fragments, fromFile);
        dumpParseCollToFileWithLLData(outputFile, doParsingToBestFrames);
        return doParsingToBestFrames;
    }
    
       /**
     * Parse the input fragment file given the grammar grmModel and dump the result in the form of std evaluation to outputParses file.
     * @param grmModel
     * @param fragmentFile
     
     * @return
     * @throws IOException
     * @throws Exception
     */
    public static Collection<ParseFrame> parseFragments(
            String grmModel,
            String fragmentFile
            
            ) throws IOException, Exception {
        List<Fragment> fragments = HelperFragmentMethods.loadFragments(fragmentFile);
        RuleMaps fromFile = RuleMaps.fromArchivedFile(grmModel);
        HelperRuleMapsMethods.addNewTerminals(fragments, fromFile);
        Collection<ParseFrame> doParsingToBestFrames = HelperParseMethods.doParsingToBestFrames(fragments, fromFile);
        return doParsingToBestFrames;
    }
    
    public static void dumpParseCollToFile(String outputParses, Collection<ParseFrame> doParsingToBestFrames) throws IOException{
         PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(outputParses));
        for (ParseFrame frame : doParsingToBestFrames) {
            pwGeneratedFrame.println(frame.toStringStdEvaluation());
        }
        pwGeneratedFrame.close();
    }
    
    public static void dumpParseCollToFileWithLLData(String outputParses, Collection<ParseFrame> doParsingToBestFrames) throws IOException{
         PrintWriter pwGeneratedFrame = new PrintWriter(new FileWriter(outputParses));
        for (ParseFrame frame : doParsingToBestFrames) {
            pwGeneratedFrame.println(frame.getLikelihood()+"\t"+frame.toStringStdEvaluation());
        }
        pwGeneratedFrame.close();
    }
    public static List<Future<Collection<CNFRule>>> bestParseUsingCYK(List<Fragment> fragmentsAll, RuleMaps theRuleMap, int threadSize) throws InterruptedException {
        //int threadSize = 40;
        double portion = fragmentsAll.size() * 1.0 / threadSize;
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        List<CYKParsingCallableBestParseCNF> mtTasks = new ArrayList<>();
        for (int t = 0; t < threadSize - 1; t++) {
            List<Fragment> subList = fragmentsAll.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
            mtTasks.add(new CYKParsingCallableBestParseCNF(subList, theRuleMap, t));
        }
        List<Fragment> subList = fragmentsAll.subList((int) Math.floor((threadSize - 1) * portion), fragmentsAll.size());
        mtTasks.add(new CYKParsingCallableBestParseCNF(subList, theRuleMap, threadSize - 1));
        List<Future<Collection<CNFRule>>> invokeAll = executor.invokeAll(mtTasks);
        executor.shutdown();
        return invokeAll;
    }
    
    /**
     * Get's the most likely parse for the collection
     * @param fcp
     * @param theRuleMap
     * @return 
     */
    public static Collection<ParseFrame> doParsingToBestFrames(Collection<Fragment> fcp, RuleMaps theRuleMap) {
        Collection<ParseFrame> parseCharQueue = new ConcurrentLinkedDeque<>();

        fcp.parallelStream().unordered().forEach(fc -> {
            try {
                CYKParser cyk = new CYKParser();
                FragmentCompact fragmentCompact = new FragmentCompact(fc, theRuleMap);
                ParseChart parseChart = cyk.parse(fragmentCompact, theRuleMap);
                parseChart.setIdentifier(fc.getSentID());
                CNFRule mostProbableParse = parseChart.getMostProbableParse(theRuleMap.getStartSymbolID());
                ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(parseChart.getIdentifier(), mostProbableParse, theRuleMap);
                parseCharQueue.add(parseTreeToFrame);
            } catch (Exception ex) {
                //System.err.println("Unparsable " + ex);
                Logger.getLogger(CYKParsingCallableBestParseCNF.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return parseCharQueue;
    }
    
    /**
     * Gives most likely parses for a randomly selected set of fragments of size maxSize from the input collection
     * @param fcp
     * @param theRuleMap
     * @param maxSize
     * @return 
     */
     public static Collection<ParseFrame> doRandomSampleParsingToBestFrames(Collection<Fragment> fcp, RuleMaps theRuleMap, double maxSize) {
        Collection<ParseFrame> parseCharQueue = new ConcurrentLinkedDeque<>();
         AtomicInteger ai = new AtomicInteger();
        int size = fcp.size();
        double portion = maxSize*1.0/size;
        Random r = new Random();
        fcp.parallelStream().forEach(fc -> {
            try {
                if (ai.incrementAndGet() < maxSize) {
                    if (r.nextDouble() <= portion) {
                        CYKParser cyk = new CYKParser();
                        FragmentCompact fragmentCompact = new FragmentCompact(fc, theRuleMap);
                        ParseChart parseChart = cyk.parse(fragmentCompact, theRuleMap);
                        parseChart.setIdentifier(fc.getSentID());
                        CNFRule mostProbableParse = parseChart.getMostProbableParse(theRuleMap.getStartSymbolID());
                        ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(parseChart.getIdentifier(), mostProbableParse, theRuleMap);
                        parseCharQueue.add(parseTreeToFrame);
                    }
                }
                
            } catch (Exception ex) {
                //System.err.println(ex);
                Logger.getLogger(CYKParsingCallableBestParseCNF.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
         System.err.println("Done parsing for randomly selected framents of size  " + ai);
        return parseCharQueue;
    }
    
    /**
     * Parse input to best parseframes using threads
     * @param fcp
     * @param theRuleMap
     * @param threads
     * @return
     * @throws InterruptedException 
     */
    
    public static Map<Long, ParseFrame> doParsingToBestFramesIdMap(Collection<Fragment> fcp, RuleMaps theRuleMap, int threads) throws InterruptedException {
        CYKBestParseFrameParses cykBestMultu = new CYKBestParseFrameParses(fcp, theRuleMap, threads);
        Map<Long, ParseFrame> doParsingToBestFrames = cykBestMultu.run();
        return doParsingToBestFrames;
    }

    public static Map<Long, ParseFrame> doParsingToBestFramesIdMap(Collection<Fragment> fcp, RuleMaps theRuleMap) {
        Map<Long,ParseFrame> parseCharQueue = new ConcurrentHashMap<>();

        fcp.parallelStream().forEach(fc -> {
            try {
                CYKParser cyk = new CYKParser();
                FragmentCompact fragmentCompact = new FragmentCompact(fc, theRuleMap);
                ParseChart parseChart = cyk.parse(fragmentCompact, theRuleMap);
                parseChart.setIdentifier(fc.getSentID());
                CNFRule mostProbableParse = parseChart.getMostProbableParse(theRuleMap.getStartSymbolID());
                ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(parseChart.getIdentifier(), mostProbableParse, theRuleMap);
                parseCharQueue.put(parseTreeToFrame.getUniqueIntID(),parseTreeToFrame);
            } catch (Exception ex) {
                //System.err.println(ex);
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return parseCharQueue;
    }

//    public static Collection<ParseFrame> doParsingToBestFrames(
//            List<Fragment> inputFramentList, 
//            RuleMaps theRuleMap) throws InterruptedException, ExecutionException, Exception {
//        Collection<ParseFrame> pfCol = new ConcurrentLinkedDeque<>();
//        CYKAllParses cykAllParses = new CYKAllParses(inputFramentList, theRuleMap, Settings.DEF_THREAD_NUM_TO_USE);
//        // int countAllParses = 0;
//        Collection<ParseChart> run = cykAllParses.run();
//        //System.err.println("Returned best parse size is " + run.size());
//        //for (Collection<ParseChart> res : run) {
//        for (ParseChart pc : run) {
//
//            int startSymbolID = theRuleMap.getStartSymbolID();
//            CNFRule mostProbableParse = pc.getMostProbableParse(startSymbolID);
//
//            ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(pc.getIdentifier(), mostProbableParse, theRuleMap);
//          
//           
//            pfCol.add(parseTreeToFrame);
//        }
//        //}
//        //  System.out.println("Obtianed frame size: " + pfCol.size());
//        return pfCol;
//    }

    public static ParseFrame doParsingToBestFrame(Fragment inputFrament, RuleMaps theRuleMap) throws InterruptedException, ExecutionException, Exception {
//        Collection<ParseFrame> pfCol = new ConcurrentLinkedDeque<>();
        CYKParser cyk = new CYKParser();
        ParseChart parse = cyk.parse(new FragmentCompact(inputFrament, theRuleMap), theRuleMap);

        int startSymbolID = theRuleMap.getStartSymbolID();
        CNFRule mostProbableParse = parse.getMostProbableParse(startSymbolID);

        ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(parse.getIdentifier(), mostProbableParse, theRuleMap);

        return parseTreeToFrame;

    }

    public static Collection<ParseChart> getAllParseCharts(List<Fragment> inputFramentList, RuleMaps theRuleMap) throws InterruptedException, ExecutionException, Exception {
        CYKAllParses cykAllParses = new CYKAllParses(inputFramentList, theRuleMap, Settings.DEF_THREAD_NUM_TO_USE);

        Collection<ParseChart> run = cykAllParses.run();
        return run;
    }

    public static Collection<Collection<ParseFrame>> getAllParseFrames(List<Fragment> inputFramentList, RuleMaps theRuleMap, int threadsize) throws InterruptedException, ExecutionException, Exception {
        Collection<Collection<ParseFrame>> pfCol = new ConcurrentLinkedDeque<>();
        CYKAllParses cykAllParses = new CYKAllParses(inputFramentList, theRuleMap, threadsize);
        // int countAllParses = 0;
        Collection<ParseChart> run = cykAllParses.run();
        run.forEach(prsChrt -> {
            try {
                List<CNFRule> validParses = prsChrt.getValidParses(theRuleMap.getStartSymbolID());
                Collection<ParseFrame> parseTreeToFrameList = new ConcurrentLinkedDeque<>();
                for (CNFRule cnf : validParses) {
                    ParseFrame parseTreeToFrame = HelperParseChartIO.parseTreeToFrame(prsChrt.getIdentifier(), cnf, theRuleMap);
                    parseTreeToFrameList.add(parseTreeToFrame);
                }
                pfCol.add(parseTreeToFrameList);
            } catch (Exception ex) {
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        return pfCol;
    }

    public static Collection<ParseDataCompact> getParseFramesStat(
            Collection<Fragment> inputFramentList, RuleMaps theRuleMap,
            int topN, int threadsize) throws InterruptedException, ExecutionException, Exception {
       // Collection<Collection<ParseFrame>> pfCol = new ConcurrentLinkedDeque<>();
        CYKAllParsesForPruningExtTopNPRS cykAllParses = new CYKAllParsesForPruningExtTopNPRS(topN, inputFramentList, theRuleMap, threadsize);
        // int countAllParses = 0;
        List<Future<Collection<ParseDataCompact>>> run = cykAllParses.run();
        Collection<ParseDataCompact> output = new ConcurrentLinkedDeque<>();
        run.forEach(compactData -> {
            try {
                output.addAll(compactData.get());
            } catch (Exception ex) {
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        return output;
    }

    public static double getLLFromParseCompactData(Collection<ParseDataCompact> pdcCol) {
        DoubleAdder da = new DoubleAdder();
        pdcCol.parallelStream().forEach(pd -> {
            // note ll must be multiplied, i.e., in log space is a summation
            da.add(pd.getLlForTheTopNParses());
        });
        return da.doubleValue();
    }

    public static Collection<ParseFrame> doParsingToBestFrameByCYKEMbd(
            Collection<Fragment> inputFramentList,
            WordPairSimilaritySmallMemoryFixedID wsfIX,
            RuleMaps theRuleMap, int threadSize) throws Exception {
        WordPairSimilarityContainer wsp = new WordPairSimilarityContainer(theRuleMap, wsfIX);
     
        //double portion = inputFramentList.size() * 1.0 / threadSize;
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        Collection<CYKEmbdParsingCallableBestFrame> tasks;

//        for (int t = 0; t < threadSize - 1; t++) {
//            List<Fragment> subList = inputFramentList.subList((int) Math.floor(t * portion), (int) Math.floor((t + 1) * portion));
//            mtTasks.add(new CYKEmbdParsingCallableBestFrame(subList, wsp, theRuleMap));
//        }
//        List<Fragment> subList = inputFramentList.subList((int) Math.floor((threadSize - 1) * portion), inputFramentList.size());
//        mtTasks.add(new CYKEmbdParsingCallableBestFrame(subList, wsp, theRuleMap));
        
        
         tasks = new ConcurrentLinkedQueue<>();
        executor = Executors.newFixedThreadPool(threadSize);
        int portion = Math.floorDiv(inputFramentList.size(), threadSize);
        List<Collection<Fragment>> splitCollectionBySize = 
                util.CollectionUtil.splitCollectionBySize(inputFramentList, portion);

        //for (Collection<Fragment> fcol : splitCollectionBySize) {
        splitCollectionBySize.parallelStream().forEach(fcol -> {
            
            try {
                tasks.add(new CYKEmbdParsingCallableBestFrame(fcol, wsp,theRuleMap));
            } catch (Exception ex) {
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            }

        });
        
        
        List<Future<Collection<ParseFrame>>> invokeAll = executor.invokeAll(tasks);
        executor.shutdown();
        Collection<ParseFrame> pfCol = new ConcurrentLinkedDeque<>();
        invokeAll.forEach(invo -> {
            Collection<ParseFrame> get;
            try {
                get = invo.get();
                pfCol.addAll(get);
            } catch (InterruptedException ex) {
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(HelperParseMethods.class.getName()).log(Level.SEVERE, null, ex);
            }

        });
        return pfCol;
    }
}
