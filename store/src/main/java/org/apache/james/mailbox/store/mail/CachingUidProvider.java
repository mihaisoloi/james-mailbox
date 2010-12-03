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
package org.apache.james.mailbox.store.mail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * {@link UidProvider} which lazy retrieve the last used uid from a {@link Mailbox} and then use a in-memory cache to handle 
 * futher increments.
 * 
 *
 * @param <Id>
 */
public abstract class CachingUidProvider<Id> implements UidProvider<Id>{

    private final Map<Id, AtomicLong> uids = new HashMap<Id, AtomicLong>();
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.UidProvider#nextUid(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long nextUid(MailboxSession session, Mailbox<Id> mailbox) throws MailboxException {       
        return retrieveLastUid(session, mailbox).incrementAndGet();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.UidProvider#lastUid(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long lastUid(MailboxSession session, Mailbox<Id> mailbox) throws MailboxException {
        return retrieveLastUid(session, mailbox).get();
    }

    /**
     * Retrieve the last uid for the {@link Mailbox} from cache or via lazy lookup.
     * 
     * @param session
     * @param mailbox
     * @return lastUid
     * @throws MailboxException
     */
    protected AtomicLong retrieveLastUid(MailboxSession session, Mailbox<Id> mailbox) throws MailboxException {
        AtomicLong uid;
        synchronized (uids) {
            uid = uids.get(mailbox.getMailboxId());
            if (uid == null) {
                uid = new AtomicLong(getLastUid(session, mailbox));
                uids.put(mailbox.getMailboxId(), uid);
            }
            
        }
        return uid;
    }
    
    /**
     * Return the last used uid for the given {@link Mailbox}. This method is called in a lazy fashion. So when the first uid is needed for a {@link Mailbox}
     * it will get called to get the last used. After that it will stored in memory and just increment there on each {@link #nextUid(MailboxSession, Mailbox)} call.
     * 
     * @param session
     * @param mailbox
     * @return lastUid
     * @throws MailboxException
     */
    protected abstract long getLastUid(MailboxSession session, Mailbox<Id> mailbox) throws MailboxException;

}
