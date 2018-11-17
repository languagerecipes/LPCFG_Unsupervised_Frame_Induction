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

import embedding.sim.SimPearsonCoef;
import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.generate.GenerateGrammarFrameRoleIndepandantsJCLK;
import frameinduction.grammar.learn.MathUtil;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mhutil.HelperFragmentMethods;
import org.apache.commons.math3.linear.SparseRealMatrix;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class WordPairSimilaritySmallMemoryFixedID {

    public  boolean isWeightedDense = false;
    final static private double DEF_VAL_FOR_NOT_FOUND = 1E-4;
    final static private double DEF_MAX_SIM_VALUE = 1.0;//1.0;
    SparseRealMatrix srm;

    //  private final int dim;
    private final Map<String, Integer> wordIDMap;

    private Map<Integer, Map<Integer, Double>> sparseSimilarityMap;
    private double DEF_VALUE_FOR_MINSIM= 0.000000001;

    public Integer getWordID(String word) {
        return wordIDMap.getOrDefault(word, null);
    }

    public int getVocabSetSize() {
        return vocabSet.size();
    }
    
    private Set<String> vocabSet = null;
  private final static Logger LOGGER =  Logger.getLogger(WordPairSimilaritySmallMemoryFixedID.class.getName());
//    public WordPairSimilaritySmallMemoryFixedID(String pathVector, RuleMaps rm, int dim) throws Exception {
//        System.err.println("Caching word similarities once and forever...");
//        //this.dim = dim;
//        vocabSet =  new HashSet<>(rm.getActiveVocab().keySet());
//        
//        
//        WordEmbeddingSet embeddingSet = UtilEmbeddings.loadPoPVectorsFromFile(
//                new File(pathVector), 
//                dim,vocabSet,true);
//
//        wordIDMap = Collections.unmodifiableMap(new HashMap<>(embeddingSet.getIdMap()));
//        System.err.println("WORD ID SIZE " + wordIDMap.size());
//        sparseSimilarityMap = new ConcurrentHashMap<>();
//        
//        computePairwiseSimMatrix(embeddingSet.getWordVectors(), dim);
//        // once done freeze everything, free the objects and make similairies unmodifiable
//        sparseSimilarityMap = Collections.unmodifiableMap(sparseSimilarityMap);
//        System.err.println("Done caching similaries done for vector of size: " + wordIDMap.size());
//        embeddingSet = null;
//    }
    public WordPairSimilaritySmallMemoryFixedID(String pathVector, Collection<Fragment> fragmentList, int dim) throws Exception {
       
        //this.dim = dim;
        vocabSet = HelperFragmentMethods.makeVocab(fragmentList);
      //  vocabSet =  new HashSet<>(rm.getActiveVocab().keySet());

        WordEmbeddingSet embeddingSet = UtilEmbeddings.loadPoPVectorsFromFile(
                new File(pathVector),
                dim, vocabSet, true);

        wordIDMap = Collections.unmodifiableMap(new HashMap<>(embeddingSet.getIdMap()));
        LOGGER.log(Level.FINE, " vocab size, i.e., WORD ID SIZE is " + wordIDMap.size());
        sparseSimilarityMap = new ConcurrentHashMap<>();
        ISimMethod simPearson = new SimPearsonCoef();
        computePairwiseSimMatrix(embeddingSet.getWordVectors(), dim,simPearson);
        // once done freeze everything, free the objects and make similairies unmodifiable
        sparseSimilarityMap = Collections.unmodifiableMap(sparseSimilarityMap);
        LOGGER.log(Level.FINE,"Done caching similaries done for vector of size: " + wordIDMap.size());
        embeddingSet = null;
    }
    
    
    public WordPairSimilaritySmallMemoryFixedID(String pathVector, Set<String> vocab, int dim) throws Exception {
        LOGGER.log(Level.FINE,"Caching word similarities once and forever...");
        //this.dim = dim;
        vocabSet = vocab;
      //  vocabSet =  new HashSet<>(rm.getActiveVocab().keySet());

        WordEmbeddingSet embeddingSet = UtilEmbeddings.loadPoPVectorsFromFile(
                new File(pathVector),
                dim, vocabSet, true);

        wordIDMap = Collections.unmodifiableMap(new HashMap<>(embeddingSet.getIdMap()));
        LOGGER.log(Level.FINE,"WORD ID SIZE " + wordIDMap.size());
        sparseSimilarityMap = new ConcurrentHashMap<>();
        
        ISimMethod simPearson = new SimPearsonCoef(); // pass this as an argument
        computePairwiseSimMatrix(embeddingSet.getWordVectors(), dim, simPearson);
        // once done freeze everything, free the objects and make similairies unmodifiable
        sparseSimilarityMap = Collections.unmodifiableMap(sparseSimilarityMap);
        LOGGER.log(Level.FINE,"Done caching similaries done for vector of size: " + wordIDMap.size());
        embeddingSet = null;
    }

     public WordPairSimilaritySmallMemoryFixedID(String pathVector, Set<String> vocab, int dim,  ISimMethod simYourChoice) throws Exception {
        LOGGER.log(Level.FINE,"Caching word similarities once and forever...");
        //this.dim = dim;
        vocabSet = vocab;
      //  vocabSet =  new HashSet<>(rm.getActiveVocab().keySet());

        WordEmbeddingSet embeddingSet = UtilEmbeddings.loadPoPVectorsFromFile(
                new File(pathVector),
                dim, vocabSet, true);

        wordIDMap = Collections.unmodifiableMap(new HashMap<>(embeddingSet.getIdMap()));
        LOGGER.log(Level.FINE,"WORD ID SIZE " + wordIDMap.size());
        sparseSimilarityMap = new ConcurrentHashMap<>();
        
       
        computePairwiseSimMatrix(embeddingSet.getWordVectors(), dim, simYourChoice);
        // once done freeze everything, free the objects and make similairies unmodifiable
        sparseSimilarityMap = Collections.unmodifiableMap(sparseSimilarityMap);
        LOGGER.log(Level.FINE,"Done caching similaries done for vector of size: " + wordIDMap.size());
        embeddingSet = null;
    }
     public WordPairSimilaritySmallMemoryFixedID(boolean dense, String pathVector, Set<String> vocab, int dim) throws Exception {
         if(dense){
             this.isWeightedDense=true;
         }
        vocabSet = vocab;
        WordEmbeddingSet embeddingSet = UtilEmbeddings.loadDenseVectorsFromFile(
                new File(pathVector),
                dim, vocabSet, true);

        wordIDMap = Collections.unmodifiableMap(new HashMap<>(embeddingSet.getIdMap()));
        LOGGER.log(Level.FINE,"WORD ID SIZE " + wordIDMap.size());
        sparseSimilarityMap = new ConcurrentHashMap<>();

        computePairwiseSimMatrix(embeddingSet.getWordVectors(), dim, new SimPearsonCoef());
        // once done freeze everything, free the objects and make similairies unmodifiable
        sparseSimilarityMap = Collections.unmodifiableMap(sparseSimilarityMap);
        LOGGER.log(Level.FINE,"Done caching similaries done for vector of size: " + wordIDMap.size());
        embeddingSet = null;
    }

  
    private void computePairwiseSimMatrix(Map<Integer, double[]> embeddingsRaw, int dim, ISimMethod isim) {

        HashMap<Integer, String> inverseMapForDebug = new HashMap<>();
        wordIDMap.entrySet().forEach(e -> {
            inverseMapForDebug.put(e.getValue(), e.getKey());
        });
        Map<Integer, double[]> embeddings = null;
        if (!isWeightedDense) {
            LOGGER.log(Level.FINE,"Weighting process ... ");
            FWPPMI iw = new FWPPMI(); // move this as a paramtere
            embeddings = iw.weightingProcessI(embeddingsRaw, dim);
           // embeddings = iw.weightingProcessI(embeddings, dim);
        } else {
            embeddings = embeddingsRaw;
        }
        double maxSimExact = DEF_MAX_SIM_VALUE; // give higher value to exact matches!
        for (Entry<Integer, double[]> spsE : embeddings.entrySet()) {
            int key1 = spsE.getKey();
            final double[] vector1 = spsE.getValue();
            if (MathUtil.getSum(vector1) == 0) {

                embeddings.entrySet().parallelStream().forEach(spsEntry -> {
                    int key2 = spsEntry.getKey();
                    if (key1 == key2) {
                        try {
                            updateSimilarityMap(key1, key2, maxSimExact);
                        } catch (Exception ex) {
                            Logger.getLogger(WordPairSimilaritySmallMemoryFixedID.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        if (key1 > key2) {
                            try {
                              
                                updateSimilarityMap(key1, key2, 0.000001);
                            } catch (Exception ex) {
                                Logger.getLogger(WordPairSimilaritySmallMemoryFixedID.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });

            } else {
                embeddings.entrySet().parallelStream().forEach(spsEntry -> {
                    int key2 = spsEntry.getKey();
                    if (key1 == key2) {
                        try {
                            updateSimilarityMap(key1, key2, maxSimExact);
                        } catch (Exception ex) {
                            Logger.getLogger(WordPairSimilaritySmallMemoryFixedID.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        if (key1 > key2) {
                            try {
                                double sim;
                                double[] vector2 = spsEntry.getValue();
                                double sum2 = MathUtil.getSum(vector2);
                                if (sum2 == 0) {
                                    sim = DEF_VALUE_FOR_MINSIM;

                                } else {
                                     sim = isim.measureSim(vector1, vector2);
                                }
                                if (!Double.isFinite(sim)) {

                                    // System.err.println("HERE! " + sim + " " + inverseMapForDebug.get(key1) + " " + inverseMapForDebug.get(key2));
                                    sim = DEF_VAL_FOR_NOT_FOUND;
                                } else {
                                    //    System.err.println("-->HERE! "  + sim + " " + inverseMapForDebug.get(key1) + " " + inverseMapForDebug.get(key2));
                                }
                                updateSimilarityMap(key1, key2, sim);
                            } catch (Exception ex) {
                                Logger.getLogger(WordPairSimilaritySmallMemoryFixedID.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });

            }
        }

    }

    private static double squash(double input) {
        return 1.0 / (1.0 + Math.exp(-input));
    }

    public double getSimValue(String wordID1, String wordID2) {
        if(wordID1.equals(wordID2)){
            return DEF_MAX_SIM_VALUE;
        }
        Integer getID1 = this.wordIDMap.get(wordID1);
        if (getID1 == null) {
            return DEF_VAL_FOR_NOT_FOUND;
        }
        Integer getID2 = this.wordIDMap.get(wordID2);
        if (getID2 == null) {
            return DEF_VAL_FOR_NOT_FOUND;
        }

        double sim = getSimValue(getID1, getID2);
        return sim;
    }

    Double getSimValue(Integer id1, Integer id2) {
        if (id1 < id2) {
            return getSimValue(id2, id1);
        }
        if (this.sparseSimilarityMap.containsKey(id1)) {
            Map<Integer, Double> get = this.sparseSimilarityMap.get(id1);
            if (get.containsKey(id2)) {
                Double value = get.get(id2);
                return value;
            } else {
                return DEF_VAL_FOR_NOT_FOUND;

            }

        } else {
            return DEF_VAL_FOR_NOT_FOUND;
        }
    }

    private void updateSimilarityMap(int key1, int key2, double simValue) throws Exception {
        if (key1 < key2) {
            updateSimilarityMap(key2, key1, simValue);
        } else {
            if (this.sparseSimilarityMap.containsKey(key1)) {
                Map<Integer, Double> get = sparseSimilarityMap.get(key1);
                Double putIfAbsent = get.putIfAbsent(key2, simValue);
                if (putIfAbsent != null) {
                    if (putIfAbsent - simValue != 0) {
                        throw new Exception("Unseen race condition in vec. sim comp");
                    }
                }
            } else {
                Map<Integer, Double> get = new ConcurrentHashMap<>();
                get.put(key2, simValue);
                Map<Integer, Double> putIfAbsent = sparseSimilarityMap.putIfAbsent(key1, get);
                if (putIfAbsent != null) {
                    Double putIfAbsent1 = putIfAbsent.putIfAbsent(key2, simValue);
                    if (putIfAbsent1 != null) {
                        if (putIfAbsent1 - simValue != 0) {
                            throw new Exception("Unseen race condition in vec. sim comp");
                        }
                    }
                }
            }

        }
    }

    public static void main(String[] ard) throws Exception {

        System.err.println("READING FRAMES... and generating grammar ....");
        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments("../lr_other/stanford-parse.txt", null, null, settings, 5);
        GenerateGrammarFrameRoleIndepandantsJCLK gg = new GenerateGrammarFrameRoleIndepandantsJCLK();
        gg.genRules(inputFramentList, 2, 7);
        RuleMaps theRuleMap = gg.getTheRuleMap();
        WordPairSimilaritySmallMemoryFixedID wsm = new WordPairSimilaritySmallMemoryFixedID("../lr_other/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt", inputFramentList, 900);

        Set<String> keySet = theRuleMap.getActiveVocab().keySet();
        for (String k : keySet) {
            for (String k2 : keySet) {
                System.out.println(k + " " + k2 + " " + wsm.getSimValue(k, k2));
            }
        }

        System.out.println(wsm.getSimValue("build", "Make"));

    }
    
    
    
    
    private static void testRank(double[] vector1, double[] vector2){
        for (int i = 0; i < vector2.length; i++) {
            for (int j = 0; j < i; j++) {
                double d = Math.pow(vector1[i] - vector2[j],2);
                
            }
            
        }
    }

}
