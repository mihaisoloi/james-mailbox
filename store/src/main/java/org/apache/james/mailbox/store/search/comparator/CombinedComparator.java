/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mailbox.store.search.comparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.james.mailbox.SearchQuery.Sort;
import org.apache.james.mailbox.store.mail.model.Message;

/**
 * {@link Comparator} which takes a Array of other {@link Comparator}'s and use them to compare two {@link Message} instances till one of them
 * return <> 0
 *
 */
public class CombinedComparator implements Comparator<Message<?>>{

    private final Comparator<Message<?>>[] comparators;
    public CombinedComparator(Comparator<Message<?>>[] comparators) {
        if(comparators == null || comparators.length < 1) {
            throw new IllegalArgumentException();
        }
        this.comparators = comparators;
    }
    @Override
    public int compare(Message<?> o1, Message<?> o2) {
        int i = 0;
        for (int a = 0; a < comparators.length; a++) {
            i = comparators[a].compare(o1, o2);
            if (i != 0) {
                break;
            }
        }
        return i;
    }
    
    @SuppressWarnings("unchecked")
    public Comparator<Message<?>> create(List<Sort> sorts) {
        List<Comparator<?>> comps = new ArrayList<Comparator<?>>();
        for (int i = 0; i < sorts.size(); i++) {
            Sort sort = sorts.get(i);
            boolean reverse = sort.isReverse();
            Comparator<Message<?>> comparator = null;
            
            switch (sort.getSortClause()) {
            case Arrival:
                comparator = InternalDateComparator.internalDate(reverse);
                break;
            case Cc:
                comparator = HeaderMailboxComparator.cc(reverse);
                break;
            case From:
                comparator = HeaderMailboxComparator.from(reverse);
                break;
            case Size:
                comparator = SizeComparator.size(reverse);
                break;
            case Subject:
                // TODO: fix me
                break;
            case To:
                comparator = HeaderMailboxComparator.to(reverse);
                break;
            case Uid:
                comparator = UidComparator.uid(reverse);
                break;
            default:
                break;
            }
            if (comparator != null) {
                comps.add(comparator);
            }
        }
        return new CombinedComparator(comps.toArray(new Comparator[0]));
    }

}
