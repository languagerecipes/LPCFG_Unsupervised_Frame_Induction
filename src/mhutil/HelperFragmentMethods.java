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


import frameinduction.grammar.parse.io.ParseFrame;
import frameinduction.settings.Settings;
import input.preprocess.FragmentGeneration;
import input.preprocess.FragmentGenerationNoStx;
import input.preprocess.FragmentGenerationOnlySyntax;
import input.preprocess.ProcessDependencyParses;
import input.preprocess.objects.Fragment;
import input.preprocess.objects.Sentence;
import input.preprocess.objects.TerminalToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import frameinduction.grammar.parse.io.HierachyBuilderResultWarpper;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.CollectionUtil;

import utils.io.ParsedFileReader;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperFragmentMethods extends HelperFragmentIOUtils{


    static final   Logger LOGGER = Logger.getLogger(HelperFragmentMethods.class.getName());
            
    
    public static Collection<Fragment> removeDuplicateLexicals(List<Fragment> inputFramentList){
     
        
        Set<String> strSet =ConcurrentHashMap.newKeySet();
        Map<Long, Fragment> fragmentListTOMap = HelperFragmentMethods.fragmentIDMap(inputFramentList);
        inputFramentList.forEach(f->{
             String[] split = f.toLexicalizedString().split("\t");
            if(strSet.contains(split[1])){
               // System.err.println(split[0]);
               fragmentListTOMap.remove(f.getLongID());
            }else{
            strSet.add(split[1]);}
            //System.err.println(f.toLexicalizedString().split("\t")[1]);
        });
        System.out.println(strSet.size() + " vs " + inputFramentList.size());
        return fragmentListTOMap.values();
       
    
    }
    
    /**
     * Build a vocabulary from lexicalzation asserted in fragments
     *
     * @param inputFramentList
     * @return
     */
    public static Set<String> makeVocab(Collection<Fragment> inputFramentList) {

        Set<String> strSet = ConcurrentHashMap.newKeySet();

        inputFramentList.parallelStream().forEach(f -> {
            List<String> seq = f.getLexicalizedSequence();
            seq.forEach(s -> {
                strSet.add(s);
            });

            //System.err.println(f.toLexicalizedString().split("\t")[1]);
        });
       
        return strSet;

    }
    
    
    /**
     * Get the set of all syntactic relations used in fragments
     * @param inputFramentList
     * @return 
     */
       public static Set<String> makeSyntacticRelationSet(Collection<Fragment> inputFramentList) {

        Set<String> strSet = ConcurrentHashMap.newKeySet();

        inputFramentList.parallelStream().forEach(f -> {
            List<String> seq = f.getDependencyStringList(true);
            seq.forEach(s -> {
                strSet.add(s);
            });

            //System.err.println(f.toLexicalizedString().split("\t")[1]);
        });
       
        return strSet;

    }
     public static Set<String> makeVocabColF(Collection<Collection<Fragment>> inputFramentList) {

        Set<String> strSet = ConcurrentHashMap.newKeySet();

        inputFramentList.parallelStream().forEach(fCol -> {
            
            Set<String> makeVocab = makeVocab(fCol);
            
                strSet.addAll(makeVocab);
            

            //System.err.println(f.toLexicalizedString().split("\t")[1]);
        });
       
        return strSet;

    }

    public static Collection<Fragment> flattenFragmentCollections(Collection<Collection<Fragment>> fgColofCol) {
        Collection<Fragment> fList = new ConcurrentLinkedDeque<>();
        fgColofCol.parallelStream().forEach(fl->{fList.addAll(fl);});
        return fList;
    }
    public static List<Fragment> flattenFragmentCollections(List<List<Fragment>> fgColofCol){
        List<Fragment> fList = new ArrayList<>();
        fgColofCol.forEach(fl->{fList.addAll(fl);});
        return fList;
    }
    
    public static Collection<Map<String, Collection<Fragment>>> breakFragmentClusters(Map<String, Collection<Fragment>> fragmentClustersx, int size) {
        Collection<Map<String, Collection<Fragment>>> toReturn = new ArrayList<>();
        Set<String> keySet = fragmentClustersx.keySet();
        List<Collection<String>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(new ArrayList<>(keySet), Math.floorDiv(keySet.size(), size) );
        for (Collection<String> kys : splitCollectionBySize) {
            Map<String, Collection<Fragment>> map = new HashMap<>();
            kys.forEach(k -> {
                map.put(k, fragmentClustersx.get(k));
            });
            toReturn.add(map);
        }
        return toReturn;
    }
   
    
    
    /**
     * tested, works ok, at least all objects are there
     * @param fragmentClustersx
     * @param size
     * @return 
     */
     public static Collection<Map<String, Collection<Fragment>>> breakFragmentClustersWithRandom(Map<String, Collection<Fragment>> fragmentClustersx, int size) {
         List<Map<String, Collection<Fragment>>> toReturn = new ArrayList<>();
         for (int i = 0; i <= size; i++) {
             Map<String, Collection<Fragment>> map = new HashMap<>();
             toReturn.add(map);
         }

         Set<String> keySet = fragmentClustersx.keySet();
        // System.err.println("Size is " +toReturn.size());
        int clusterCounter = 0;
        for (String kys : keySet) {
            
            Collection<Fragment> getFragment = fragmentClustersx.get(kys);
            
            List<Collection<Fragment>> splitCollectionBySize = CollectionUtil.splitCollectionBySize(getFragment, 1+ Math.floorDiv(getFragment.size(),size ) );
           
            for (int i = 0; i < splitCollectionBySize.size(); i++) {
                int key =clusterCounter++;
                toReturn.get(i).put(key+"", splitCollectionBySize.get(i));
                
            }
            
           
        }
        
        return toReturn;
    }
     

   
    /**
     * @ TO DO add Method to convert a parse tree to a list of fragments
     * @param inputParsedFilePath
     * @param tgtLemmaSet
     * @param lemmaSetToFilter
     * @param settings
     * @param limit
     * @return
     * @throws Exception
     */
    public static List<Fragment> parseTreeToFragments(
            String inputParsedFilePath,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit) throws Exception {
      
       
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputParsedFilePath);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();

            //ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            FragmentGeneration fg = new FragmentGeneration(
                    sentence, tgtLemmaSet, lemmaSetToFilter, settings.getActiveDepr(),
                    settings.getTargetPos());
            List<Fragment> fragments = fg.getFragments();
            fragmentList.addAll(fragments);
            if (limit > 0 && fragmentList.size() > limit) {
                break;
            }
        }
        return fragmentList;
    }

    
   
    
    
    public static List<Fragment> parseTreeToFragments(
            String inputParsedFilePath,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit, int lengthLimit) throws Exception {
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputParsedFilePath);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();
            //ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            ProcessDependencyParses.normalizeNumbers(nextSentence);
            
            FragmentGeneration fg = new FragmentGeneration(
                    sentence, tgtLemmaSet,
                    lemmaSetToFilter, 
                    settings.getActiveDepr(),
                
                    settings.getTargetPos());
            
            List<Fragment> fragments = fg.getFragments();
            for (Fragment f : fragments) {
             
                if (lengthLimit > 0 
                        && f.getTerminals().size() < lengthLimit
                        ) {
                    fragmentList.add(f);
                }
            }

            if (limit > 0 && fragmentList.size() > limit) {
                break;
            }
        }
        return fragmentList;
    }
    
    

      public static Collection<Fragment> parseTreeToFragments(
            String inputParsedFilePath,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Set<String>  dependencies, String tgtPosHead,int limit, int lengthLimit) throws Exception {
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputParsedFilePath);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();
            //ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            ProcessDependencyParses.normalizeNumbers(nextSentence);
            
            FragmentGeneration fg = new FragmentGeneration(
                    sentence, tgtLemmaSet,
                    lemmaSetToFilter, 
                    dependencies,
                
                    tgtPosHead);
            
            List<Fragment> fragments = fg.getFragments();
            for (Fragment f : fragments) {
                if (lengthLimit > 0 && f.getTerminals().size() < lengthLimit) {
                    fragmentList.add(f);
                }
            }

            if (limit > 0 && fragmentList.size() > limit) {
                break;
            }
        }
        return fragmentList;
    }
    
      /**
       * Get distribution of length for fragments
       * @param fragCol
       * @return 
       */
    public static Map<Integer, Integer> getFragmentLengthDist(Collection<Fragment> fragCol){
        Map<Integer,Integer> frqDistMap = new HashMap<>();
        fragCol.iterator().forEachRemaining(fr->{
            List<String> lexicalizedSequence = fr.getLexicalizedSequence();
            int len= lexicalizedSequence.size();
            if(frqDistMap.containsKey(len)){
                Integer get = frqDistMap.get(len);
                get++;
                frqDistMap.put(len, get);
            }else{
                frqDistMap.put(len, 1);
            }
        });
        return frqDistMap;
    }

    public static List<Fragment> parseTreeToFragments(
            String inputParsedFilePath,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int maxInstanceNum, int lengthLimit, String goldDataPath) throws Exception {
        List<Fragment> inputFramentList = 
                HelperFragmentMethods.parseTreeToFragments(
                        inputParsedFilePath, tgtLemmaSet, 
                        lemmaSetToFilter, settings, maxInstanceNum, lengthLimit);
       
        Map<String, Fragment> idGoldFragmentMap = HelperFragmentIOUtils.loadFragmentsIDMap(goldDataPath);
       
        Iterator<Fragment> iterator = inputFramentList.iterator();
        while (iterator.hasNext()) {
            Fragment next = iterator.next();
            if (!idGoldFragmentMap.containsKey(next.getEvaluationID())) {
                iterator.remove();
            }
        }
        return inputFramentList;
    }

    /**
     * To compelted
     *
     * @param ifg
     * @param inputParsedFilePath
     * @param tgtLemmaSet
     * @param lemmaSetToFilter
     * @param settings
     * @param limit
     * @return
     * @throws Exception
     */
    public static List<Fragment> parseTreeToFragments(
            FragmentGeneration ifg,
            String inputParsedFilePath,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit) throws Exception {
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputParsedFilePath);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();
            ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
