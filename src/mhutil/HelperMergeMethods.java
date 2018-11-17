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
import frameinduction.grammar.learn.InOutParameterChart;
import frameinduction.grammar.learn.splitmerge.merger.FrameTerminalSymbolsAndSiblings;
import frameinduction.settings.Settings;
import frameinduction.grammar.SymbolFractory;
import frameinduction.grammar.learn.splitmerge.merger.MergeRandVariableBaseStat;
import frameinduction.grammar.learn.splitmerge.merger.MergeStategies;
import frameinduction.grammar.learn.splitmerge.merger.RuleMapsMergeByMerge;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.grammar.parse.io.SemRolePair;
import input.preprocess.objects.Fragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.PairKeySparseSymetricValueCollapsed;
import util.embedding.FrameClusterSimilarityContainer;
import util.embedding.FrameClusterSimilarityInfo;
import util.embedding.WordPairSimilarityContainer;
import util.embedding.WordPairSimilaritySmallMemoryFixedID;

/**
 *
 * @author behra
 */
public class HelperMergeMethods {

    /**
     * Merge method for roles based on st1 strategy
     * @param parseChartCollection
     * @param rc
     * @param ruleMap
     * @param portion
     * @param minRoleSize
     * @return
     * @throws Exception 
     */
    public static RuleMaps mergeSemanticRoles( 
            Collection<InOutParameterChart> parseChartCollection, 
            RulesCounts rc,
            RuleMaps ruleMap, double portion, int minRoleSize) throws Exception {
        MergeRandVariableBaseStat mrst =new MergeRandVariableBaseStat(parseChartCollection, rc, ruleMap);
        Set<Integer> semanticRoleSymbolIDSet = ruleMap.getSemanticRoleSymbolIDSet();
        List<PairKeySparseSymetricValueCollapsed> mergeCandidates = mrst.getMergeCandidates(semanticRoleSymbolIDSet);
        
        Map<Integer, Integer> fromToMap = MergeStategies.mergeSt1(ruleMap, mergeCandidates, portion, minRoleSize);
        
        //System.err.println("fromToMap map size " + fromToMap.size());
        RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
        RuleMaps mergedMap = rmMerge.ruleMapsMerge(ruleMap, fromToMap);
        Set<Integer> semanticRoleSymbolIDSet1 = mergedMap.getSemanticRoleSymbolIDSet();
        System.err.println("Roles are merged from " + semanticRoleSymbolIDSet.size() +" to " + semanticRoleSymbolIDSet1.size());
        return mergedMap;
    }
    
