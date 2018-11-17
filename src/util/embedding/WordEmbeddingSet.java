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

import java.util.Map;

/**
 *
 * @author behra
 */
public class WordEmbeddingSet {
    
    private final Map<String, Integer> idMap;
    private Map<Integer,double[]> wordVectors;

    public WordEmbeddingSet(Map<String, Integer> idMap, Map<Integer, double[]> wordVectors) {
        this.idMap = idMap;
        this.wordVectors = wordVectors;
    }

    public Map<String, Integer> getIdMap() {
        return idMap;
    }

    public Map<Integer, double[]> getWordVectors() {
        return wordVectors;
    }

    public void freeWordVectors() {
        this.wordVectors = null;
        
    }

    
    
    
}
