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

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class CNFRule {
    //  private static final int NONE_TERMINAL_RULE= -1;

    final private int lhs;
    final private CNFRule rhs1;
    final private CNFRule rhs2;
    private int[] spanRowColStartEnd = null;
    private double prob;
   String positionInSent; // this is a redundant variable and can be computed from the rest, just to speed up the hacking this added

    public CNFRule(int lhs, CNFRule rhs1, CNFRule rhs2, double prob) {
        this.lhs = lhs;
        this.rhs1 = rhs1;
        this.rhs2 = rhs2;
        this.prob = prob;
    }

    public void setPositionInSent(String positionInSent) {
        this.positionInSent = positionInSent;
    }

    public String getPositionInSent() {
        return positionInSent;
    }

    public CNFRule(int lhs, int rhs, int row, int col, double prob) {

        this.lhs = lhs;
        this.rhs1 = new CNFRule(rhs, null, null, -1);
        this.rhs2 = null;
        spanRowColStartEnd = new int[]{row, col};
        this.prob = prob;
    }

    public boolean isTerminal() throws Exception {
        if (rhs1 != null && rhs2 != null) {
            return false;
            // the following is added to make sure all the assignments 
            // are done correctly
        } else if ((rhs1 != null && rhs2 == null)) {
            return false;
            // undary rule

        } else if (rhs1 == null && rhs2 != null) {
            throw new Exception("Error in rules, find the source of error");

        } else {
            return true;
        }
    }

//    public void setRootLHS(String rootLHS) {
//        this.rootLHS = rootLHS;
//    }
//
//    
//    public String getAncestorLHS() {
//        return rootLHS;
//    }
//    public void setSpan(int start, int end){
//        this.end  =end;
//        this.start =start;
//    }
    public int getSpanStart() {
        if (spanRowColStartEnd != null) {
            return spanRowColStartEnd[0];
        } else {
            return this.rhs1.getSpanStart();
        }
    }

    public int getEnd() {
        if (spanRowColStartEnd != null) {
            return spanRowColStartEnd[1];
        } else {
            return this.rhs1.getSpanStart();
        }
    }

    public int getSymbolLhs() {
        return lhs;
    }

    public CNFRule getRhs1() {
        return rhs1;
    }

    public CNFRule getRhs2() {
        return rhs2;
    }

//    public void setProb(double prob) {
//        this.prob = prob;
//    }

    public double getProb() {
        return prob;
    }

//    public int getRHSSize() {
//        return this.rhs.size();
//    }
//    public CNFRule getRHSFirstElement() {
//        return this.rhs.get(0);
//    }
//    public CNFRule getRHSSecondElement() {
//        return this.rhs.get(1);
//    }
//    public String getRhsCanonicalString() throws Exception {
//
//        if (this.rhs.size() == 1) {
//            return rhs.get(0).getSymbolLhs();
//        } else if (this.rhs.size() == 2) {
//            return SymbolFractory.getCombinedRuledStringRHS(rhs.get(0).getSymbolLhs(), rhs.get(1).getSymbolLhs());
//        } else {
//            throw new Exception("this.rhs.size()!=2 or 1");
//        }
//
//    }
    @Override
    public String toString() {
        String returnStr = new String();
        try {
            returnStr = "[" + getSpanStart() + "," + getEnd() + "] " + lhs + "⇒" + rhs1.getSymbolLhs() + " " + rhs2.getSymbolLhs() + " pr=" + prob;
            return returnStr;
        } catch (Exception ex) {

            StringBuilder sbRHS = new StringBuilder();
            sbRHS.append(this.getSymbolLhs()).append(rhs1.getSymbolLhs()).append(" ").append(rhs2.getSymbolLhs());
            returnStr = "problematic rule! " + sbRHS.toString() + " " + ex.toString();

            return returnStr;
        }
    }

//    public String toStringCompact() {
//        return "[" + getSpanStart() + "," + getEnd() + "] " + lhs;
//    }
//
//    public String toStringRule() throws Exception {
//
//        return lhs + "⇒" + getSpanStart() + " + " + getEnd();
//    }

    public String toStringRuleWithProb() throws Exception {

        NumberFormat f = new DecimalFormat("0.0E0");
        return lhs + "⇒" + rhs1.getSymbolLhs() + " + " + rhs2.getSymbolLhs() + " " + f.format(this.prob);
    }

//    @Override
//    public int compareTo(CNFRule t) {
//        int lsC = t.lhs.compareTo(this.lhs);
//        int rsC = t.rhs.compareTo(this.rhs);
//        if (lsC == 0 && rsC == 0) {
//            return 0;
//        } else if (lsC != 0) {
//            return lsC;
//        } else {
//            return rsC;
//        }
//    }
}