//            FragmentGeneration fg = new FragmentGeneration(
//                    sentence, tgtLemmaSet, lemmaSetToFilter, settings.getActiveDepr(),
//                    settings.getTargetPos());
            List<Fragment> fragments = ifg.getFragments();
            //List<Fragment> fragments = fg.getFragments();
            fragmentList.addAll(fragments);
            if (limit > 0 && fragmentList.size() > limit) {
                break;
            }
        }
        return fragmentList;
    }

    public static void printFragments(List<Fragment> inputFramentList, String inputFragmentToLearnFromPath) throws FileNotFoundException, IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(inputFragmentToLearnFromPath)), StandardCharsets.UTF_8);
        for (Fragment f : inputFramentList) {
            writer.append(f.toStringPosition()).append("\n");
        }
        writer.close();
    }

    public static List<Fragment> mixGoldDataWithRandomFagments(List<Fragment> gold, List<Fragment> all, int sizeToAdd) {

        Random r = new Random();
        List<Fragment> allForThisTime = new ArrayList<>(gold);
        for (int i = 0; i < sizeToAdd; i++) {
            int nextInt = r.nextInt(all.size());
            allForThisTime.add(all.get(nextInt));

        }

        return allForThisTime;

    }

    public static Map<Long, Fragment> fragmentIDMap(Collection<Fragment> fragList) {
        // given that ids are unique
        Map<Long, Fragment> idFragList = new ConcurrentHashMap<>();
        fragList.parallelStream().forEach(f -> {
            long evaluationID = f.getLongID();
            idFragList.put(evaluationID, f);
        });
        return idFragList;

    }

    public static Map<String, Collection<Fragment>> partitionFragmentsByHeadVerb(
            Collection<Fragment> fragmentCollection) {
        Map<String, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        //Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        fragmentCollection.parallelStream().forEach( frag -> {
            long fID = frag.getLongID();
            String head = frag.getHead();
           // if(parseListToIDMap.containsKey(fID)){
            //ParseFrame get = parseListToIDMap.get(fID);
            //int frameNumber = get.getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(head)) {
                clusterPartitionFragments.get(head).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(head, fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
           // }  
        });

        return clusterPartitionFragments;
    }
    
    
   
    
    public static Map<Integer, Collection<Fragment>> partitionFragmentsByHeadVerbIntKWithHash(
            Collection<Fragment> fragmentCollection) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        //Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        
        fragmentCollection.parallelStream().forEach( frag -> {
            long fID = frag.getLongID();
            String head = frag.getHead();
           // if(parseListToIDMap.containsKey(fID)){
            //ParseFrame get = parseListToIDMap.get(fID);
            //int frameNumber = get.getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(head.hashCode())) {
                clusterPartitionFragments.get(head.hashCode()).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(head.hashCode(), fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
           // }  
        });

        return clusterPartitionFragments;
    }
    
    public static Map<Integer, Collection<Fragment>> partitionFragmentsByHeadVerbIntoBuckets(
            Collection<Fragment> fragmentCollection, int numberOfCLusters) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        //Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        
        fragmentCollection.parallelStream().forEach( frag -> {
            long fID = frag.getLongID();
            String head = frag.getHead();
            // if(parseListToIDMap.containsKey(fID)){
            //ParseFrame get = parseListToIDMap.get(fID);
            //int frameNumber = get.getFrameClusterNumber();
            int hash = head.hashCode()%numberOfCLusters;
            if (clusterPartitionFragments.containsKey(hash)) {
                clusterPartitionFragments.get(hash).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(hash, fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
           // }  
        });

        return clusterPartitionFragments;
    }
    
    
    public static Map<Integer, Collection<Fragment>> partitionFragmentsByClusters(
            Collection<Fragment> fragmentCollection, Collection<ParseFrame> pfList) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        fragmentCollection.parallelStream().forEach( frag -> {
            long fID = frag.getLongID();
            if(parseListToIDMap.containsKey(fID)){
            ParseFrame get = parseListToIDMap.get(fID);
            int frameNumber = get.getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(frameNumber)) {
                clusterPartitionFragments.get(frameNumber).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber, fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
            }  
        });

        return clusterPartitionFragments;
    }
    
    public static Map<Integer, Collection<Fragment>> partitionFragmentsByClusters(
            Collection<Fragment> fragmentCollection, Map<Long, ParseFrame> parseListToIDMap) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        fragmentCollection.parallelStream().forEach( frag -> {
            long fID = frag.getLongID();
            ParseFrame get = parseListToIDMap.get(fID);
            int frameNumber = get.getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(frameNumber)) {
                clusterPartitionFragments.get(frameNumber).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber, fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
        });

        return clusterPartitionFragments;
    }
    
    
