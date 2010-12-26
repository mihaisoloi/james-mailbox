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

import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.functional.InMemoryUserManager;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.inmemory.InMemorySubscriptionManager;
import org.apache.james.mailbox.inmemory.mail.InMemoryCachingUidProvider;
import org.apache.james.test.functional.HostSystem;

public class InMemoryHostSystem extends ImapHostSystem {

    private InMemoryMailboxManager mailboxManager;
    private InMemoryUserManager userManager; 
    private InMemoryMailboxSessionMapperFactory factory;
    
    static HostSystem build() throws Exception {
        InMemoryHostSystem host =  new InMemoryHostSystem();
        return host;
    }
    
    private InMemoryHostSystem() throws MailboxException {
        initFields();
    }
    
    public boolean addUser(String user, String password) throws Exception {
        userManager.addUser(user, password);
        return true;
    }

    @Override
    protected void resetData() throws Exception {
        initFields();
    }
    
    private void initFields() throws MailboxException {
        userManager = new InMemoryUserManager();
        factory = new InMemoryMailboxSessionMapperFactory();
        InMemoryCachingUidProvider uidProvider = new InMemoryCachingUidProvider();
        mailboxManager = new InMemoryMailboxManager(factory, userManager, uidProvider);
        mailboxManager.init();

        final ImapProcessor defaultImapProcessorFactory = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager, new InMemorySubscriptionManager(factory));
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                defaultImapProcessorFactory);
    }

}
