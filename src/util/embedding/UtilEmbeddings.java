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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class UtilEmbeddings {

    public static Map<String, float[]> loadWord2VecVectorsFromFile(File fileName, Set<String> listOfWords, int dimension) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileName), "UTF8"));

        Map<String, float[]> wordVectorMap = new ConcurrentHashMap<>();
        String delimit = "\\s";
        br.readLine();
        Set<String> foudnVocab = ConcurrentHashMap.newKeySet();

        br.lines().parallel().forEach(line -> {
            // while ((line = br.readLine()) != null) {
            String[] split = line.split(delimit);
            String word = split[0];
            if (listOfWords.contains(word)) {
                //SimpleSparseFloat ss = new SimpleSparseFloat(dimension);

                // double[] dense = new double[dimension];
                // System.out.println(word);
                float[] vec = new float[dimension];
                for (int i = 1; i < dimension; i++) {

                    float dense = Float.parseFloat(split[i]);
                    if (!Double.isFinite(dense)) {
                        System.err.println(" error at line " + line);
                    }
                    vec[i - 1] = dense;
                }
                wordVectorMap.put(word, vec);
                foudnVocab.add(word);
            }
        });
        br.close();
        // System.out.println("Loaded "+ wordVectorMap.size() + " vectors ..
        // sanity checking ...");
        // StringBuilder sb = new StringBuilder("Wrng, vec mis 4:" );

        // System.out.println(sb.toString());
        for (String woS : listOfWords) {
            if (!foudnVocab.contains(woS)) {
                Integer get = woS.hashCode();
                float[] vec = new float[dimension];
                vec[get % dimension - 1] = .1f;
                vec[get % dimension / 2] = .1f;
                wordVectorMap.put(woS, vec);
            }
        }

        return wordVectorMap;
    }

    public static Map<Integer, double[]> loadPoPVectorsFromFile(File fileName, Map<String, Integer> wordIDMap, int dimension) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileName), "UTF8"));

        Map<Integer, double[]> wordVectorMap = new ConcurrentHashMap<>();
        String delimit = "\t";
        br.readLine();
        Set<String> foudnVocab = ConcurrentHashMap.newKeySet();
        br.lines().parallel().forEach(line -> {
            // while ((line = br.readLine()) != null) {
            String[] split = line.split(delimit);
            String word = split[0].replace(" ", "_");

            if (wordIDMap.containsKey(word)) {

                double[] ss = new double[dimension];

                // double[] dense = new double[dimension];
                // System.out.println(word);
                String[] splitVal = split[1].split(" ");
                for (int i = 0; i < splitVal.length; i++) {
                    String[] split1 = splitVal[i].split(":");
                    int dim = Integer.parseInt(split1[0]);
                    double val = Double.parseDouble(split1[1]);

                    if (!Double.isFinite(val)) {
                        System.err.println(" *uck at line " + line);
                        val = 0;
                    }
                    ss[dim] = val;
                }
                wordVectorMap.put(wordIDMap.get(word), ss);
                foudnVocab.add(word);
            } else {
                if (wordIDMap.containsKey(word.toLowerCase())) {
                    Logger.getGlobal().log(Level.FINER, "Word in vectors not in active {0}", word);
                    // System.err.println("Word in vectors not in active " + word);
                }

            }
        });
        br.close();
        // System.out.println("Loaded "+ wordVectorMap.size() + " vectors ..
        // sanity checking ...");
        // StringBuilder sb = new StringBuilder("Wrng, vec mis 4:" );

        // System.out.println(sb.toString());
        // System.out.println(sb.toString());
        for (String woS : wordIDMap.keySet()) {
            if (!foudnVocab.contains(woS)) {
                Integer get = wordIDMap.get(woS);
                double[] simpleSparseFloat = new double[dimension];
                simpleSparseFloat[Math.abs(get % dimension - 1)] = 10f;
                simpleSparseFloat[Math.abs(get % dimension / 2)] = 10f;
                wordVectorMap.put(get, simpleSparseFloat);
            }
        }

        return wordVectorMap;
    }

    /**
     * load the n-@param dimension vector (note that dimension can be less than
     * the actual dimenionality of the vector). If the last boolean parameter
     * @param addMissing is true, then random one-hot vectors are generated for
     * entries with no vector.
     *
     * @param fileName
     * @param dimension
     * @param tgtVocab
     * @param addMissing
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     */
    public static WordEmbeddingSet loadPoPVectorsFromFile(File fileName, int dimension, Set<String> tgtVocab, boolean addMissing) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileName), "UTF8"));
        Map<String, Integer> wordIDMap = new ConcurrentHashMap<>();
        Map<Integer, double[]> wordVectorMap = new ConcurrentHashMap<>();
        String delimit = "\t";
        br.readLine();
        Set<String> foudnVocab = ConcurrentHashMap.newKeySet();
        AtomicInteger wordIDCounter = new AtomicInteger();

        br.lines().parallel()
                .forEach(line -> {
                    // while ((line = br.readLine()) != null) {
                    String[] split = line.split(delimit);
                    String word = split[0].replace(" ", "_");
//                    if (lengthFilter && word.length() < 2) {
//                   
//                    // prehaps this is not a good idea
//                    } else 
                    // {
                    if (tgtVocab == null || tgtVocab.contains(word)) {
                        Integer wordIDUSed = null;
                        if (wordIDMap.containsKey(word)) {
                            wordIDUSed = wordIDMap.get(word);
                        } else {
                            int addAndGet = wordIDCounter.incrementAndGet();
                            Integer putIfAbsent = wordIDMap.putIfAbsent(word, addAndGet);
                            if (putIfAbsent != null) {
                                wordIDUSed = putIfAbsent;
                            } else {
                                wordIDUSed = addAndGet;
                            }
                        }
                        double[] ss = new double[dimension];

                        // double[] dense = new double[dimension];
                        // System.out.println(word);
                        String[] splitVal = split[1].split(" ");
                        for (int i = 0; i < splitVal.length; i++) {
                            String[] split1 = splitVal[i].split(":");
                            int dim = Integer.parseInt(split1[0]);
                            double val = Double.parseDouble(split1[1]);
                            // filter the vector if necessary
                            if (dim < dimension) {
                                if (!Double.isFinite(val)) {
                                    System.err.println("Error at line when reading vectors ...  " + line);
                                    val = 0;
                                }
                                ss[dim] = val;
                            }
                        }
                        wordVectorMap.put(wordIDUSed, ss);
                        foudnVocab.add(word);
                    }
                    // }
                });
        br.close();