public static Map<Integer, Collection<Fragment>> partitionFragmentsByClustersMap(
        Collection<Fragment> fragmentCollection, Map<Long, ParseFrame> parseListToIDMap) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        // = HelperParseChartIO.parseListToIDMap(pfList);
        fragmentCollection.parallelStream()
                .forEach(frag -> {
            long fID = frag.getLongID();
            int frameNumber = parseListToIDMap.get(fID).getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(frameNumber)) {
                clusterPartitionFragments.get(frameNumber).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber, fLis);
                if(putIfAbsent!=null){
                    putIfAbsent.add(frag);
                }
            }
        });

        return clusterPartitionFragments;
    }
    public static Map<String, Collection<Fragment>> partitionFragmentsByClustersToMapList(
            Collection<Fragment> fragmentCollection, Collection<ParseFrame> pfList) {
       
        Map<String, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        
        // fragmentCollection.parallelStream().forEach(frag -> {
        for (Fragment frag : fragmentCollection) {            
            long fID = frag.getLongID();
            ParseFrame get = parseListToIDMap.get(fID);
            if (get == null) {
                System.out.println("<<dk " + fID + " " + frag.toStringPosition("nonm"));
            }
            int frameNumber = parseListToIDMap.get(fID).getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(frameNumber + "")) {
                clusterPartitionFragments.get(frameNumber + "").add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber + "", fLis);
                if (putIfAbsent != null) {
                    putIfAbsent.add(frag);
                }
            }
        }
