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
package util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * @author Behrang QasemiZadeh <zadeh at phil.hhu.de>
 */
public class MapUtils {

    /**
     * Print a map of maps
     *
     * @param <T>
     * @param map
     */
    public static <A, B, C> void printMap(Map<A, Map<B, C>> map) {
        for (A t : map.keySet()) {
            Map<B, C> get = map.get(t);
            for (B t2 : get.keySet()) {
                C val = get.get(t2);
                System.out.println(t + "\t" + t2 + "\t" + val);
            }
        }

    }

    /**
     * Got this bit of code from so, needs to be checked
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static <K, V> K getKeyOfLargestCollection(Map<K, Collection< V>> map) {
        int maxSize = Integer.MIN_VALUE;
        K key = null;
        for (Map.Entry<K, Collection<V>> e : map.entrySet()) {
            if (e.getValue().size() > maxSize) {
                key = e.getKey();
                maxSize = e.getValue().size();
            }
        }
        return key;
    }

    public static <T, X> int countInstances(Map<T, Set<X>> cluster) {
        AtomicInteger ai = new AtomicInteger();
        cluster.values().parallelStream().forEach(v -> {
            ai.addAndGet(v.size());
        });
        return ai.intValue();
    }
//
//    public static <K extends Comparable<? super K>, V> K getLargestKey(Map<K, V> parseListToClustterMap) {
//        K max = parseListToClustterMap.keySet().stream().max(K::compareTo).get();
//        return max;
//
//    }
}
