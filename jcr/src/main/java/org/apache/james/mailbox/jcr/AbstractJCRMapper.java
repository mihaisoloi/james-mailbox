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
package org.apache.james.mailbox.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.store.transaction.TransactionalMapper;
import org.slf4j.Logger;

/**
 * Abstract Mapper base class for Level 1 Implementations of JCR. So no real transaction management is used. 
 * 
 * The Session.save() will get called on commit() method,  session.refresh(false) on rollback, and session.refresh(true) on begin()
 *
 */
public abstract class AbstractJCRMapper extends TransactionalMapper implements JCRImapConstants {
    public final static String MAILBOXES_PATH =  "mailboxes";

    private final MailboxSessionJCRRepository repository;
    protected final MailboxSession mSession;

    
    public AbstractJCRMapper(final MailboxSessionJCRRepository repository, MailboxSession mSession) {
        this.repository = repository;
        this.mSession = mSession;
    }

    /**
     * Return the logger
     * 
     * @return logger
     */
    protected Logger getLogger() {
        return mSession.getLog();
    }
    
    /**
     * Return the JCR Session
     * 
     * @return session
     */
    protected Session getSession() throws RepositoryException{
        return repository.login(mSession);
    }

    /**
     * Begin is not supported by level 1 JCR implementations, however we refresh the session
     */
    protected void begin() throws MailboxException {  
        try {
            getSession().refresh(true);
        } catch (RepositoryException e) {
            // do nothin on refresh
        }
        // Do nothing
    }

    /**
     * Just call save on the underlying JCR Session, because level 1 JCR implementation does not offer Transactions
     */
    protected void commit() throws MailboxException {
        try {
            if (getSession().hasPendingChanges()) {
                getSession().save();
            }
        } catch (RepositoryException e) {
            throw new MailboxException("Unable to commit", e);
        }
    }

    /**
     * Rollback is not supported by level 1 JCR implementations, so just do nothing
     */
    protected void rollback() throws MailboxException {
        try {
            // just refresh session and discard all pending changes
            getSession().refresh(false);
        } catch (RepositoryException e) {
            // just catch on rollback by now
        }
    }

    /**
     * Logout from open JCR Session
     */
    public void endRequest() {
       repository.logout(mSession);
    }

}