//);

        return clusterPartitionFragments;
    }

    /**
     * fragments are not in the parses are removed from cluster
     * @param fragmentCollection
     * @param pfList
     * @return 
     */
//     public static Map<String, Collection<Fragment>> partitionFragmentsByClustersToMapListPartialSpan(
//            Collection<Fragment> fragmentCollection, Collection<ParseFrame> pfList) {
//        Map<String, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
//        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
//        fragmentCollection.parallelStream().forEach(frag -> {
//            long fID = frag.getLongID();
//            int frameNumber = parseListToIDMap.get(fID).getFrameClusterNumber();
//            if (clusterPartitionFragments.containsKey(frameNumber+"")) {
//                clusterPartitionFragments.get(frameNumber+"").add(frag);
//            } else {
//                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
//                fLis.add(frag);
//                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber+"", fLis);
//                if(putIfAbsent!=null){
//                putIfAbsent.add(frag);}
//            }
//        });
//
//        return clusterPartitionFragments;
//    }
//   
     
     public static Map<String, Collection<Fragment>> partitionFragmentsByClustersToMapListPartialSpan(
            Collection<Fragment> fragmentCollection, Collection<ParseFrame> pfList) {
        Map<String, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        // this can be written in many ways
        fragmentCollection.parallelStream().forEach(frag -> {
            long fID = frag.getLongID();
            ParseFrame get = parseListToIDMap.get(fID);
            int frameNumber =-1;
            if (get != null) {
                frameNumber = get.getFrameClusterNumber();
            } 
            if (clusterPartitionFragments.containsKey(frameNumber+"")) {
                clusterPartitionFragments.get(frameNumber+"").add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber+"", fLis);
                if(putIfAbsent!=null){
                putIfAbsent.add(frag);}
            }
            
        });

        return clusterPartitionFragments;
    }
    
     public static Map<Integer, Collection<Fragment>> partitionFragmentsByClustersToMapListIntKey(
            Collection<Fragment> fragmentCollection, Collection<ParseFrame> pfList) {
        Map<Integer, Collection<Fragment>> clusterPartitionFragments = new ConcurrentHashMap<>();
        Map<Long, ParseFrame> parseListToIDMap = HelperParseChartIO.parseListToIDMap(pfList);
        fragmentCollection.stream().unordered().parallel().forEach(frag -> {
            long fID = frag.getLongID();
            int frameNumber = parseListToIDMap.get(fID).getFrameClusterNumber();
            if (clusterPartitionFragments.containsKey(frameNumber)) {
                clusterPartitionFragments.get(frameNumber).add(frag);
            } else {
                Collection<Fragment> fLis = new ConcurrentLinkedQueue<>();
                fLis.add(frag);
                Collection<Fragment> putIfAbsent = clusterPartitionFragments.putIfAbsent(frameNumber, fLis);
                if(putIfAbsent!=null){
                    putIfAbsent.add(frag);
                }
            }
        }
        );

        return clusterPartitionFragments;
    }
     
    public static Map<Integer, Collection<Fragment>> mergeFragmentMaps(
            Map<Integer, Collection<Fragment>> map1, Map<Integer, Collection<Fragment>> map2) {
        

        Map<Integer, Collection<Fragment>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
        for(Collection<Fragment> e: map1.values()){
            
            mapMrged.put(clusterCounter, e);
            clusterCounter++;
        }
        for(Collection<Fragment> e: map2.values()){
             
            mapMrged.put(clusterCounter, e);
            clusterCounter++;
        }
        return mapMrged;
        
    }


