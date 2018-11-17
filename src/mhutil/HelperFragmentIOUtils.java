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

import frameinduction.settings.Settings;
import input.preprocess.objects.DepandantNode;
import input.preprocess.objects.Fragment;

import input.preprocess.objects.TerminalType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import mhutil.HelperFragmentMethods;


/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class HelperFragmentIOUtils {

    /**
     * List fragment from a single file
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static List<Fragment> loadFragmentsFromInnerFile(File file) throws IOException {
        //System.out.println("Adding instances from file " + file.toPath().toString());
        List<Fragment> fragmentList = new ArrayList<>();
        //AtomicInteger ai = new AtomicInteger();
        try (Stream<String> lines = Files.lines(file.toPath())) {
            lines.forEach(line -> {
                if (line.trim().length() > 0) {
                    // System.out.println(line);
                    Fragment fromTextLine = Fragment.fromTextLine(line.trim());
//                if (fromTextLine.getTerminals().size() < 4 || fromTextLine.getTerminals().size() > 11) {
//                    System.err.println("!Removed from input:  " + fromTextLine.toStringPosition());
//                    ai.addAndGet(1);
//                } else {
fragmentList.add(fromTextLine);
//          }
                }
            });
        }
      //  System.err.println("\t\t* Removed " + ai + " instances from " + file.toString());
        return fragmentList;
    }

    /**
     * List fragment from a single file
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static List<Fragment> loadFragmentsFromInnerFile(File file, Set<String> lemmaSet) throws IOException {
        // System.out.println("Adding instances from file " + file.toPath().toString());
        List<Fragment> fragmentList = new ArrayList<>();
        AtomicInteger ai = new AtomicInteger();
        Stream<String> lines = Files.lines(file.toPath());
        lines.forEach(line -> {
            if (line.trim().length() > 0) {
                // System.out.println(line);
                Fragment fromTextLine = Fragment.fromTextLine(line.trim());
                if (lemmaSet == null || lemmaSet.contains(fromTextLine.getHead())) {
                    if (fromTextLine.getTerminals().size() < 4 || fromTextLine.getTerminals().size() > 11) {
                        //System.err.println("!Removed from input:  " + fromTextLine.toStringPosition());
                        ai.addAndGet(1);
                    } else {
                        fragmentList.add(fromTextLine);
                    }
                }
            }
        });
        lines.close();
        //   System.err.println("\t\t* Removed " + ai + " instances from " + file.toString());
        return fragmentList;
    }

    
    /**
     * Extract fragments, dump them in the specified file and also return the extracted list
     * @param settings
     * @param inputParseFile
     * @param path
     * @return
     * @throws Exception 
     */
    public static List<Fragment> generateDumpRawFragmentsToFile(
            Settings settings, 
            String inputParseFile,
            String path) throws Exception{
    
        
        
        List<Fragment> parseTreeToFragments = 
                HelperFragmentMethods.parseTreeToFragments(
                      inputParseFile, null, null, settings , -1);
        
       
        PrintWriter simpleFragment = new PrintWriter(new FileWriter(new File(path)));
        
        
        parseTreeToFragments.forEach(pf->{
            String toStringPosition = pf.toStringPosition(pf.getHead());
            simpleFragment.println(toStringPosition.toLowerCase());
        });
        
        simpleFragment.close();
        return parseTreeToFragments;
    }
    /**
     * Load fragments from a file or files within a directory
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static List<Fragment> loadFragments(String file) throws IOException {
        List<Fragment> fragmentList = new ArrayList<>();
        File f = new File(file);
        if (f.isFile()) {
            List<Fragment> fragList = loadFragmentsFromInnerFile(f);
            fragmentList.addAll(fragList);
        } else {
            for (File fi : f.listFiles()) {
                List<Fragment> fragList = loadFragmentsFromInnerFile(fi);
                fragmentList.addAll(fragList);
            }

        }
        return fragmentList;
    }
    
       
      /**
       * Load and filter by the gold file
       * @param fragmentFile
       * @param gold
       * @return 
       */
    public static List<Fragment> loadFragments(String fragmentFile, String gold) throws IOException {
        Stream<String> lines = Files.lines(new File(gold).toPath());
        Set<String> keys = new HashSet<>();
        lines.forEach((String l)->{
            String[] split = l.split(" ");
            String key = split[0]+" "+split[1];
            keys.add(key);
        });
        lines.close();
        System.err.println("Gold key size is " + keys.size());
        List<Fragment> fragmentList = new ArrayList<>();
        File f = new File(fragmentFile);
        if (f.isFile()) {
            List<Fragment> fragList = loadFragmentsFromInnerFile(f);
            fragList.forEach(fI -> {
                String evaluationID = fI.getEvaluationID();
                if (keys.contains(evaluationID)) {

                    fragmentList.add(fI);
                }
            });

        } else {
            for (File fi : f.listFiles()) {
                List<Fragment> fragList = loadFragmentsFromInnerFile(fi);
                fragList.forEach(fI -> {
                String evaluationID = fI.getEvaluationID();
                if (keys.contains(evaluationID)) {

                    fragmentList.add(fI);
                }
            });
            }

        }
        return fragmentList;
    }

    /**
     * Replace the role at the given position; replace it with the head and build a model again
     * Note that the policy for choosing roles at a position can be changed with something like roles with syntactic type to a head
     */
    public static Collection<Fragment> transformFragmentsWithRaH(Collection<Fragment> fragments, int positionArgument) throws IOException {
        List<Fragment> fragmentList = new ArrayList<>();
        fragments.forEach(f->{
            List<DepandantNode> terminals = f.getTerminals();
            List<DepandantNode> newDepNodes = new ArrayList<>(terminals);
            
            DepandantNode removeLexical = newDepNodes.remove(positionArgument+1);
            DepandantNode removeType = newDepNodes.remove(positionArgument+1);
            TerminalType type = removeLexical.getType();
            TerminalType tpHEad = newDepNodes.get(0).getType();
            
            DepandantNode headType = newDepNodes.remove(1);
            newDepNodes.add(0,removeLexical);
            newDepNodes.add(1,headType);
            newDepNodes.add(3,removeType);
            Fragment newFragment = new Fragment(newDepNodes,f.getEvaluationID());
            System.err.println(newFragment.toStringTrype());
            fragmentList.add(newFragment);
//            int size = terminals.size();
//            int sizeOfArguments = size%Fragment.DEP_NODE_PER_WORD;
//            System.err.println(sizeOfArguments);
//            for (int i = 0; i < sizeOfArguments; i++) {
//                DepandantNode dn = new DepandantNode(null, TerminalType.HEAD, size)
//                for (int j = i; j < Fragment.DEP_NODE_PER_WORD; j++) {
//                    
//                    
//                }
//                
//            }
//            terminals.forEach(dn->{
//                
//            });
            System.err.println("--");
        });
        return fragmentList;
    }

    public static Map<String, Fragment> loadFragmentsIDMap(String file) throws IOException {
        Map<String, Fragment> fragmentList = new ConcurrentHashMap<>();
        File f = new File(file);
        if (f.isFile()) {
            List<Fragment> fragList = loadFragmentsFromInnerFile(f);
            for (Fragment fragment : fragList) {
                fragmentList.put(fragment.getEvaluationID(), fragment);
            }
        } else {
            for (File fi : f.listFiles()) {
                List<Fragment> fragList = loadFragmentsFromInnerFile(fi);
                for (Fragment fragment : fragList) {
                    fragmentList.put(fragment.getEvaluationID(), fragment);
                }

            }

        }
        return fragmentList;
    }

    
    
    public static List<Fragment> loadFragments(String file, Set<String> lemma) throws IOException {
        List<Fragment> fragmentList = new ArrayList<>();
        File f = new File(file);
        if (f.isFile()) {
            List<Fragment> fragList = loadFragmentsFromInnerFile(f, lemma);
            fragmentList.addAll(fragList);
        } else {
            for (File fi : f.listFiles()) {
                List<Fragment> fragList = loadFragmentsFromInnerFile(fi, lemma);
                fragmentList.addAll(fragList);
            }

        }

        return fragmentList;
    }

    public static Map<String, List<Fragment>> fragmentListTOMap(List<Fragment> fragList) {
        Map<String, List<Fragment>> fragmentMap = new ConcurrentHashMap<>();
        for (Fragment f : fragList) {
            String terminalString = f.getTerminals().get(0).getTerminalString();
            if (fragmentMap.containsKey(terminalString)) {
                fragmentMap.get(terminalString).add(f);
            } else {
                List<Fragment> newList = new ArrayList<>();
                newList.add(f);
                fragmentMap.put(terminalString, newList);
            }
        }
        return fragmentMap;
    }

    public static Map<String, List<String>> loadVerbLocationMap(String vlocFileLocation) throws IOException {
        List<String> readAllLines = Files.readAllLines(Paths.get(vlocFileLocation));
        Map<String, List<String>> verbLocationMap = new HashMap<>();
        readAllLines.forEach(line -> {
            String[] sp = line.split(" ");
            String verb = sp[3];
            String verbLocInSent = sp[2];
            String id = sp[0].split("/")[2].split(".mrg")[0] + "-" + sp[1] + " " + sp[2];
            if (verbLocationMap.containsKey(verb)) {
                verbLocationMap.get(verb).add(id);
            } else {
                List<String> ids = new ArrayList<>();
                ids.add(id);
                verbLocationMap.put(verb, ids);
            }
        });
        //System.out.println(verbLocationMap.size());
        return verbLocationMap;
    }

    public static Set<String> loadLexicalHeadLemmaSet(String file) throws IOException {
        Set<String> readAllLines = ConcurrentHashMap.newKeySet();
        Stream<String> lines = Files.lines(Paths.get(file));
        lines.parallel().forEach(line -> {
            readAllLines.add(line.trim());
        });
lines.close();
        return readAllLines;
    }
    
    public static Map<String, Collection<Fragment>> loadParseFramesAsFragmentsToClusterMap(File file) throws IOException {
        // System.out.println("Adding instances from file " + file.toPath().toString());
        Map<String, Collection<Fragment>> clusterMapFragmentList = new HashMap<>();
        //   new ArrayList<>();
        // AtomicInteger ai = new AtomicInteger();
        Stream<String> lines = Files.lines(file.toPath());
        lines.forEach(line -> {
            if (line.trim().length() > 0) {
                // System.out.println(line);
                Fragment fromTextLine = Fragment.fromTextLineOfParsedFrame(line.trim());
                String cluster = line.split(" ")[2].split("\\.")[1];
                
                if (fromTextLine.getTerminals().size() < 4 || fromTextLine.getTerminals().size() > 11) {
                    //System.err.println("!Removed from input:  " + fromTextLine.toStringPosition());
                   // ai.addAndGet(1);
                } else {
                    if (clusterMapFragmentList.containsKey(cluster)) {
                        clusterMapFragmentList.get(cluster).add(fromTextLine);

                    } else {
                        List<Fragment> fragmentList = new ArrayList<>();

                        fragmentList.add(fromTextLine);
                        Collection<Fragment> putIfAbsent = clusterMapFragmentList.putIfAbsent(cluster, fragmentList);
                        if (putIfAbsent != null) {
                            putIfAbsent.add(fromTextLine);
                        }
                    }
                }
            }

        });
        lines.close();
        //   System.err.println("\t\t* Removed " + ai + " instances from " + file.toString());
        return clusterMapFragmentList;
    }
    
    
}
