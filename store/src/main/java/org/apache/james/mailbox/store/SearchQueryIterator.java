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
package org.apache.james.mailbox.store;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;

/**
 * {@link Iterator} implementation which use a {@link MessageSearches} instance to lazy 
 * match the elements of a wrapped {@link Iterator} against a {@link SearchQuery}. This class should be used
 * if the store implementation can not filter the {@link Iterator} fore-hand against the {@link SearchQuery}
 * 
 *
 */
public final class SearchQueryIterator implements Iterator<Long>{

    private final MessageSearches searches = new MessageSearches();
    private Iterator<MailboxMembership<?>> it;
    private SearchQuery query;
    private Long next;
    
    public SearchQueryIterator(Iterator<MailboxMembership<?>> it, SearchQuery query) {
        this(it, query, null);
    }

    public SearchQueryIterator(Iterator<MailboxMembership<?>> it, SearchQuery query, Log log) {
        this.it = it;
        this.query = query;
        if (log != null) {
            searches.setLog(log);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {       
        // check if we already did the lazy loading 
        if (next == null) {
            while (it.hasNext()) {
                MailboxMembership<?> nextMembership = it.next();
                try {
                    if (searches.isMatch(query, nextMembership)) {
                        next = nextMembership.getUid();
                        return true;
                    } else {
                        next = null;
                    }
                } catch (MailboxException e) {
                    searches.getLog().info("Cannot test message against search criteria. Will continue to test other messages.", e);
                    if (searches.getLog().isDebugEnabled())
                        searches.getLog().debug("UID: " + nextMembership.getUid());
                }
            }
            return false;
        } else {
            return true;
        }
    }


    /**
     * Return the next Uid of the {@link MailboxMembership} which matched the 
     * {@link SearchQuery}
     * 
     * @return uid
     */
    public Long next() {
        if (hasNext()) {
            Long copy = next;
            next = null;
            return copy;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Not supported.
     * 
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException("Read-only Iterator");
    }

}
