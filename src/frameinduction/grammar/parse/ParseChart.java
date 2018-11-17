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
package frameinduction.grammar.parse;

//import gnu.trove.map.hash.TIntObjectHashMap;
import frameinduction.grammar.RulesCounts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * a 3dimensional matrix: first is a non-terminal
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class ParseChart {

    String identifier; // important to use in the evaluation
    // the value of the map is alpha_A-i-j = P(A => w_i ... w_j | A)
    //Map<CNFRule, Map<Integer, Map<Integer,Set<> parseMat;
    private final Map<Integer, Map<Integer, List<CNFRule>>> parseMat;
    //Map<Integer, Map<Integer, List<CNFRule>>> parseMat;
    private final int nonterminalsNumber;
    

   
    
    
    
    //  String prettyPrintInput;

//    public String getPrettyPrintInput() {
//        return prettyPrintInput;
//    }
//
//    public void setPrettyPrintInput(String prettyPrintInput) {
//        this.prettyPrintInput = prettyPrintInput;
//    }
    public void setIdentifier(String identifier) {
      
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ParseChart(int nonterminalsNumber) {
        parseMat = new ConcurrentHashMap<>(nonterminalsNumber);
        this.nonterminalsNumber = nonterminalsNumber;
    }

    public int getNonterminalsNumber() {
        return nonterminalsNumber;
    }

//    public Map<Integer, Map<Integer, List<CNFRule>>> getParseMat() {
//        return parseMat;
//    }
    public void addValue(int start, int end, CNFRule cnfRule) {
        if (parseMat.containsKey(start)) {
            Map<Integer, List<CNFRule>> colMap = parseMat.get(start);
            if (colMap.containsKey(end)) {
                List<CNFRule> rulesAtThisPosition = colMap.get(end);
                rulesAtThisPosition.add(cnfRule);
            } else {
                List<CNFRule> rulesAtThisPosition = new ArrayList<>();
                rulesAtThisPosition.add(cnfRule);
                colMap.put(end, rulesAtThisPosition);
            }
        } else {
            Map<Integer, List<CNFRule>> colMap = new ConcurrentHashMap<>();
            List<CNFRule> rulesAtThisPosition = new ArrayList<>();
            rulesAtThisPosition.add(cnfRule);
            colMap.put(end, rulesAtThisPosition);
            parseMat.put(start, colMap);
        }
    }

    /**
     * return list of productions inserted at a position in the CYK Table
     *
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    public List<CNFRule> getCell(int start, int end) {
        List<CNFRule> rules = null;
        if (parseMat.containsKey(start)) {
            Map<Integer, List<CNFRule>> cols = parseMat.get(start);

            //if (cols.containsKey(end)) {
            rules = cols.get(end);
            // }
        }
        return rules;

    }

    public List<CNFRule> getCell(int start, int end, int targetSymbol) {
        List<CNFRule> cnfRules = null;
        if (parseMat.containsKey(start)) {
            Map<Integer, List<CNFRule>> cols = parseMat.get(start);
            if (cols.containsKey(end)) {
                List<CNFRule> rules = cols.get(end);
                cnfRules = new ArrayList<>();
                for (CNFRule rule : rules) {
                    if (rule.getSymbolLhs() == targetSymbol) {
                        cnfRules.add(rule);
                    }
                }
            }
        }
        return cnfRules;

    }

//    public List<CNFRule> getParseMatRules() {
//        List<CNFRule> list = new ArrayList();
//        for (int k : parseMat.keySet()) {
//            Map<Integer, List<CNFRule>> get = parseMat.get(k);
//            for (int e : get.keySet()) {
//                list.addAll(get.get(e));
//            }
//        }
//        return list;
//    }
//    public void serialiseParseMat() {
//        List<CNFRule> parseMatRules = getParseMatRules();
//        for (CNFRule cnf : parseMatRules) {
//            System.err.println(cnf);
//        }
//    }
//    public void printToAsciiTable(String name) throws Exception {
//        Object[][] tableRows = new String[nonterminalsNumber + 1][nonterminalsNumber + 1];
//        for (int i = 0; i < tableRows.length; i++) {
//            tableRows[0][i] = i + "";
//            tableRows[i][0] = i + "";
//        }
//
//        for (int i = 1; i < nonterminalsNumber + 1; i++) {
//            for (int j = 1; j < tableRows.length; j++) {
//                StringBuilder sb = new StringBuilder();
//                if (parseMat.containsKey(i)) {
//                    if (parseMat.get(i).containsKey(j)) {
//                        List<CNFRule> get = parseMat.get(i).get(j);
//
//                        for (CNFRule cnr : get) {
//                            sb.append(cnr.toStringRuleWithProb()).append("\n");
//                        }
//                    }
//                }
//                tableRows[i][j] = sb.toString().trim();
//            }
//        }
//        V2_AsciiTable at = new V2_AsciiTable();
//
//        System.err.println("Computed " + name);
//        for (int i = 0; i < tableRows.length; i++) {
//            at.addRule();
//            at.addRow(tableRows[i]);
//        }
//        at.addRule();
//        V2_AsciiTableRenderer rend = new V2_AsciiTableRenderer();
//        rend.setTheme(V2_E_TableThemes.PLAIN_7BIT_STRONG.get());
//        rend.setWidth(new WidthLongestLine());
//        RenderedTable rt = rend.render(at);
//        System.err.println(rt);
//
//    }
////    public void print() {
//        if (this.parseMat.size() < 1) {
//            System.err.println("empty alpha/cyk matrix");
//            return;
//        }
//
//        String[][] tableRows = new String[parseMat.get(1).size() + 1][parseMat.get(1).size() + 1];
//        for (int i = 0; i < tableRows.length; i++) {
//            tableRows[0][i] = i + "";
//            tableRows[i][0] = i + "";
//
//        }
//        Set<Integer> rows = parseMat.keySet();
//        System.err.println("** The Generated CYK Table **");
//        for (int row : rows) {
//
//            for (int col : parseMat.get(row).keySet()) {
//                List<CNFRule> get = parseMat.get(row).get(col);
//                StringBuilder sb = new StringBuilder();
//                for (CNFRule cnr : get) {
//                    sb.append(cnr.getSymbolLhs()).append(" ");
//                }
//                tableRows[row][col] = sb.toString().trim();
//
//            }
//
//        }
//
//        PrettyPrinter p = new PrettyPrinter(System.err, " ");
//        p.print(tableRows);
//
//    }
    /**
     * Print all the parse trees for this input (all combination of the chain of
     * production rules used for this parse)
     *
     * @param startSymbol
     * @param probMaptoWriteInitialProbs : this is used only for printing the
     * initially assigned probs
     * @throws Exception
     */
    public void printParses() throws Exception {
        if (this.parseMat.size() < 1) {
            System.err.println("no parse available");
            return;
        }
        System.out.println(parseMat.keySet());
        System.out.println(nonterminalsNumber);
        List<CNFRule> get = parseMat.get(0).get(nonterminalsNumber);
        System.err.println("===");
        System.err.println("Parse Trees (employed re-write rules)");
        System.err.println("===");
        System.err.println(get.size());
        int option = 0;
        for (CNFRule cf : get) {
            System.err.println("* Option number " + ++option);
            System.err.println("---");
            printTree(cf, "  ");
            System.err.println("---");
        }
        System.err.println("===");
    }

    private void printTree(CNFRule cnf, String tabbing) throws Exception {
        if (cnf == null || cnf.isTerminal()) {
            return;
        }
        //if (cnf != null) {
        System.err.print(tabbing + cnf.getSymbolLhs());
        CNFRule rhs1 = cnf.getRhs1();
        CNFRule rhs2 = cnf.getRhs2();
        if (rhs1 != null) {
            System.err.print("⇒");
            // for (CNFRule cr : rhss) {
            System.err.print(rhs1.getSymbolLhs() + " ");

            if (rhs2 != null) {
                System.err.print(rhs2.getSymbolLhs() + " ");

            }
        }
        System.err.println(" pr(" + cnf.getProb() + ")");
        tabbing += "↳ \t";

        printTree(rhs1, tabbing);
        printTree(rhs2, tabbing);
    }

    //}
    /**
     * Count the number of valid parses for this fragment
     *
     * @param startSymbolID
     * @return
     */
    public boolean hasParse(int startSymbolID) {
        if (this.parseMat.size() > 1) {
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber); // I am not sure what must be the start index 
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    return true;
                }
            }
        }
        return false;
    }

    public double sumProbValidParses(int startSymbolID) {
        double probSum = 0;

        if (this.parseMat.size() > 1) {
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);
            //int countValid = 0;
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    // countValid++;
                    double prob = startCell.getProb();
                    probSum += Math.exp(prob);
                    //Math.exp(prob);
                    //  probSum= MathUtil.logSumExp( probSum,prob);
                }
            }
            //  System.err.println("count valid " + countValid);
        }
        return probSum;
    }

    public int countValidParse(int startSymbolID) {
        int count = 0;
        if (this.parseMat.size() > 1) {
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);

            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @param startSymbolID
     * @param TopN
     * @return 
     */
    public CNFRule getMostProbableParse(int startSymbolID) {
        if (this.parseMat.size() > 1) {
            //System.out.println("  "+parseMat.get(0).size() +"  vs "+ nonterminalsNumber);
            if(parseMat.containsKey(0) && parseMat.get(0).containsKey(nonterminalsNumber)){
            
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);
            Collections.sort(aParse, CNF_BY_PROB_COMP);
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    return startCell;
                }
            }
            }
            return null;
        } else {
            return null;
        }
    }

    public List<CNFRule> getSortedValidParses(int startSymbolID) {
        if (this.parseMat.size() > 1) {
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);
            List<CNFRule> validSortedParses = new ArrayList<>();
            Collections.sort(aParse, CNF_BY_PROB_COMP);
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    validSortedParses.add(startCell);
                }
            }
            return validSortedParses;
        } else {
            return null;
        }
    }

    
      public List<CNFRule> getValidParses(int startSymbolID) {
        if (this.parseMat.size() > 1) {
            Map<Integer, List<CNFRule>> get = parseMat.get(0);
            
            if (get==null || !get.containsKey(nonterminalsNumber)) {
                System.err.println("*** code832782 Returung null for this " + startSymbolID +" key is " + get);
                return null;
            }
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);
            List<CNFRule> validSortedParses = new ArrayList<>();
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    validSortedParses.add(startCell);
                }
            }
            return validSortedParses;
        } else {
            return null;
        }
    }
    /**
     * @param startSymbolID
     * @param TopN
     */
    public List<CNFRule> getTopNProbableParse(int startSymbolID, int n) {
        if (this.parseMat.size() > 1) {
            List<CNFRule> aParse = parseMat.get(0).get(nonterminalsNumber);
            Collections.sort(aParse, CNF_BY_PROB_COMP);
            List<CNFRule> parses = new ArrayList<>();
            int count = 0;
            for (CNFRule startCell : aParse) {
                if (startSymbolID == startCell.getSymbolLhs()) {
                    parses.add(startCell);
                    if (++count == n) {
                        return parses;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

//    public void printFlatParses(RulesParameters ruleParameters, PrintWriter pw) throws Exception {
//        if (this.parseMat.size() < 1) {
//            System.err.println("no partial parse available");
//            return;
//        }
//        //System.out.println(parseMat.keySet());
//        //System.out.println(nonterminalsNumber);
//        List<CNFRule> get = parseMat.get(1).get(nonterminalsNumber);
//        // System.err.println("===");
//        //System.err.println("Parse Trees (employed re-write rules)");
//        // System.err.println("===");
//        System.err.println(get.size());
//        int option = 0;
//        for (CNFRule cf : get) {
//            // System.err.println("* Option number " + ++option);
//            //System.err.println("---");
//            printFlatTree(cf, ruleParameters, pw);
//            pw.println();
//            // System.err.println("---");
//        }
//
//        // System.err.println("===");
//    }
//
//    private static final Comparator<CNFRule> CNF_BY_PROB_COMP = new Comparator<CNFRule>() {
//        @Override
//        public int compare(CNFRule t, CNFRule t1) {
//            return -Double.compare(t.getProb(), t1.getProb());
//        }
//    };
    private final Comparator<CNFRule> CNF_BY_PROB_COMP = (CNFRule t, CNFRule t1) -> -Double.compare(t.getProb(), t1.getProb());

    /**
     * recursively go through the CNF rules
     *
     * @param rc
     * @param rm
     * @param cnf
     * @param tabbing
     * @param probMap
     * @throws Exception
     */
//    private static void printTree(CNFRule cnf, String tabbing, RulesParameters ruleParameters) throws Exception {
//        NumberFormat formatter = new DecimalFormat("#0000000E0");
//        if (cnf != null) {
//            List<CNFRule> rhss = cnf.getRhs();
//            System.err.print(tabbing + cnf.getSymbolLhs());
//            if (rhss != null) {
//                System.err.print("⇒");
//                for (CNFRule cr : rhss) {
//                    System.err.print(cr.getSymbolLhs() + " ");
//                }
//                //System.err.println("lhs "+cnf.getSymbolLhs());
//                // System.err.println("rhs "+cnf.getRhsCanonicalString());
//                Double initProb = ruleParameters.getLhs2RhsParam(cnf.getSymbolLhs(), cnf.getRhsCanonicalString());
//                String prob = "error!!";
//                if (initProb != null) {
//                    prob = formatter.format(initProb);
//                }
////                if(cnf.getProb()>1.0){
////                    throw new Exception("no nono" + cnf.getProb());
////                }
//                System.err.println(" Production pr(" + formatter.format(cnf.getProb()) + ")  \u26A0 init rewrite pr is " + prob + "");
//                tabbing += "↳ \t";
//                for (CNFRule cr : rhss) {
//                    if (cr.getRhs() != null) {
//                        printTree(cr, tabbing, ruleParameters);
//                    }
//                }
//
//            }
//
//        }
//
//    }
//
//    private static void printFlatTree(CNFRule cnf, RulesParameters ruleParameters, PrintWriter pw) throws Exception {
//        //NumberFormat formatter = new DecimalFormat("#0000000E0");
//        if (cnf != null) {
//            List<CNFRule> rhss = cnf.getRhs();
//            pw.print(cnf.getSymbolLhs());
//            if (rhss != null) {
//                pw.print("->");
//                for (CNFRule cr : rhss) {
//                    pw.print(cr.getSymbolLhs() + " ");
//                }
//                pw.println();
//                //System.err.println("lhs "+cnf.getSymbolLhs());
//                // System.err.println("rhs "+cnf.getRhsCanonicalString());
//                //              Double initProb = ruleParameters.getLhs2RhsParam(cnf.getSymbolLhs(), cnf.getRhsCanonicalString());
//                //               String prob = "error!!";
////                if (initProb != null) {
////                    prob = formatter.format(initProb);
////                }
////                if(cnf.getProb()>1.0){
////                    throw new Exception("no nono" + cnf.getProb());
////                }
//                //     System.err.println(" Production pr(" + formatter.format(cnf.getProb()) + ")  \u26A0 init rewrite pr is " + prob + "");
//                //  tabbing += "↳ \t";
//                for (CNFRule cr : rhss) {
//                    if (cr.getRhs() != null) {
//                        printFlatTree(cr, ruleParameters, pw);
//                    }
//                }
//
//            }
//
//        }
//
//    }
//    public List<FrameStructure> generateFrameStructure1Output(int topN) throws Exception {
//        TreeMap<String, Set<String>> parseTerminals = new TreeMap();
//        List<FrameStructure> frameStruct = new ArrayList<>();
//        if (this.parseMat.size() > 1) {
//            List<CNFRule> aParse = parseMat.get(1).get(nonterminalsNumber);
//            Collections.sort(aParse, CNFByProb);
//
//            int option = 0;
//            for (int i = 0; i < topN; i++) {
//
//                CNFRule startCell = aParse.get(i);
//                //System.err.println(startCell.getProb());
//                if (startCell.getSymbolLhs().equals(SymbolFractory.getStartSymbol())) {
//
//                    //System.err.println(startCell.toStringRuleWithProb());
//                    int frameNumber = Integer.parseInt(startCell.getRHSFirstElement().getSymbolLhs().split("_")[1].split("\\^")[0]);
//                    //System.err.println("FN " + frameNumber);
//                    FrameStructure fs = new FrameStructure(frameNumber);
//                    getTreeTerminal(startCell, parseTerminals, fs);
//                    frameStruct.add(fs);
//
//                } else {
//                    System.err.println("***** it was not start symbol");
//                }
//
//            }
//
//        }
//        return frameStruct;
//    }
//
//    private static void getTreeTerminal(CNFRule cnf, Map<String, Set<String>> frameElements, FrameStructure fs) throws Exception {
//        if (cnf != null) {
//            List<CNFRule> rhss = cnf.getRhs();
//            //System.err.println("* " + cnf.getSymbolLhs());
//            //String currentHead= cnf.getSymbolLhs();
//            if (rhss != null) {
//
//                for (CNFRule cr : rhss) {
//
//                    if (cr.getRHSSize() == 2) {
//
//                        getTreeTerminal(cr, frameElements, fs);
//
//                    } else {
//                        // you were here
//                        //System.err.println(cr.getRHSSize()+ "! " + cr.toString());
//                        //System.err.println("* " + cnf.getSymbolLhs());
//                        //System.err.println("* " + cr.getSymbolLhs());
//
//                       // fs.addElementFromCNF(cr);
//                    }
//                }
//
//            }
//
//        }
//
//    }
//
//    private static void getTreeTerminal2(CNFRule cnf, Map<String, Set<String>> frameElements, FrameStructure fs) throws Exception {
//        if (cnf != null) {
//            List<CNFRule> rhss = cnf.getRhs();
//            //System.err.println("* " + cnf.getSymbolLhs());
//            //String currentHead= cnf.getSymbolLhs();
//            if (rhss != null) {
//
//                for (CNFRule cr : rhss) {
//
//                    if (cr.getRHSSize() == 2) {
//                        //String rootLHS = cr.getSymbolLhs();
//                      //  if (SymbolFractory.isAFramesArgument(fs.getFrameNumber(), rootLHS)) {
//                            fs.addArgumentFromCNF(cr);
//                        //} else {
//                            getTreeTerminal2(cr, frameElements, fs);
//                        //}
//
//                    } else {
//                         String lHS = cr.getSymbolLhs();
//                        // you were here
//                        //System.err.println(cr.getRHSSize()+ "! " + cr.toString());
//                        //System.err.println("* " + cnf.getSymbolLhs());
//                        //System.err.println("* " + cr.getSymbolLhs());
//                        if (!SymbolFractory.isUnaryDepHead(lHS)) {
//                            fs.addArgumentFromCNF(cr);
//                        }
//                    }
//                }
//
//            }
//
//        }
//
//    }
    /**
     * Get the inventory of rules in this parse chart. If the filter=true, then
     * only those that are used in valid (spanning) parses are collected
     *
     * @param rc
     * @param startSymbolID
     * @param filter
     * @throws Exception
     */
    public void getRules(RulesCounts rc, final int startSymbolID, boolean filter) throws Exception {

        if (this.parseMat.size() < 1) {
            System.err.println("no parse available");
            return;
        }
        List<CNFRule> get = parseMat.get(0).get(nonterminalsNumber);
        if (filter) {
            for (CNFRule cf : get) {
                if (cf.getSymbolLhs() == startSymbolID) {
                    getRules(cf, rc);
                }

            }
        } else {
            for (CNFRule cf : get) {
                getRules(cf, rc);

            }
        }

    }

    /**
     *
     * @param cnf
     * @param rc
     * @throws Exception
     */
    private static void getRules(CNFRule cnf, RulesCounts rc) throws Exception {
        if (cnf == null || cnf.isTerminal()) {
            return;
        }
        int symbolLhs = cnf.getSymbolLhs();
        CNFRule rhs1 = cnf.getRhs1();
        CNFRule rhs2 = cnf.getRhs2();
        if (rhs1 != null && rhs2 != null) {
            rc.incOWBinary(symbolLhs, rhs1.getSymbolLhs(), rhs2.getSymbolLhs(), cnf.getProb());
        } else if (rhs1 != null && rhs2 == null) {

            // System.out.println(rm.getSymbolFromID(rhs1.getSymbolLhs()));
            rc.incOWUnary(symbolLhs, rhs1.getSymbolLhs(), cnf.getProb());
        } else {
            throw new RuntimeException("WHAT ?! should not happen");
        }
        getRules(rhs1, rc);
        getRules(rhs2, rc);
    }

}
