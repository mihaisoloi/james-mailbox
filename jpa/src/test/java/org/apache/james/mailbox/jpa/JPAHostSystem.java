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

import java.io.File;
import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.functional.ImapHostSystem;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.jpa.JPAMailboxSessionMapperFactory;
import org.apache.james.mailbox.jpa.JPASubscriptionManager;
import org.apache.james.mailbox.jpa.mail.JPACachingUidProvider;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.JPAProperty;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMailboxMembership;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMailboxMembership;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMessage;
import org.apache.james.mailbox.jpa.openjpa.OpenJPAMailboxManager;
import org.apache.james.mailbox.jpa.user.model.JPASubscription;
import org.apache.james.mailbox.store.MockAuthenticator;
import org.apache.james.test.functional.HostSystem;
import org.apache.openjpa.persistence.OpenJPAPersistence;

public class JPAHostSystem extends ImapHostSystem {

    public static final String META_DATA_DIRECTORY = "target/user-meta-data";

    public static HostSystem build() throws Exception {        
        JPAHostSystem host =  new JPAHostSystem();
        return host;
    }
    
    private final OpenJPAMailboxManager mailboxManager;
    private final MockAuthenticator userManager; 
    private final EntityManagerFactory entityManagerFactory;

    public JPAHostSystem() throws Exception {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:mem:imap;DB_CLOSE_DELAY=-1");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=org.apache.james.mailbox.jpa.mail.model.JPAHeader;" +
                JPAMailbox.class.getName() + ";" +
                AbstractJPAMailboxMembership.class.getName() + ";" +
                JPAMailboxMembership.class.getName() + ";" +
                AbstractJPAMessage.class.getName() + ";" +
                JPAMessage.class.getName() + ";" +
                JPAProperty.class.getName() + ";" +
                JPASubscription.class.getName() + ")");
        userManager = new MockAuthenticator();
        entityManagerFactory = OpenJPAPersistence.getEntityManagerFactory(properties);
        JPACachingUidProvider uidProvider = new JPACachingUidProvider(entityManagerFactory);

        JPAMailboxSessionMapperFactory mf = new JPAMailboxSessionMapperFactory(entityManagerFactory);
        mailboxManager = new OpenJPAMailboxManager(mf, userManager, uidProvider);
        mailboxManager.init();
        SubscriptionManager subscriptionManager = new JPASubscriptionManager(mf);
        final ImapProcessor defaultImapProcessorFactory = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager, subscriptionManager);
        resetUserMetaData();
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                defaultImapProcessorFactory);
    }

    public boolean addUser(String user, String password) {
        userManager.addUser(user, password);
        return true;
    }

    public void resetData() throws Exception {
        resetUserMetaData();
        MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("TestLog"));
        mailboxManager.startProcessingRequest(session);
        mailboxManager.deleteEverything(session);
        mailboxManager.endProcessingRequest(session);
        mailboxManager.logout(session, false);
        
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

}
