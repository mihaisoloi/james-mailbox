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

package org.apache.james.mailbox.jpa.openjpa;


import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jpa.JPAMailboxManager;
import org.apache.james.mailbox.jpa.JPAMailboxSessionMapperFactory;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.MapperStoreMessageManager;
import org.apache.james.mailbox.store.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * OpenJPA implementation of MailboxManager
 *
 */
public class OpenJPAMailboxManager extends JPAMailboxManager {

    private boolean useStreaming;

    public OpenJPAMailboxManager(JPAMailboxSessionMapperFactory mapperFactory, Authenticator authenticator, UidProvider<Long> uidProvider, MailboxPathLocker locker, boolean useStreaming) {
        super(mapperFactory, authenticator, uidProvider, locker);
        this.useStreaming = useStreaming;
    }

    public OpenJPAMailboxManager(JPAMailboxSessionMapperFactory mapperFactory, Authenticator authenticator, UidProvider<Long> uidProvider) {
        this(mapperFactory, authenticator, uidProvider, new JVMMailboxPathLocker(), false);
    }

    @Override
    protected MapperStoreMessageManager<Long> createMessageManager(UidProvider<Long> uidProvider, MailboxEventDispatcher dispatcher, Mailbox<Long> mailboxRow, MailboxSession session) throws MailboxException {
        MapperStoreMessageManager<Long> result =  new OpenJPAMessageManager((JPAMailboxSessionMapperFactory) mailboxSessionMapperFactory, uidProvider, dispatcher, mailboxRow, useStreaming);
        return result;
    }
}
