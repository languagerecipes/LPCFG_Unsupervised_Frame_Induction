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
import frameinduction.grammar.RuleMapsCollector;
import frameinduction.grammar.parse.CNFRule;
import frameinduction.grammar.SymbolFractory;
import frameinduction.grammar.learn.MathUtil;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.grammar.parse.io.SemRolePair;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.TerminalType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperParseChartIO {
    private static final Logger LOGGER = Logger.getLogger( HelperParseChartIO.class.getName() );
    /**
     * Return a string contains the parse (sub)tree which starts from the given
     * CNFRule
     *
     * @param cnf
     * @param tabbing
     * @param rm
     * @return
     * @throws Exception
     */
    public static String printTree(CNFRule cnf, String tabbing, RuleMaps rm) throws Exception {
        StringBuilder sb = new StringBuilder();
        NumberFormat formatter = new DecimalFormat("#0000000E0");
        if (cnf != null && !cnf.isTerminal()) {
            CNFRule rhs1 = cnf.getRhs1();
            CNFRule rhs2 = cnf.getRhs2();
            sb.append(tabbing).append(rm.getSymbolFromID(cnf.getSymbolLhs()));
            if (rhs1 != null) {
                sb.append("->");
                sb.append(rm.getSymbolFromID(rhs1.getSymbolLhs())).append(" ");
                if (rhs2 != null) {
                    sb.append(rm.getSymbolFromID(rhs2.getSymbolLhs()));
                }
            }
            String format = formatter.format(cnf.getProb());
            sb.append("\t [pr: ").append(format).append("]\n");
            String leftTree = printTree(rhs1, tabbing + tabbing, rm);
            String rightTree = printTree(rhs2, tabbing + tabbing, rm);
            sb.append(leftTree).append(rightTree);
            return sb.toString();
        }
        return "";
    }

//    private static void treeToPositionPair(CNFRule cnf, Fragment f, RuleMaps rm, ParseFrame pf) throws Exception {
//        
//        if (cnf != null && !cnf.isTerminal()) {
//            CNFRule rhs1 = cnf.getRhs1();
//            CNFRule rhs2 = cnf.getRhs2();
//            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());
//            if (SymbolFractory.isFrameHeadRule(symbolFromID)) {
//                int frameID = SymbolFractory.getFrameIndex(symbolFromID);
//                pf.setFrameNumber(frameID);
//                String head = getLexicalisedTerminal(rhs1, rm);
//                pf.setHead(head);
//                
//            } else if (SymbolFractory.isAFramesArgument(symbolFromID)) {
//                String lexicalisedTerminal = getLexicalisedTerminal(rhs1, rm);
//                
//               // PositionTagPair smr = new PositionTagPair(symbolFromID, lexicalisedTerminal);
//               // pf.addSemanticRoles(smr);
//            } else {
//                treeToFrame(rhs1, rm, pf);
//                treeToFrame(rhs2, rm, pf);
//            }
//        }
//    }
    private static void treeToFrame(CNFRule cnf, RuleMaps rm, ParseFrame pf) throws Exception {
        if (cnf != null && !cnf.isTerminal()) {
            CNFRule rhs1 = cnf.getRhs1();
            CNFRule rhs2 = cnf.getRhs2();
            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());

            if (SymbolFractory.isFrameHeadRule(symbolFromID)) {
                int frameID = SymbolFractory.getFrameIndex(symbolFromID);
                pf.setFrameNumber(frameID);
                String head = getLexicalisedTerminal(rhs1, rm);
                String positionInSent = rhs1.getPositionInSent();
                pf.setHeadPositionInSentence(positionInSent);
                pf.setHead(head);
            } else if (SymbolFractory.isAFramesArgument(symbolFromID)) {
                String lexicalisedTerminal
                        = getLexicalisedRole(rhs1, rm);

                String positionInSent
                        = cnf.getPositionInSent();

                SemRolePair smr = new SemRolePair(symbolFromID, lexicalisedTerminal, positionInSent);
                pf.addSemanticRoles(smr);
            } else {
                treeToFrame(rhs1, rm, pf);
                treeToFrame(rhs2, rm, pf);
            }
        }
    }

    public static ParseFrame parseTreeToFrameRoleSyntax(
            String identifier, CNFRule cnf, RuleMaps rm) throws Exception {
        ParseFrame pf = new ParseFrame(identifier);
        treeToFrameRoleSyntax(cnf, rm, pf);
        return pf;
    }

    public static ParseFrame parseTreeToFrame(String identifier,
            CNFRule cnf, RuleMaps rm) throws Exception {
        ParseFrame pf = new ParseFrame(identifier);
        if (cnf != null) {
            double prob = cnf.getProb();
            pf.setLikelihood(prob);
            treeToFrame(cnf, rm, pf);
        } else {
            //System.err.println("You can have new policies for unparsed data here, e.g., we can look for the longest string which is spanned by the model");
            LOGGER.log(Level.WARNING, "Can parse input " +identifier);
        }
        return pf;
    }

    public static void parseTreeToProductionRules(CNFRule cnf, RuleMaps rm, RuleMapsCollector result) throws Exception {
        if (cnf.isTerminal()) {
            return;
        }
        //double prob = cnf.getProb();
        int symbolLhs = cnf.getSymbolLhs();
        CNFRule rhs1 = cnf.getRhs1();
        CNFRule rhs2 = cnf.getRhs2();
        if (rhs1 != null) {
            int symbolLhs1 = rhs1.getSymbolLhs();
            if (rhs2 != null) {
                int symbolLhs2 = rhs2.getSymbolLhs();
                result.addRule(symbolLhs, symbolLhs1, symbolLhs2);
                // result.addRule(rm.getSymbolFromID(symbolLhs), rm.getSymbolFromID(symbolLhs1), rm.getSymbolFromID(symbolLhs2));
                parseTreeToProductionRules(rhs2, rm, result);
            } else {

                result.addRule(symbolLhs, symbolLhs1);
                // we have unary ... add unary
                // result.addRule(rm.getSymbolFromID(symbolLhs), rm.getSymbolFromID(symbolLhs1));
            }
            parseTreeToProductionRules(rhs1, rm, result);
        } else if (rhs2 != null) {
            LOGGER.log(Level.SEVERE, "Malformed rule for rhs2 " +rhs2);
            throw new Exception("malformed cnf grammar rule");
        }

    }

