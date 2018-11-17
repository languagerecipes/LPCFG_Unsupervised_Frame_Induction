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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author behra
 */
public class CollectionUtil {

     
    

    public static <T, U extends Collection<T>> List<U> splitCollectionBySize(final U collection, final int sizePerList) {
        if (collection == null) {
            throw new IllegalArgumentException("given collection may not be null");
        }

        if (sizePerList < 1) {
            throw new IllegalArgumentException("sizePerList must be at least 1");
        }

        Iterator<T> iterator = collection.iterator();
        List<U> resultList = new ArrayList<>();

        int counter = 0;
        try {
            U currentCollection = (U) collection.getClass().newInstance();
            while (iterator.hasNext()) {
                currentCollection.add(iterator.next());
                counter++;

                if (counter > sizePerList - 1) {
                    resultList.add(currentCollection);
                    currentCollection = (U) collection.getClass().newInstance();
                    counter = 0;
                }
            }
            if (!currentCollection.isEmpty()) {
                resultList.add(currentCollection);
            }

        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("could not create a instance of the given collection of type " + collection.getClass());
        }
        return resultList;
    }

}
