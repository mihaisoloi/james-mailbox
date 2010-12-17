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
package org.apache.james.mailbox.jcr.mail;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.MailboxSessionJCRRepository;
import org.apache.james.mailbox.jcr.mail.model.JCRMessage;
import org.apache.james.mailbox.store.mail.CachingUidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Lazy-fetch the uid of the last insert Message if needed
 * 
 *
 */
public class JCRCachingUidProvider extends CachingUidProvider<String>{

    private MailboxSessionJCRRepository repos;

    public JCRCachingUidProvider(final MailboxSessionJCRRepository repos) {
        this.repos = repos;
    }
    

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.CachingUidProvider#getLastUid(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long getLastUid(MailboxSession session, Mailbox<String> mailbox) throws MailboxException {
        Session s = null;
        try {
            s = repos.login(session);
            // we use order by because without it count will always be 0 in jackrabbit
            String queryString = "/jcr:root/" + ISO9075.encodePath(s.getNodeByIdentifier(mailbox.getMailboxId()).getPath()) + "//element(*,jamesMailbox:message) order by @" + JCRMessage.UID_PROPERTY + " descending";
            QueryManager manager = s.getWorkspace().getQueryManager();
            Query q = manager.createQuery(queryString, Query.XPATH);
            q.setLimit(1);
            QueryResult result = q.execute();
            NodeIterator nodes = result.getNodes();
            if (nodes.hasNext()) {
                return nodes.nextNode().getProperty(JCRMessage.UID_PROPERTY).getLong();
            }
            return 0;
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to count unseen messages in mailbox " + mailbox, e);
        }
    }

}
