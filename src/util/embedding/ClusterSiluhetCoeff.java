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

/**
 *
 * @author behra
 */
public class ClusterSiluhetCoeff implements Comparable<ClusterSiluhetCoeff>{
    int cluseterID;
    double sil;
int clustSize;

    public ClusterSiluhetCoeff(int cluseterID, double sil, int clustSize) {
        this.cluseterID = cluseterID;
        this.sil = sil;
        this.clustSize = clustSize;
    }

    public ClusterSiluhetCoeff(int cluseterID, double sil) {
        this.cluseterID = cluseterID;
        this.sil = sil;
    }

    public int getCluseterID() {
        return cluseterID;
    }

    public double getSil() {
        return sil;
    }

    @Override
    public int compareTo(ClusterSiluhetCoeff t) {
        return Double.compare(this.sil,t.sil);
    }

    public int getClustSize() {
        return clustSize;
    }
    
    
            
}
