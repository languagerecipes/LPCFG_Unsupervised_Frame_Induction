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
package frameinduction.grammar;

import frameinduction.grammar.learn.MathUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class RuleMaps {

    Map<String, Set<Integer>> symbolTypesAndTheirInstances;
    Map<String, Set<String>> symbolTypesAndTheirInstcesID;
    private final static String UNARY_RULE_FILE_NAME = "unary-rules.txt";
    private final static String BINARY_RULE_FILE_NAME = "binary-rules.txt";
    private static final String LHS_RHS_DELIM = " -> ";
    private static final String WEIGHT_DELIM = "\t";
    private static final String RHS_DELIM = " ";
    //private int startSymbolID;
    protected AtomicInteger symbolCounter;
    private final Map<String, Integer> symbolToIDMap;
    //private final List<String> symbolInverseMap;
    private Map<Integer, String> symbolInverseMap; // @BQ this must be changed to an array for an efficient IO

    // private int countBinaryRules;
    // private int countUnaryRules;
    // at some stage change to immutable?!
    private Map<Integer, Set<Integer>> unaryRuleMap;
    private Map<Integer, Map<Integer, Set<Integer>>> binaryRuleMap;

    private Map<Integer, Map<Integer, Double>> reversUnaryRuleMap;
    private Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap;

    private Map<Integer, Map<Integer, Double>> ruleUnaryWeightMap;
    private Map<Integer, Map<Integer, Map<Integer, Double>>> binaryRuleWeightMap;
    private final double PROB_POW_ONE = 1.3;
    private final double INNER_PROB_POWER = -1;
    private AtomicInteger clusterIndexCounter = new AtomicInteger();
    private static final Logger LOGGER = Logger.getLogger(RuleMaps.class.getName());
    static final String PATTERN_FOR_CLUSTER_ID_REGEX = "^(.*\\_)(\\d+)(\\^.*)$"; // this pattern is defined wrt preassumptions in grammar genneration

    public RuleMaps() {
        this(0);

    }

    public RuleMaps(int symbolIDCounterValue) {
        symbolCounter = new AtomicInteger(symbolIDCounterValue);
        clusterIndexCounter = new AtomicInteger(0);
        symbolInverseMap
                = new ConcurrentHashMap<>();
        this.unaryRuleMap = new ConcurrentHashMap<>();
        this.binaryRuleMap = new ConcurrentHashMap<>();
        this.symbolToIDMap = new ConcurrentHashMap<>();

    }

    public void setSymbolTypesAndTheirInstances(Map<String, Set<Integer>> symbolTypesAndTheirInstances) {
        this.symbolTypesAndTheirInstances = symbolTypesAndTheirInstances;
    }

    public AtomicInteger getSymbolIDCounter() {
        return symbolCounter;
    }

    public RuleMaps(AtomicInteger symbolCounter,
            Map<String, Integer> symbolToIDMap,
            Map<Integer, String> symbolInverseMap,
            Map<Integer, Set<Integer>> unaryRuleMap,
            Map<Integer, Map<Integer, Set<Integer>>> binaryRuleMap) {
        this.symbolCounter = symbolCounter;
        this.symbolToIDMap = symbolToIDMap;
        this.symbolInverseMap = symbolInverseMap;
        this.unaryRuleMap = unaryRuleMap;
        this.binaryRuleMap = binaryRuleMap;

    }

    public synchronized void removeSympolIDMapping(int symbolID) {
        if (this.symbolInverseMap.containsKey(symbolID)) {
            String remove = symbolInverseMap.remove(symbolID);

            this.symbolToIDMap.remove(remove);
            LOGGER.log(Level.FINE, "Removed {0} with id {1}", new Object[]{remove, symbolID});
        } else {
            LOGGER.log(Level.FINE, "No such id to remove code: rm6562 -- id=" + symbolID);

        }
    }

    public int getSymbolTableSize() {
        return this.symbolToIDMap.size() + 1;
    }

    public int getMaxIDUsed() {
        return this.symbolCounter.get();
    }

    public synchronized void setKnownSymbolToIDMap(Map<String, Integer> newSymbol) throws Exception {
        int maxIDUsed = this.symbolCounter.get();

        for (String key : newSymbol.keySet()) {
            Integer getNewID = newSymbol.get(key);
            if (this.symbolToIDMap.containsKey(key)) {
                Integer get = this.symbolToIDMap.get(key);

                if (!getNewID.equals(get)) {
                    throw new Exception("ID mismatch in symbol mapping ... !! :-(");
                } else {

                    this.symbolToIDMap.put(key, newSymbol.get(key));
                }
            } else {

                this.symbolToIDMap.put(key, newSymbol.get(key));
            }
            maxIDUsed = Math.max(getNewID, maxIDUsed);
        }
        this.symbolCounter.set(maxIDUsed);
    }

    public synchronized void setKnownIDToSymbolMap(Map<Integer, String> newSymbol) throws Exception {
        int maxIDUsed = this.symbolCounter.get();
        for (Integer key : newSymbol.keySet()) {
            String getNewSymbol = newSymbol.get(key);
            if (this.symbolInverseMap.containsKey(key)) {
                String symbol = this.symbolInverseMap.get(key);

                if (!getNewSymbol.equals(symbol)) {
                    throw new Exception("Symbol-ID mismatch in symbol mapping ... !! :-(");
                } else {

                    this.symbolInverseMap.put(key, newSymbol.get(key));
                }
            } else {

                this.symbolInverseMap.put(key, newSymbol.get(key));
            }
            maxIDUsed = Math.max(key, maxIDUsed);
        }
        this.symbolCounter.set(maxIDUsed);
    }

    public RuleMaps(RuleMaps rm) {
        // this.startSymbolID = rm.getStartSymbolID();

        this.symbolCounter = new AtomicInteger(rm.symbolCounter.get());

        this.symbolToIDMap = new ConcurrentHashMap<>(rm.symbolToIDMap); // a map of immutable, it is ok as a deep copy
        //this.symbolInverseMap = new CopyOnWriteArrayList<>(rm.symbolInverseMap);
        this.symbolInverseMap = new ConcurrentHashMap<>(rm.symbolInverseMap);

        //   this.countBinaryRules = rm.countBinaryRules;
        //this.countUnaryRules = rm.countUnaryRules;
        // @BQ: at some stage in this object's lifecycle change to unmodifiable
        this.unaryRuleMap = new ConcurrentHashMap<>();
        // for (Integer k : rm.unaryRuleMap.keySet()) {
        rm.unaryRuleMap.keySet().parallelStream().forEach(k -> {
            Set<Integer> get = rm.unaryRuleMap.get(k);
            Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
            get.parallelStream().forEach(v -> {
                newKeySet.add(v);
            });
            unaryRuleMap.put(k, newKeySet);
        });

        // deep copy binary rule map
        this.binaryRuleMap = new ConcurrentHashMap<>();
        //for (Integer ky : rm.binaryRuleMap.keySet()) {
        rm.binaryRuleMap.keySet().parallelStream().forEach(ky -> {
            Map<Integer, Set<Integer>> getRMInner = rm.binaryRuleMap.get(ky);
            Map<Integer, Set<Integer>> getNewInner = new ConcurrentHashMap<>();
            for (Integer keyRhs2 : getRMInner.keySet()) {
                Set<Integer> get = getRMInner.get(keyRhs2);
                Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
                get.parallelStream().forEach(v -> {
                    newKeySet.add(v);
                });
                getNewInner.put(keyRhs2, newKeySet);
            }

            this.binaryRuleMap.put(ky, getRMInner);
        }
        );
        // deep copy the reverse map
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        //for (Integer key : rm.reversUnaryRuleMap.keySet()) {
        rm.reversUnaryRuleMap.keySet().parallelStream().forEach(key -> {;
            Map<Integer, Double> intMap = rm.reversUnaryRuleMap.get(key);
            Map<Integer, Double> reversUnaryRuleMapInner = new ConcurrentHashMap<>(intMap);
            this.reversUnaryRuleMap.put(key, reversUnaryRuleMapInner);
        }
        );
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        // for (Entry<Integer, Map<Integer, Map<Integer, Double>>> eBin : rm.reverseBinaryRuleMap.entrySet()) {
        rm.reverseBinaryRuleMap.entrySet().parallelStream().forEach(eBin -> {
            Integer keyRhs1 = eBin.getKey();
            Map<Integer, Map<Integer, Double>> rhs22LhsMap = eBin.getValue();
            Map<Integer, Map<Integer, Double>> innerreverseBinaryRuleMap = new ConcurrentHashMap<>();
            for (Integer rhsKey2 : rhs22LhsMap.keySet()) {
                Map<Integer, Double> lhsParam = rhs22LhsMap.get(rhsKey2);
                Map<Integer, Double> getInnerInner = new ConcurrentHashMap<>(lhsParam);
                innerreverseBinaryRuleMap.put(rhsKey2, getInnerInner);
            }
            this.reverseBinaryRuleMap.put(keyRhs1, innerreverseBinaryRuleMap);
        });

    }

    /**
     * Create rule map by removing symbols that are listed in the @param
     * symbolsToFilter
     *
     * @param rm
     * @param symbolsToFilter
     * @throws Exception
     */
    public RuleMaps(RuleMaps rm, Collection<String> symbolsToFilter) throws Exception {
        // this.startSymbolID = rm.getStartSymbolID();

        Set<Integer> toFilter = new HashSet<>();
        for (String s : symbolsToFilter) {
            int idFromSymbol = rm.getIDFromSymbol(s);
            LOGGER.log(Level.FINE, "adding " + s + " " + idFromSymbol);
            toFilter.add(idFromSymbol);
        }

        this.symbolCounter = new AtomicInteger(rm.symbolCounter.get());
        this.symbolToIDMap = new ConcurrentHashMap<>(rm.symbolToIDMap); // a map of immutable, it is ok as a deep copy
        this.symbolInverseMap = new ConcurrentHashMap<>(rm.symbolInverseMap);

        // @BQ: at some stage in this object's lifecycle, change the datastructs to unmodifiable
        this.unaryRuleMap = new ConcurrentHashMap<>();
        for (Integer k : rm.unaryRuleMap.keySet()) {

            if (!toFilter.contains(k)) {
                Set<Integer> get = rm.unaryRuleMap.get(k);
                Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
                get.forEach((Integer v) -> {
                    if (!toFilter.contains(v)) {
                        newKeySet.add(v);
                    } else {
                        try {
                            LOGGER.log(Level.FINE, "Filtered/Merged symbol " + v + rm.getSymbolFromID(v));

                        } catch (Exception ex) {
                            Logger.getLogger(RuleMaps.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                unaryRuleMap.put(k, newKeySet);
            } else {
                System.err.println("Filtered/Merged symbol " + k + rm.getSymbolFromID(k));
            }
        }

        // deep copy binary rule map
        this.binaryRuleMap = new ConcurrentHashMap<>();
        for (Integer ky : rm.binaryRuleMap.keySet()) {
            if (!toFilter.contains(ky)) {
                Map<Integer, Set<Integer>> getRMInner = rm.binaryRuleMap.get(ky);
                Map<Integer, Set<Integer>> getNewInner = new ConcurrentHashMap<>();
                for (Integer keyRhs2 : getRMInner.keySet()) {
                    if (!toFilter.contains(keyRhs2)) {
                        Set<Integer> get = getRMInner.get(keyRhs2);
                        Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
                        get.forEach(v -> {
                            if (!toFilter.contains(v)) {

                                newKeySet.add(v);

                            } else {
                                try {
                                    LOGGER.log(Level.FINE, "Filtered/Merged symbol " + v + rm.getSymbolFromID(v));

                                } catch (Exception ex) {
                                    Logger.getLogger(RuleMaps.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                        getNewInner.put(keyRhs2, newKeySet);
                    } else {
                        LOGGER.log(Level.FINE, "Filtered/Merged symbol " + keyRhs2 + rm.getSymbolFromID(keyRhs2));

                    }
                }

                this.binaryRuleMap.put(ky, getRMInner);
            } else {
                LOGGER.log(Level.FINE, "Filtered/Merged symbol " + ky + rm.getSymbolFromID(ky));

            }
        }
        // deep copy the reverse map
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();

        for (Integer key : rm.reversUnaryRuleMap.keySet()) {
            if (!toFilter.contains(key)) {
                Map<Integer, Double> intMap = rm.reversUnaryRuleMap.get(key);
                Map<Integer, Double> reversUnaryRuleMapInner = new ConcurrentHashMap<>();

                for (Integer keyInner : intMap.keySet()) {
                    if (!toFilter.contains(keyInner)) {
                        intMap.put(keyInner, intMap.get(keyInner));
                    } else {
                        LOGGER.log(Level.FINE, "Filtered/Merged symbol " + keyInner + rm.getSymbolFromID(keyInner));

                    }

                }

                this.reversUnaryRuleMap.put(key, reversUnaryRuleMapInner);
            } else {
                LOGGER.log(Level.FINE, "Filtered/Merged symbol " + key + rm.getSymbolFromID(key));

            }
        }

        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> eBin : rm.reverseBinaryRuleMap.entrySet()) {
            Integer keyRhs1 = eBin.getKey();
            if (!toFilter.contains(keyRhs1)) {
                Map<Integer, Map<Integer, Double>> rhs22LhsMap = eBin.getValue();
                Map<Integer, Map<Integer, Double>> innerreverseBinaryRuleMap = new ConcurrentHashMap<>();
                for (Integer rhsKey2 : rhs22LhsMap.keySet()) {
                    if (!toFilter.contains(rhsKey2)) {
                        Map<Integer, Double> lhsParam = rhs22LhsMap.get(rhsKey2);
                        Map<Integer, Double> getInnerInner = new ConcurrentHashMap<>();

                        for (Integer keyInnerInner : lhsParam.keySet()) {
                            if (!toFilter.contains(keyInnerInner)) {
                                getInnerInner.put(keyInnerInner, lhsParam.get(keyInnerInner));
                            } else {
                                LOGGER.log(Level.FINE, "Filtered/Merged symbol " + keyInnerInner + rm.getSymbolFromID(keyInnerInner));

                            }
                        }

                        innerreverseBinaryRuleMap.put(rhsKey2, getInnerInner);
                    } else {
                        LOGGER.log(Level.FINE,
                                "Filtered/Merged symbol " + rhsKey2 + rm.getSymbolFromID(rhsKey2));
                    }
                }
                this.reverseBinaryRuleMap.put(keyRhs1, innerreverseBinaryRuleMap);
            } else {
                LOGGER.log(Level.FINE,
                        "Filtered/Merged symbol " + keyRhs1 + rm.getSymbolFromID(keyRhs1));
            }
        }

    }

    public static RuleMaps partialRuleMapCopy(RuleMaps rm) {
        // this.startSymbolID = rm.getStartSymbolID();
        AtomicInteger aiSymbolCounter = new AtomicInteger(rm.symbolCounter.get());
        ConcurrentHashMap<String, Integer> symbolToIDMAP = new ConcurrentHashMap<>(rm.symbolToIDMap);
        ConcurrentHashMap<Integer, String> inversMAp = new ConcurrentHashMap<>(rm.symbolInverseMap);

        Map<Integer, Set<Integer>> unaryRuleMap = new ConcurrentHashMap<>();
        Map<Integer, Map<Integer, Set<Integer>>> binaryRuleMap = new ConcurrentHashMap<>();
        for (Integer k : rm.unaryRuleMap.keySet()) {
            Set<Integer> get = rm.unaryRuleMap.get(k);
            Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
            get.forEach(v -> {
                newKeySet.add(v);
            });
            unaryRuleMap.put(k, newKeySet);
        }

        for (Integer ky : rm.binaryRuleMap.keySet()) {
            Map<Integer, Set<Integer>> getRMInner = rm.binaryRuleMap.get(ky);
            Map<Integer, Set<Integer>> getNewInner = new ConcurrentHashMap<>();
            for (Integer keyRhs2 : getRMInner.keySet()) {
                Set<Integer> get = getRMInner.get(keyRhs2);
                Set<Integer> newKeySet = ConcurrentHashMap.newKeySet(get.size());
                get.forEach(v -> {
                    newKeySet.add(v);
                });
                getNewInner.put(keyRhs2, newKeySet);
            }

            binaryRuleMap.put(ky, getRMInner);
        }
        RuleMaps rmToReturn = new RuleMaps(aiSymbolCounter, symbolToIDMAP, inversMAp, unaryRuleMap, binaryRuleMap);
        return rmToReturn;
    }

    public synchronized void nulifyUnaryAndBinaryMaps() {
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
    }

    public synchronized void instNewReverseMaps() {
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
    }

    //@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public static RuleMaps fromArchivedFile(String zippedRulsfile) throws IOException {
        RuleMaps rm = new RuleMaps();
        ZipFile zipFile = new ZipFile(zippedRulsfile);
        ZipEntry unary = zipFile.getEntry(UNARY_RULE_FILE_NAME);
        InputStream inputStream = zipFile.getInputStream(unary);

        rm.reversUnaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        br.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                int rhsIntID = rm.getIDFromSymbol(lhsRhs[1]);
                //if(weight<-1E50){
                // System.err.println("FILTER " + weight);
                rm.addReverseUnaryRule(lhsIntID, rhsIntID, weight);
                //}

//                rm.countUnaryRules++;
            } catch (Exception ex) {

                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);
                return;
            }
        });

        ZipEntry binary = zipFile.getEntry(BINARY_RULE_FILE_NAME);
        InputStream inputStreamBin = zipFile.getInputStream(binary);

        rm.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader brBin = new BufferedReader(new InputStreamReader(inputStreamBin));
        brBin.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                String[] rhsBits = lhsRhs[1].split(RHS_DELIM);
                int rhsIntID1 = rm.getIDFromSymbol(rhsBits[0]);
                int rhsIntID2 = rm.getIDFromSymbol(rhsBits[1]);
                rm.addReverseBinaryRule(lhsIntID, rhsIntID1, rhsIntID2, weight);
//                rm.countBinaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);

            }
        });
        // rm.startSymbolID = rm.getIDFromSymbol(SymbolFractory.getStartSymbol());
        LOGGER.log(Level.INFO, "Loaded {0} Binary Rules and {1} Unaries.", new Object[]{rm.getCountBinaryRules(), rm.getCountUnaryRules()});
        return rm;
    }

       public static RuleMaps fromArchivedFileSymbolSetTypeMap(String zippedRulsfile) throws IOException {
        RuleMaps rm = new RuleMaps();
        ZipFile zipFile = new ZipFile(zippedRulsfile);
        ZipEntry unary = zipFile.getEntry(UNARY_RULE_FILE_NAME);
        InputStream inputStream = zipFile.getInputStream(unary);

        rm.reversUnaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        br.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                String[] split = lhsRhs[0].split("_");
                System.err.println(split[0]+" "+ split[1]);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                int rhsIntID = rm.getIDFromSymbol(lhsRhs[1]);
                //if(weight<-1E50){
                // System.err.println("FILTER " + weight);
                rm.addReverseUnaryRule(lhsIntID, rhsIntID, weight);
                //}

//                rm.countUnaryRules++;
            } catch (Exception ex) {

                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);
                return;
            }
        });

        ZipEntry binary = zipFile.getEntry(BINARY_RULE_FILE_NAME);
        InputStream inputStreamBin = zipFile.getInputStream(binary);

        rm.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader brBin = new BufferedReader(new InputStreamReader(inputStreamBin));
        brBin.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                String[] rhsBits = lhsRhs[1].split(RHS_DELIM);
                int rhsIntID1 = rm.getIDFromSymbol(rhsBits[0]);
                int rhsIntID2 = rm.getIDFromSymbol(rhsBits[1]);
                rm.addReverseBinaryRule(lhsIntID, rhsIntID1, rhsIntID2, weight);