//        if (tgtVocab != null) {
//            for (String tgtWord : tgtVocab) {
//                if (!foudnVocab.contains(tgtWord)) {
//                    System.err.println("No vector for " + tgtWord);
//                }
//            }
//        }
        if (addMissing) {
           // System.err.println("Appending not found words to model using random 1-hot vectors ... ");
            for (String woS : wordIDMap.keySet()) {
                if (!foudnVocab.contains(woS)) {
                    Integer get = wordIDMap.get(woS);
                    double[] vec = new double[dimension];
                    vec[get % dimension] = 1;
                    // vec[get % dimension / 2] = 1f;
                    wordVectorMap.put(get, vec);
                }
            }
        }
        
        
        
        //System.err.println("Last Word ID used was " + wordIDCounter);

        return new WordEmbeddingSet(wordIDMap, wordVectorMap);
    }

    public static WordEmbeddingSet loadDenseVectorsFromFile(File fileName, int dimension, Set<String> tgtVocab, boolean addMissing) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileName), "UTF8"));
        Map<String, Integer> wordIDMap = new ConcurrentHashMap<>();
        Map<Integer, double[]> wordVectorMap = new ConcurrentHashMap<>();
        String delimit = "\t";
        br.readLine();
        Set<String> foudnVocab = ConcurrentHashMap.newKeySet();
        AtomicInteger wordIDCounter = new AtomicInteger();

        br.lines().parallel()
                .forEach(line -> {
                    // while ((line = br.readLine()) != null) {
                    String[] split = line.split(" ");
                    String word = split[0];//.replace(" ", "_");
                    
//                    if (lengthFilter && word.length() < 2) {
//                   
//                    // prehaps this is not a good idea
//                    } else 
                    // {
                    if (tgtVocab == null || tgtVocab.contains(word)) {
                       
                        Integer wordIDUSed = null;
                        if (wordIDMap.containsKey(word)) {
                            wordIDUSed = wordIDMap.get(word);
                        } else {
                            int addAndGet = wordIDCounter.incrementAndGet();
                            Integer putIfAbsent = wordIDMap.putIfAbsent(word, addAndGet);
                            if (putIfAbsent != null) {
                                wordIDUSed = putIfAbsent;
                            } else {
                                wordIDUSed = addAndGet;
                            }
                        }
                        

                        // double[] dense = new double[dimension];
                        // System.out.println(word);
                        double[] vec = new double[dimension];
                        for (int i = 1; i < dimension; i++) {

                            double dense = Double.parseDouble(split[i]);
                            if (!Double.isFinite(dense)) {
                                System.err.println(" error at line " + line);
                            }
                            vec[i - 1] = dense;
                        }

                        wordVectorMap.put(wordIDUSed, vec);
                        foudnVocab.add(word);
                    }
                    // }
                });
        br.close();
