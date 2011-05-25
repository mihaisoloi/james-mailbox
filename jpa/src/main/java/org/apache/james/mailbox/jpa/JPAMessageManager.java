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
package org.apache.james.mailbox.jpa;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMessage;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * Abstract base class which should be used from JPA implementations.
 */
public class JPAMessageManager extends StoreMessageManager<Long> {
    
    public JPAMessageManager(JPAMailboxSessionMapperFactory mapperFactory, final MailboxEventDispatcher dispatcher,final Mailbox<Long> mailbox) throws MailboxException {
        super(mapperFactory, dispatcher, mailbox);     
    }
    
    @Override
    protected Message<Long> createMessage(Date internalDate, final int size, int bodyStartOctet, final InputStream document, 
            final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException{
        final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
        for (Header header: headers) {
            jpaHeaders.add((JPAHeader) header);
        }
        final Message<Long> message = new JPAMessage((JPAMailbox) getMailboxEntity(), internalDate, size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        return message;
    }
    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        final Header header = new JPAHeader(lineNumber, name, value);
        return header;
    }

    /**
     * Support user flags
     */
    @Override
    protected Flags getPermanentFlags(MailboxSession session) {
        Flags flags =  super.getPermanentFlags(session);
        flags.add(Flags.Flag.USER);
        return flags;
    }
    
}
