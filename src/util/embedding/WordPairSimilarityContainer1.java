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
import frameinduction.grammar.generate.GenerateGrammarFrameRoleIndepandantsJCLK;
import frameinduction.settings.Settings;
import input.preprocess.objects.Fragment;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mhutil.HelperFragmentMethods;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class WordPairSimilarityContainer1 {

    // need to make some policy for maintaining the consistencies after split/merge.
    private WordPairSimilaritySmallMemoryFixedID wordPairSimilaritySmallMemoryFixedID;
    private Map<Integer, Map<Integer, Double>> sparseSimilarityMap;

    Map<Integer, Integer> ruleMapToVectorID;
    private static final Double DEF_VAL_FOR_NOT_FOUND = 0.0;
    private static final double DEFUALT_MAX_SIMILARITY=1.0;

    public WordPairSimilarityContainer1(RuleMaps rm, WordPairSimilaritySmallMemoryFixedID wsfIXED) throws Exception {
        sparseSimilarityMap = new ConcurrentHashMap<>();
        ruleMapToVectorID = new HashMap<>();
        wordPairSimilaritySmallMemoryFixedID = wsfIXED;
        Map<String, Integer> symbolToIDMap = rm.getSymbolToIDMap();
        for (Map.Entry<String, Integer> symbolTableA : symbolToIDMap.entrySet()) {
            Integer ruleMapID = symbolTableA.getValue();
            String word = symbolTableA.getKey();
            Integer wordID = wordPairSimilaritySmallMemoryFixedID.getWordID(word);
            for (Map.Entry<String, Integer> symbolTablex: symbolToIDMap.entrySet()) {
                Integer ruleMapID2 = symbolTablex.getValue();
                if (ruleMapID == ruleMapID2) {
                    updateSimilarityMap(ruleMapID, ruleMapID2, DEFUALT_MAX_SIMILARITY);
                }else
                if (ruleMapID > ruleMapID2) {
                    String word2 = symbolTablex.getKey();
                    Integer wordID2 = wordPairSimilaritySmallMemoryFixedID.getWordID(word2);
                    if (wordID != null) {
                        if (wordID2 != null) {
                            Double simValue = wordPairSimilaritySmallMemoryFixedID.getSimValue(wordID, wordID2);
                           // System.err.println("** " +simValue);
                            updateSimilarityMap(ruleMapID, ruleMapID2, simValue);
                        } else {
                            updateSimilarityMap(ruleMapID, ruleMapID2, DEF_VAL_FOR_NOT_FOUND);
                        }

                    } else {

                        updateSimilarityMap(ruleMapID, ruleMapID2, DEF_VAL_FOR_NOT_FOUND);
                    }

                }

            }

        }

    }

//    public Double getSimValue(int wordID1, int wordID2) {
//
//        try {
//            Integer realWordID1 = ruleMapToVectorID.get(wordID1);
//            Integer realWordID2 = ruleMapToVectorID.get(wordID2);
//            if (realWordID1 == null || realWordID2 == null) {
//                return 0.0;
//            }else{
//            return this.wordPairSimilaritySmallMemoryFixedID.getSimValue(realWordID1, realWordID2);
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(WordPairSimilarityContainer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//
//    }
    public Double getSimValue(Integer id1, Integer id2) {
        if (id1 < id2) {
            return getSimValue(id2, id1);
        }
        Map<Integer, Double> get = this.sparseSimilarityMap.get(id1);
        if (get == null) {
            return DEF_VAL_FOR_NOT_FOUND;
        } else {
            Double get1 = get.get(id2);
            if (get1 != null) {
                return get1;
            }
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
    public static void main(String[] args) throws Exception {
          System.err.println("READING FRAMES... and generating grammar ....");
        Settings settings = new Settings();
        List<Fragment> inputFramentList = HelperFragmentMethods.parseTreeToFragments("../lr_other/stanford-parse.txt", null, null, settings, 5);
        GenerateGrammarFrameRoleIndepandantsJCLK gg = new GenerateGrammarFrameRoleIndepandantsJCLK();
        gg.genRules(inputFramentList, 2, 7);
        RuleMaps theRuleMap = gg.getTheRuleMap();
        WordPairSimilaritySmallMemoryFixedID wsm = new WordPairSimilaritySmallMemoryFixedID("../lr_other/vectors/AfterFinPVHFNG-en-900-cLR-5-3.txt", inputFramentList, 900);
        WordPairSimilarityContainer1 wsp = new WordPairSimilarityContainer1(theRuleMap, wsm);
        Set<Integer> keySet = theRuleMap.getSymbolInverseMap().keySet();
        for (int k : keySet) {
            for (int k2 : keySet) {
                System.out.println(theRuleMap.getSymbolFromID(k2) + " " + theRuleMap.getSymbolFromID(k) + " " + wsp.getSimValue(k, k2));
            }
        }
      
        System.out.println(wsm.getSimValue("build", "Make"));

    }
}
