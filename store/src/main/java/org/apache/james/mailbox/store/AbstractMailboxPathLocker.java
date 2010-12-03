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

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;


public abstract class AbstractMailboxPathLocker implements MailboxPathLocker{

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.MailboxPathLocker#executeWithLock(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.MailboxPath, org.apache.james.mailbox.store.MailboxPathLocker.LockAwareExecution)
     */
    public void executeWithLock(MailboxSession session, MailboxPath path, LockAwareExecution execution) throws MailboxException {
        try {
            lock(session, path);
            execution.execute(session, path);
        } finally {
            unlock(session, path);
        }
    }
    
    /**
     * Perform lock
     * 
     * @param session
     * @param path
     * @throws MailboxException
     */
    protected abstract void lock(MailboxSession session, MailboxPath path) throws MailboxException;

    /**
     * Release lock
     * 
     * @param session
     * @param path
     * @throws MailboxException
     */
    protected abstract void unlock(MailboxSession session, MailboxPath path) throws MailboxException;

}