//        if (tgtVocab != null) {
//            for (String tgtWord : tgtVocab) {
//                if (!foudnVocab.contains(tgtWord)) {
//                    System.err.println("No vector for " + tgtWord);
//                }
//            }
//        }
        int countMissing = 0;
        if (addMissing) {
            //System.err.println("Appending not found words to model using random 1-hot vectors ... ");
        
            for (String woS : wordIDMap.keySet()) {
                if (!foudnVocab.contains(woS)) {
                    Integer get = wordIDMap.get(woS);
                    double[] vec = new double[dimension];
                    vec[get % dimension] = .01;
                    // vec[get % dimension / 2] = 1f;
                    wordVectorMap.put(get, vec);
                    countMissing++;
                }
            }
        }
       // System.err.println("Loaded " + wordIDMap.size()+" of which " + countMissing+ " are initialized randomly.");
       // System.err.println("Last Word ID used was " + wordIDCounter);

        return new WordEmbeddingSet(wordIDMap, wordVectorMap);
    }

    public static void main(String[] args) throws IOException, Exception {
//        RuleMaps fromArchivedFile = RuleMaps.fromArchivedFile(
//                "-fg.zip-ati0.zip");
        Set<String> ss = new HashSet<>();
        ss.add("apprend");
        ss.add("apprender");
        ss.add("que");
        WordEmbeddingSet loadDenseVectorsFromFile = UtilEmbeddings.loadDenseVectorsFromFile(new File("../lr/vectors/wiki.300.fr.vec"),300,ss,true);
        System.out.println(loadDenseVectorsFromFile.getIdMap().keySet());

//
//                activeVocab, 100);
//        Map<Integer, SimpleSparseFloat> loadPoPVectorsFromFile = 
//                UtilEmbeddings.loadPoPVectorsFromFile(new File(
//                        "C:\\prj\\codes\\frame_induction_codes\\"
//                                + "FrameInductionEvaluation\\"
//                                + "2-8embd\\vectors\\"
//                                + "AfterFinPVHFNG-en-900-cLR-5-3.txt"), 
//                activeVocab, 900);
//        for(double[] f: loadPoPVectorsFromFile.values()){
//            
//            for(double[] f2: loadPoPVectorsFromFile.values()){
//              
//            }
//        }

    }
}
