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
package frameinduction.grammar.parse.io;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class SemRolePair {
 
    private String semRoleID;
    private String lexicalization;
    private String positionInSentence;

    public SemRolePair(String semRoleID, String lexicalization, String positionInSentence) {
        this.semRoleID = semRoleID;
        this.lexicalization = lexicalization;
        this.positionInSentence = positionInSentence;
    }

    public String getPositionInSentence() {
        return positionInSentence;
    }

    public void setPositionInSentence(String positionInSentence) {
        this.positionInSentence = positionInSentence;
    }

    
    
    public SemRolePair(String semRoleID, String lexicalization) {
        this.semRoleID = semRoleID;
        this.lexicalization = lexicalization;
    }

    public String getLexicalization() {
        return lexicalization;
    }

    public String getSemRoleID() {
        return semRoleID;
    }

    
    @Override
    public String toString() {
        return this.semRoleID+":"+this.lexicalization;
    }
    
    
    
    
}