//                rm.countBinaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);

            }
        });
        // rm.startSymbolID = rm.getIDFromSymbol(SymbolFractory.getStartSymbol());
        LOGGER.log(Level.INFO, "Loaded {0} Binary Rules and {1} Unaries.", new Object[]{rm.getCountBinaryRules(), rm.getCountUnaryRules()});
        return rm;
    }
    public RuleMaps shiftClusterAndSymbolIDs(int offsetSymbolID, int offsetClusterID) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);
        ZipEntry eUnary = new ZipEntry(UNARY_RULE_FILE_NAME);
        out.putNextEntry(eUnary);
        serializeUnaryRules(out);
        out.closeEntry();
        ZipEntry bUnary = new ZipEntry(BINARY_RULE_FILE_NAME);
        out.putNextEntry(bUnary);
        serializeBinaryRules(out);
        out.closeEntry();
        out.close();
        byte[] toByteArray = bos.toByteArray();
        ByteArrayInputStream bit = new ByteArrayInputStream(toByteArray);
        ZipInputStream zipInputStream = new ZipInputStream(bit);
        
        RuleMaps convertedIDs = fromArchivedFile(zipInputStream, offsetSymbolID, offsetClusterID);
        return convertedIDs;
    }

    public static RuleMaps fromArchivedFile(ZipInputStream inputStream, int offsetSymbolID, int offsetClusterID) throws IOException {
        RuleMaps rm = new RuleMaps(offsetSymbolID);
        ZipEntry unary = inputStream.getNextEntry(); // set position of stream to the first entry, which is always unaries
        rm.reversUnaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        br.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                String lhs = lhsRhs[0];
                String rhs = lhsRhs[1];

                lhs = ruleClusterIndexModifierAdd(lhs, offsetClusterID);
                rhs = ruleClusterIndexModifierAdd(rhs, offsetClusterID);

                int lhsIntID = rm.getIDFromSymbol(lhs);
                int rhsIntID = rm.getIDFromSymbol(rhs);
                //if(weight<-1E50){
                // System.err.println("FILTER " + weight);
                rm.addReverseUnaryRule(lhsIntID, rhsIntID, weight);
                //}

//                rm.countUnaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);
                return;
            }
        });
        ZipInputStream inputStreamBin = inputStream;//zipFile.getInputStream(binary);
        inputStreamBin.getNextEntry();
        rm.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader brBin = new BufferedReader(new InputStreamReader(inputStreamBin));
        brBin.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                String[] rhsBits = lhsRhs[1].split(RHS_DELIM);
                lhsRhs[0] = ruleClusterIndexModifierAdd(lhsRhs[0], offsetClusterID);
                rhsBits[0] = ruleClusterIndexModifierAdd(rhsBits[0], offsetClusterID);
                rhsBits[1] = ruleClusterIndexModifierAdd(rhsBits[1], offsetClusterID);

                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                int rhsIntID1 = rm.getIDFromSymbol(rhsBits[0]);
                int rhsIntID2 = rm.getIDFromSymbol(rhsBits[1]);
                rm.addReverseBinaryRule(lhsIntID, rhsIntID1, rhsIntID2, weight);
