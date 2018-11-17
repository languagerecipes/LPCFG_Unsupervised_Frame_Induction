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

import java.util.regex.Pattern;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SymbolFractory {

//    public static String verbHeadDep(){
//        return "dep_verb_root";
//    }
    private static final String SPECIAL_SYMBOL_FOR_SPLIT_VAR_INDPNT = "$";
    private static final String FRAME_NODE_SYMBOL = "F";
    private static final String HEAD_IDENTIFIER = "H";
    private static final String SEM_REL_REMAINING_IDENTIFIER = "r";
    private static final String SEM_ROL_NODE_SYMBOL = "X";
    private static final String SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL = "R";
    private static final String SEM_ROL_TOPIC_NON_TEMINAL_NODE_SYMBOL = "T";
    private static final String PREPOSITION_NON_TEMINAL_NODE_SYMBOL = "P";
    private static final String NO_PREPOSITION_TERMINAL_SYMBOL = "NO_PREP";
    private static final String END_OF_SENTECE_SPECIAL_SYMBOL = "EOS";
    private static final String END_OF_SENTECE_SPECIAL_TEMINAL = "EOS_Terminal";

//     private static final String PROB_ONE_SPECIAL_SYMBOL ="FILLER_ONE";
//     private static final String PROB_ONE_SPECIAL_SYMBOL_TERMINAL ="FILLER_ONE_Terminal";
    private static final String SEM_ROLE_NOMINAL_MARKER = "n";
    private static final String VERB_LEXICAL_ROLE_MARKER = "v";

    
    
    public static String getEOSSymbol() {
        return END_OF_SENTECE_SPECIAL_SYMBOL;
    }
    
    
    public static String getNO_PREPOSITION_TERMINAL_SYMBOL() {
        return NO_PREPOSITION_TERMINAL_SYMBOL;
    }

    public static String getEOSTerminalSymb() {
        return END_OF_SENTECE_SPECIAL_TEMINAL;
    }
    
    public static String getPrepositionNonTerminalSymb(int index) {
        return PREPOSITION_NON_TEMINAL_NODE_SYMBOL+"_"+index;
    }
     
    public static String getPrepositionNonTerminalSymb(String type) {
        return PREPOSITION_NON_TEMINAL_NODE_SYMBOL+"_"+type;
    }
    
    
    public static String getFramesJSntxArgument(int frameIdentifier, String slotFillerDepe){
        return SEM_ROL_NODE_SYMBOL+ "_" + frameIdentifier + "^" + slotFillerDepe ;
    }
    
   
    
     public static String getDepRelJntSyntaxArg(String lhsNodeString) throws Exception {
        if(isAFramesJArgumentSyntax(lhsNodeString)){
            String dep = lhsNodeString.split("\\^")[1];
            
            return dep;
        }else{
            throw new Exception("miss use of method getFrameIndex");
        }
    } 
    
    public static int getFrameIndex(String lhsNodeString) throws Exception {
        if(isFrameHeadRule(lhsNodeString)){
            String valStr = lhsNodeString.split(FRAME_NODE_SYMBOL + "\\_")[1].split("\\^")[0];
            
            int parseInt = Integer.parseInt(valStr);
        //    System.err.println("** " + valStr +"   " + parseInt);
            return parseInt;
        }else{
            throw new Exception("miss use of method getFrameIndex");
        }
    } 
    
//     public static boolean isRoleLexicalisationRule(String symbol) {
//        if(sym bol.startsWith(SEM_ROL_NODE_SYMBOL+"_" )&& symbol.endsWith("^"+SEM_REL_REMAINING_IDENTIFIER)){
//            return true;
//        }else{
//            return false;
//        }
//    }
    public static boolean isLexicalisationRule(String symbol) {

        if (symbol.startsWith(SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL + "_")) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isFrameRemainingArguments(String symbol) {
        String symbolGeneral = symbol.split("\\"+SPECIAL_SYMBOL_FOR_SPLIT_VAR_INDPNT)[0];
        if(symbolGeneral.startsWith(SEM_ROL_NODE_SYMBOL+"_" )&& symbolGeneral.endsWith("^"+SEM_REL_REMAINING_IDENTIFIER)){
            return true;
        }else{
            return false;
        }
    }
    
    public static boolean isAFramesJArgumentSyntax(String lhsNodeString) {
        if (lhsNodeString.startsWith(SEM_ROL_NODE_SYMBOL + "_")
                && !isFrameRemainingArguments(lhsNodeString)) {
            return true;
        } else {
            return false;
        }
    }
    
      public static boolean isAFramesArgument(String lhsNodeString) {
        if (lhsNodeString.startsWith(SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL + "_")
                && !isFrameRemainingArguments(lhsNodeString)) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isASemanticRole(String lhsNodeString) {

        boolean match = lhsNodeString.matches(SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL + "_\\d+\\^"+SEM_ROLE_NOMINAL_MARKER);
        return match;
    }

    public static String getHeadFrameSymbol(int frameIndex) {
        return FRAME_NODE_SYMBOL + "_" + frameIndex + "^" + HEAD_IDENTIFIER;
    }
    
     public static String changeMainIndexOfSymbolTo(String symbol, int frameIndex) {
         String[] splt =symbol.split("\\_");
         String aftrt = splt[1].split("\\^")[1];
         
        return splt[0] + "_" + frameIndex + "^" + aftrt;
    }

//     public static String getHeadVerbNonTerminal(String verb) {
//        String rhs = "( " + verb + ",V )";
//        return rhs;
//    }
     
//    public static String getHeadVerbTerminal(String verb) {
//        String rhs = "(" + verb + ")";
//        return rhs;
//    }
    
 public static String getHeadVerbDependencyTerminal() {
        String rhs = "root";
        return rhs;
    }
    public static String getStartSymbol() {
        return "S";
    }

    public static String getFrameRemainingArguments(int frameIndex) {
        return SEM_ROL_NODE_SYMBOL+"_" + frameIndex+ "^"+SEM_REL_REMAINING_IDENTIFIER;
    }

//    public static String getFrameSlotFillerNonTerminalSymbol(int frameIndex, int semRoleIndex, String depType) {
//        return "F_" + frameIndex + "^{R_" + semRoleIndex + "^{" + depType + "}}";
//    }

//    public static String getSemanticRoleFillerTerminal(String terminalWord) {
//        String rhs = "(" + terminalWord + ")"; // add iPoS here 
//        return rhs;
//    }

//    public static String getTerminal(String headVerb) {
//        return "(" + headVerb + ",V)";
//    }

//     public static String[] getCombinedRuledStringRHS(String rule1, String rule2){
//         String[] combi = {rule1,rule2};
//        return  combi;
//    }
    
//    public static String[] splitCombinedRuledStringRHS(String rhs){
//        return rhs.split(" ");
//    }
    
    public static String getUnaryDepHead(String depType){
        return "D"+"_"+depType;
    }
    
    public static boolean isUnaryDepHead(String lhs){
        return lhs.startsWith("D"+"_");
    }
    
    public static String getLhsForUnaryHeadRole1(int si){
        return SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+VERB_LEXICAL_ROLE_MARKER;
    }
    public static String getUnaryLHSForNonVerbial(int si){
        return SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+SEM_ROLE_NOMINAL_MARKER;
    }
    
    
    public static String getLhsForUnaryHeadRole(int si){
        return SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+VERB_LEXICAL_ROLE_MARKER;
    }
    public static String getUnaryLHSForNonVerbialRole(int si){
        return SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+SEM_ROLE_NOMINAL_MARKER;
    }
    
    public static String getUnaryLHSForNonVerbial(int si, int cluster){
        return SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"_"+cluster+"^"+SEM_ROLE_NOMINAL_MARKER;
    }
    
    public static int getFrameIndexFromHeadUnary(String frameHeadLicaliztion) {
        int frameIdentifier = Integer.parseInt(frameHeadLicaliztion.split("\\_")[1].split("\\^")[0]);
        return frameIdentifier;
    }
    
     public static boolean isFrameHeadRule(String lhsNodeString) {
        if (lhsNodeString.startsWith(FRAME_NODE_SYMBOL + "_") && lhsNodeString.endsWith("^"+HEAD_IDENTIFIER)) {
            return true;
        } else {
            return false;
        }
    }
   
    public static boolean isLhsForUnaryHead(String symbol, int si){
        
        String expected = SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+VERB_LEXICAL_ROLE_MARKER;
         if(symbol.equals(expected)){
             return true;
         }else{
             String[] split = symbol.split("\\$");
             if(split[0].endsWith(expected)){
                 return true;
             }
             
             return false;
         }
        
    }
     public static boolean isLhsForSemanticRole(String symbol, int si){
         if(symbol.equals(SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL+"_"+si+"^"+SEM_ROLE_NOMINAL_MARKER)){
             return true;
         }else{
             return false;
         }
        
    }
     
    public static boolean isFrameRemainingArguments(String symbol, int frameIndex) {
        String tgtStr = SEM_ROL_NODE_SYMBOL + "_" + frameIndex + "^" + SEM_REL_REMAINING_IDENTIFIER;
        if (symbol.equals(tgtStr)) {
            return true;
        } else {
            String split = symbol.split("\\" + SPECIAL_SYMBOL_FOR_SPLIT_VAR_INDPNT)[0];
            if (split.equals(tgtStr)) {
                return true;
            }
            return false;
        }
    }
    
   public static boolean isFramesJSntxArgument(String symbol, int frameIdentifier){
         if(symbol.startsWith(SEM_ROL_NODE_SYMBOL+ "_" + frameIdentifier + "^")){
             return true;
        }  else{
             return false;
         }
    }
    
     public static boolean isHeadFrameSymbol(String symbol, int frameIndex) {
        if(symbol.equals(FRAME_NODE_SYMBOL + "_" + frameIndex + "^"+HEAD_IDENTIFIER)){
            return true;
        }else{
            return false;
        }
    }

    public static int getSymbolIndex(String symbol) {
     return Integer.parseInt(symbol.split("\\_")[1].split("\\^")[0]);
    }

    public static String replaceSymbolIndex(String symbol, int newIndex) {
     
    String returnVal =  symbol.replaceFirst("\\_(\\d)+\\^", "\\_"+newIndex+"\\^");
     return returnVal;
    }

    public static boolean hasDepStxSymbolSpltID(String symbolFromID) {
        if (isUnaryDepHead(symbolFromID)
               
                ){
          
        if( symbolFromID.contains("$")){
             // System.err.println("9897772");
            return true;
        }
        }
        return false;
    }
    
    public static int getDepStxLHSSplitCounterID(String symbolFromID) {
        if (hasDepStxSymbolSpltID(symbolFromID)) {
          //  System.err.println("--> " + symbolFromID);
            return Integer.parseInt(symbolFromID.split("\\$")[1]);
        } else {
            return -1;
        }
    }
    
     public static String getChangeDepStxSpltCounterID(String depStxLHSNodeToBeSplted, int theNewIDForDepStxNode) {
        if (hasDepStxSymbolSpltID(depStxLHSNodeToBeSplted)) {
            String newNode = depStxLHSNodeToBeSplted.split("\\$")[0]+"$"+theNewIDForDepStxNode;
            return newNode;
        } else {
            return depStxLHSNodeToBeSplted+"$"+theNewIDForDepStxNode;
        }
    }
     
     public static int getSymbolLocacSubstate(String symbol){
        String[] split = symbol.split("\\"+SPECIAL_SYMBOL_FOR_SPLIT_VAR_INDPNT);
        if(split.length==1){
             return -1;
        }else if(split.length==2){
            return Integer.parseInt(split[1]);
        }else{
            throw new RuntimeException("Substate in format not expected.");
        }
     }

    
    
     public static String changeSymbolLocacSubstateTo(String symbol, int newSubState){
        String[] split = symbol.split("\\"+SPECIAL_SYMBOL_FOR_SPLIT_VAR_INDPNT);
        if(split.length==1){
             String newNode = symbol+"$"+newSubState;
            return newNode;
        }else if(split.length==2){
             String newNode = split[0]+"$"+newSubState;
            return newNode;
        }else{
            throw new RuntimeException("Substate in format not expected.");
        }
     }

    /**
     * The method is supposed to check the category of given random variables
     *
     * @param a
     * @param b
     */
    public static boolean areTheSameCategory(String lhs1, String lhs2) {
        
        String lhs1Cat = lhs1.replaceAll("\\d", "");
        String lhs2Cat = lhs2.replaceAll("\\d", "");
        
        return lhs1Cat.equals(lhs2Cat);

    }
    
     public static String getGenCategory(String lhs1) {
        
        String lhs1Cat = lhs1.replaceAll("\\d", "");
        
        
        return lhs1Cat;

    }
     
//     public static void main(String[] args) {
//        boolean matches = Pattern.matches(SEM_ROL_TO_NON_TEMINAL_NODE_SYMBOL + "_\\d+\\^" + SEM_ROLE_NOMINAL_MARKER, "customer");
//         System.out.println(matches);
//    }
}
