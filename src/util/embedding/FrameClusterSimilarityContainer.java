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
package util.embedding;


import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.learn.MathUtil;
import mhutil.HelperParseChartIO;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.grammar.parse.io.SemRolePair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

/**
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class FrameClusterSimilarityContainer {

    private final WordPairSimilaritySmallMemoryFixedID wsm;
    private final Collection<ParseFrame> parseFramList;
    private final RuleMaps rm;
    private final Map<Long, Map<Long, Double>> framePairSimilarityInt;
    private final Map<Integer, ConcurrentHashMap<Integer, Double>> clusterPairSimilarityInt;
    private final Set<Integer> clusterSetIDs;
    private final Map<Integer, Set<Long>> frameClusterInstances;
    private final double headWeight;
    private final double rolesWeight;
   
    


//    public FrameClusterSimilarityContainer(
//            double headWeight, double rolesWeight, WordPairSimilarityContainer wsm,
//            Collection<ParseFrame> parseFramList, RuleMaps rm) {
//        this.headWeight = headWeight;
//        this.rolesWeight = rolesWeight;
//        this.wsm = wsm;
//        this.rm = rm;
//        this.parseFramList = parseFramList;
//         
//        framePairSimilarityInt = new ConcurrentHashMap<>();
//        clusterPairSimilarityInt = new ConcurrentHashMap<>();
//        clusterSetIDs = ConcurrentHashMap.newKeySet();
//        frameClusterInstances = new ConcurrentHashMap<>();
//        init();
//    }
    
    /**
     * THIS is a temp fix for getting wsm insrtead of container
     * @param headWeight
     * @param rolesWeight
     * @param wsm
     * @param parseFramList
     * @param rm 
     */
     public FrameClusterSimilarityContainer(
            double headWeight, double rolesWeight, WordPairSimilaritySmallMemoryFixedID wsm,
            Collection<ParseFrame> parseFramList, RuleMaps rm) throws Exception {
        this.headWeight = headWeight;
        this.rolesWeight = rolesWeight;
        this.wsm = wsm;//new WordPairSimilarityContainer(rm, wsm);
        this.rm = rm;
        this.parseFramList = parseFramList;
         
        framePairSimilarityInt = new ConcurrentHashMap<>();
        clusterPairSimilarityInt = new ConcurrentHashMap<>();
        clusterSetIDs = ConcurrentHashMap.newKeySet();
        frameClusterInstances = new ConcurrentHashMap<>();
        init();
    }

    public Map<Integer, Set<Long>> getFrameClusterInstances() {
        return frameClusterInstances;
    }

