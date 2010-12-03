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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.commons.logging.Log;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.jcr.mail.model.JCRHeader;
import org.apache.james.mailbox.jcr.mail.model.JCRMailbox;
import org.apache.james.mailbox.jcr.mail.model.JCRMessage;
import org.apache.james.mailbox.store.MapperStoreMessageManager;
import org.apache.james.mailbox.store.UidProvider;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * JCR implementation of a {@link MapperStoreMessageManager}
 *
 */
public class JCRMessageManager extends MapperStoreMessageManager<String> {

    private final Log log;

    public JCRMessageManager(JCRMailboxSessionMapperFactory mapperFactory, final UidProvider<String> uidProvider,
            final MailboxEventDispatcher dispatcher, final JCRMailbox mailbox, final Log log, final char delimiter) throws MailboxException {
        super(mapperFactory, uidProvider, dispatcher, mailbox);
        this.log = log;
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new JCRHeader(lineNumber, name, value, log);
    }

    @Override
    protected MailboxMembership<String> createMessage(Date internalDate, int size, int bodyStartOctet, InputStream document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException{
        final List<JCRHeader> jcrHeaders = new ArrayList<JCRHeader>(headers.size());
        for (Header header: headers) {
            jcrHeaders.add((JCRHeader) header);
        }
        final MailboxMembership<String> message = new JCRMessage(getMailboxEntity().getMailboxId(), internalDate, 
                size, flags, document, bodyStartOctet, jcrHeaders, propertyBuilder, log);
        return message;
    }
}