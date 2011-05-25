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

package org.apache.james.mailbox.inmemory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.inmemory.mail.model.InMemoryMailbox;
import org.apache.james.mailbox.inmemory.mail.model.SimpleHeader;
import org.apache.james.mailbox.inmemory.mail.model.SimpleMailboxMembership;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

public class InMemoryStoreMessageManager extends StoreMessageManager<Long> {

    public InMemoryStoreMessageManager(MailboxSessionMapperFactory<Long> mapperFactory, MailboxEventDispatcher dispatcher, InMemoryMailbox mailbox) throws MailboxException {
        super(mapperFactory, dispatcher,mailbox);
    }
    
    @Override
    protected Header createHeader(int lineNumber, String name, String value) {
        return new SimpleHeader(name, lineNumber, value);
    }

    @Override
    protected Message<Long> createMessage(Date internalDate, int size, int bodyStartOctet, 
            InputStream  document, Flags flags, List<Header> headers, PropertyBuilder propertyBuilder) throws MailboxException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] byteContent;
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = document.read(buf)) != -1) {
                out.write(buf, 0, i);
            }
            byteContent = out.toByteArray();
            if (out != null)
                out.close();

        } catch (Exception e) {
            e.printStackTrace();
            byteContent = new byte[0];
        }
        InMemoryMailbox mailbox = (InMemoryMailbox) getMailboxEntity();
        return new SimpleMailboxMembership(internalDate, size, bodyStartOctet, byteContent, flags, headers, propertyBuilder, mailbox.getMailboxId());
    }
    
}
