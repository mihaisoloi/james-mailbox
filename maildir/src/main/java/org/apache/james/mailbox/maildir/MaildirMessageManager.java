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

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.maildir.mail.model.MaildirMessage;
import org.apache.james.mailbox.store.MailboxEventDispatcher;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
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
    protected Message<Integer> createMessage(Date internalDate,
            int size, int bodyStartOctet, SharedInputStream content, Flags flags, PropertyBuilder propertyBuilder)
            throws MailboxException {
        int headerEnd = bodyStartOctet -2;
        if (headerEnd < 0) {
            headerEnd = 0;
        }
        final Message<Integer> message = new MaildirMessage(getMailboxEntity(), internalDate, 
                size, flags, content, bodyStartOctet, propertyBuilder);
        return message;
    }

}
