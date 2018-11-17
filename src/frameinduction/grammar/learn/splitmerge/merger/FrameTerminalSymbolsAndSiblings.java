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
package frameinduction.grammar.learn.splitmerge.merger;

import frameinduction.grammar.RuleMaps;
import frameinduction.grammar.SymbolFractory;
import static java.lang.System.in;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * give the set of symbol employed in the construct of a given frame with a
 * particular index
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class FrameTerminalSymbolsAndSiblings {

//    private static void get(int frameIdentifier, Set<String> dependencyRelations) {
//        SymbolFractory.getFrameRemainingArguments(frameIdentifier);
//        String headFrameSymbol = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
//        String headFrameSymbolLHS = SymbolFractory.getHeadFrameSymbol(frameIdentifier);
//        String rhs1 = SymbolFractory.getLhsForUnaryHeadRole(frameIdentifier);
//        String lhsVerb = SymbolFractory.getLhsForUnaryHeadRole(frameIdentifier);
//        for (String dep : dependencyRelations) {
//            //      for (int i = 0; i < semanticRoleCardinality; i++) {
//            String frameArgument1 = SymbolFractory.getFramesJSntxArgument(frameIdentifier, dep);
//        }
//
//    }
//    public FrameTerminalSymbolsAndSiblings(int fromFrameID, int toFrameID, RuleMaps rm, Set<String> dependencyRelations) {
//    }
    
    /**
     * make the mapping
     * @param fromFrameIdentifier
     * @param toFrameIdentifier
     * @param rm
     * @param dependencyRelations
     * @return
     * @throws Exception 
     */
    public static Map<Integer, Integer> getMap(int fromFrameIdentifier, int toFrameIdentifier, RuleMaps rm, Set<String> dependencyRelations) throws Exception {
        Map<Integer, Integer> mapping = new HashMap<>();
        // this method must be changed to something more general, all rules must be scanned to make surea that future indepandant splits do not give us conflic and orphan rules
        String frameRemainingArgumentsF = SymbolFractory.getFrameRemainingArguments(fromFrameIdentifier);
        String frameRemainingArgumentsT = SymbolFractory.getFrameRemainingArguments(toFrameIdentifier);
        updateMap(rm, mapping, frameRemainingArgumentsF, frameRemainingArgumentsT);

        String fHeadFrameSymbol = SymbolFractory.getHeadFrameSymbol(fromFrameIdentifier);
        String tHeadFrameSymbol = SymbolFractory.getHeadFrameSymbol(toFrameIdentifier);
        updateMap(rm, mapping, fHeadFrameSymbol, tHeadFrameSymbol);

        String fromLhsForUnaryHeadRole = SymbolFractory.getLhsForUnaryHeadRole(fromFrameIdentifier);
        String toLhsForUnaryHeadRole = SymbolFractory.getLhsForUnaryHeadRole(toFrameIdentifier);
        updateMap(rm, mapping, fromLhsForUnaryHeadRole, toLhsForUnaryHeadRole);
        for (String dep : dependencyRelations) {
            //      for (int i = 0; i < semanticRoleCardinality; i++) {
            String fromFrameArgument = SymbolFractory.getFramesJSntxArgument(fromFrameIdentifier, dep);
            String toFrameArgument = SymbolFractory.getFramesJSntxArgument(toFrameIdentifier, dep);
            updateMap(rm, mapping, fromFrameArgument, toFrameArgument);
        }
        return mapping;
    }

    private static void updateMap(RuleMaps rm, Map<Integer, Integer> mapping, String symbolFrom, String symbolTo) throws Exception {

        int idFromSymbol = rm.getIDFromSymbol(symbolFrom);
        int idTo = rm.getIDFromSymbol(symbolTo);
        mapping.put(idFromSymbol, idTo);
    }
}