    public static RuleMaps mergeSntxDependencies(
            Collection<InOutParameterChart> parseChartCollection,
            RulesCounts rc,
            RuleMaps ruleMap, double portion, int minRoleSize) throws Exception {
        System.err.println("Start merging depndency annotations ... ");
        MergeRandVariableBaseStat mrst = new MergeRandVariableBaseStat(parseChartCollection, rc, ruleMap);
        Set<String> depSetSymbols = ruleMap.getSntxDepIDSet();
        Set<Integer> depNodeIDSet = ruleMap.convertToSymbolID(depSetSymbols);
        List<PairKeySparseSymetricValueCollapsed> mergeCandidates = mrst.getMergeCandidates(depNodeIDSet);

        Map<Integer, Integer> fromToMap = MergeStategies.mergeSt1(ruleMap, mergeCandidates, portion, minRoleSize);
        RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
        RuleMaps mergedMap = rmMerge.ruleMapsMerge(ruleMap, fromToMap);
        System.err.println("Syntacric Dependencies are merged from " + depNodeIDSet.size() + " to " + mergedMap.getSemanticRoleSymbolIDSet().size());
        return mergedMap;
    }
    
    
    public static RuleMaps mergeFramesByLookingAtHeads( 
            Collection<InOutParameterChart> parseChartCollection, 
            RulesCounts rc,
            Settings settings,
            
            RuleMaps ruleMap, double portionToMerge, int minFrameSize) throws Exception {
        
        
        Set<String> activeDepr = settings.getActiveDepr();
        Set<Integer> frameHeadSymbolIDSet = ruleMap.getFrameHeadSymbolIDSet();
        System.err.println("** >>> target id set size is " + frameHeadSymbolIDSet.size());
        MergeRandVariableBaseStat mrst =new MergeRandVariableBaseStat(parseChartCollection, rc, ruleMap);
        
        List<PairKeySparseSymetricValueCollapsed> mergeCandidates = mrst.getMergeCandidates(frameHeadSymbolIDSet);
        
        Map<Integer,Integer> finalMapToUse = new HashMap<>();
        Map<Integer, Integer> mamFromToSymbolIDs = MergeStategies.mergeSt1(ruleMap, mergeCandidates, portionToMerge, minFrameSize);
        
        for(Map.Entry<Integer, Integer> mapEntry: mamFromToSymbolIDs.entrySet()) {
            try {
                int frameSymbolID1 = mapEntry.getKey();
                int frameSymbolID2 = mapEntry.getValue();
                int frameIndex11 = SymbolFractory.getFrameIndex(ruleMap.getSymbolFromID(frameSymbolID1));
                int frameIndex12 = SymbolFractory.getFrameIndex(ruleMap.getSymbolFromID(frameSymbolID2));
                Map<Integer, Integer> map = FrameTerminalSymbolsAndSiblings.getMap(frameIndex11, frameIndex12, ruleMap, activeDepr);
                finalMapToUse.putAll(map);
             
        RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
        ruleMap = rmMerge.ruleMapsMerge(ruleMap, map);
            } catch (Exception ex) {
                Logger.getLogger(HelperMergeMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.err.println(">> Merging >> Final mapping size is " + finalMapToUse.size());
      //  RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
      //  RuleMaps mergedMap = rmMerge.ruleMapsMerge(ruleMap, finalMapToUse);

        
   //     System.err.println("Frame Arguments are merged from " + frameHeadSymbolIDSet.size() +" " + mergedMap.getFrameHeadSymbolIDSet().size());
        return ruleMap;
    }
    
    
    
      public static RuleMaps mergeFramesByLookingAtHeadsOG( 
            Collection<InOutParameterChart> parseChartCollection, 
            RulesCounts rc,
            Settings settings,
            
            RuleMaps ruleMap, double portionToMerge, int minFrameSize) throws Exception {
        
        
        Set<String> activeDepr = settings.getActiveDepr();
        Set<Integer> frameHeadSymbolIDSet = ruleMap.getFrameHeadSymbolIDSet();
        System.err.println("** >>> target id set size is " + frameHeadSymbolIDSet.size());
        MergeRandVariableBaseStat mrst =new MergeRandVariableBaseStat(parseChartCollection, rc, ruleMap);
        
        List<PairKeySparseSymetricValueCollapsed> mergeCandidates = mrst.getMergeCandidates(frameHeadSymbolIDSet);
        
        Map<Integer,Integer> finalMapToUse = new HashMap<>();
        Map<Integer, Integer> mamFromToSymbolIDs = MergeStategies.mergeSt1(ruleMap, mergeCandidates, portionToMerge, minFrameSize);
        
        mamFromToSymbolIDs.entrySet().forEach(mapEntry -> {
            try {
                int frameSymbolID1 = mapEntry.getKey();
                int frameSymbolID2 = mapEntry.getValue();
                int frameIndex11 = SymbolFractory.getFrameIndex(ruleMap.getSymbolFromID(frameSymbolID1));
                int frameIndex12 = SymbolFractory.getFrameIndex(ruleMap.getSymbolFromID(frameSymbolID2));
                Map<Integer, Integer> map = FrameTerminalSymbolsAndSiblings.getMap(frameIndex11, frameIndex12, ruleMap, activeDepr);
                finalMapToUse.putAll(map);
             
        
            } catch (Exception ex) {
                Logger.getLogger(HelperMergeMethods.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        System.err.println("Final mapping size is " + finalMapToUse.size());
        RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
        RuleMaps mergedMap = rmMerge.ruleMapsMerge(ruleMap, finalMapToUse);

        
        System.err.println("Frame Arguments are merged from " + frameHeadSymbolIDSet.size() +" " + mergedMap.getFrameHeadSymbolIDSet().size());
        return mergedMap;
    }
    
     public static RuleMaps mergeAFrameRoles( 
            Collection<InOutParameterChart> parseChartCollection, 
            RulesCounts rc,
            Settings settings,
            int frameID,
            RuleMaps ruleMap, double portion, int minFrameSize) throws Exception {
        
        Set<Integer> targetIDs = new HashSet<>();
         for (String dep : settings.getActiveDepr()) {
            //      for (int i = 0; i < semanticRoleCardinality; i++) {
            String fromFrameArgument = SymbolFractory.getFramesJSntxArgument(frameID, dep);
            int idFromSymbol = ruleMap.getIDFromSymbol(fromFrameArgument);
            targetIDs.add(idFromSymbol);
        }
        MergeRandVariableBaseStat mrst =new MergeRandVariableBaseStat(parseChartCollection, rc, ruleMap);
        
        List<PairKeySparseSymetricValueCollapsed> mergeCandidates = mrst.getMergeCandidates(targetIDs);
        
        Map<Integer, Integer> fromToMap = MergeStategies.mergeSt1(ruleMap, mergeCandidates, portion, minFrameSize);
        RuleMapsMergeByMerge rmMerge = new RuleMapsMergeByMerge();
        RuleMaps mergedMap = rmMerge.ruleMapsMerge(ruleMap, fromToMap);
        System.err.println("Frame Arguments are merged from " + targetIDs.size() +" " + mergedMap.getFrameHeadSymbolIDSet().size());
        return mergedMap;
    }

    public static Map<String, Collection<Fragment>> discMergeFragmentsWithSampledClusters(
            RuleMaps rm,
            
            Map<String, Collection<Fragment>> fragmentClustersx, 
            Map<String, Collection<Fragment>> fragmentClustersxSampled,
            WordPairSimilarityContainer wordSimilarities) {
        
       Collection<FrameClusterSimilarityInfo> fscfCol = new ConcurrentLinkedQueue<>();
       fragmentClustersx.entrySet().forEach(fcEnt->{
           Collection<Fragment> value = fcEnt.getValue();
          // Collection<ParseFrame> parseFragmentToFrame = HelperParseChartIO.parseFragmentToFrame(value, 0);
           
       fragmentClustersxSampled.entrySet().forEach(fcxEnt->{
            Collection<Fragment> valueSampled = fcxEnt.getValue();
         //  Collection<ParseFrame> parseFragmentToFrameSampled = HelperParseChartIO.parseFragmentToFrame(valueSampled, 0);
         //  getSimiliarity(.5, .5, rm, wordSimilarities, null)
           
       });
       });
//        HelperParseChartIO.parseFragmentToFrame(null, id)
//        FrameClusterSimilarityContainer fscd = new FrameClusterSimilarityContainer(null, null, null)
        return null;
    }
     protected double computerClusterPairSimilaritySt(Collection<ParseFrame> frameInstanceSet1, Collection<ParseFrame> frameInstanceSet2) {
//let's do it ineefficiencyy first
        // let's get an average of similarities // many other methods such min, max, single linkage etc can be used
        DoubleAdder da1 = new DoubleAdder();
//        Set<Long> idsU = new HashSet<>(frameInstanceSet1);
//        idsU.addAll(frameInstanceSet2);
//        //int toDevide = frameInstanceSet1.size() * frameInstanceSet2.size();
//        idsU.forEach(frameInstanceID1 -> {
//            idsU.forEach(frameInstanceID2 -> {
//                // not in this context ... if(Long.compare(frameInstanceID1,frameInstanceID2)!=0){//this is always true since we are doing hard clustering
//                Double frameCachedSim = getSimiliarity(.5frameInstanceID1, frameInstanceID2);
////                if (frameCachedSim == null) {
////                    System.err.println(frameInstanceID1 + " " + frameInstanceID2 +" sim is null (id:763482)");
////                }
//                da1.add(frameCachedSim);
//           // }
//            });
//        });
//        
        int card = frameInstanceSet1.size()+frameInstanceSet2.size();
        double aveSim = da1.doubleValue()/(card* (card-1));
        return aveSim;
    }
     
    public static double getSimiliarity(
            double headWeight, double rolesWeight,
            WordPairSimilarityContainer wsm, RuleMaps rm, ParseFrame pf1, ParseFrame pf2) throws Exception {
        
        int head1 = rm.getIDFromSymbol(pf1.getHead());
        int head2 = rm.getIDFromSymbol(pf2.getHead());
        
        double simValueHead = wsm.getSimValue(head1, head2);

        double sumSimForRoles = 0;
        Map<String, List<String>> semRoleAsMap1 = pf1.getSemRoleAsMap();
        Map<String, List<String>> semRoleAsMap2 = pf2.getSemRoleAsMap();

        Set<String> commonRoleSet = new HashSet<>();
        Set<String> unionSet = new HashSet<>();

        for (String role2 : semRoleAsMap2.keySet()) {
            if (semRoleAsMap1.keySet().contains(role2)) {
                commonRoleSet.add(role2);
            }
            unionSet.add(role2);

        }
        unionSet.addAll(semRoleAsMap1.keySet());

        double simRawRole = 0;
        double countRole =0;
        for (SemRolePair role2 : pf1.getSemanticRoles()) {
            for (SemRolePair role1 : pf2.getSemanticRoles()) {
                String lexicalization = role1.getLexicalization();
                String lexicalization1 = role2.getLexicalization();
                int lex1ID = rm.getIDFromSymbol(lexicalization);
                int lex2ID = rm.getIDFromSymbol(lexicalization1);
                Double simValue = wsm.getSimValue(lex1ID, lex2ID);
                if (simValue <= 0) {
                    //  System.err.println(lexicalization +" " + lexicalization1 +" code 98e22" +" "+ simValue);
                } else {
                    simRawRole += simValue;
                    countRole++;
                }
            }
        }
        if(countRole!=0){
        simRawRole/=countRole;}
        // int commonRolesSize = commonRoleSet.size();
        for (String commonRole : commonRoleSet) {
            List<String> get1 = semRoleAsMap1.get(commonRole);
            List<String> get2 = semRoleAsMap2.get(commonRole);
            // for all the lexicalization of a role compute sim and make an average
            double thisRoleSimilaritySums = 0.0;
            int toDevide = 0;
            for (String lex1 : get1) {
                int lex1ID = rm.getIDFromSymbol(lex1);
                for (String lex2 : get2) {
                    int lex12D = rm.getIDFromSymbol(lex2);
                    toDevide++;

                    double simThisLexicalization = wsm.getSimValue(lex1ID, lex12D);
                    thisRoleSimilaritySums += simThisLexicalization;
                }
            }
            // here bunch of things can be done
            thisRoleSimilaritySums /= toDevide;
            sumSimForRoles += thisRoleSimilaritySums;
        }

        
        sumSimForRoles /= unionSet.size();
        double frameSim = headWeight * simValueHead + .7*rolesWeight * sumSimForRoles
                +  .3*rolesWeight *simRawRole
               // + MathUtil.suqash(simRawRole)
                ;

        return frameSim;
    }

    
    
}
