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

/**
 * The {@link MailboxPathLocker} is responsible to help to synchronize the access to a {@link MailboxPath}
 * and execute an given {@link LockAwareExecution}
 *
 */
public interface MailboxPathLocker {
    
    
    /**
     * Execute the {@link LockAwareExecution} while holding a lock on the {@link MailboxPath}
     * 
     * @param session
     * @param path
     * @param execution
     * @throws MailboxException
     */
    public void executeWithLock(MailboxSession session, MailboxPath path, LockAwareExecution execution) throws MailboxException;
    
    
    /**
     * Execute code while holding a lock
     * 
     *
     */
    public interface LockAwareExecution {
        
        /**
         * Execute code block
         * 
         * @param session
         * @param path
         * @throws MailboxException
         */
        public void execute(MailboxSession session, MailboxPath path) throws MailboxException;
    }

}