//                rm.countBinaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);

            }
        });
        // rm.startSymbolID = rm.getIDFromSymbol(SymbolFractory.getStartSymbol());
        LOGGER.log(Level.INFO, "Loaded {0} Binary Rules and {1} Unaries.", new Object[]{rm.getCountBinaryRules(), rm.getCountUnaryRules()});
        return rm;
    }

    public static RuleMaps fromArchivedFile2(String zippedRulsfile) throws IOException {
        RuleMaps rm = new RuleMaps();
        ZipFile zipFile = new ZipFile(zippedRulsfile);
        ZipEntry unary = zipFile.getEntry(UNARY_RULE_FILE_NAME);
        InputStream inputStream = zipFile.getInputStream(unary);

        rm.reversUnaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        br.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                int rhsIntID = rm.getIDFromSymbol(lhsRhs[1]);
                rm.addReverseUnaryRule(lhsIntID, rhsIntID, weight);
                rm.addRule(lhsRhs[0], lhsRhs[1]);
//                rm.countUnaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);
                return;
            }
        });

        ZipEntry binary = zipFile.getEntry(BINARY_RULE_FILE_NAME);
        InputStream inputStreamBin = zipFile.getInputStream(binary);

        rm.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        BufferedReader brBin = new BufferedReader(new InputStreamReader(inputStreamBin));
        brBin.lines().forEach(line -> {
            try {

                String[] restWeight = line.split(WEIGHT_DELIM);
                double weight = Double.parseDouble(restWeight[1]);
                String[] lhsRhs = restWeight[0].split(LHS_RHS_DELIM);
                int lhsIntID = rm.getIDFromSymbol(lhsRhs[0]);
                String[] rhsBits = lhsRhs[1].split(RHS_DELIM);
                int rhsIntID1 = rm.getIDFromSymbol(rhsBits[0]);
                int rhsIntID2 = rm.getIDFromSymbol(rhsBits[1]);
                rm.addReverseBinaryRule(lhsIntID, rhsIntID1, rhsIntID2, weight);
                rm.addRule(lhsRhs[0], rhsBits[0], rhsBits[1]);
//                rm.countBinaryRules++;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
                LOGGER.log(Level.SEVERE, "Error cause when loading rules for line " + line);
                return;
            }
        });
        // rm.startSymbolID = rm.getIDFromSymbol(SymbolFractory.getStartSymbol());
        LOGGER.log(Level.INFO, "Loaded " + rm.getCountBinaryRules() + " Binary Rules and " + rm.getCountUnaryRules() + " Unaries.");

        return rm;
    }

    /**
     * Method for mapping from String symbols to integer keys if the id does not
     * exist, it will add the symbol to the map
     *
     * @param symbol
     * @return
     */
    public synchronized int getIDFromSymbol(String symbol) throws Exception {
        if (symbolToIDMap.containsKey(symbol)) {
            return symbolToIDMap.get(symbol);
        } else {
            int id = symbolCounter.addAndGet(1);
            String putIfAbsent = symbolInverseMap.putIfAbsent(id, symbol); // this must have a small complexity since there is an order in place?!
            if (putIfAbsent != null) {

                LOGGER.log(Level.SEVERE, "Error cause 7566753 ");
                throw new Exception("code 5632: heh?! :-)");
            }
            Integer putIfAbsent1 = symbolToIDMap.putIfAbsent(symbol, id);
            if (putIfAbsent1 != null) {
                LOGGER.log(Level.SEVERE, "Possible Error due to race condition ... check! code76372 ");
            }
            return id;
        }
    }

    /**
     * Method used during embedding-based parsing to make sure that the assigned
     * new ids to terminals not in the grammar do not override already exisiting
     * ids.
     */
    public synchronized void verifySymbolCounterStatus() {
        Set<Integer> keySet = this.symbolInverseMap.keySet();
        if (keySet.isEmpty()) {
            this.symbolCounter.set(0);
        } else {
            int maxID = keySet.stream().mapToInt(i -> i).max().getAsInt();
            this.symbolCounter.set(maxID + 1);
        }
    }

    public AtomicInteger getClusterIndexCounter() {
        setClusterIDCounterStatus(); // this can be done more efficiently 
        return clusterIndexCounter;
    }

    public synchronized void setClusterIDCounterStatus() {
        Collection<String> symbolSet = this.symbolInverseMap.values();
        if (symbolSet.isEmpty()) {

            this.clusterIndexCounter.set(0);
        } else {

            OptionalInt mapToInt = symbolSet.stream().parallel().unordered().mapToInt(
                    x -> {
                        return getClusterID(x);
                    }
            ).max();//.orElseThrow(NoSuchElementException::new);

            this.clusterIndexCounter.set(mapToInt.getAsInt() + 1);
        }
    }

    public static int getClusterID(String rhsOrLhs) {

        Pattern pm = Pattern.compile(PATTERN_FOR_CLUSTER_ID_REGEX);
        Matcher m = pm.matcher(rhsOrLhs);

        int assignedClusterIndex = 0;
        if (m.matches()) {

            String assignedCluster = m.group(2);
            assignedClusterIndex = Integer.parseInt(assignedCluster);

        }
        return assignedClusterIndex;
    }

    public static String ruleClusterIndexModifierAdd(String rhsOrLhs, int offset) {

        Pattern pm = Pattern.compile(PATTERN_FOR_CLUSTER_ID_REGEX);
        Matcher m = pm.matcher(rhsOrLhs);
        String returnValue = rhsOrLhs;
        if (m.matches()) {
            String nodeTypePrefix = m.group(1).trim();
            String assignedCluster = m.group(2);
            int assignedClusterIndex = Integer.parseInt(assignedCluster);
            assignedClusterIndex += offset;

            String prefixRemaining = "";
            for (int j = 3; j <= m.groupCount(); ++j) {

                prefixRemaining += m.group(j);
            }
            returnValue = nodeTypePrefix + assignedClusterIndex + prefixRemaining;
        }
        return returnValue;
    }

    public String getSymbolFromID(int id) throws Exception {

        //if (id > this.symbolInverseMap.size()) {
        if (!symbolInverseMap.containsKey(id)) {
            throw new Exception("ID does not exist " + id);
        } else {
            return symbolInverseMap.get(id);
        }
    }

    public synchronized void addRule(String lhs, String rhs1, String rhs2) throws Exception {
        addRule(lhs, rhs1, rhs2, false);
    }

    public synchronized void addRule(String lhs, String rhs1, String rhs2, boolean allowOverride) throws Exception {
        int idLHS = getIDFromSymbol(lhs);
        int idRHS1 = getIDFromSymbol(rhs1);
        int idRHS2 = getIDFromSymbol(rhs2);

        if (binaryRuleMap.containsKey(idLHS)) {
            Map<Integer, Set<Integer>> rhsSet = binaryRuleMap.get(idLHS);
            if (rhsSet.containsKey(idRHS1)) {
                Set<Integer> rhs2Set = rhsSet.get(idRHS1);
                if (rhs2Set.contains(idRHS2)) {
                    if (!allowOverride) {
                        throw new Exception("A rule was asserted twice, an error in your grammar generation!"
                                + lhs
                                + " " + rhs1 + " " + rhs2);
                    }
                } else {
//                    countBinaryRules++;
                    rhs2Set.add(idRHS2);
                }
            } else {
                Set<Integer> rhs2Set = ConcurrentHashMap.newKeySet();
                //     countBinaryRules++;
                rhs2Set.add(idRHS2);
                rhsSet.put(idRHS1, rhs2Set);
            }
        } else {
            Map<Integer, Set<Integer>> binrayRuleRHS = new ConcurrentHashMap<>();
            Set<Integer> rhs2Set = ConcurrentHashMap.newKeySet();
//            Map<String, Set<String>> binrayRuleRHS = new HashMap<>();
//            Set<String> rhs2Set = new HashSet<>();
            //  countBinaryRules++;
            rhs2Set.add(idRHS2);
            binrayRuleRHS.put(idRHS1, rhs2Set);
            binaryRuleMap.put(idLHS, binrayRuleRHS);
        }
    }

    public synchronized void addRule(String lhs, String rhs1) throws Exception {
        addRule(lhs, rhs1, false);
    }

    /**
     * Main method to add unary rules
     *
     * @param lhs
     * @param rhs1
     * @param allowOverride
     * @throws Exception
     */
    public synchronized void addRule(String lhs, String rhs1, boolean allowOverride) throws Exception {
        int idLHS = getIDFromSymbol(lhs);
        int idRHS1 = getIDFromSymbol(rhs1);
        if (unaryRuleMap.containsKey(idLHS)) {
            Set<Integer> rhsSet = unaryRuleMap.get(idLHS);
            if (rhsSet.contains(idRHS1)) {
                if (!allowOverride) {
                    throw new Exception("A rule was asserted twice, an error in your grammar generation! " + lhs + " -> " + rhs1);
                }
            } else {
//                countUnaryRules++;
                rhsSet.add(idRHS1);
            }

        } else {
            Set<Integer> rhsSet = ConcurrentHashMap.newKeySet();
            rhsSet.add(idRHS1);
            //  countUnaryRules++;
            Set<Integer> putIfAbsent = unaryRuleMap.putIfAbsent(idLHS, rhsSet);
            if (putIfAbsent != null) {
                putIfAbsent.add(idRHS1);
            }
        }
    }

    public synchronized int getCountBinaryRules() {
        Collection<Map<Integer, Map<Integer, Double>>> valueCollection = this.reverseBinaryRuleMap.values();
        int count = 0;
        for (Map<Integer, Map<Integer, Double>> tt : valueCollection) {
            Collection<Map<Integer, Double>> valueCollection1 = tt.values();
            for (Map<Integer, Double> ti : valueCollection1) {
                count += ti.size();
            }
        }
        //  return countBinaryRules;
        return count;
    }

    public synchronized int getCountUnaryRules() {

        Collection<Map<Integer, Double>> valueCollection = reversUnaryRuleMap.values();
        int count = 0;
        for (Map<Integer, Double> tihmap : valueCollection) {
            count += tihmap.size();
        }
        //return countUnaryRules;
        return count;
    }

    public synchronized void buildReverseRandomIndices() throws Exception {
//        this.startSymbolID = this.getIDFromSymbol(SymbolFractory.getStartSymbol());
        buildUnaryReverseMap(1.0);
        buildBinaryReverseMap(1.0);

    }

    public synchronized void buildReverseRandomIndicesEqual() throws Exception {
//        this.startSymbolID = this.getIDFromSymbol(SymbolFractory.getStartSymbol());
        buildUnaryReverseMapEqual(1.0);
        buildBinaryReverseMapEqual(1.0);

    }

    public synchronized void buildReverseRandomIndices(double sumTo) throws Exception {
//        this.startSymbolID = this.getIDFromSymbol(SymbolFractory.getStartSymbol());
        buildUnaryReverseMap(sumTo);
        buildBinaryReverseMap(sumTo);

    }

    private synchronized void buildBinaryReverseMap(double sumTo) throws Exception {
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
//        TIntObjectIterator<TIntObjectHashMap<TIntHashSet>> itBinary = this.binaryRuleMap.iterator();
//        while (itBinary.hasNext()) {
//            itBinary.advance();
        for (Entry<Integer, Map<Integer, Set<Integer>>> e : binaryRuleMap.entrySet()) {
            int keyLHS = e.getKey();
            Map<Integer, Set<Integer>> binaryRHSForThisKey = e.getValue();
            int ruleNumbersForLHS = binaryRHSForThisKey.size();
            double[] randSum = util.RndWeightsVectors.randSum(ruleNumbersForLHS, sumTo, PROB_POW_ONE);
            Set<Entry<Integer, Set<Integer>>> iterator = binaryRHSForThisKey.entrySet();
            int count = 0;
//            while (iterator.hasNext()) {
//                iterator.advance();
            for (Entry<Integer, Set<Integer>> e2 : iterator) {
                int firstRHS = e2.getKey();
                Set<Integer> secondRHSSet = e2.getValue();
                int sizeSeond = secondRHSSet.size();

                double[] randSumRHS2 = util.RndWeightsVectors.randSum(sizeSeond, randSum[count++], INNER_PROB_POWER);

                int countInnerRHS = 0;
                Iterator<Integer> iteratorSeondRHS = secondRHSSet.iterator();
                while (iteratorSeondRHS.hasNext()) {
                    int secondRHSElemntForLHS = iteratorSeondRHS.next();
                    Double log = FastMath.log(randSumRHS2[countInnerRHS++]);
                    addReverseBinaryRule(keyLHS, firstRHS, secondRHSElemntForLHS, log);
                }
            }
        }

    }

    private synchronized void buildBinaryReverseMapEqual(double sumTo) throws Exception {
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();
        for (Entry<Integer, Map<Integer, Set<Integer>>> e : binaryRuleMap.entrySet()) {
            int keyLHS = e.getKey();
            Map<Integer, Set<Integer>> binaryRHSForThisKey = e.getValue();
            int ruleNumbersForLHS = binaryRHSForThisKey.size();
            double randSum = sumTo / ruleNumbersForLHS;// util.RndWeightsVectors.randSum(ruleNumbersForLHS, sumTo, PROB_POW_ONE);
            Set<Entry<Integer, Set<Integer>>> iterator = binaryRHSForThisKey.entrySet();

            for (Entry<Integer, Set<Integer>> e2 : iterator) {
                int firstRHS = e2.getKey();
                Set<Integer> secondRHSSet = e2.getValue();
                int sizeSeond = secondRHSSet.size();

                double randSumRHS2 = randSum / sizeSeond;//

                Iterator<Integer> iteratorSeondRHS = secondRHSSet.iterator();
                while (iteratorSeondRHS.hasNext()) {
                    int secondRHSElemntForLHS = iteratorSeondRHS.next();
                    Double log = FastMath.log(randSumRHS2);
                    addReverseBinaryRule(keyLHS, firstRHS, secondRHSElemntForLHS, log);
                }
            }
        }

    }

    public synchronized void updateParameters(RulesCounts rc) throws Exception {

        Set<Integer> indexesUSed = new HashSet<>();
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();

        for (int lhs : rc.getOwUnaryRules().keySet()) {
            indexesUSed.add(lhs);
            Map<Integer, DoubleAccumulator> rhsSet = rc.getOwUnaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();
            for (DoubleAccumulator d : rhsSet.values()) {
                sumRhs.accumulate(d.doubleValue());
            }
            for (int keyRhs : rhsSet.keySet()) {
                indexesUSed.add(keyRhs);
                addReverseUnaryRule(lhs, keyRhs, (rhsSet.get(keyRhs).doubleValue() - sumRhs.doubleValue()));
            }
        }

        for (int lhs : rc.getOwBinaryRules().keySet()) {
            indexesUSed.add(lhs);
            Map<Integer, Map<Integer, DoubleAccumulator>> rhsSet = rc.getOwBinaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();
            for (int keyRhs1 : rhsSet.keySet()) {

                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    sumRhs.accumulate(rhs2Set.get(keyRhs2).doubleValue());
                }
            }
            for (int keyRhs1 : rhsSet.keySet()) {
                indexesUSed.add(keyRhs1);
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    indexesUSed.add(keyRhs2);
                    addReverseBinaryRule(lhs, keyRhs1, keyRhs2, (rhs2Set.get(keyRhs2).doubleValue() - sumRhs.doubleValue()));
                }
            }
        }

        updateSymbolMapsByRemoveUnused(indexesUSed);

    }

    public synchronized void filterRulesEqualWeight(RulesCounts rc) throws Exception {

        Set<Integer> indexesUSed = new HashSet<>();
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();

        for (int lhs : rc.getOwUnaryRules().keySet()) {
            indexesUSed.add(lhs);
            Map<Integer, DoubleAccumulator> rhsSet = rc.getOwUnaryRules().get(lhs);
            for (int keyRhs : rhsSet.keySet()) {
                indexesUSed.add(keyRhs);
                addReverseUnaryRule(lhs, keyRhs, Math.log(.1));
            }
        }

        for (int lhs : rc.getOwBinaryRules().keySet()) {
            indexesUSed.add(lhs);
            Map<Integer, Map<Integer, DoubleAccumulator>> rhsSet = rc.getOwBinaryRules().get(lhs);
            for (int keyRhs1 : rhsSet.keySet()) {
                indexesUSed.add(keyRhs1);
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    indexesUSed.add(keyRhs2);
                    addReverseBinaryRule(lhs, keyRhs1, keyRhs2, Math.log(.1));//(rhs2Set.get(keyRhs2).doubleValue() - sumRhs.doubleValue()));
                }
            }
        }

        updateSymbolMapsByRemoveUnused(indexesUSed);

    }

    public synchronized void updateParameters(RulesCounts rc, double minWeight) throws Exception {
        //ino baraks bayad benevisi ta harchi ke inja nist seffr beshe haminto zarb maghdir e avaliye yadet nara
        // rooye set mojood bayad iterate koni! na anche ke miayad ta betooni zero prob ro bardai
        // hamintor ezafe kardan e multithread
        Set<Integer> indexesUSed = new HashSet<>();
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        this.reverseBinaryRuleMap = new ConcurrentHashMap<>();

        for (int lhs : rc.getOwUnaryRules().keySet()) {

            Map<Integer, DoubleAccumulator> rhsSet = rc.getOwUnaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();
            for (DoubleAccumulator d : rhsSet.values()) {
                sumRhs.accumulate(d.doubleValue());
            }
            for (int keyRhs : rhsSet.keySet()) {
                indexesUSed.add(lhs); // since we are not sure the lhs will be added
                indexesUSed.add(keyRhs);
                double w = (rhsSet.get(keyRhs).doubleValue() - sumRhs.doubleValue());
                if (w > minWeight) {
                    addReverseUnaryRule(lhs, keyRhs, w);
                }
            }
        }

        for (int lhs : rc.getOwBinaryRules().keySet()) {
            Map<Integer, Map<Integer, DoubleAccumulator>> rhsSet = rc.getOwBinaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();
            for (int keyRhs1 : rhsSet.keySet()) {
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    sumRhs.accumulate(rhs2Set.get(keyRhs2).doubleValue());
                }
            }
            for (int keyRhs1 : rhsSet.keySet()) {
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {

                    double w = (rhs2Set.get(keyRhs2).doubleValue() - sumRhs.doubleValue());
                    if (w > minWeight) {
                        addReverseBinaryRule(lhs, keyRhs1, keyRhs2, w);
                        indexesUSed.add(lhs); // since we are not sure the lhs will be added
                        indexesUSed.add(keyRhs1);
                        indexesUSed.add(keyRhs2);

                    }
                }
            }
        }
        updateSymbolMapsByRemoveUnused(indexesUSed);
    }

    /**
     * Build the reverse map and assign the parameters randomly
     *
     * @throws Exception
     */
    private synchronized void buildUnaryReverseMap(double sumTo) throws Exception {
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        Set<Entry<Integer, Set<Integer>>> itUnary = this.unaryRuleMap.entrySet();
//        while (itUnary.hasNext()) {
//            itUnary.advance();
        for (Entry<Integer, Set<Integer>> e : itUnary) {
            int keyLHS = e.getKey();
            Set<Integer> unaryRHSForThisKey = e.getValue();
            int ruleNumbersForLHS = unaryRHSForThisKey.size();
            double[] randSum = util.RndWeightsVectors.randSum(ruleNumbersForLHS, sumTo, PROB_POW_ONE);
            Iterator<Integer> iterator = unaryRHSForThisKey.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                int nextRhs = iterator.next();
                addReverseUnaryRule(keyLHS, nextRhs, FastMath.log(randSum[count++]));
            }
        }
    }

    private synchronized void buildUnaryReverseMapEqual(double sumTo) throws Exception {
        this.reversUnaryRuleMap = new ConcurrentHashMap<>();
        Set<Entry<Integer, Set<Integer>>> itUnary = this.unaryRuleMap.entrySet();

        for (Entry<Integer, Set<Integer>> e : itUnary) {
            int keyLHS = e.getKey();
            Set<Integer> unaryRHSForThisKey = e.getValue();
            int ruleNumbersForLHS = unaryRHSForThisKey.size();
            double randSum = sumTo / ruleNumbersForLHS; ///util.RndWeightsVectors.randSum(ruleNumbersForLHS, sumTo, PROB_POW_ONE);
            Iterator<Integer> iterator = unaryRHSForThisKey.iterator();

            while (iterator.hasNext()) {
                int nextRhs = iterator.next();
                addReverseUnaryRule(keyLHS, nextRhs, FastMath.log(randSum));
            }
        }
    }

    public synchronized void addReverseBinaryRule(int keyLHS, int firstRHS, int secondRHSElemntForLHS, double param) throws Exception {
        addReverseBinaryRule(keyLHS, firstRHS, secondRHSElemntForLHS, param, false);
    }

    public synchronized void addReverseBinaryRule(int keyLHS, int firstRHS, int secondRHSElemntForLHS, double param, boolean allowOverride) throws Exception {
        if (reverseBinaryRuleMap.containsKey(firstRHS)) {
            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = reverseBinaryRuleMap.get(firstRHS);
            if (secondRHSToLHSWeightMap.containsKey(secondRHSElemntForLHS)) {
                Map<Integer, Double> lhsWeightMap = secondRHSToLHSWeightMap.get(secondRHSElemntForLHS);
                if (!allowOverride) {
                    if (lhsWeightMap.containsKey(keyLHS)) {
                        throw new Exception("this must not happen " + this.symbolInverseMap.get(keyLHS) + " -> "
                                + this.symbolInverseMap.get(firstRHS) + " " + this.symbolInverseMap.get(secondRHSElemntForLHS)
                        );
                    }
                }
                //System.out.println(randSumRHS2[countInnerRHS]);
                lhsWeightMap.put(keyLHS, param);

            } else {

                Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
                lhsWeightMap.put(keyLHS, param);
                secondRHSToLHSWeightMap.put(secondRHSElemntForLHS, lhsWeightMap);

            }
        } else {

            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = new ConcurrentHashMap<>();
            Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
            lhsWeightMap.put(keyLHS, param);
            secondRHSToLHSWeightMap.put(secondRHSElemntForLHS, lhsWeightMap);
            reverseBinaryRuleMap.put(firstRHS, secondRHSToLHSWeightMap);

        }
    }

    public synchronized void addReverseUnaryRule(int keyLHS, int nextRhs, double param) throws Exception {
        addReverseUnaryRule(keyLHS, nextRhs, param, false);
    }

    public synchronized void addReverseUnaryRule(int keyLHS, int nextRhs, double param, boolean overrideAllowed) throws Exception {
        if (reversUnaryRuleMap.containsKey(nextRhs)) {
            if (!overrideAllowed) {
                if (reversUnaryRuleMap.get(nextRhs).containsKey(keyLHS)) {
                    Double get = reversUnaryRuleMap.get(nextRhs).get(keyLHS);

                    if (Double.compare(get, param) != 0) {
                        throw new Exception("this should not happen in reverese appending of rules! new and old param " + param + "  vs " + get);
                    } else {
                        LOGGER.log(Level.WARNING, "WARNING: dependance conditions are violated somehow");
                    }

                }
            }
            reversUnaryRuleMap.get(nextRhs).put(keyLHS, param);
        } else {
            Map<Integer, Double> ruleWeights = new ConcurrentHashMap<>();
            ruleWeights.put(keyLHS, param);
            reversUnaryRuleMap.put(nextRhs, ruleWeights);
        }
    }

    // to add: methods for getting weights from the reverse grammar
    // to add serialization in case we are dealing with very very large text corpora
    public synchronized void seralizaRules(String fileName) throws FileNotFoundException, IOException {
        File f = new File(fileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f), Charset.forName("UTF-8"));
        ZipEntry eUnary = new ZipEntry(UNARY_RULE_FILE_NAME);
        out.putNextEntry(eUnary);
        this.serializeUnaryRules(out);
        out.closeEntry();
        ZipEntry bUnary = new ZipEntry(BINARY_RULE_FILE_NAME);
        out.putNextEntry(bUnary);
        this.serializeBinaryRules(out);
        out.closeEntry();
        out.close();
    }

    public synchronized void seralizaRules(ZipOutputStream out) throws FileNotFoundException, IOException {

        ZipEntry eUnary = new ZipEntry(UNARY_RULE_FILE_NAME);
        out.putNextEntry(eUnary);
        this.serializeUnaryRules(out);
        out.closeEntry();
        ZipEntry bUnary = new ZipEntry(BINARY_RULE_FILE_NAME);
        out.putNextEntry(bUnary);
        this.serializeBinaryRules(out);
        out.closeEntry();

    }

    private synchronized void serializeUnaryRules(OutputStream out) throws IOException {
        int count = 0;

        Set<Entry<Integer, Map<Integer, Double>>> iterator = this.reversUnaryRuleMap.entrySet();
        for (Entry<Integer, Map<Integer, Double>> e : iterator) {
            int rhsKey = e.getKey();
            Map<Integer, Double> lhsWeightMap = e.getValue();
            Set<Entry<Integer, Double>> iteratorLHSWeightMap = lhsWeightMap.entrySet();
            for (Entry<Integer, Double> e2 : iteratorLHSWeightMap) {
                int lhsKey = e2.getKey();
                double weight = e2.getValue();
                String ruleEntry = this.symbolInverseMap.get(lhsKey) + LHS_RHS_DELIM + this.symbolInverseMap.get(rhsKey) + WEIGHT_DELIM + weight + "\n";
                out.write(ruleEntry.getBytes());
                count++;
            }
        }
        LOGGER.log(Level.INFO, "#Serialized unary rules of size {0}", count);

    }

    private synchronized void serializeBinaryRules(ZipOutputStream out) throws IOException {
        int count = 0;
        Set<Entry<Integer, Map<Integer, Map<Integer, Double>>>> iterator = this.reverseBinaryRuleMap.entrySet();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : iterator) {
            int rhsKey1 = e.getKey();
            Map<Integer, Map<Integer, Double>> rhs2LhsMap = e.getValue();
            Set<Entry<Integer, Map<Integer, Double>>> iteratorRHSLHSWeightMap = rhs2LhsMap.entrySet();
            for (Entry<Integer, Map<Integer, Double>> e2 : iteratorRHSLHSWeightMap) {
                int rhs2Key = e2.getKey();
                Map<Integer, Double> lhsWeightMap = e2.getValue();
                Set<Entry<Integer, Double>> iteratorLHSMap = lhsWeightMap.entrySet();
                for (Entry<Integer, Double> e3 : iteratorLHSMap) {
                    int lhs = e3.getKey();
                    double weight = e3.getValue();
                    String ruleEntry = this.symbolInverseMap.get(lhs) + LHS_RHS_DELIM
                            + this.symbolInverseMap.get(rhsKey1) + RHS_DELIM
                            + this.symbolInverseMap.get(rhs2Key) + WEIGHT_DELIM + weight + "\n";
                    out.write(ruleEntry.getBytes());
                    count++;
                }
            }

        }
        LOGGER.log(Level.INFO, "#Serialized binary rules of size {0}", count);
        //System.out.println("#Serialized binary rules of size " + this.countBinaryRules);
    }

    public synchronized void pruneRules(double min) {
        pruneBinaryRules(min);
        pruneUnaryRules(min);
    }

    private synchronized void pruneBinaryRules(double min) {
        int count = 0;
        Set<Entry<Integer, Map<Integer, Map<Integer, Double>>>> iterator = this.reverseBinaryRuleMap.entrySet();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : iterator) {
            //int rhsKey1 = e.getKey();
            Map<Integer, Map<Integer, Double>> rhs2LhsMap = e.getValue();
            Set<Entry<Integer, Map<Integer, Double>>> iteratorRHSLHSWeightMap = rhs2LhsMap.entrySet();
            for (Entry<Integer, Map<Integer, Double>> e2 : iteratorRHSLHSWeightMap) {
                //int rhs2Key = e2.getKey();
                Map<Integer, Double> lhsWeightMap = e2.getValue();
                Set<Entry<Integer, Double>> iteratorLHSMap = lhsWeightMap.entrySet();
                Iterator<Entry<Integer, Double>> iterator1 = iteratorLHSMap.iterator();
                while (iterator1.hasNext()) {
                    Entry<Integer, Double> e3 = iterator1.next();
                    //for (Entry<Integer, Double> e3 : iteratorLHSMap) {
                    //int lhs = e3.getKey();
                    double weight = e3.getValue();
                    if (!Double.isFinite(weight)) {
//                        String ruleEntry = this.symbolInverseMap.get(lhs) + LHS_RHS_DELIM
//                                + this.symbolInverseMap.get(rhsKey1) + RHS_DELIM
//                                + this.symbolInverseMap.get(rhs2Key) + WEIGHT_DELIM + weight + "\n";
//                        System.err.println(ruleEntry);
//                        e3.setValue(-Double.MAX_VALUE);
                        iterator1.remove();
                        count++;
                    } else if (weight < min) {
//                        String ruleEntry = this.symbolInverseMap.get(lhs) + LHS_RHS_DELIM
//                                + this.symbolInverseMap.get(rhsKey1) + RHS_DELIM
//                                + this.symbolInverseMap.get(rhs2Key) + WEIGHT_DELIM + weight + "\n";
                        // System.err.println(ruleEntry);
                        // must change this to iterator in order to remove it
                        // e3.setValue(-Double.MAX_VALUE);
                        iterator1.remove();
                        count++;
                    }
                    //}
                }
            }

        }
        LOGGER.log(Level.INFO, "#Serialized binary rules of size {0}", count);
        //System.out.println("#Serialized binary rules of size " + this.countBinaryRules);
    }

    private synchronized void pruneUnaryRules(double min) {
        int count = 0;

        Set<Entry<Integer, Map<Integer, Double>>> iterator = this.reversUnaryRuleMap.entrySet();
        for (Entry<Integer, Map<Integer, Double>> e : iterator) {
            Map<Integer, Double> lhsWeightMap = e.getValue();
            Set<Entry<Integer, Double>> iteratorLHSWeightMap = lhsWeightMap.entrySet();
            Iterator<Entry<Integer, Double>> iterator1 = iteratorLHSWeightMap.iterator();
            while (iterator1.hasNext()) {
                Entry<Integer, Double> e2 = iterator1.next();
                double weight = e2.getValue();

                if (!Double.isFinite(weight)) {
                    iterator1.remove();
                    count++;
                } else if (weight < min) {
                    iterator1.remove();
                    count++;
                }
                count++;
            }
        }
        LOGGER.log(Level.INFO, "#Serialized unary rules of size {0}", count);

    }

    public synchronized Map<Integer, Map<Integer, Map<Integer, Double>>> getReverseBinaryRuleMap() {
        return Collections.unmodifiableMap(reverseBinaryRuleMap);
    }

    public synchronized Map<Integer, Double> getLhsToUnary(int rhs) throws Exception {
        if (!this.reversUnaryRuleMap.containsKey(rhs)) {
            LOGGER.log(Level.WARNING, "RHS not found in RM: " + rhs + ":" + getSymbolFromID(rhs));

            ConcurrentHashMap<Integer, Double> chm = new ConcurrentHashMap<>();
            this.reversUnaryRuleMap.putIfAbsent(rhs, chm);
        }
        return this.reversUnaryRuleMap.get(rhs);

    }

    /**
     * Return the map if such production exists, otherwise return null
     *
     * @param rhs1
     * @param rhs2
     * @return
     */
    public synchronized Map<Integer, Double> getLhsToBinary(int rhs1, int rhs2) {
        //System.err.println(rhs1 + "* " + rhs2);
        Map<Integer, Map<Integer, Double>> get = this.reverseBinaryRuleMap.get(rhs1);
        if (get != null) {
            Map<Integer, Double> get2 = get.get(rhs2);
            return get2;
        } else {
            return null;
        }
    }

    public synchronized Double getBinaryRuleParam(int lhs, int rhs1, int rhs2) {

        Map<Integer, Map<Integer, Double>> get = this.reverseBinaryRuleMap.get(rhs1);
        if (get != null) {
            Map<Integer, Double> get2 = get.get(rhs2);
            if (get2 == null) {
                return null;
            }
            if (get2.containsKey(lhs)) {
                double param = get2.get(lhs);
                return param;
            }
        }
        return null;
    }

    public synchronized Double getBinaryRuleParam(String lhs, String rhs1, String rhs2) throws Exception {

        Map<Integer, Map<Integer, Double>> get = this.reverseBinaryRuleMap.get(getIDFromSymbol(rhs1));
        if (get != null) {
            Map<Integer, Double> get2 = get.get(getIDFromSymbol(rhs2));
            if (get2.containsKey(getIDFromSymbol(lhs))) {
                double param = get2.get(getIDFromSymbol(lhs));
                return param;
            }
        }
        return null;
    }

    public synchronized Double getUnaryRuleParam(int lhs, int rhs1) {

        Map<Integer, Double> rhsToLhsParamMap = this.reversUnaryRuleMap.get(rhs1);
        if (rhsToLhsParamMap != null) {
            if (rhsToLhsParamMap.containsKey(lhs)) {
                double param = rhsToLhsParamMap.get(lhs);
                return param;

            }
        }

        return null;
    }

    public synchronized Double getSumParamForUnaryRule(int lhs) {

        double sum = -Double.MAX_VALUE;
        Set<Integer> rhsSet = this.reversUnaryRuleMap.keySet();
        for (int rhs : rhsSet) {
            Map<Integer, Double> rhsToLhsParamMap = this.reversUnaryRuleMap.get(rhs);

            if (rhsToLhsParamMap.containsKey(lhs)) {
                double param = rhsToLhsParamMap.get(lhs);
                sum = MathUtil.logSumExp(param, sum);

            }

        }
        return sum;
    }

    public synchronized Double getSumParamForLHS(String lhsStringSymbol) throws Exception {
        int lhs = this.getIDFromSymbol(lhsStringSymbol);
        return MathUtil.logSumExp(getSumParamForBinaryRule(lhs), getSumParamForUnaryRule(lhs));
    }

    public synchronized Double getSumParamForLHS(int lhs) {
        return MathUtil.logSumExp(getSumParamForBinaryRule(lhs), getSumParamForUnaryRule(lhs));
    }

    public synchronized Double getSumParamForBinaryRule(int lhs) {

        double sum = -Double.MAX_VALUE;
        Set<Integer> rhsSet1 = this.reverseBinaryRuleMap.keySet();
        for (int rhs : rhsSet1) {
            Map<Integer, Map<Integer, Double>> rhsToLhsParamMap = this.reverseBinaryRuleMap.get(rhs);
            Set<Integer> keySetRHS2 = rhsToLhsParamMap.keySet();
            for (int rhs2 : keySetRHS2) {
                Map<Integer, Double> lhsSet = rhsToLhsParamMap.get(rhs2);
                if (lhsSet.containsKey(lhs)) {
                    double param = lhsSet.get(lhs);
                    sum = MathUtil.logSumExp(param, sum);

                }
            }
        }
        return sum;
    }

    public int getStartSymbolID() throws Exception {
        return getIDFromSymbol(SymbolFractory.getStartSymbol());
    }

    public synchronized Map<Integer, Map<Integer, Set<Integer>>> getBinaryRuleMap() {
        return Collections.unmodifiableMap(binaryRuleMap);
    }

    public synchronized Map<Integer, Map<Integer, Double>> getReversUnaryRuleMap() {
        return Collections.unmodifiableMap(reversUnaryRuleMap);
    }

    public synchronized Map<String, Integer> getSymbolToIDMap() {
        return Collections.unmodifiableMap(symbolToIDMap);
    }

    public Map<Integer, String> getSymbolInverseMap() {
        return Collections.unmodifiableMap(symbolInverseMap);
    }

    public synchronized Map<Integer, Set<Integer>> getUnaryRuleMap() {
        return Collections.unmodifiableMap(unaryRuleMap);
    }

    public synchronized void updateParametersPartial(RulesCounts rc, double eta) throws Exception {

//        assert (alpha >= .5
//                && alpha <= 1);
        //double alphaInterpolWeight = Math.pow(2 + iteration, -alpha);
        for (int lhs : rc.getOwUnaryRules().keySet()) {
            Map<Integer, DoubleAccumulator> rhsSet = rc.getOwUnaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();

            for (DoubleAccumulator d : rhsSet.values()) {
                sumRhs.accumulate(d.doubleValue());
            }
            for (int keyRhs : rhsSet.keySet()) {
                updateReverseUnaryRulePartialWithRate(lhs, keyRhs, (rhsSet.get(keyRhs).doubleValue() - sumRhs.doubleValue()), eta);

            }
        }

        for (int lhs : rc.getOwBinaryRules().keySet()) {
            Map<Integer, Map<Integer, DoubleAccumulator>> rhsSet = rc.getOwBinaryRules().get(lhs);
            DoubleAccumulator sumRhs = MathUtil.getDoubleLogAcc();
            for (int keyRhs1 : rhsSet.keySet()) {
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    sumRhs.accumulate(rhs2Set.get(keyRhs2).doubleValue());
                }
            }
            for (int keyRhs1 : rhsSet.keySet()) {
                Map<Integer, DoubleAccumulator> rhs2Set = rhsSet.get(keyRhs1);
                for (int keyRhs2 : rhs2Set.keySet()) {
                    double wp = (rhs2Set.get(keyRhs2).doubleValue() - sumRhs.doubleValue());
                    updateReverseBinaryRulePartial(lhs, keyRhs1, keyRhs2, wp, eta);
                }
            }
        }

    }

    public synchronized void updateReverseBinaryRulePartial(int keyLHS, int firstRHS, int secondRHSElemntForLHS, double param, double rate) throws Exception {
        if (reverseBinaryRuleMap.containsKey(firstRHS)) {

            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = reverseBinaryRuleMap.get(firstRHS);
            if (secondRHSToLHSWeightMap.containsKey(secondRHSElemntForLHS)) {
                Map<Integer, Double> lhsWeightMap = secondRHSToLHSWeightMap.get(secondRHSElemntForLHS);
                if (lhsWeightMap.containsKey(keyLHS)) {
                    Double get = lhsWeightMap.get(keyLHS);
                    // double newWeight = get * (1 - rate) + param * rate;
                    double newWeight = MathUtil.logSumExp(get + FastMath.log(1 - rate), param + FastMath.log(rate));
                    ///
                    if (!Double.isFinite(newWeight)) {

                        LOGGER.log(Level.FINE, "Code 83y67: Infinite weight when interpolating... : "
                                + get + " and "
                                + FastMath.log(1 - rate) + " with" + param + " and " + FastMath.log(rate)
                                + " " + this.getSymbolFromID(keyLHS) + " ->" + this.getSymbolFromID(firstRHS)
                                + " " + this.getSymbolFromID(secondRHSElemntForLHS));

                        if (Double.isFinite(param)) {
                            newWeight = param;
                        } else if (Double.isFinite(get)) {
                            newWeight = get;
                        } else {
                            LOGGER.log(Level.SEVERE, "erorr related to rule params code 425652");

                            newWeight = //0;
                                            //
Math.log(.0000000000001);
                        }
                    }
                    ///
                    lhsWeightMap.put(keyLHS, newWeight);
                } else {
                    lhsWeightMap.put(keyLHS, param);
                }
            } else {
                Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
                lhsWeightMap.put(keyLHS, param + FastMath.log(rate)
                );
                Map<Integer, Double> putIfAbsent = secondRHSToLHSWeightMap.putIfAbsent(secondRHSElemntForLHS, lhsWeightMap);
                // I am not sure of the below check it out later
                if (putIfAbsent != null) {
                    throw new RuntimeException("unseen situation PARAM RM code:83627");
//                    Double get = putIfAbsent.get(keyLHS);
//                    if(get!=null){
//                         putIfAbsent.put(keyLHS,MathUtil.logSumExp(param + FastMath.log(rate),get+FastMath.log(1-rate)));
//                    }else{
//                        putIfAbsent.put(keyLHS,param);
//                    }
                }

            }
        } else {
            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = new ConcurrentHashMap<>();
            Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
            Double putIfAbsent = lhsWeightMap.putIfAbsent(keyLHS, param + FastMath.log(rate));
            if (putIfAbsent != null) {
                LOGGER.log(Level.SEVERE,
                        "*********** ++++++++++ *********** RM: Unseen situation in threads!!!");
            }
            Map<Integer, Double> putIfAbsent1 = secondRHSToLHSWeightMap.putIfAbsent(secondRHSElemntForLHS, lhsWeightMap);
            if (putIfAbsent1 != null) {
                LOGGER.log(Level.SEVERE, "*********** ++++++++++ *********** RM: Unseen situation in threads!!! (2)");
            }
            Map<Integer, Map<Integer, Double>> putIfAbsent2 = reverseBinaryRuleMap.putIfAbsent(firstRHS, secondRHSToLHSWeightMap);
            if (putIfAbsent2 != null) {
                LOGGER.log(Level.SEVERE, "*********** ++++++++++ ***********  RM: Unseen situation in threads!!! (3)");
            }
        }
    }

    public synchronized void updateReverseUnaryRulePartialWithRate(int keyLHS,
            int nextRhs, double param, double rate) throws Exception {
        //assert (rate > 0 && rate < 1);
        if (!Double.isFinite(param)) {
            LOGGER.log(Level.SEVERE, "ERROR PARAM update UNARY --- code:36252");

            //return; // change back -- known good with this strategy
            param=0; // instead of zero --> a very small number 
        }
        if (reversUnaryRuleMap.containsKey(nextRhs)) {
            Map<Integer, Double> get = reversUnaryRuleMap.get(nextRhs);
            if (get.containsKey(keyLHS)) {
                Double get1 = get.get(keyLHS);
                double newWeight = MathUtil.logSumExp(get1 + FastMath.log(1 - rate), param + FastMath.log(rate));
                //////////////////////////////////////// BQ look
                if (!Double.isFinite(newWeight)) {
                    LOGGER.log(Level.SEVERE, "Code 83y665: Infinite weight when interpolating... : "
                            + get1 + " and "
                            + FastMath.log(1 - rate) + " with " + param + " and " + FastMath.log(rate)
                            + " symbols are " + this.getSymbolFromID(keyLHS) + " " + this.getSymbolFromID(nextRhs));

                    if (Double.isFinite(param)) {
                        newWeight = param;
                    } else if (Double.isFinite(get1)) {
                        newWeight = get1;
                    } else {
                        newWeight = 0;//Math.log(1);//Math.log(1e-17);
                    }
                }
                reversUnaryRuleMap.get(nextRhs).put(keyLHS, newWeight);
            } else {
                reversUnaryRuleMap.get(nextRhs).put(keyLHS, param + Math.log(rate)
                );
            }
        } else {
            //TIntDoubleHashMap ruleWeights = new TIntDoubleHashMap();
            Map<Integer, Double> ruleWeights = new ConcurrentHashMap<>();
            Double putIfAbsent = ruleWeights.putIfAbsent(keyLHS, param + Math.log(rate));
            if (putIfAbsent != null) {
                LOGGER.log(Level.SEVERE, "error in rulemap interpolate code:3872");
            }
            Map<Integer, Double> putIfAbsent1 = reversUnaryRuleMap.putIfAbsent(nextRhs, ruleWeights);
            if (putIfAbsent1 != null) {
                LOGGER.log(Level.SEVERE, "error in rulemap interpolate code:736742");
            }
        }
    }

    public Map<String, Integer> getFrameHeadSymbolIndexMap() {

        Map<String, Integer> frameHeadSymbols = new ConcurrentHashMap<>();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean frameHeadRule = SymbolFractory.isFrameHeadRule(symbol);
            if (frameHeadRule) {

                frameHeadSymbols.put(symbol, symbolToIDMap.get(symbol));
            }
        }
        return frameHeadSymbols;

    }

    public Set<Integer> getFrameHeadSymbolIDSet() throws Exception {

        Set< Integer> frameHeadSymbols = ConcurrentHashMap.newKeySet();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean frameHeadRule = SymbolFractory.isFrameHeadRule(symbol);
            if (frameHeadRule) {
                //int frameIndex = SymbolFractory.getFrameIndex(symbol);
                frameHeadSymbols.add(this.getIDFromSymbol(symbol));
            }
        }

        return frameHeadSymbols;

    }

    public int getFrameHeadSymboIDs() throws Exception {

        Set< Integer> all = getFrameIndexSet();
        Iterator<Integer> iterator = all.iterator();
        int next = iterator.next();
        while (iterator.hasNext()) {
            Integer next1 = iterator.next();
            if (next < next1) {
                next = next1;
            }
        }
        return next;

    }

    public Set<String> getSntxDepIDSet() throws Exception {

        Set<String> roleSymbolSet = ConcurrentHashMap.newKeySet();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean aRole = SymbolFractory.isUnaryDepHead(symbol);
            if (aRole) {
                //  SymbolFractory.getSymbolIndex(symbol);
                roleSymbolSet.add(symbol);
            }
        }
        return roleSymbolSet;

    }

    public Set<Integer> getSemanticRoleSymbolIDSet() throws Exception {

        Set<Integer> roleSymbolSet = ConcurrentHashMap.newKeySet();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean aRole = SymbolFractory.isASemanticRole(symbol);
            if (aRole) {
                // int symbolIndex = SymbolFractory.getSymbolIndex(symbol);
                roleSymbolSet.add(this.getIDFromSymbol(symbol));
            }
        }
        return roleSymbolSet;

    }

    public Set<Integer> getSemanticRoleIndexSet() throws Exception {

        Set<Integer> roleSymbolSet = ConcurrentHashMap.newKeySet();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean aRole = SymbolFractory.isASemanticRole(symbol);
            if (aRole) {
                int roleIndex = SymbolFractory.getSymbolIndex(symbol);
                roleSymbolSet.add(roleIndex);
            }
        }

        return roleSymbolSet;

    }

    public int getRolesLargestIndex() throws Exception {

        Set< Integer> all = getSemanticRoleIndexSet();
        Iterator<Integer> iterator = all.iterator();
        if (iterator.hasNext()) {
            int next = iterator.next();
            while (iterator.hasNext()) {
                Integer next1 = iterator.next();
                if (next < next1) {
                    next = next1;
                }
            }
            return next;
        } else {
            return 0;
        }

    }

    public synchronized int getLargestSpltDepSplitCounterID() throws Exception {

        Set<String> stxSymbolSet = this.getSntxDepIDSet();
        int maxCounterID = -1;
        for (String symboldIDStr : stxSymbolSet) {

            int depStxLHSSplitCounterID = SymbolFractory.getDepStxLHSSplitCounterID(symboldIDStr);
            maxCounterID = Math.max(maxCounterID, depStxLHSSplitCounterID);
        }

        return maxCounterID;

    }

    public int getNumberOfFramesVariable() throws Exception {
        return this.getFrameIndexSet().size();
    }

    public Set<Integer> getFrameIndexSet() throws Exception {

        Set<Integer> frameHeadSymbols = ConcurrentHashMap.newKeySet();
        for (String symbol : symbolToIDMap.keySet()) {
            boolean frameHeadRule = SymbolFractory.isFrameHeadRule(symbol);
            if (frameHeadRule) {
                int frameIndex = SymbolFractory.getFrameIndex(symbol);
                frameHeadSymbols.add(frameIndex);
            }
        }

        return frameHeadSymbols;

    }

    public Map<String, Integer> getActiveVocab() {
        return Collections.unmodifiableMap(symbolToIDMap);

    }

    public synchronized void setReversUnaryRuleMap(Map<Integer, Map<Integer, Double>> reversUnaryRuleMap) {
        this.reversUnaryRuleMap = reversUnaryRuleMap;
    }

    public synchronized void setReverseBinaryRuleMap(Map<Integer, Map<Integer, Map<Integer, Double>>> reverseBinaryRuleMap) {
        this.reverseBinaryRuleMap = reverseBinaryRuleMap;
    }

    public synchronized int getFrameHeadLargestIndex() throws Exception {
        Set<Integer> frameIndexSet = getFrameIndexSet();
        int largest = -1;
        for (Integer i : frameIndexSet) {
            largest = Math.max(largest, i);
        }
        return largest;
    }

    public Set<Integer> convertToSymbolID(Set<String> depSetSymbols) throws Exception {
        Set<Integer> intSet = new HashSet<>();
        for (String sym : depSetSymbols) {
            intSet.add(this.getIDFromSymbol(sym));
        }
        return intSet;
    }

    public Integer getMaxFrameHeadLexclztinSubstatIndices(int frameIndexID) {
        int max = -1;
        for (Integer i : getFrameHeadLexclztinSubstatIndices(frameIndexID)) {
            max = Math.max(max, i);
        }
        return max;
    }

    public Set<Integer> getFrameHeadLexclztinSubstatIndices(int frameIndexID) {
        Set<Integer> states = new HashSet<>();
        for (String symbol : getSubstatesOfFrameHeadLexicalization(frameIndexID)) {
            int subStateIndex = SymbolFractory.getSymbolLocacSubstate(symbol);
            states.add(subStateIndex);
        }
        return states;
    }

    public Set<String> getSubstatesOfFrameHeadLexicalization(int frameIndexID) {
        Set<String> substates = new HashSet<>();
        for (String symbol : this.symbolToIDMap.keySet()) {

            if (SymbolFractory.isLhsForUnaryHead(symbol, frameIndexID)) {
                substates.add(symbol);
            }
        }
        return substates;
    }

    private synchronized void updateSymbolMapsByRemoveUnused(Set<Integer> indexesUSed) {
        Iterator<Entry<Integer, String>> iterator = this.symbolInverseMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Integer, String> entry = iterator.next();
            Integer symboldID = entry.getKey();
            if (!indexesUSed.contains(symboldID)) {
                iterator.remove();
                this.symbolToIDMap.remove(entry.getValue());
            }

        }
    }

    public synchronized void removeRulesRelatedTo(Set<String> symbolID) throws Exception {
        Set<Integer> intSet = new HashSet<>();
        for (String s : symbolID) {
            int tgtIDToRemove = getIDFromSymbol(s);
            intSet.add(tgtIDToRemove);
            removeSympolIDMapping(tgtIDToRemove);

        }
        removeFromBinaryReverse(intSet);
        removeFromUnaryReverse(intSet);

    }

    /**
     * remove all the unaries involving the given symbolID
     *
     * @param symbolID
     */
    private synchronized void removeFromUnary(int symbolID) {
        Iterator<Integer> iterator = this.unaryRuleMap.keySet().iterator();
        while (iterator.hasNext()) {
            Integer nextLHS = iterator.next();
            if (nextLHS == symbolID) {
                iterator.remove();
            } else {
                Set<Integer> get = this.unaryRuleMap.get(nextLHS);
                Iterator<Integer> iteratorRHS = get.iterator();
                while (iteratorRHS.hasNext()) {
                    Integer symbolRHS = iteratorRHS.next();
                    if (symbolRHS == symbolID) {
                        iteratorRHS.remove();
                    }
                }

            }
        }
    }

    private synchronized void removeFromBinary(int symbolID) {
        Iterator<Integer> iteratorLHS = this.binaryRuleMap.keySet().iterator();
        while (iteratorLHS.hasNext()) {
            Integer nextLHS = iteratorLHS.next();
            if (nextLHS == symbolID) {
                iteratorLHS.remove();
            } else {
                Map<Integer, Set<Integer>> rhs1Set = this.binaryRuleMap.get(nextLHS);
                Iterator<Integer> iteratorRHS1 = rhs1Set.keySet().iterator();
                while (iteratorRHS1.hasNext()) {
                    Integer symbolRHS1 = iteratorRHS1.next();
                    if (symbolRHS1 == symbolID) {
                        iteratorRHS1.remove();
                    } else {
                        Set<Integer> rhs2 = rhs1Set.get(symbolRHS1);
                        Iterator<Integer> iteratorRHS2 = rhs2.iterator();
                        while (iteratorRHS2.hasNext()) {
                            Integer nextRhs2 = iteratorRHS2.next();
                            if (nextRhs2 == symbolID) {
                                iteratorRHS2.remove();
                            }
                        }
                    }
                }

            }
        }
    }

    private synchronized void removeFromUnaryReverse(Set<Integer> symbolID) {
        Iterator<Integer> iterator = this.reversUnaryRuleMap.keySet().iterator();
        while (iterator.hasNext()) {
            Integer nextRHS = iterator.next();
            if (symbolID.contains(nextRHS)) {
                iterator.remove();
            } else {
                Map<Integer, Double> get = this.reversUnaryRuleMap.get(nextRHS);
                Iterator<Integer> iteratorRHS = get.keySet().iterator();
                while (iteratorRHS.hasNext()) {
                    Integer lhsSymbol = iteratorRHS.next();
                    if (symbolID.contains(lhsSymbol)) {
                        iteratorRHS.remove();
                    }
                }

            }
        }
    }

    private synchronized void removeFromBinaryReverse(Set<Integer> symbolID) {
        Iterator<Integer> iteratorRHS1 = this.reverseBinaryRuleMap.keySet().iterator();
        while (iteratorRHS1.hasNext()) {
            Integer rhs1Sym = iteratorRHS1.next();
            if (symbolID.contains(rhs1Sym)) {
                iteratorRHS1.remove();
            } else {
                Map<Integer, Map<Integer, Double>> rhs1Set = this.reverseBinaryRuleMap.get(rhs1Sym);
                Iterator<Integer> iteratorRHS2 = rhs1Set.keySet().iterator();
                while (iteratorRHS2.hasNext()) {
                    Integer rhs2Symb = iteratorRHS2.next();
                    if (symbolID.contains(rhs2Symb)) {
                        iteratorRHS2.remove();
                    } else {
                        Map<Integer, Double> rhs2 = rhs1Set.get(rhs2Symb);
                        Iterator<Integer> iteratorLHS = rhs2.keySet().iterator();
                        while (iteratorLHS.hasNext()) {
                            Integer lhsSymbol = iteratorLHS.next();
                            if (symbolID.contains(lhsSymbol)) {
                                iteratorLHS.remove();
                            }
                        }
                    }
                }

            }
        }
    }

    public synchronized void addRawRuleMap(RuleMaps rawRuleMaps) throws Exception {
        verifySymbolCounterStatus();
        // add binaries
        Map<Integer, Map<Integer, Set<Integer>>> binaryRuleMapRawInput = rawRuleMaps.getBinaryRuleMap();
        for (Entry<Integer, Map<Integer, Set<Integer>>> e : binaryRuleMapRawInput.entrySet()) {
            String lhs = rawRuleMaps.getSymbolFromID(e.getKey());
            Map<Integer, Set<Integer>> valueRHS = e.getValue();
            for (int rhs1Key : valueRHS.keySet()) {
                String rhs1 = rawRuleMaps.getSymbolFromID(rhs1Key);
                Set<Integer> getRHS2KeySet = valueRHS.get(rhs1Key);
                for (int rhs2ID : getRHS2KeySet) {
                    String rhs2 = rawRuleMaps.getSymbolFromID(rhs2ID);
                    this.addRule(lhs, rhs1, rhs2, true);
                }
            }

        }
        //add unaries
        Map<Integer, Set<Integer>> rawRuleMapsUnary = rawRuleMaps.getUnaryRuleMap();
        for (int lhsKey : rawRuleMapsUnary.keySet()) {
            String lhs = rawRuleMaps.getSymbolFromID(lhsKey);
            for (int rhsKey : rawRuleMapsUnary.get(lhsKey)) {
                String rhs = rawRuleMaps.getSymbolFromID(rhsKey);
                this.addRule(lhs, rhs, true);
            }
        }

    }

    public synchronized void interpolateInverseRuleMaps(RuleMaps inputRuleMaps, double rate) throws Exception {
        verifySymbolCounterStatus();
        //add unaries
        Map<Integer, Map<Integer, Double>> rawRuleMapsUnary = inputRuleMaps.getReversUnaryRuleMap();
        for (int rhsKey : rawRuleMapsUnary.keySet()) {
            String rhs = inputRuleMaps.getSymbolFromID(rhsKey);
            Map<Integer, Double> getLHSParam = rawRuleMapsUnary.get(rhsKey);
            for (int lhsKey : getLHSParam.keySet()) {
                String lhs = inputRuleMaps.getSymbolFromID(lhsKey);
                Double param = getLHSParam.get(lhsKey);
                this.addRule(lhs, rhs, true); // to make sure ids are in place
                this.updateReverseUnaryRulePartialWithRate(
                        this.getIDFromSymbol(lhs),
                        this.getIDFromSymbol(rhs),
                        param, rate);
            }
        }

        // add binaries
        Map<Integer, Map<Integer, Map<Integer, Double>>> binaryRuleMapRawInput = inputRuleMaps.getReverseBinaryRuleMap();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : binaryRuleMapRawInput.entrySet()) {
            String rhs1 = inputRuleMaps.getSymbolFromID(e.getKey());
            Map<Integer, Map<Integer, Double>> valueRHS12LHS = e.getValue();
            for (int rhs2Key : valueRHS12LHS.keySet()) {
                String rhs2 = inputRuleMaps.getSymbolFromID(rhs2Key);
                Map<Integer, Double> getLHSKeySetWeight = valueRHS12LHS.get(rhs2Key);
                for (int lhsID : getLHSKeySetWeight.keySet()) {
                    String lhs = inputRuleMaps.getSymbolFromID(lhsID);
                    this.addRule(lhs, rhs1, rhs2, true); // to make sure symbols are in place and the ids are set to similar ones ...
                    Double get = getLHSKeySetWeight.get(lhsID);
                    //System.err.println("* " + get + lhs + " " + rhs1 + "  " + rhs2);
                    // maybe write your own sum, without interpolaing!?! the first one is more important here
                    this.updateReverseBinaryRulePartial(
                            this.getIDFromSymbol(lhs),
                            this.getIDFromSymbol(rhs1),
                            this.getIDFromSymbol(rhs2), get, rate);

                }
            }

        }
    }

