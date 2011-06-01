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

package org.apache.james.mailbox.store.search;

import java.util.Iterator;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
public interface MessageSearchIndex<Id> {

    /**
     * Add the {@link MailboxMembership} to the search index
     * 
     * @param mailbox
     * @param message
     * @throws MailboxException
     */
    public void add(MailboxSession session, Mailbox<Id> mailbox, Message<Id> message) throws MailboxException;
    
    /**
     * Update the Flags in the search index for the given {@link MessageRange} 
     * 
     * @param mailbox
     * @param range
     * @param flags
     * @throws MailboxException
     */
    public void update(MailboxSession session, Mailbox<Id> mailbox, MessageRange range, Flags flags) throws MailboxException;
    
    /**
     * Delete the data for the given {@link MessageRange} from the search index
     * 
     * @param mailbox
     * @param range
     * @throws MailboxException
     */
    public void delete(MailboxSession session, Mailbox<Id> mailbox, MessageRange range) throws MailboxException;

    /**
     * Return all uids of the previous indexed {@link MailboxMembership}'s which match the {@link SearchQuery}
     * 
     * @param mailbox
     * @param searchQuery
     * @return
     * @throws MailboxException
     */
    public Iterator<Long> search(MailboxSession session, Mailbox<Id> mailbox, SearchQuery searchQuery) throws MailboxException;
}