public static Map<Integer, Collection<Fragment>> mergeFragmentMaps(
           Collection< Map<Integer, Collection<Fragment>>> mapCollection) {
        

        Map<Integer, Collection<Fragment>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
       for( Map<Integer, Collection<Fragment>> map1 :mapCollection){
        for(Collection<Fragment> e: map1.values()){
            
            mapMrged.put(clusterCounter, e);
            clusterCounter++;
        }
       }
        
        return mapMrged;
        
    }    
//    public static Map<String, List<Fragment>> mergeFragmentMaps(
//            List<Map<String, List<Fragment>>> splittedFragments) {
//
//        Map<String, List<Fragment>> mapMrged = new HashMap<>();
//        int clusterCounter = 0;
//        for (Map<String, List<Fragment>> fgMap : splittedFragments) {
//            for (List<Fragment> e : fgMap.values()) {
//
//                mapMrged.put("" + clusterCounter, e);
//                clusterCounter++;
//            }
//        }
//
//        return mapMrged;
//
//    }
    
    public static Map<Integer, Collection<Fragment>> mergeFragmentFromHirResult(
            Collection<HierachyBuilderResultWarpper> splittedFragments2In) {

        Map<Integer, Collection<Fragment>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
       // for (List<HierachyBuilderResultWarpper> entryList : splittedFragments2In) {
            for (HierachyBuilderResultWarpper e : splittedFragments2In) {
                Map<Integer, Collection<Fragment>> fgMap = e.getPartitionFragmentsByClusters();
                for (Collection<Fragment> l : fgMap.values()) {

                    mapMrged.put( clusterCounter, new ArrayList<>(l));
                    clusterCounter++;
                }
            }
       // }

        return mapMrged;

    }
    
     public static Map<String, Collection<Fragment>> mergeFragmentMapsFromHirResult(
            Collection<Collection<HierachyBuilderResultWarpper>> splittedFragments2In) {

         Map<String, Collection<Fragment>> mapMrged = new ConcurrentHashMap<>();
         AtomicInteger clusterCounter = new AtomicInteger();
         //for (Collection<HierachyBuilderResultWarpper> entryList : splittedFragments2In) {
         splittedFragments2In.parallelStream().forEach(entryList -> {
             entryList.parallelStream().forEach(e->{
             //for (HierachyBuilderResultWarpper e : entryList) {
                 Map<Integer, Collection<Fragment>> fgMap = e.getPartitionFragmentsByClusters();
                 for (Collection<Fragment> fc : fgMap.values()) {

                     mapMrged.put("" + clusterCounter.incrementAndGet(), fc);

                 }
             }
             );
         });

         return mapMrged;

    }
     public static Map<Integer, Collection<Fragment>> mergeFragmentMapsFromHirResultWithIntKey(
            Collection<Collection<HierachyBuilderResultWarpper>> splittedFragments2In) {

        Map<Integer, Collection<Fragment>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
        for (Collection<HierachyBuilderResultWarpper> entryList : splittedFragments2In) {
            for (HierachyBuilderResultWarpper e : entryList) {
                Map<Integer, Collection<Fragment>> fgMap = e.getPartitionFragmentsByClusters();
                for (Collection<Fragment> l : fgMap.values()) {

                    mapMrged.put( clusterCounter, l);
                    clusterCounter++;
                }
            }
        }

        return mapMrged;

    }
    public static Map<String, Collection<Fragment>> mergeFragmentMapsFromListHirResult(
            List<HierachyBuilderResultWarpper> splittedFragments2In) {

        Map<String, Collection<Fragment>> mapMrged = new HashMap<>();
        int clusterCounter = 0;
        
            for (HierachyBuilderResultWarpper e : splittedFragments2In) {
                Map<Integer, Collection<Fragment>> fgMap = e.getPartitionFragmentsByClusters();
                for (Collection<Fragment> l : fgMap.values()) {

                    mapMrged.put("" + clusterCounter, new ArrayList<>(l));
                    clusterCounter++;
                }
            }
        

        return mapMrged;

    }


    public static Map<String, Collection<Fragment>> creatFragmentClusterMap(
            Collection<Fragment> flattenFragmentCollections, 
            Map<Integer, Collection<ParseFrame>> clusteredParses) {
        
         Map<String, Collection<Fragment>> outputCluster = new HashMap<>();
        Map<Long, Fragment> fragmentIDMap = fragmentIDMap(flattenFragmentCollections);
        for(int cluster: clusteredParses.keySet()){
            List<Fragment> fgL = new ArrayList<>();
            outputCluster.put(cluster+"", fgL);
            for(ParseFrame pf: clusteredParses.get(cluster)){
                Long uniqueIntID = pf.getUniqueIntID();
                Fragment get = fragmentIDMap.get(uniqueIntID);
                fgL.add(get);
            }
            
        }
         return outputCluster;
    }

    public static Map<String, Collection<Fragment>> convertKeyType(Map<Integer, Collection<Fragment>> frgmntsClustered) {
        Map<String, Collection<Fragment>> frgmntsClusteredKeyStr = new HashMap<>();
        frgmntsClustered.entrySet().forEach(keyInt ->{
            frgmntsClusteredKeyStr.put(keyInt.getKey()+"",  new ArrayList<>(keyInt.getValue()));
        });
        return frgmntsClusteredKeyStr;
    }
    
     public static Map<Integer, Collection<Fragment>> convertKeyTypeInt(Map<String, Collection<Fragment>> frgmntsClustered) {
        Map<Integer, Collection<Fragment>> frgmntsClusteredKeyStr = new HashMap<>();
        frgmntsClustered.entrySet().forEach(keyInt ->{
            frgmntsClusteredKeyStr.put(Integer.parseInt(keyInt.getKey()),  new ArrayList<>(keyInt.getValue()));
        });
        return frgmntsClusteredKeyStr;
    }

    /**
     * sample from clusters by simple size cut-off
     * @param fragmentClusters
     * @param maxSize
     * @return 
     */
    public static Map<String, Collection<Fragment>> sampleClusters(Map<String, Collection<Fragment>> fragmentClusters, int maxSize) {
        Map<String, Collection<Fragment>> fragmentClustersSampled = new ConcurrentHashMap<>();
        Random rnd = new Random();
        fragmentClusters.entrySet().parallelStream().forEach(entry -> {
            String key = entry.getKey();
            Collection<Fragment> value = entry.getValue();
            int size = value.size();
            if(size<maxSize){
                fragmentClustersSampled.put(key, value);
            }else{
                boolean done = false;
                Collection<Fragment> fgTo = new ArrayList<>();
                Collection<Fragment> removed = new ArrayList<>();
                int counter =0;
                int removeSize = 0;
                Iterator<Fragment> iterator = value.iterator();
                while(iterator.hasNext()){
                    Fragment next = iterator.next();
                    if(rnd.nextBoolean()){
                        fgTo.add(next);
                        counter++;
                        
                    }else{
                        removeSize++;
                        removed.add(next);
                    }
                    if(removeSize==maxSize){
                      fragmentClustersSampled.put(key, removed);
                      done = true;
                      break;
                    }else if(counter==maxSize){
                      fragmentClustersSampled.put(key, fgTo);
                      done = true;
                      break;
                    }
                }
                if (!done) {
                    int toAddFromRem = maxSize - fgTo.size();
                    Iterator<Fragment> iteratorRem = removed.iterator();
                    while (toAddFromRem != 0&& iteratorRem.hasNext()) {
                        Fragment next = iteratorRem.next();
                        fgTo.add(next);
                        toAddFromRem--;
                    }
                    fragmentClustersSampled.put(key, fgTo);
                }
                
               //  int counter = 0;
           
            }
          
        }
        
        );
        return fragmentClustersSampled;

    }

     public static Map<String, Collection<Fragment>> sampleClustersProportional(
             Map<String, Collection<Fragment>> fragmentClusters, double proportion) {
     //    System.err.println("Sampling proportion is " + proportion);
         if(proportion==1.0){
             return fragmentClusters;
         }
        Map<String, Collection<Fragment>> fragmentClustersSampled = new ConcurrentHashMap<>();
        
         fragmentClusters.entrySet().parallelStream().unordered().forEach(entry -> {
             String key = entry.getKey();
             List<Fragment> value = new ArrayList<>(entry.getValue());
             int size = value.size();
             int maxSize = (int) Math.floor(Math.max(Math.floor(size * proportion), 2));
             Collections.shuffle(value, new Random(0));

             fragmentClustersSampled.put(key, value.subList(0, Math.min(size, maxSize)));
   

            //  int counter = 0;
           
            
          
        }
        
        );
        return fragmentClustersSampled;

    }

     
  
     public static Map<String, Collection<Fragment>> sampleClustersProportionalO(
             Map<String, Collection<Fragment>> fragmentClusters, double proportion) {
     //    System.err.println("Sampling proportion is " + proportion);
         if(proportion==1.0){
             return fragmentClusters;
         }
        Map<String, Collection<Fragment>> fragmentClustersSampled = new ConcurrentHashMap<>();
        Random rnd = new Random();
        fragmentClusters.entrySet().parallelStream().forEach(entry -> {
            String key = entry.getKey();
            Collection<Fragment> value = entry.getValue();
            
            int size = value.size();
            int maxSize =  (int) Math.floor(Math.max(Math.floor(size*proportion), Math.min(2, size)));
           
                boolean done = false;
                Collection<Fragment> fgTo = new ArrayList<>();
                Collection<Fragment> removed = new ConcurrentLinkedDeque<>();
                int counter =0;
                int removeSize = 0;
                Iterator<Fragment> iterator = value.iterator();
      
//int characteristics = Spliterator.CONCURRENT;
//             Spliterator<Fragment> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
//
//        boolean parallel = false;
//        Stream<Fragment> stream = StreamSupport.stream(spliterator, true);
            while (iterator.hasNext()) {
                Fragment next = iterator.next();

                if (rnd.nextDouble() < .3) {
                    fgTo.add(next);
                    counter++;

                } else {
                    removeSize++;
                    removed.add(next);
                }
                if (removeSize >= maxSize) {
                    fragmentClustersSampled.put(key, removed);
                    done = true;
                    break;
                } else if (counter >= maxSize) {
                    fragmentClustersSampled.put(key, fgTo);
                    done = true;
                    break;
                }
            }
            if (!done) {
                int toAddFromRem = maxSize - fgTo.size();
                while (toAddFromRem > 0) {
                    Iterator<Fragment> iteratorRem = removed.iterator();
                    while (toAddFromRem > 0 && iteratorRem.hasNext()) {
                        Fragment next = iteratorRem.next();
                        if (rnd.nextBoolean()) {
                            fgTo.add(next);
                            toAddFromRem--;
                        }
                    }
                }
                fragmentClustersSampled.put(key, fgTo);
            }

            //  int counter = 0;
           
            
          
        }
        
        );
        return fragmentClustersSampled;

    }

     
      public static Map<String, Collection<Fragment>> sampleClustersProportional(Map<String, Collection<Fragment>> fragmentClusters, double proportion, Set<Long> idsUsedToFilter) {
           if(proportion==1.0){
             return fragmentClusters;
         }
          if(idsUsedToFilter==null || idsUsedToFilter.isEmpty()){
              return sampleClustersProportional(fragmentClusters, proportion);
          }
        
        Map<String, Collection<Fragment>> fragmentClustersSampled = new ConcurrentHashMap<>();
        Random rnd = new Random();
        fragmentClusters.entrySet().parallelStream().forEach(entry -> {
            String key = entry.getKey();
            Collection<Fragment> value = new ArrayList<>( entry.getValue());
          //  System.err.println("------------ Before filter " + value.size());
            Iterator<Fragment> iteratorFilter = value.iterator();
            while(iteratorFilter.hasNext()){
                long longID = iteratorFilter.next().getLongID();
                if(idsUsedToFilter.contains(longID)){
                    iteratorFilter.remove();
                }
            }
            
            int size = value.size();
            // System.err.println("--------------- After filter " + value.size());
            if(size!=0){
            int maxSize =  (int) Math.floor(Math.max(Math.floor(size*proportion), 1));
           
                boolean done = false;
                Collection<Fragment> fgTo = new ArrayList<>();
                Collection<Fragment> removed = new ArrayList<>();
                int counter =0;
                int removeSize = 0;
                Iterator<Fragment> iterator = value.iterator();
                
                while(iterator.hasNext()){
                    Fragment next = iterator.next();
                    if(rnd.nextDouble()<.3){
                        fgTo.add(next);
                        counter++;
                        
                    }else{
                        removeSize++;
                        removed.add(next);
                    }
                    if(removeSize==maxSize){
                      fragmentClustersSampled.put(key, removed);
                      done = true;
                      break;
                    }else if(counter==maxSize){
                      fragmentClustersSampled.put(key, fgTo);
                      done = true;
                      break;
                    }
                }
               if (!done) {
                int toAddFromRem = maxSize - fgTo.size();
                while (toAddFromRem > 0) {
                    Iterator<Fragment> iteratorRem = removed.iterator();
                    while (toAddFromRem > 0 && iteratorRem.hasNext()) {
                        Fragment next = iteratorRem.next();
                        if (rnd.nextBoolean()) {
                            fgTo.add(next);
                            toAddFromRem--;
                        }
                    }
                }
                fragmentClustersSampled.put(key, fgTo);
            }}

               //  int counter = 0;
           
            
          
        }
        
        );
        return fragmentClustersSampled;

    }

      /**
       * Modify the D_root for the head by appending the given integer to the end of symbol, as a method to assert/distinguish some information known for the given fragment collection, e.g., their previously assigned senses
       * @param key
       * @param value 
       */
    public static void modifyHeadRoot(int key, Collection<Fragment> value) {
        value.forEach(f->{
            f.modifyHeadRelationDepency(key);
        });
    }

    /**
     * Create a map from the given collection, the keys are the evaluation id for the fragment
     * @param inputFramentList
     * @return 
     */
    public static Map<String, Fragment> toEvaluationIDMap(Collection<Fragment> inputFramentList) {
        Map<String,Fragment> idMap = new HashMap<>();
        Iterator<Fragment> iterator = inputFramentList.iterator();
        while(iterator.hasNext()){
            Fragment next = iterator.next();
            String evaluationID = next.getEvaluationID();
            idMap.put(evaluationID,next);
        }
        return idMap;
    }

    public static Map<String, Collection<Fragment>> fragmentsAsMapWithOneKey(Collection<Fragment> loadFragments) {
       Map<String, Collection<Fragment>> map = new ConcurrentHashMap<>();
       map.put("0",loadFragments);
       return map;
               
    }

    
     public static List<Fragment> vertical2Fragments(
            String inputVerticalFile,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit
            , int notused) throws Exception{
     return vertical2Fragments(inputVerticalFile, tgtLemmaSet, lemmaSetToFilter, settings, limit, notused,null);
     }
    /**
     * inital attempt to eliminate syntax
     * @param inputVerticalFile
     * @param tgtLemmaSet
     * @param lemmaSetToFilter
     * @param settings
     * @param limit
     * @param notused
     * @param goldDataPath
     * @return
     * @throws Exception 
     */
     public static List<Fragment> vertical2Fragments(
            String inputVerticalFile,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit
            , int notused, String goldDataPath
    ) throws Exception {
      
       
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputVerticalFile);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();
            
            //ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            FragmentGenerationNoStx fg = new FragmentGenerationNoStx(
                    sentence, tgtLemmaSet, lemmaSetToFilter, settings.getActiveDepr(),
                    settings.getTargetPos());
            List<Fragment> fragments = fg.getFragments();
            fragmentList.addAll(fragments);
            if (limit > 0 && fragmentList.size() > limit) {
                break;
            }
        }
        
         if (goldDataPath != null) {
             Map<String, Fragment> idGoldFragmentMap = HelperFragmentIOUtils.loadFragmentsIDMap(goldDataPath);
             Iterator<Fragment> iterator = fragmentList.iterator();
             while (iterator.hasNext()) {
                 Fragment next = iterator.next();
                 if (!idGoldFragmentMap.containsKey(next.getEvaluationID())) {
                     iterator.remove();
                 }
             }
         }
        
        return fragmentList;
    }

     
      public static List<Fragment> parse2SyntxOnlyFragments(
            String inputVerticalFile,
            Set<String> tgtLemmaSet,
            Set<String> lemmaSetToFilter,
            Settings settings, int limit
            , int notused, String goldDataPath
    ) throws Exception {
      
       
        List<Fragment> fragmentList = new ArrayList<>();
        ParsedFileReader vrf = new ParsedFileReader(inputVerticalFile);
        Sentence sentence;
        while ((sentence = vrf.getNextSentence()) != null) {
            
            List<TerminalToken> nextSentence = sentence.getSentenceTerminals();
            
            //ProcessDependencyParses.normalizePronouns(nextSentence);
            ProcessDependencyParses.normalizeDepTypes(nextSentence);
            FragmentGenerationOnlySyntax fg = new FragmentGenerationOnlySyntax(
                    sentence, tgtLemmaSet, lemmaSetToFilter, settings.getActiveDepr(),
                    settings.getTargetPos());
            List<Fragment> fragments = fg.getFragments();
           
            fragmentList.addAll(fragments);
            if (limit > 0 && fragmentList.size() > limit) {
              
                break;
            }
        }
        
         
        Map<String, Fragment> idGoldFragmentMap = HelperFragmentIOUtils.loadFragmentsIDMap(goldDataPath);
        Iterator<Fragment> iterator = fragmentList.iterator();
        while (iterator.hasNext()) {
            Fragment next = iterator.next();
            if (!idGoldFragmentMap.containsKey(next.getEvaluationID())) {
                iterator.remove();
            }
        }
        
        return fragmentList;
    }

    public static Map<String, Integer> getArgumentFeature(Collection<Fragment> fg) {
        Map<String, Integer> mp = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        fg.forEach(s -> {
        
            Collection<String> lexicalizedSequence = s.getDependencyStringList(false);
            lexicalizedSequence.forEach(lexicalSeq -> {
                mp.put(lexicalSeq, ai.incrementAndGet());
            });

        });
        return mp;
    }

      
   
    
}