//    private static void treeToPositionPair(CNFRule cnf, Fragment f, RuleMaps rm, ParseFrame pf) throws Exception {
//        
//        if (cnf != null && !cnf.isTerminal()) {
//            CNFRule rhs1 = cnf.getRhs1();
//            CNFRule rhs2 = cnf.getRhs2();
//            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());
//            if (SymbolFractory.isFrameHeadRule(symbolFromID)) {
//                int frameID = SymbolFractory.getFrameIndex(symbolFromID);
//                pf.setFrameNumber(frameID);
//                String head = getLexicalisedTerminal(rhs1, rm);
//                pf.setHead(head);
//                
//            } else if (SymbolFractory.isAFramesArgument(symbolFromID)) {
//                String lexicalisedTerminal = getLexicalisedTerminal(rhs1, rm);
//                
//               // PositionTagPair smr = new PositionTagPair(symbolFromID, lexicalisedTerminal);
//               // pf.addSemanticRoles(smr);
//            } else {
//                treeToFrame(rhs1, rm, pf);
//                treeToFrame(rhs2, rm, pf);
//            }
//        }
//    }
    private static void treeToFrameRoleSyntax(CNFRule cnf, RuleMaps rm, ParseFrame pf) throws Exception {
        if (cnf != null && !cnf.isTerminal()) {
            CNFRule rhs1 = cnf.getRhs1();
            CNFRule rhs2 = cnf.getRhs2();
            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());

            if (SymbolFractory.isFrameHeadRule(symbolFromID)) {
                int frameID = SymbolFractory.getFrameIndex(symbolFromID);
                pf.setFrameNumber(frameID);
                String head = getLexicalisedTerminalForSyntax(rhs1, rm);
                String positionInSent = rhs1.getPositionInSent();
                pf.setHeadPositionInSentence(positionInSent);
                pf.setHead(head);
            } else if (SymbolFractory.isAFramesJArgumentSyntax(symbolFromID)) {
                String lexicalisedTerminal = getLexicalisedTerminalForSyntax(rhs1, rm);
                String positionInSent = rhs1.getPositionInSent();
                SemRolePair smr = new SemRolePair(symbolFromID, lexicalisedTerminal, positionInSent);
                pf.addSemanticRoles(smr);
            } else {
                treeToFrameRoleSyntax(rhs1, rm, pf);
                treeToFrameRoleSyntax(rhs2, rm, pf);
            }
        }
    }

    private static String getLexicalisedTerminal(CNFRule cnf, RuleMaps rm) throws Exception {
        if (cnf != null) {

            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());
            //System.out.println(symbolFromID);

            if (SymbolFractory.isLexicalisationRule(symbolFromID) //   SymbolFractory.isLexicalisationTopic(symbolFromID)
                    ) {

                return rm.getSymbolFromID(cnf.getRhs1().getSymbolLhs());
            } else {

                return getLexicalisedTerminal(cnf.getRhs1(), rm);
            }
        } else {
            throw new Exception("Error in lexicalization");
        }
    }

    private static String getLexicalisedRole(CNFRule cnf, RuleMaps rm) throws Exception {
        if (cnf != null) {

            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());
            //System.out.println(symbolFromID);
            return symbolFromID;
//            if (SymbolFractory.isLexicalisationRule(symbolFromID)) {
//                return rm.getSymbolFromID(cnf.getRhs1().getSymbolLhs());
//            } else {
//                return getLexicalisedTerminal(cnf.getRhs1(), rm);
//            }
        } else {
            throw new Exception("Error in lexicalization");
        }
    }

    private static String getLexicalisedTerminalForSyntax(CNFRule cnf, RuleMaps rm) throws Exception {
        if (cnf != null) {
            String symbolFromID = rm.getSymbolFromID(cnf.getSymbolLhs());
            //System.out.println(cnf.getSpanStart());

            if (SymbolFractory.isLexicalisationRule(symbolFromID) //  SymbolFractory.isLexicalisationTopic(symbolFromID)||SymbolFractory.isLexicalisationHead(symbolFromID)
                    ) {
                return rm.getSymbolFromID(cnf.getRhs1().getSymbolLhs());
            } else {
                return getLexicalisedTerminalForSyntax(cnf.getRhs1(), rm);
            }
        } else {
            throw new Exception("Error in lexicalization");
        }
    }

    /**
     * Map Fragments to parseFrames and assgin them the given id
     *
     * @param inputFramentList
     * @param id
     * @return
     * @throws Exception
     */
    public static Collection<ParseFrame> parseFragmentToFrame(Collection<Fragment> inputFramentList, int id) throws Exception {
        Collection<ParseFrame> pfList = new ArrayList<>();
        for (Fragment f : inputFramentList) {
            ParseFrame parseFragmentToFrame = HelperParseChartIO.parseFragmentToFrame(f);
            parseFragmentToFrame.setFrameNumber(id);
            pfList.add(parseFragmentToFrame);
        }
        return pfList;
    }
    
    public static Collection<ParseFrame> loadParseFrameFromFile(String file) throws IOException{
        // #20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
        Stream<String> lines = Files.lines(Paths.get(file));
        Collection<ParseFrame> pfCol = new ArrayList<>();
        lines.forEach(l->{
            ParseFrame parseFrameFromLine = ParseFrame.parseFrameFromLine(l);
            pfCol.add(parseFrameFromLine);
        });
         lines.close();
        return pfCol;
    }
    
    
     public static Collection<ParseFrame> loadGoldFrameFromFile(String file) throws IOException{
        // #20728009 5 select.1584 I-:-3-:-R_46^n stock-:-7-:-R_31^n return-:-12-:-R_4^n
        Stream<String> lines = Files.lines(Paths.get(file));
        Collection<ParseFrame> pfCol = new ArrayList<>();
        lines.forEach(l->{
            ParseFrame parseFrameFromLine = ParseFrame.parseFrameFromLine(l);
            pfCol.add(parseFrameFromLine);
        });
         lines.close();
        return pfCol;
    }
    
     
    /**
     * returns verbs that are clustered together
     * @param parseFrameCollection
     * @return 
     */
    public static Map<Integer, Set<String>> getClustersOfVerbs(Collection<ParseFrame> parseFrameCollection){
        Map<Integer, Collection<ParseFrame>> parseListToClustterMap
                = HelperParseChartIO.parseListToClustterMap(parseFrameCollection);

        Map<Integer, Set<String>> clustersToVerbs = new HashMap<>();
        parseListToClustterMap.entrySet().forEach(entry -> {
            Integer key = entry.getKey();
            Collection<ParseFrame> value = entry.getValue();

            Set<String> verbSet = new HashSet<>();
            clustersToVerbs.put(key, verbSet);
            value.forEach(pf -> {
                String head = pf.getHead();
                verbSet.add(head);
            });

        });
        return clustersToVerbs;
    }

    /**
     * Convert a Fragment Object to a ParseFrame object
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static ParseFrame parseFragmentToFrame(Fragment f) throws Exception {
        ParseFrame pf = new ParseFrame(f.getSentID());
        pf.setHead(f.getHead());
        pf.setHeadPositionInSentence(f.getTerminals().get(0).getPositionInSent());
        List<DepandantNode> terminals = f.getTerminals();
        List<SemRolePair> semroleList = new ArrayList<>();
        for (int i = 0; i < terminals.size(); i++) {
            DepandantNode dep = terminals.get(i);
            TerminalType type = dep.getType();

            if (TerminalType.Argument == type) {
                DepandantNode depType = terminals.get(i + 1);
                if (depType.getType() != TerminalType.DependancyRelation) {
                    throw new Exception("Expected dependency relation in parsing fragment to frame");
                }
                SemRolePair semRP = new SemRolePair(depType.getTerminalString(), dep.getTerminalString());
                semRP.setPositionInSentence(dep.getPositionInSent());
                semroleList.add(semRP);
            }
        }
        pf.setSemanticRoles(semroleList);

        return pf;
    }

    public static Map<Integer, Collection<ParseFrame>> parseListToClustterMap(Collection<ParseFrame> parseFrameList) {
        Map<Integer, Collection<ParseFrame>> clusterParseFrameMap = new ConcurrentHashMap<>();
        parseFrameList.parallelStream().forEach(pf -> {
            int fCLusterID = pf.getFrameClusterNumber();
            if (clusterParseFrameMap.containsKey(fCLusterID)) {
                clusterParseFrameMap.get(fCLusterID).add(pf);
            } else {
                Collection<ParseFrame> pfL = new ConcurrentLinkedQueue<>();
                pfL.add(pf);
                Collection<ParseFrame> putIfAbsent = clusterParseFrameMap.putIfAbsent(fCLusterID, pfL);
                if (putIfAbsent != null) {
                    putIfAbsent.add(pf);
                }

            }

        });

        return clusterParseFrameMap;
    }

    public static Map<Integer, Collection<ParseFrame>> parseListToClustterMap(Map<Long, ParseFrame> parseFrameList) {
        Map<Integer, Collection<ParseFrame>> clusterParseFrameMap = new ConcurrentHashMap<>();
        parseFrameList.values().parallelStream().forEach((ParseFrame pf) -> {
            int fCLusterID = pf.getFrameClusterNumber();
            if (clusterParseFrameMap.containsKey(fCLusterID)) {
                clusterParseFrameMap.get(fCLusterID).add(pf);
            } else {
                Collection<ParseFrame> pfL = new ConcurrentLinkedQueue<>();
                pfL.add(pf);
                Collection<ParseFrame> putIfAbsent = clusterParseFrameMap.putIfAbsent(fCLusterID, pfL);
                if (putIfAbsent != null) {
                    putIfAbsent.add(pf);
                }

            }

        });

        return clusterParseFrameMap;
    }

    public static Map<Long, ParseFrame> parseListToIDMap(Collection<ParseFrame> parseFrameList) {
        Map<Long, ParseFrame> clusterParseFrameMap = new ConcurrentHashMap<>();
        parseFrameList.parallelStream().forEach(pf -> {
            long pfID=0;
            try{
             pfID = pf.getUniqueIntID();
            }catch(Exception ex){
                System.err.println(">>>> ex " + ex);
            }
            if (clusterParseFrameMap.containsKey(pfID)) {
                int fID = clusterParseFrameMap.get(pfID).getFrameClusterNumber();
                if (fID != pf.getFrameClusterNumber()) {
                    throw new RuntimeException("We assumed hard clustering ... This should not happen");
                } else {
                    Logger.getGlobal().log(Level.INFO, "Duplicate fragement/parseframe in your input (id:476726).");
                }
            } else {

                ParseFrame putIfAbsent = clusterParseFrameMap.putIfAbsent(pfID, pf);
                if (putIfAbsent != null) {
                    Long uniqueIntID = putIfAbsent.getUniqueIntID();
                    if (uniqueIntID != pfID) {
                        throw new RuntimeException("this should never happend .. ");
                    }
                }

            }

        });

        return clusterParseFrameMap;
    }
    
    
    public static Map<String, List<ParseFrame>> parseListToSentenceMap(Collection<ParseFrame> parseFrameList) {
        Map<String, List<ParseFrame>> sentenceMap = new TreeMap<>();
        parseFrameList.forEach(pf -> {
            long pfID = pf.getUniqueIntID();
            String headPosition = pf.getHeadPositionInSentence();
            String frameSentenceIdentifier = pf.getFrameSentenceIdentifier();
            if (sentenceMap.containsKey(frameSentenceIdentifier)) {
                List<ParseFrame> fID = sentenceMap.get(frameSentenceIdentifier);
                fID.add(pf);
            } else {
                List<ParseFrame> fID = new ArrayList<>();
                fID.add(pf);
                sentenceMap.put(frameSentenceIdentifier, fID);

            }

        });

        return sentenceMap;
    }

    public static Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps(
            Map<Integer, Collection<ParseFrame>> map1,
            Map<Integer, Collection<ParseFrame>> map2) {

        Map<Integer, Collection<ParseFrame>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
        for (Collection<ParseFrame> e : map1.values()) {
            for (ParseFrame pf : e) {
                pf.setFrameNumber(clusterCounter);
            }
            mapMrged.put(clusterCounter, e);
            clusterCounter++;
        }
        for (Collection<ParseFrame> e : map2.values()) {
            for (ParseFrame pf : e) {
                pf.setFrameNumber(clusterCounter);
            }
            mapMrged.put(clusterCounter, e);
            clusterCounter++;
        }
        return mapMrged;

    }

    public static Map<Integer, Collection<ParseFrame>> mergeParseFrameMaps(
            Collection<HierachyBuilderResultWarpper> resultList) {

        Map<Integer, Collection<ParseFrame>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
        for (HierachyBuilderResultWarpper re : resultList) {
            Map<Integer, Collection<ParseFrame>> map2 = re.getParseFrameMap();
            for (Collection<ParseFrame> e : map2.values()) {
                for (ParseFrame pf : e) {
                    pf.setFrameNumber(clusterCounter);
                }
                mapMrged.put(clusterCounter, e);
                clusterCounter++;
            }
        }
        return mapMrged;

    }

//    public static Map<Integer, Collection<ParseFrame>> mergeParseFrameMapsLoL(
//            Collection< Collection<HierachyBuilderResultWarpper>> resultListofList) {
//
//        Map<Integer, Collection<ParseFrame>> mapMrged = new HashMap<>();
//        int clusterCounter = 0;
//        for (Collection<HierachyBuilderResultWarpper> resultList : resultListofList) {
//            for (HierachyBuilderResultWarpper re : resultList) {
//                Map<Integer, Collection<ParseFrame>> map2 = re.getParseFrameMap();
//                for (Collection<ParseFrame> e : map2.values()) {
//                    for (ParseFrame pf : e) {
//                        pf.setFrameNumber(clusterCounter);
//                    }
//                    mapMrged.put(clusterCounter, e);
//                    clusterCounter++;
//                }
//            }
//        }
//        return mapMrged;
//
//    }
    public static Map<Integer, Collection<ParseFrame>> mergeParseFrameMapsLoL(
            Collection< Collection<HierachyBuilderResultWarpper>> resultListofList) {

        Map<Integer, Collection<ParseFrame>> mapMrged = new ConcurrentHashMap<>();
        AtomicInteger clusterCounter = new AtomicInteger();
        for (Collection<HierachyBuilderResultWarpper> resultList : resultListofList) {
            for (HierachyBuilderResultWarpper re : resultList) {
                //   resultList.parallelStream().forEach(re->{
                Map<Integer, Collection<ParseFrame>> map2 = re.getParseFrameMap();
                for (Collection<ParseFrame> e : map2.values()) {
                    final int clusterID = clusterCounter.incrementAndGet();
                    e.parallelStream().forEach(pf -> {
                        // for (ParseFrame pf : e) {
                        pf.setFrameNumber(clusterID);
                    }
                    );
                    Collection<ParseFrame> putIfAbsent = mapMrged.putIfAbsent(clusterID, e);
                    if (putIfAbsent != null) {
                        throw new RuntimeException("Error beyond my understanding :P code:6727829");
                    }
                    //    clusterCounter++;
                }
            }
            //  );
        }
        return mapMrged;

    }

//    public static Collection<ParseFrame> flatParseFrameMaps(Map<Integer, Collection<ParseFrame>> mapParseFrame) {
//        Collection<ParseFrame> pfCollection = new ConcurrentLinkedQueue<>();
//        mapParseFrame.values().parallelStream().forEach(l -> {
//            
//            pfCollection.addAll(l);
//        });
//        return pfCollection;
//
//    }
    public static Collection<ParseFrame> flatParseFrameMaps(Map<Integer, Collection<ParseFrame>> mapParseFrame) {
        Collection<ParseFrame> pfCollection = new ConcurrentLinkedQueue<>();
        mapParseFrame.entrySet().parallelStream().forEach(e -> {
            Integer key = e.getKey();
            Collection<ParseFrame> l = e.getValue();
            l.parallelStream().forEach(pd -> {
                pd.setFrameNumber(key);
            });
            pfCollection.addAll(l);
        });
        return pfCollection;

    }

//    public void getParseFrameHeads(List<ParseFrame> parseFrameList) {
//        Set<String> heads = new HashSet<>();
//        for (ParseFrame pf : parseFrameList) {
//            heads.add(pf.getHead());
//        }
//        
//    }
    private static int getMaxKey(Set<Integer> intSet) {

        int max = Integer.MIN_VALUE;

        for (int i : intSet) {
            if (i > max) {
                max = i;
            }
        }

        return max;

    }

    public static double getLikelihood(Collection<ParseFrame> pfCollection) {
        DoubleAdder da = new DoubleAdder();
        pfCollection.parallelStream().forEach(pf -> {
            double likelihood = pf.getLikelihood();
            da.add(likelihood);
        });
        return da.doubleValue();

    }

    public static double sumProbs(Collection<ParseFrame> pfCollection) {
        DoubleAccumulator da = new DoubleAccumulator((double x, double y) -> MathUtil.logSumExp(x, y), Double.NEGATIVE_INFINITY);
        pfCollection.parallelStream().forEach(pf -> {
            double likelihood = pf.getLikelihood();
            da.accumulate(likelihood);
        });
        return da.doubleValue();

    }

    /**
     * Cluster by head verb, i.e., to break on clustering to one based on head verbs
     * @param mainMergeProcessToTgtedClustNumInner
     * @return 
     */
    public static Collection<ParseFrame> assignToClusterByHead(Collection<ParseFrame> mainMergeProcessToTgtedClustNumInner) {
        Map<Integer, Collection<ParseFrame>> parseListToClustterMap = parseListToClustterMap(mainMergeProcessToTgtedClustNumInner);
        List<Map<String, Collection<ParseFrame>>> saveMaps = new ArrayList<>();
        for (Collection<ParseFrame> v : parseListToClustterMap.values()) {
            Map<String, Collection<ParseFrame>> vMap = new HashMap<>();
            for (ParseFrame p : v) {
                String head = p.getHead();
                if (vMap.containsKey(head)) {
                    vMap.get(head).add(p);
                } else {
                    Collection<ParseFrame> pfList = new ArrayList<>();
                    pfList.add(p);
                    vMap.put(head, pfList);

                }
            }
            saveMaps.add(vMap);
        }

        int key = 0;
        Collection<ParseFrame> pfFinal = new ArrayList<>();
        for (Map<String, Collection<ParseFrame>> map : saveMaps) {
            for (Collection<ParseFrame> col : map.values()) {
                for (ParseFrame p : col) {
                    p.setFrameNumber(key);

                    pfFinal.add(p);
                }
                key++;
            }

        }
        return pfFinal;

    }

}
