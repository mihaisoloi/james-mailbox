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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.mail.model.JCRMailbox;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * JCR implementation of a MailboxManager
 * 
 */
public class JCRMailboxManager extends StoreMailboxManager<String> implements JCRImapConstants {

    private final JCRMailboxSessionMapperFactory mapperFactory;
    private final Log logger = LogFactory.getLog(JCRMailboxManager.class);
    
    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator, final UidProvider<String> uidProvider) {
	    this(mapperFactory, authenticator, uidProvider, new JVMMailboxPathLocker());
    }

    public JCRMailboxManager(JCRMailboxSessionMapperFactory mapperFactory, final Authenticator authenticator, final UidProvider<String> uidProvider, final MailboxPathLocker locker) {
        super(mapperFactory, authenticator, uidProvider, locker);
        this.mapperFactory = mapperFactory;
    }

    
    @Override
    protected StoreMessageManager<String> createMessageManager(UidProvider<String> uidProvider, MailboxEventDispatcher dispatcher, Mailbox<String> mailboxEntity, MailboxSession session) throws MailboxException{
        return new JCRMessageManager(mapperFactory, uidProvider, dispatcher, (JCRMailbox) mailboxEntity, logger, getDelimiter());
    }

    @Override
    protected Mailbox<String> doCreateMailbox(MailboxPath path, MailboxSession session) throws MailboxException {
        return new org.apache.james.mailbox.jcr.mail.model.JCRMailbox(path, randomUidValidity(), logger);
    }

}