//    public FrameClusterSimilarityContainer(WordPairSimilarityContainer wsm,
//            Collection<ParseFrame> parseFramList, RuleMaps rm) {
//        headWeight = 0.75;// .75;
//        rolesWeight = .25;//.25;
//        this.wsm = wsm;
//        this.rm = rm;
//        this.parseFramList = parseFramList;
//        framePairSimilarityInt = new ConcurrentHashMap<>();
//        clusterPairSimilarityInt = new ConcurrentHashMap<>();
//        clusterSetIDs = ConcurrentHashMap.newKeySet();
//        frameClusterInstances = new ConcurrentHashMap<>();
//        init();
////        Set<Long> intFrID = ConcurrentHashMap.newKeySet();
////
////        boolean forTest = false;
////        if(forTest){
////        parseFramList.forEach(f -> {
////            Long uniqueIntID = f.getUniqueIntID();
////            //System.err.println(uniqueIntID);
////            if (intFrID.contains(uniqueIntID)) {
////                System.err.println("Error + " + f + uniqueIntID);
////            }
////            intFrID.add(uniqueIntID);
////
////        });
////        }
//
//         // make the clusters and their assigned instances
//    }

    private void init() {
        // order frames into their assigned clusters
        parseFramList.parallelStream().forEach((ParseFrame frame) -> {
            int frameCluster1 = frame.getFrameClusterNumber();
            // String frameID = frame.SentHeadPositionKey();
            Long frameInstanceID = frame.getUniqueIntID();
            if (frameClusterInstances.containsKey(frameCluster1)) {
                frameClusterInstances.get(frameCluster1).add(frameInstanceID);
            } else {
                //Set<String> frameSetID = ConcurrentHashMap.newKeySet();
                Set<Long> frameSetID = ConcurrentHashMap.newKeySet();
                frameSetID.add(frameInstanceID);
                //Set<String> putIfAbsent = frameClusterInstances.putIfAbsent(frameNumber, frameSetID);
                Set<Long> putIfAbsent = frameClusterInstances.putIfAbsent(frameCluster1, frameSetID);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frameInstanceID);
                }
            }
        });
        clusterSetIDs.addAll(frameClusterInstances.keySet());
        //System.err.println("Computing frame-wise simialrities...");
        computePairwiseFrameSimilarities();
    }

    public Map<Long, Map<Long, Double>> getFramePairSimilarity() {
        return framePairSimilarityInt;
    }

    public Map<Integer, Double> computeLoglikelihoodsPerCluster() {
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(parseFramList);
        Map<Integer,Double> clusterLLMap = new ConcurrentHashMap<>();
        frameClusterInstances.entrySet().parallelStream().forEach(entry -> {
            Integer key = entry.getKey();
            Set<Long> value = entry.getValue();
            DoubleAdder sum = new DoubleAdder();
            value.forEach(parseID -> {
                double likelihood = parseListToIDMap.get(parseID).getLikelihood();
                sum.add(likelihood);
            });
            clusterLLMap.put(key, sum.doubleValue());
        });
        return clusterLLMap;
    }
    private void computePairwiseFrameSimilarities() {
        AtomicInteger ai = new AtomicInteger();
        parseFramList.parallelStream().forEach(parseFrame1 -> {
            // final String uniqueIdentifier1 = parseFrame1.SentHeadPositionKey();
            long uniqueIdentifier1 = parseFrame1.getUniqueIntID();
            parseFramList.parallelStream().forEach(parseFrame2 -> {
                //  String uniqueIdentifier2 = parseFrame2.SentHeadPositionKey();
                long uniqueIdentifier2 = parseFrame2.getUniqueIntID();
                if (uniqueIdentifier1 >= uniqueIdentifier2) {
                    if (getFrameWiseCachedSim(uniqueIdentifier1, uniqueIdentifier2) == null) {

                        try {
                            double frameSim = 0d;

                            if (uniqueIdentifier1 == uniqueIdentifier2) {
                                frameSim = 1.0;
                            } else {
                                frameSim = getSimiliarity(parseFrame1, parseFrame2);

                            }
                            ai.incrementAndGet();
                            setFrameWiseSim(uniqueIdentifier1, uniqueIdentifier2, frameSim);
                            //System.err.println("computing similarity for frames " + uniqueIdentifier2 +" and " + uniqueIdentifier1 +"  " + frameSim);
                        } catch (Exception ex) {
                            Logger.getLogger(FrameClusterSimilarityContainer.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
            });
        });
       // System.err.println("Computed  "  +ai.intValue() + " frame-wise similarities...");

    }
//    private void setSim(String frameID1, String frameID2, double sim) {
//        if (frameID1.compareTo(frameID2) > 0) {
//            this.framePairSimilarity.put(frameID1 + "-" + frameID2, sim);
//        } else {
//            this.framePairSimilarity.put(frameID2 + "-" + frameID1, sim);
//        }
//    }

    public Double getFrameWiseCachedSim(long frameID1, long frameID2) {
        if (frameID1 < frameID2) {
            return getFrameWiseCachedSim(frameID2, frameID1);
        }
        if (framePairSimilarityInt.containsKey(frameID1)) {

            Map<Long, Double> get = this.framePairSimilarityInt.get(frameID1);

            Double sim = get.get(frameID2);
            return sim;

        } else {
            return null;
        }

    }

    private void setFrameWiseSim(Long frameID1, Long frameID2, Double sim) throws Exception {
        if (frameID1 < frameID2) {
            setFrameWiseSim(frameID2, frameID1, sim);
        } else {
            if (this.framePairSimilarityInt.containsKey(frameID1)) {
                Map<Long, Double> get = this.framePairSimilarityInt.get(frameID1);
                //if (get != null) {
                Double putIfAbsent = get.putIfAbsent(frameID2, sim);
                if (putIfAbsent != null) {
                    if (putIfAbsent.compareTo(sim) != 0) {
                        throw new Exception("Unseen race condition that resulted in contradictory frame sim value " + putIfAbsent + " vs " + sim);
                    }
                }
            } else {
                Map<Long, Double> clust2SimValMap = new ConcurrentHashMap<>();
                clust2SimValMap.put(frameID2, sim);
                Map<Long, Double> putIfAbsent = this.framePairSimilarityInt.putIfAbsent(frameID1, clust2SimValMap);
                if (putIfAbsent != null) {
                    Double putIfAbsent1 = putIfAbsent.putIfAbsent(frameID2, sim);
                    if (putIfAbsent1 != null) {
                        if (putIfAbsent1.compareTo(sim) != 0) {
                            throw new Exception("Unseen race condition that resulted in contradictory frame sim value");
                        }
                    }
                }

            }
        }
    }

//    public Double getFrameCachedSim(String frameID1, String frameID2) {
//        if (frameID1.compareTo(frameID2) > 0) {
//
//            if (this.framePairSimilarity.containsKey(frameID1 + "-" + frameID2)) {
//                return this.framePairSimilarity.get(frameID1 + "-" + frameID2);
//            }
//        } else {
//            if (this.framePairSimilarity.containsKey(frameID2 + "-" + frameID1)) {
//                return this.framePairSimilarity.get(frameID2 + "-" + frameID1);
//            }
//        }
//        return null;
//    }
    public double getSimiliarity(ParseFrame pf1, ParseFrame pf2) throws Exception {
        
//        int head1 = rm.getIDFromSymbol(pf1.getHead());
//        int head2 = rm.getIDFromSymbol(pf2.getHead());
        
        double simValueHead = wsm.getSimValue(pf1.getHead(), pf2.getHead());

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
//                int lex1ID = rm.getIDFromSymbol(lexicalization);
//                int lex2ID = rm.getIDFromSymbol(lexicalization1);
                Double simValue = wsm.getSimValue(lexicalization, lexicalization1);
                if (simValue <= 0) {
                    //  System.err.println(lexicalization +" " + lexicalization1 +" code 98e22" +" "+ simValue);
                } else {
                    simRawRole += simValue;
                    countRole++;
                }
            }
        }
        if(countRole!=0){
        simRawRole/=countRole;
        
        }
        // int commonRolesSize = commonRoleSet.size();
        //double[] weights = new double[commonRoleSet.size()+1];
        //int roleCounter=1;
        for (String commonRole : commonRoleSet) {
            List<String> get1 = semRoleAsMap1.get(commonRole);
            List<String> get2 = semRoleAsMap2.get(commonRole);
            // for all the lexicalization of a role compute sim and make an average
            double thisRoleSimilaritySums = 0.0;
            int toDevide = 0;
            for (String lex1 : get1) {
                //int lex1ID = rm.getIDFromSymbol(lex1);
                for (String lex2 : get2) {
                   // int lex12D = rm.getIDFromSymbol(lex2);
                    toDevide++;

                   // double simThisLexicalization = wsm.getSimValue(lex1ID, lex12D);
                     double simThisLexicalization = wsm.getSimValue(lex1, lex2);
                    thisRoleSimilaritySums += simThisLexicalization;
                }
            }
            // here bunch of things can be done
            thisRoleSimilaritySums /= toDevide;
           // weights[roleCounter++]=thisRoleSimilaritySums;
            sumSimForRoles += thisRoleSimilaritySums;
        }

        
        sumSimForRoles /= unionSet.size();
      //  weights[0]=2*simValueHead;
      //double v1=  org.apache.commons.math3.stat.StatUtils.geometricMean(weights);
     // double v2 = org.apache.commons.math3.stat.StatUtils.mean(weights);
      //double frameSim2 = .5*(v1+v2);
        double frameSim = headWeight * simValueHead + .9*rolesWeight * sumSimForRoles
                +  .1*rolesWeight *simRawRole
               // + MathUtil.suqash(simRawRole)
                ;
//double frameSimII = headWeight * simValueHead + rolesWeight * sumSimForRoles
//               // +  .3*rolesWeight *simRawRole
//               // + MathUtil.suqash(simRawRole)
//                ;
        return frameSim;
    }

    
    protected double getSimiliarityO(ParseFrame pf1, ParseFrame pf2) throws Exception {
        
//        int head1 = rm.getIDFromSymbol(pf1.getHead());
//        int head2 = rm.getIDFromSymbol(pf2.getHead());
        
        double simValueHead = wsm.getSimValue(pf1.getHead(), pf2.getHead());

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

        // int commonRolesSize = commonRoleSet.size();
        for (String commonRole : commonRoleSet) {
            List<String> get1 = semRoleAsMap1.get(commonRole);
            List<String> get2 = semRoleAsMap2.get(commonRole);
            // for all the lexicalization of a role compute sim and make an average
            double thisRoleSimilaritySums = 0.0;
            int toDevide = 0;
            for (String lex1 : get1) {
           //     int lex1ID = rm.getIDFromSymbol(lex1);
                for (String lex2 : get2) {
                   // int lex12D = rm.getIDFromSymbol(lex2);
                    toDevide++;

                    double simThisLexicalization = wsm.getSimValue(lex1, lex2);
                    thisRoleSimilaritySums += simThisLexicalization;
                }
            }
            // here bunch of things can be done
            thisRoleSimilaritySums /= toDevide;
            sumSimForRoles += thisRoleSimilaritySums;
        }

        sumSimForRoles /= unionSet.size();
        double frameSim = headWeight * simValueHead + rolesWeight * sumSimForRoles;

        return frameSim;
    }

    public Set<Integer> getClusterSetIDs() {
        return clusterSetIDs;
    }

    public Set<Long> getFrameClusterInstances(int clusterID) {
        return frameClusterInstances.get(clusterID);
    }

    public Integer getClusterSize() {
        return clusterSetIDs.size();
    }

    public double getGlobalSilhouette(List<ClusterSiluhetCoeff> silList){
        double sumSil = 0;
        for(ClusterSiluhetCoeff s: silList){
            sumSil+=s.getSil();
        }
        return sumSil/silList.size();
    }
    public List<ClusterSiluhetCoeff> getFrameClustersSilhouette() {

        List<ClusterSiluhetCoeff> siList = new ArrayList<>();
        frameClusterInstances.keySet().forEach(cluster -> {
            Set<Long> instances = frameClusterInstances.get(cluster);
            if(instances.size()>1){
            double aveInstSil = 0.0;
            for (Long eachInstance : instances) {
                double averageSimToOtherA_i = computeAverageDisSimilarity(eachInstance, cluster);
                //System.err.println("* "+averageSimToOtherA_i);
                double minB_i = Double.POSITIVE_INFINITY;
                for (int otherCluster : clusterSetIDs) {
                    if (otherCluster != cluster) {
                        double averageSimToOtherB_i = computeAverageDisSimilarity(eachInstance, otherCluster);

                        minB_i = Math.min(averageSimToOtherB_i, minB_i);
                    }
                }
                double denominator = Math.max(averageSimToOtherA_i, minB_i);
                if ((denominator != 0)) {
                    // this can happen for a number of reasons the first one is missing vectors for the target word!
                    double silu = (minB_i - averageSimToOtherA_i) / denominator;
                aveInstSil += silu;
                }else{
                     aveInstSil += 1;
                }
                


            }
            
            aveInstSil = aveInstSil / instances.size();
           
            
            ClusterSiluhetCoeff csfc = new ClusterSiluhetCoeff(cluster, aveInstSil, instances.size());
            siList.add(csfc);
            } });

        Collections.sort(siList);

        return siList;

    }
    
    public double getSomthingIDonotKnowWhatToClass(Set<Long> instances) {

            //if(instances.size()>1){
            double aveInstSil = 0.0;
            for (Long eachInstance : instances) {
                double averageSimToOtherA_i = computeAverageDisSimilarityInstanceToSet(eachInstance,  instances);
              aveInstSil += averageSimToOtherA_i;

            }
            
            aveInstSil = aveInstSil / instances.size();
           
            
            //ClusterSiluhetCoeff csfc = new ClusterSiluhetCoeff(cluster, aveInstSil, instances.size());
            //siList.add(csfc);
           // } 
           // });

        //Collections.sort(siList);

        return aveInstSil;

    }

    private double computeAverageDisSimilarity(Long instanceID, int clusterID) {
        Set<Long> remainingInstances = this.frameClusterInstances.get(clusterID);
        // get the avergae distrance
        int counterForAverage = 0;
        double pairDistSum = 0;
        for (Long otherInstance : remainingInstances) {

            if (otherInstance != instanceID) {
                counterForAverage++;
                pairDistSum += (1- getFrameWiseCachedSim(instanceID, otherInstance));
            }

        }
        if (counterForAverage == 0) {
            return 0.0;
        }
        return - pairDistSum / counterForAverage;

    }
    
     private double computeAverageDisSimilarityInstanceToSet(Long instanceID, Set<Long> otherInstances) {
        //Set<Long> remainingInstances = this.frameClusterInstances.get(clusterID);
        // get the avergae distrance
        int counterForAverage = 0;
        double pairDistSum = 0;
        for (Long otherInstance : otherInstances) {

            if (!Objects.equals(otherInstance, instanceID)) {
                counterForAverage++;
                pairDistSum += (1- getFrameWiseCachedSim(instanceID, otherInstance));
            }

        }
        if (counterForAverage == 0) {
            return 0.0;
        }
        return - pairDistSum / counterForAverage;

    }

    public List<FrameClusterSimilarityInfo> getSimilarFrameClusters() {

//        AtomicInteger ai = new AtomicInteger();
//        frameClusterInstances.values().forEach(cnsmr -> {
//            ai.addAndGet(cnsmr.size());
//        });

      //  System.err.println("Frame instance total size: " + ai);
       // System.err.println("Number of Frame clusters is: " + frameClusterInstances.keySet().size());
        clusterSetIDs.addAll(frameClusterInstances.keySet());
        
       

        frameClusterInstances.keySet().parallelStream().unordered().forEach((Integer frameClusterID1) -> {
            frameClusterInstances.keySet()
                    .parallelStream()
                    .forEach((Integer frameClusterID2) -> {
                if (frameClusterID1 >= frameClusterID2) {
                    Double clusterSim = getCachedClusterSim(frameClusterID1, frameClusterID2);
                    if (clusterSim == null) {
                        try {
                            // sim is not computer!
                            double computerClusterPairSimilarity
                                   // = computerClusterPairSimilarity(
                                            
                                         =   computerClusterPairSimilarity(
                                            frameClusterInstances.get(frameClusterID1),
                                            frameClusterInstances.get(frameClusterID2));
                            setClusterWiseSim(frameClusterID1, frameClusterID2, computerClusterPairSimilarity);
                        } catch (Exception ex) {
                            Logger.getLogger(
                                    FrameClusterSimilarityContainer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
//                else{
//                    System.err.println("was here!");
//                }

            });
        });

        List<FrameClusterSimilarityInfo> clusterSimInfo = sortByValue(this.clusterPairSimilarityInt, frameClusterInstances);
//        List<FrameClusterSimilarityInfo> clusterSimInfo = new ArrayList<>();
//        for (String key : sortByValue.keySet()) {
//            int clusterID1 = Integer.parseInt(key.split("-")[0]);
//            int clusterID2 = Integer.parseInt(key.split("-")[1]);
//            int size1 = frameClusterInstances.get(clusterID1).size();
//            int size2 = frameClusterInstances.get(clusterID2).size();
//
//            FrameClusterSimilarityInfo csinfor = new FrameClusterSimilarityInfo(clusterID1, clusterID2, size1, size2, sortByValue.get(key));
//            clusterSimInfo.add(csinfor);
//           // System.out.println(key + " : " + sortByValue.get(key) + " " + size1 + " " + size2);
//        }
        return clusterSimInfo;

    }
    public List<FrameClusterSimilarityInfo> getSimilarFrameClustersrMethod2() {

        AtomicInteger ai = new AtomicInteger();
        frameClusterInstances.values().forEach(cnsmr -> {
            ai.addAndGet(cnsmr.size());
        });
        clusterSetIDs.addAll(frameClusterInstances.keySet());
        frameClusterInstances.keySet().parallelStream().forEach((Integer frameClusterID1) -> {
            frameClusterInstances.keySet().forEach((Integer frameClusterID2) -> {
                if (frameClusterID1 >= frameClusterID2) {
                    Double clusterSim = getCachedClusterSim(frameClusterID1, frameClusterID2);
                    if (clusterSim == null) {
                        try {
                            // sim is not computer!
                            double computerClusterPairSimilarity
                                   // = computerClusterPairSimilarity(
                                            
                                         =   computerClusterPairSimilaritySt(
                                            frameClusterInstances.get(frameClusterID1),
                                            frameClusterInstances.get(frameClusterID2));
                            setClusterWiseSim(frameClusterID1, frameClusterID2, computerClusterPairSimilarity);
                        } catch (Exception ex) {
                            Logger.getLogger(
                                    FrameClusterSimilarityContainer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
//                else{
//                    System.err.println("was here!");
//                }

            });
        });

        List<FrameClusterSimilarityInfo> clusterSimInfo = sortByValue(this.clusterPairSimilarityInt, frameClusterInstances);
//        List<FrameClusterSimilarityInfo> clusterSimInfo = new ArrayList<>();
//        for (String key : sortByValue.keySet()) {
//            int clusterID1 = Integer.parseInt(key.split("-")[0]);
//            int clusterID2 = Integer.parseInt(key.split("-")[1]);
//            int size1 = frameClusterInstances.get(clusterID1).size();
//            int size2 = frameClusterInstances.get(clusterID2).size();
//
//            FrameClusterSimilarityInfo csinfor = new FrameClusterSimilarityInfo(clusterID1, clusterID2, size1, size2, sortByValue.get(key));
//            clusterSimInfo.add(csinfor);
//           // System.out.println(key + " : " + sortByValue.get(key) + " " + size1 + " " + size2);
//        }
        return clusterSimInfo;

    }
    
    public List<FrameClusterSimilarityInfo> reduceClusterSizesTo(double ratio, List<FrameClusterSimilarityInfo> clusterSimInfo) throws Exception {
        if (ratio > 1.0 || ratio < 0.0) {
            throw new Exception("invalid ration value");
        }
        List<FrameClusterSimilarityInfo> forReduction = new ArrayList<>();
        int originalSize = forReduction.size();
        if (ratio == 0) {
            return forReduction;
        }
        if (originalSize == 1) {
            return forReduction;
        } else {

            int originFrameClusterSize = this.clusterSetIDs.size();
            int targetSize = originFrameClusterSize - (int) Math.floor(originFrameClusterSize * ratio);
            // System.err.println("Target reduced size is " + targetSize);
            Set<Integer> idsInReduced =new HashSet<>();
            for (FrameClusterSimilarityInfo cs : clusterSimInfo) {
                if (cs.getFrameClusterID1() != cs.getFrameClusterID2()) {
                    forReduction.add(cs);
                    idsInReduced.add(cs.getFrameClusterID1());
                    idsInReduced.add(cs.getFrameClusterID2());
                    int newCluserSize = originFrameClusterSize - idsInReduced.size();
                           // forReduction.size();

                    if (newCluserSize <= targetSize) {
                        break;
                    }
                }
            }

        }
        return forReduction;
    }

    
    public List<FrameClusterSimilarityInfo> reduceClusterSizesUsingTopN(double topN, List<FrameClusterSimilarityInfo> clusterSimInfo) throws Exception {
       
        List<FrameClusterSimilarityInfo> forReduction = new ArrayList<>();
        //int originalSize = forReduction.size();
        

          //  int originFrameClusterSize = this.clusterSetIDs.size();
            int counter=0;
            // System.err.println("Target reduced size is " + targetSize);
           // Set<Integer> idsInReduced =new HashSet<>();
            for (FrameClusterSimilarityInfo cs : clusterSimInfo) {
                if (cs.getFrameClusterID1() != cs.getFrameClusterID2()) {
                    forReduction.add(cs);
                    //idsInReduced.add(cs.getFrameClusterID1());
                   // idsInReduced.add(cs.getFrameClusterID2());
                   // int newCluserSize = originFrameClusterSize - idsInReduced.size();
                           // forReduction.size();

                    if (counter++ >= topN) {
                        break;
                    }
                }
            }

        
        return forReduction;
    }

//    private void setClusterSim(int frameClusterID1, int frameClusterID2, double sim) {
//        if (frameClusterID1.compareTo(frameID2) > 0) {
//            this.framePairSimilarity.put(frameClusterID1 + "-" + frameID2, sim);
//        } else {
//            this.framePairSimilarity.put(frameID2 + "-" + frameClusterID1, sim);
//        }
//    }
//
    public Double getCachedClusterSim(int cluster1, int cluster2) {

        if (cluster1 < cluster2) {
            return getCachedClusterSim(cluster2, cluster1);
        } else {
            if (!clusterPairSimilarityInt.containsKey(cluster1)) {
                return null;
            } else {
                Map<Integer, Double> clust2Simvalmap = clusterPairSimilarityInt.get(cluster1);
                Double sim
                        = clust2Simvalmap.get(cluster1);
                return sim;
            }

            // sim = this.clusterPairSimilarity.get(cluster2 + "-" + cluster1);
        }

    }

    private void setClusterWiseSim(int cluster1, int cluster2, double sim) throws Exception {

        if (cluster1 < cluster2) {
            setClusterWiseSim(cluster2, cluster1, sim);
        } else {
            if (clusterPairSimilarityInt.containsKey(cluster1)) {
                Map<Integer, Double> cluster2Sim = clusterPairSimilarityInt.get(cluster1);
                Double putIfAbsent = cluster2Sim.putIfAbsent(cluster2, sim);
                if (putIfAbsent != null) {
                    if (putIfAbsent.compareTo(sim) != 0) {
                        //throw new Exception
                        System.err.println("Error in paral. cluster sim computation for pair" + cluster2 + " and  " + cluster1 + " ->" + putIfAbsent + " vs " + sim);
                    }
                }
            } else {
                ConcurrentHashMap<Integer, Double> cluster2SimNew = new ConcurrentHashMap<>();
                cluster2SimNew.put(cluster2, sim);
                ConcurrentHashMap<Integer, Double> putIfAbsent = clusterPairSimilarityInt.putIfAbsent(cluster1, cluster2SimNew);
                if (putIfAbsent != null) {
                    Double putIfAbsent1 = putIfAbsent.putIfAbsent(cluster2, sim);
                    if (putIfAbsent1 != null) {
                        if (putIfAbsent1.compareTo(sim) != 0) {
                            throw new Exception("Error in paral. cluster sim computation");
                        }
                    }
                }

            }
        }

//            Double putIfAbsent = this.clusterPairSimilarity.putIfAbsent(cluster1 + "-" + cluster2, sim);
//            if (putIfAbsent != null) {
//                if (putIfAbsent.compareTo(sim) != 0) {
//                    throw new Exception("Error in paral. cluster sim computation");
//                }
//            }
//        } else {
//            Double putIfAbsent = this.clusterPairSimilarity.putIfAbsent(cluster2 + "-" + cluster1, sim);
//            if (putIfAbsent != null) {
//                if (putIfAbsent.compareTo(sim) != 0) {
//                    throw new Exception("Error in paral. cluster sim computation");
//                }
//            }
//        }
    }

     protected double computerClusterPairSimilarity(Set<Long> frameInstanceSet1, Set<Long> frameInstanceSet2) {
        
        
         double computerClusterPairSimilaritySt = computerClusterPairSimilaritySt(frameInstanceSet1, frameInstanceSet2);
         double sim2 = computerClusterPairSimilarityMain(frameInstanceSet1, frameInstanceSet2);
         double[] sim1 = computerClusterPairSimilarityMinMax(frameInstanceSet1, frameInstanceSet2);
         double hMean = (2 * sim1[1] * sim2) / (sim1[1] + sim2);
         if (!Double.isFinite(hMean)) {
             hMean = 0;
         }
         double quadratM = Math.sqrt((sim1[1]*sim2)/2);
         //Set<Long> ff = new HashSet<>(frameInstanceSet1);
        // ff.addAll(frameInstanceSet2);
        //double dissim3= getSomthingIDonotKnowWhatToClass(ff);
       // double simST = computerClusterPairSimilaritySt(frameInstanceSet1, frameInstanceSet2);
         //System.err.println("dissm3 " + dissim3);
        // double[] values = {sim2, 1+(dissim3)};//, sim1,1-dissim3, computerClusterPairSimilaritySt}; 
      // double mean =  org.apache.commons.math3.stat.StatUtils.geometricMean(values);
    //  double mean = org.apache.commons.math3.stat.StatUtils.mean(values);
    //    RealMatrix matrix = null;
    //  org.apache.commons.math3.linear.SingularValueDecomposition svd = new  SingularValueDecomposition(matrix);
     
        // System.err.println("her?");
       //  double sim3 = computerClusterPairSimilaritySt(frameInstanceSet1, frameInstanceSet2);
    //    return 2/(1.0/(1+dissim3) + (1.0/sim2) +(1.0/sim1) );
        
                
    return sim2;// hMean;//quadratM;//(sim1[1]  +sim1[2])/2;

  //os far beat

//Math.sqrt(sim1[1]*sim1[1] + sim1[2]*sim1[2] );
                // +1/sim1
                 //(1+ 2*dissim3)* harmonic
         // Heronian mean a+b+sqrt(ab)
         // Contraharmonic mean
       //  double contraharmo=((1+dissim3)*(1+dissim3) + sim2*sim2 +sim1*sim1)/(1+dissim3+sim2+sim1);
//         double quadric= (1+dissim3)*(1+dissim3) + sim2*sim2 +sim1*sim1
//                 +computerClusterPairSimilaritySt*computerClusterPairSimilaritySt; // best stable so far
         //double quadric= (1+dissim3)*(1+dissim3) +sim1*sim1; 
        // return mean;
                // * sim1
                 
              //  * sim3
                 //;// sim1*sim2*sim3;//(sim1*sim2)/(sim1+sim2);
     }
     
//     private double remapNumber(double x){
//        double d =  (2.0/(1 + Math.exp(-x)))-1;
//        return d;
//     }
    protected double computerClusterPairSimilaritySt(Set<Long> frameInstanceSet1, Set<Long> frameInstanceSet2) {
//let's do it ineefficiencyy first
        // let's get an average of similarities // many other methods such min, max, single linkage etc can be used
        DoubleAdder da1 = new DoubleAdder();
        Set<Long> idsU = new HashSet<>(frameInstanceSet1);
        idsU.addAll(frameInstanceSet2);
        //int toDevide = frameInstanceSet1.size() * frameInstanceSet2.size();
        idsU.forEach(frameInstanceID1 -> {
            idsU.forEach(frameInstanceID2 -> {
                if(Long.compare(frameInstanceID1,frameInstanceID2)!=0){//this is always true since we are doing hard clustering
                Double frameCachedSim = getFrameWiseCachedSim(frameInstanceID1, frameInstanceID2);
//                if (frameCachedSim == null) {
//                    System.err.println(frameInstanceID1 + " " + frameInstanceID2 +" sim is null (id:763482)");
//                }
                da1.add(frameCachedSim);
            }
            });
        });
        
        int card = frameInstanceSet1.size()+frameInstanceSet2.size();
        double aveSim = da1.doubleValue()/(card* (card-1));
        return aveSim;
    }

    
      protected double computerClusterPairSimilarityMax(Set<Long> frameInstanceSet1, Set<Long> frameInstanceSet2) {
//let's do it ineefficiencyy first
        // let's get an average of similarities // many other methods such min, max, single linkage etc can be used
        //DoubleAdder da1 = new DoubleAdder();
        DoubleAccumulator d1 = MathUtil.getDoubleMaxAcc(0);
          
        //Set<Long> idsU = new HashSet<>(frameInstanceSet1);
        //idsU.addAll(frameInstanceSet2);
        //int toDevide = frameInstanceSet1.size() * frameInstanceSet2.size();
        frameInstanceSet1.forEach(frameInstanceID1 -> {
            frameInstanceSet2.forEach(frameInstanceID2 -> {
              //  if(Long.compare(frameInstanceID1,frameInstanceID2)!=0){//this is always true since we are doing hard clustering
                Double frameCachedSim = getFrameWiseCachedSim(frameInstanceID1, frameInstanceID2);
//                if (frameCachedSim == null) {
//                    System.err.println(frameInstanceID1 + " " + frameInstanceID2 +" sim is null (id:763482)");
//                }
                
                d1.accumulate(frameCachedSim);
                       
          //  }
            });
        });
        
       // int card = frameInstanceSet1.size()+frameInstanceSet2.size();
        double aveSim = d1.doubleValue();
        return aveSim;
    }

        protected double[] computerClusterPairSimilarityMinMax(Set<Long> frameInstanceSet1, Set<Long> frameInstanceSet2) {
//let's do it ineefficiencyy first
        // let's get an average of similarities // many other methods such min, max, single linkage etc can be used
        //DoubleAdder da1 = new DoubleAdder();
        DoubleAccumulator d1 = MathUtil.getDoubleMaxAcc(0);
          DoubleAccumulator d1Min = MathUtil.getDoubleMinAcc(Double.MAX_VALUE);
        //Set<Long> idsU = new HashSet<>(frameInstanceSet1);
        //idsU.addAll(frameInstanceSet2);
        //int toDevide = frameInstanceSet1.size() * frameInstanceSet2.size();
        frameInstanceSet1.forEach(frameInstanceID1 -> {
            frameInstanceSet2.forEach(frameInstanceID2 -> {
              //  if(Long.compare(frameInstanceID1,frameInstanceID2)!=0){//this is always true since we are doing hard clustering
                Double frameCachedSim = getFrameWiseCachedSim(frameInstanceID1, frameInstanceID2);
//                if (frameCachedSim == null) {
//                    System.err.println(frameInstanceID1 + " " + frameInstanceID2 +" sim is null (id:763482)");
//                }
                
                d1.accumulate(frameCachedSim);
                       d1Min.accumulate(frameCachedSim);
          //  }
            });
        });
        
       // int card = frameInstanceSet1.size()+frameInstanceSet2.size();
        double[] aveSim = {d1.doubleValue() * d1Min.doubleValue(), d1.doubleValue(),d1Min.doubleValue()};
        return aveSim;
    }
    
        
         
        protected double computerClusterPairSimilarityMain(Set<Long> frameInstanceSet1, Set<Long> frameInstanceSet2) {

        // let's get an average of similarities // many other methods such min, max, single linkage etc can be used
        DoubleAdder da = new DoubleAdder();
        int toDevide = frameInstanceSet1.size() * frameInstanceSet2.size();
        frameInstanceSet1.forEach(frameInstanceID1 -> {
            frameInstanceSet2.forEach(frameInstanceID2 -> {
               // if(frameInstanceID1!=frameInstanceID2){//this is always true since we are doing hard clustering
                Double frameCachedSim = getFrameWiseCachedSim(frameInstanceID1, frameInstanceID2);
                if (frameCachedSim == null) {
                    System.err.println(frameInstanceID1 + " " + frameInstanceID2 +" sim is null (id:763482)");
                }
                da.add(frameCachedSim);
            });
        });

        double averageSim = da.doubleValue() / toDevide;
        return averageSim;
    }

    private List<FrameClusterSimilarityInfo> sortByValue(
            Map<Integer, ConcurrentHashMap<Integer, Double>> clusterPairSimilarityInt, 
            Map<Integer, Set<Long>> frameClusterInstances) {
         Map<Integer, Double> llOfClusters = computeLoglikelihoodsPerCluster();
        List<FrameClusterSimilarityInfo> csInfo = new ArrayList<>();
        clusterPairSimilarityInt.keySet().forEach(key1 -> {
            int size1 = frameClusterInstances.get(key1).size();
            clusterPairSimilarityInt.get(key1).entrySet().forEach((Map.Entry<Integer, Double> entry) -> {
                int key2 =entry.getKey();
                int size2 = frameClusterInstances.get(key2).size();
                csInfo.add(new FrameClusterSimilarityInfo(
                        key1, key2, size1, size2, entry.getValue(),llOfClusters.get(key1),llOfClusters.get(key2)));
            });
        });
        Collections.sort(csInfo,Collections.reverseOrder());
        return csInfo;
    }
    
    public void getAsVectors(){
        int numberOfClusters = this.frameClusterInstances.size();
        Map<Integer, double[]> mapVec = new HashMap<>();
        for (int i = 0; i < numberOfClusters; i++) {
            double[] vector = new double[numberOfClusters];
            for (int j = 0; j < numberOfClusters; j++) {
                Double get = this.clusterPairSimilarityInt.get(i).get(j);
                vector[j] = get;

            }
            mapVec.put(i, vector);

        }
        List<FrameClusterSimilarityInfo> fcfcL = new ArrayList<>();
        for (int i = 0; i < numberOfClusters; i++) {
            double[] get = mapVec.get(i);
            for (int j = 0; j < numberOfClusters; j++) {
                if (i != j) {
                    double[] get2 = mapVec.get(j);
                    SpearmansCorrelation sp = new SpearmansCorrelation();
                    double correlation = sp.correlation(get, get2);
                    FrameClusterSimilarityInfo frameClusterSimilarityInfo = new FrameClusterSimilarityInfo(i, j, 1, 1, correlation);
                    fcfcL.add(frameClusterSimilarityInfo);
                }
            }
//            mapVec.put(i, vector);

        }
    
        
    
        
   
    }

    
}