//    public synchronized void simplyAddThisRuleMapParams(
//            RuleMaps inputRuleMaps
//    ) throws Exception {
//        
//    }
    /**
     * Simply sums the param for this rulemap, for rules that do not exist, they
     * are added
     *
     * @param inputRuleMaps
     * @throws Exception
     */
    public synchronized void simplyAddThisRuleMapParams(
            RuleMaps inputRuleMaps
    ) throws Exception {
        verifySymbolCounterStatus();
        AtomicInteger clusterIndexCounter1 = this.getClusterIndexCounter();
        AtomicInteger symbolIDCounter = this.getSymbolIDCounter();
        inputRuleMaps = inputRuleMaps.shiftClusterAndSymbolIDs(symbolIDCounter.intValue(), clusterIndexCounter1.intValue());
        //inputRuleMaps.seralizaRules("insidesimplyadd.zip");
        // buildWeightMapsFromReverse();
        //add unaries
        Map<Integer, Map<Integer, Double>> reverseUnaryRuleMap = inputRuleMaps.getReversUnaryRuleMap();
        for (int rhsKey : reverseUnaryRuleMap.keySet()) {
            String rhs = inputRuleMaps.getSymbolFromID(rhsKey);
            Map<Integer, Double> getLHSParam = reverseUnaryRuleMap.get(rhsKey);
            for (int lhsKey : getLHSParam.keySet()) {
                String lhs = inputRuleMaps.getSymbolFromID(lhsKey);
                Double paramInput = getLHSParam.get(lhsKey);
                if (Double.isFinite(paramInput)) {
                    this.addRule(lhs, rhs, true); // to make sure ids are in place
                    int lhsKeyNEw = this.getIDFromSymbol(lhs);
                    int rhsKeyNew = this.getIDFromSymbol(rhs);
                    Double unaryRuleParam = this.getUnaryRuleParam(lhsKeyNEw, rhsKeyNew);
                    if (unaryRuleParam != null) {
                        // double logSumExp = Math.max(paramInput, unaryRuleParam);
                        double logSumExp = MathUtil.logSumExp(paramInput, unaryRuleParam);
                        this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, logSumExp, true);

//                     else{
//                     this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, param + Math.log(ratio), true);
//                     }
                    } else {

                        this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, paramInput, true);
                    }
                }else{
                    LOGGER.log(Level.FINE, "We have a problem cx7362784682");
                }
            }
        }

        // add binaries
        Map<Integer, Map<Integer, Map<Integer, Double>>> binaryRuleMapRawInput = inputRuleMaps.getReverseBinaryRuleMap();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : binaryRuleMapRawInput.entrySet()) {
            String rhs1 = inputRuleMaps.getSymbolFromID(e.getKey());
            Map<Integer, Map<Integer, Double>> valueRHS12LHS = e.getValue();
            for (int rhs2Key : valueRHS12LHS.keySet()) {
                String rhs2 = inputRuleMaps.getSymbolFromID(rhs2Key);
                Map<Integer, Double> getLHSKeySetWeight = valueRHS12LHS.get(rhs2Key);
                for (int lhsID : getLHSKeySetWeight.keySet()) {
                    String lhs = inputRuleMaps.getSymbolFromID(lhsID);
                    this.addRule(lhs, rhs1, rhs2, true); // to make sure symbols are in place and the ids are set to similar ones ...
                    Double paramFromInput = getLHSKeySetWeight.get(lhsID);
                    
                    if (Double.isFinite(paramFromInput)) {
                        int lhsNewKey = this.getIDFromSymbol(lhs);
                        int rhs1NewKey = this.getIDFromSymbol(rhs1);
                        int rhs2NewKey = this.getIDFromSymbol(rhs2);
                        //System.err.println("* " + get + lhs + " " + rhs1 + "  " + rhs2);
                        // maybe write your own sum, without interpolaing!?! the first one is more important here

                        Double binParam = this.getBinaryRuleParam(lhsNewKey, rhs1NewKey, rhs2NewKey);
                        if (binParam != null) {

                            double logSumExp = MathUtil.logSumExp(paramFromInput, binParam);
                            this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, logSumExp, true);

                        } else {
                            this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, paramFromInput, true);
                        }
                    } else {
                       
                        LOGGER.log(Level.FINE, "We have a problem cx33784682");
                    }
                }
            }

        }
    }

    public synchronized void simplyAsssignMaxParamBetweenTheTwo(
            RuleMaps inputRuleMaps
    ) throws Exception {
        verifySymbolCounterStatus();
        // buildWeightMapsFromReverse();
        //add unaries
        Map<Integer, Map<Integer, Double>> reverseUnaryRuleMap = inputRuleMaps.getReversUnaryRuleMap();
        for (int rhsKey : reverseUnaryRuleMap.keySet()) {
            String rhs = inputRuleMaps.getSymbolFromID(rhsKey);
            Map<Integer, Double> getLHSParam = reverseUnaryRuleMap.get(rhsKey);
            for (int lhsKey : getLHSParam.keySet()) {
                String lhs = inputRuleMaps.getSymbolFromID(lhsKey);
                Double paramInput = getLHSParam.get(lhsKey);
                this.addRule(lhs, rhs, true); // to make sure ids are in place
                int lhsKeyNEw = this.getIDFromSymbol(lhs);
                int rhsKeyNew = this.getIDFromSymbol(rhs);
                Double unaryRuleParam = this.getUnaryRuleParam(lhsKeyNEw, rhsKeyNew);
                if (unaryRuleParam != null) {
                    double logSumExp = Math.max(paramInput, unaryRuleParam);
                    //    double logSumExp =    MathUtil.logSumExp(paramInput, unaryRuleParam);
                    this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, logSumExp, true);

//                     else{
//                     this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, param + Math.log(ratio), true);
//                     }
                } else {

                    this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, paramInput, true);
                }
            }
        }

        // add binaries
        Map<Integer, Map<Integer, Map<Integer, Double>>> binaryRuleMapRawInput = inputRuleMaps.getReverseBinaryRuleMap();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : binaryRuleMapRawInput.entrySet()) {
            String rhs1 = inputRuleMaps.getSymbolFromID(e.getKey());
            Map<Integer, Map<Integer, Double>> valueRHS12LHS = e.getValue();
            for (int rhs2Key : valueRHS12LHS.keySet()) {
                String rhs2 = inputRuleMaps.getSymbolFromID(rhs2Key);
                Map<Integer, Double> getLHSKeySetWeight = valueRHS12LHS.get(rhs2Key);
                for (int lhsID : getLHSKeySetWeight.keySet()) {
                    String lhs = inputRuleMaps.getSymbolFromID(lhsID);
                    this.addRule(lhs, rhs1, rhs2, true); // to make sure symbols are in place and the ids are set to similar ones ...
                    Double paramFromInput = getLHSKeySetWeight.get(lhsID);
                    int lhsNewKey = this.getIDFromSymbol(lhs);
                    int rhs1NewKey = this.getIDFromSymbol(rhs1);
                    int rhs2NewKey = this.getIDFromSymbol(rhs2);
                    //System.err.println("* " + get + lhs + " " + rhs1 + "  " + rhs2);
                    // maybe write your own sum, without interpolaing!?! the first one is more important here

                    Double unaryRuleParam = this.getBinaryRuleParam(lhsNewKey, rhs1NewKey, rhs2NewKey);
                    if (unaryRuleParam != null) {
                        double logSumExp = Math.max(paramFromInput, unaryRuleParam);
                        // double logSumExp = MathUtil.logSumExp(paramFromInput, unaryRuleP aram);
                        this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, logSumExp, true);

                    } else {
                        this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, paramFromInput, true);
                    }
                }
            }

        }
    }

    public synchronized void accumulateInverseRuleMaps(
            RuleMaps inputRuleMaps,
            boolean useInterpolateIsTrueARatioOfOldWeightIsFalse,
            double ratio) throws Exception {
        verifySymbolCounterStatus();
        buildWeightMapsFromReverse();
        //add unaries
        Map<Integer, Map<Integer, Double>> reverseUnaryRuleMap = inputRuleMaps.getReversUnaryRuleMap();
        for (int rhsKey : reverseUnaryRuleMap.keySet()) {
            String rhs = inputRuleMaps.getSymbolFromID(rhsKey);
            Map<Integer, Double> getLHSParam = reverseUnaryRuleMap.get(rhsKey);
            for (int lhsKey : getLHSParam.keySet()) {
                String lhs = inputRuleMaps.getSymbolFromID(lhsKey);
                Double param = getLHSParam.get(lhsKey);
                this.addRule(lhs, rhs, true); // to make sure ids are in place
                int lhsKeyNEw = this.getIDFromSymbol(lhs);
                int rhsKeyNew = this.getIDFromSymbol(rhs);
                Double unaryRuleParam = this.getUnaryRuleParam(lhsKeyNEw, rhsKeyNew);
                if (unaryRuleParam != null) {

                    if (useInterpolateIsTrueARatioOfOldWeightIsFalse) {
                        double logSumExp = MathUtil.logSumExp(param + Math.log(ratio), unaryRuleParam + Math.log(1 - ratio));
                        this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, logSumExp, true);
                    }
//                     else{
//                     this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, param + Math.log(ratio), true);
//                     }
                } else {

                    this.addReverseUnaryRule(lhsKeyNEw, rhsKeyNew, param + Math.log(ratio), true);
                }
            }
        }

        // add binaries
        Map<Integer, Map<Integer, Map<Integer, Double>>> binaryRuleMapRawInput = inputRuleMaps.getReverseBinaryRuleMap();
        for (Entry<Integer, Map<Integer, Map<Integer, Double>>> e : binaryRuleMapRawInput.entrySet()) {
            String rhs1 = inputRuleMaps.getSymbolFromID(e.getKey());
            Map<Integer, Map<Integer, Double>> valueRHS12LHS = e.getValue();
            for (int rhs2Key : valueRHS12LHS.keySet()) {
                String rhs2 = inputRuleMaps.getSymbolFromID(rhs2Key);
                Map<Integer, Double> getLHSKeySetWeight = valueRHS12LHS.get(rhs2Key);
                for (int lhsID : getLHSKeySetWeight.keySet()) {
                    String lhs = inputRuleMaps.getSymbolFromID(lhsID);
                    this.addRule(lhs, rhs1, rhs2, true); // to make sure symbols are in place and the ids are set to similar ones ...
                    Double param = getLHSKeySetWeight.get(lhsID);
                    int lhsNewKey = this.getIDFromSymbol(lhs);
                    int rhs1NewKey = this.getIDFromSymbol(rhs1);
                    int rhs2NewKey = this.getIDFromSymbol(rhs2);
                    //System.err.println("* " + get + lhs + " " + rhs1 + "  " + rhs2);
                    // maybe write your own sum, without interpolaing!?! the first one is more important here

                    Double unaryRuleParam = this.getBinaryRuleParam(lhsNewKey, rhs1NewKey, rhs2NewKey);
                    if (unaryRuleParam != null) {
                        if (useInterpolateIsTrueARatioOfOldWeightIsFalse) {
                            double logSumExp = MathUtil.logSumExp(param + Math.log(ratio), unaryRuleParam + Math.log(1 - ratio));
                            this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, logSumExp, true);
                        }

                    } else {
                        this.addReverseBinaryRule(lhsNewKey, rhs1NewKey, rhs2NewKey, param + Math.log(ratio), true);
                    }
                }
            }

        }
    }

    public synchronized void buildWeightMapsFromReverse() throws Exception {

        this.binaryRuleWeightMap = new ConcurrentHashMap<>();
        this.ruleUnaryWeightMap = new ConcurrentHashMap<>();
        Iterator<Integer> iterator = this.reversUnaryRuleMap.keySet().iterator();
        while (iterator.hasNext()) {
            Integer nextRHS = iterator.next();
            Map<Integer, Double> get = this.reversUnaryRuleMap.get(nextRHS);
            Iterator<Entry<Integer, Double>> iteratorRHS = get.entrySet().iterator();
            while (iteratorRHS.hasNext()) {
                Entry<Integer, Double> ent = iteratorRHS.next();
                Integer lhsSymbol = ent.getKey();
                Double param = ent.getValue();
                addParameterizedUnaryRule(lhsSymbol, nextRHS, param);
            }
        }

        Iterator<Integer> iteratorRHS1 = this.reverseBinaryRuleMap.keySet().iterator();
        while (iteratorRHS1.hasNext()) {
            Integer rhs1Sym = iteratorRHS1.next();

            Map<Integer, Map<Integer, Double>> rhs1Set = this.reverseBinaryRuleMap.get(rhs1Sym);
            Iterator<Entry<Integer, Map<Integer, Double>>> iteratorRHS2 = rhs1Set.entrySet().iterator();
            while (iteratorRHS2.hasNext()) {
                Entry<Integer, Map<Integer, Double>> e1 = iteratorRHS2.next();
                Integer rhs2Symb = e1.getKey();
                Map<Integer, Double> rhs2 = e1.getValue();

                Iterator<Entry<Integer, Double>> iteratorLHS = rhs2.entrySet().iterator();
                while (iteratorLHS.hasNext()) {
                    Entry<Integer, Double> e2 = iteratorLHS.next();
                    Integer lhsSymbol = e2.getKey();
                    double param = e2.getValue();
                    addParameterizedBinaryRule(lhsSymbol, rhs1Sym, rhs2Symb, param);
                }

            }

        }

    }

    /**
     * Do not rely on the naming of the variables below... they are adapted from
     * the earlier reverese map..
     *
     */
    public synchronized void addParameterizedUnaryRule(int nextLHSNOW, int keyRHSNOW, double param) throws Exception {
        addParameterizedUnaryRule(nextLHSNOW, keyRHSNOW, param, false);
    }

    public synchronized void addParameterizedUnaryRule(int nextLHSNOW, int keyRHSNOW, double param, boolean overrideAllowed) throws Exception {
        if (this.ruleUnaryWeightMap.containsKey(nextLHSNOW)) {
            if (!overrideAllowed) {
                if (ruleUnaryWeightMap.get(nextLHSNOW).containsKey(keyRHSNOW)) {
                    Double get = ruleUnaryWeightMap.get(nextLHSNOW).get(keyRHSNOW);

                    if (Double.compare(get, param) != 0) {
                        throw new Exception("this should not happen in reverese appending of rules! new and old param " + param + "  vs " + get);
                    } else {
                        LOGGER.log(Level.SEVERE, "WARNING: dependance conditions are violated somehow");
                    }

                }
            }
            ruleUnaryWeightMap.get(nextLHSNOW).put(keyRHSNOW, param);
        } else {
            Map<Integer, Double> ruleWeights = new ConcurrentHashMap<>();
            ruleWeights.put(keyRHSNOW, param);
            ruleUnaryWeightMap.put(nextLHSNOW, ruleWeights);
        }
    }

    public synchronized void addParameterizedBinaryRule(int keyLHS, int firstRHS, int rhs2, double param) throws Exception {
        addParameterizedBinaryRule(keyLHS, firstRHS, rhs2, param, false);
    }

    public synchronized void addParameterizedBinaryRule(int nowLHS, int nowRHS1, int nowRHS2, double param, boolean allowOverride) throws Exception {
        if (this.binaryRuleWeightMap.containsKey(nowLHS)) {
            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = binaryRuleWeightMap.get(nowLHS);
            if (secondRHSToLHSWeightMap.containsKey(nowRHS1)) {
                Map<Integer, Double> lhsWeightMap = secondRHSToLHSWeightMap.get(nowRHS1);
                if (!allowOverride) {
                    if (lhsWeightMap.containsKey(nowRHS2)) {
                        throw new Exception("this must not happen " + this.symbolInverseMap.get(nowRHS2) + " "
                                + this.symbolInverseMap.get(nowLHS) + " " + this.symbolInverseMap.get(nowRHS1));
                    }
                }
                //System.out.println(randSumRHS2[countInnerRHS]);
                lhsWeightMap.put(nowRHS2, param);

            } else {

                Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
                lhsWeightMap.put(nowRHS2, param);
                secondRHSToLHSWeightMap.put(nowRHS1, lhsWeightMap);

            }
        } else {

            Map<Integer, Map<Integer, Double>> secondRHSToLHSWeightMap = new ConcurrentHashMap<>();
            Map<Integer, Double> lhsWeightMap = new ConcurrentHashMap<>();
            lhsWeightMap.put(nowRHS2, param);
            secondRHSToLHSWeightMap.put(nowRHS1, lhsWeightMap);
            binaryRuleWeightMap.put(nowLHS, secondRHSToLHSWeightMap);

        }
    }

    /**
     * each param is normalized by div of its value to n, this is used when
     * summing rulemaps also, not that a/b is translated to log(a) - log(b)
     *
     * @param n
     * @throws Exception
     */
    public synchronized void normalizeParametersByDevidingTo(double n) throws Exception {
        double divVal = Math.log(n);

        for (int lhs : reversUnaryRuleMap.keySet()) {
            Map<Integer, Double> rhsSet = reversUnaryRuleMap.get(lhs);
            for (Entry<Integer, Double> keyRhsEntry : rhsSet.entrySet()) {
                Double value = keyRhsEntry.getValue();
                keyRhsEntry.setValue(value - divVal);
            }
        }

        for (int lhs : reverseBinaryRuleMap.keySet()) {

            Map<Integer, Map<Integer, Double>> rhsSet = reverseBinaryRuleMap.get(lhs);
            for (int keyRhs1 : rhsSet.keySet()) {

                Map<Integer, Double> rhs2Set = rhsSet.get(keyRhs1);
                for (Entry<Integer, Double> keyRhs2Entry : rhs2Set.entrySet()) {
                    Double value = keyRhs2Entry.getValue();
                    keyRhs2Entry.setValue(value - divVal);
                }
            }
        }

    }

    public synchronized void normalizeToOne() {
        Collection<Map<Integer, Map<Integer, Double>>> entrySet
                = this.reverseBinaryRuleMap.values();
        Map<Integer, DoubleAccumulator> sumEntries = new HashMap<>();
        entrySet.forEach(ruleMap -> {
            ruleMap.values().forEach(lhsSet -> {
                lhsSet.entrySet().forEach(lhsAndItsWeight -> {
                    Integer key = lhsAndItsWeight.getKey();
                    Double weight = lhsAndItsWeight.getValue();
//                    if(!Double.isFinite(weight)){
//                        System.err.println("Errrorr jj ");
//                    }
//                    DoubleAccumulator accu = sumEntries.getOrDefault(key, MathUtil.getDoubleLogAcc());
//                    accu.accumulate(weight);
//                    sumEntries.put(key, accu);
                    DoubleAccumulator accu = sumEntries.get(key);//, MathUtil.getDoubleLogAcc());
                    if (accu == null) {
                        accu = MathUtil.getDoubleLogAcc();
                        DoubleAccumulator putIfAbsent = sumEntries.putIfAbsent(key, accu);
                        if (putIfAbsent != null) {
                            throw new RuntimeException("not implemented yet (it seems that there was no need for it) --- code:735462");
                        }
                        accu.accumulate(weight);
                    } else {
                        accu.accumulate(weight);
                        //DoubleAccumulator putIfAbsent = sumEntriesUnary.put(lhs, accu);
                    }
                });
            });
        });

        Collection<Map<Integer, Double>> entrySetUnary = this.reversUnaryRuleMap.values();
        // Map<Integer, DoubleAccumulator> sumEntriesUnary = new HashMap<>();
        entrySetUnary.forEach(ruleLhsMap -> {
            ruleLhsMap.entrySet().forEach(entry -> {
                int lhs = entry.getKey();
                double weight = entry.getValue();

                DoubleAccumulator accu = sumEntries.get(lhs);//, MathUtil.getDoubleLogAcc());
                if (accu == null) {
                    accu = MathUtil.getDoubleLogAcc();
                    DoubleAccumulator putIfAbsent = sumEntries.putIfAbsent(lhs, accu);
                    if (putIfAbsent != null) {
                        throw new RuntimeException("not implemented yet (it seems that there was no need for it) --- code:735463");
                    }
                    accu.accumulate(weight);
                } else {
                    accu.accumulate(weight);
                    //DoubleAccumulator putIfAbsent = sumEntriesUnary.put(lhs, accu);
                }

            });
        });

        entrySet.forEach(ruleMap -> {
            ruleMap.values().forEach(lhsSet -> {
                lhsSet.entrySet().forEach(lhsAndItsWeight -> {
                    Integer key = lhsAndItsWeight.getKey();
                    Double weight = lhsAndItsWeight.getValue();
                    DoubleAccumulator toNormalize = sumEntries.get(key);

                    // if (toNormalize > 0.0 && !Double.isNaN(toNormalize)) {
                    weight -= toNormalize.doubleValue();
                    lhsAndItsWeight.setValue(weight);
                    // }
                });
            });
        });

        entrySetUnary.forEach(ruleLhsMap -> {
            ruleLhsMap.entrySet().forEach(entry -> {
                int lhs = entry.getKey();
                double weight = entry.getValue();
                DoubleAccumulator accu = sumEntries.get(lhs);

                //if (toNormalize > 0.0 && !Double.isNaN(toNormalize)) {
                weight -= accu.doubleValue();
                entry.setValue(weight);
                //}
            });
        });

    }
}
