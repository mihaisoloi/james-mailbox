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
import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMailboxMembership;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * Abstract base class which should be used from JPA 2.0 implementations
 */
public class JPAMessageManager extends StoreMessageManager<Long> {
    
    public JPAMessageManager(JPAMailboxSessionMapperFactory mapperFactory, UidProvider<Long> uidProvider,
            final MailboxEventDispatcher dispatcher,final Mailbox<Long> mailbox) throws MailboxException {
        super(mapperFactory, uidProvider, dispatcher, mailbox);     
    }
    
    @Override
    protected MailboxMembership<Long> createMessage(Date internalDate, final int size, int bodyStartOctet, final InputStream document, 
            final Flags flags, final List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException{
        final List<JPAHeader> jpaHeaders = new ArrayList<JPAHeader>(headers.size());
        for (Header header: headers) {
            jpaHeaders.add((JPAHeader) header);
        }
        final MailboxMembership<Long> message = new JPAMailboxMembership(getMailboxEntity().getMailboxId(), internalDate, size, flags, document, bodyStartOctet, jpaHeaders, propertyBuilder);
        return message;
    }

    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        final Header header = new JPAHeader(lineNumber, name, value);
        return header;
    }
    
}
