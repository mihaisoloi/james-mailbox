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
package org.apache.james.mailbox.maildir;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.maildir.mail.model.MaildirHeader;
import org.apache.james.mailbox.maildir.mail.model.MaildirMessage;
import org.apache.james.mailbox.store.MailboxEventDispatcher;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.search.MessageSearchIndex;

public class MaildirMessageManager extends StoreMessageManager<Integer> {

    public MaildirMessageManager(MessageMapperFactory<Integer> mapperFactory, MessageSearchIndex<Integer> index,  MailboxEventDispatcher<Integer> dispatcher, Mailbox<Integer> mailboxEntiy)
    throws MailboxException {
        super(mapperFactory, index, dispatcher, mailboxEntiy);
    }

    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new MaildirHeader(lineNumber, name, value);
    }

    @Override
    protected Message<Integer> createMessage(Date internalDate,
            int size, int bodyStartOctet, InputStream documentIn, Flags flags,
            List<Header> headers, PropertyBuilder propertyBuilder)
            throws MailboxException {
        final List<MaildirHeader> maildirHeaders = new ArrayList<MaildirHeader>(headers.size());
        for (Header header: headers) {
            maildirHeaders.add((MaildirHeader) header);
        }
        final Message<Integer> message = new MaildirMessage(getMailboxEntity(), internalDate, 
                size, flags, documentIn, bodyStartOctet, maildirHeaders, propertyBuilder);
        return message;
    }

}
