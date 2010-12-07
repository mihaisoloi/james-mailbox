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

import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.imap.functional.AbstractStressTest;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jpa.mail.JPACachingUidProvider;
import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.JPAProperty;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMailboxMembership;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMailboxMembership;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMessage;
import org.apache.james.mailbox.jpa.openjpa.OpenJPAMailboxManager;
import org.apache.james.mailbox.jpa.user.model.JPASubscription;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.junit.After;
import org.junit.Before;

/**
 * Proof of bug https://issues.apache.org/jira/browse/IMAP-137
 */
public class JPAStressTest extends AbstractStressTest{
    
    private OpenJPAMailboxManager mailboxManager;
    
    private long locktimeout = 60000;
    
    private EntityManagerFactory entityManagerFactory;
    
    @Before
    public void setUp() {
        
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("openjpa.ConnectionDriverName", "org.h2.Driver");
        properties.put("openjpa.ConnectionURL", "jdbc:h2:mem:imap;DB_CLOSE_DELAY=-1");
        properties.put("openjpa.Log", "JDBC=WARN, SQL=WARN, Runtime=WARN");
        properties.put("openjpa.ConnectionFactoryProperties", "PrettyPrint=true, PrettyPrintLineLength=72");
        properties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        properties.put("openjpa.MetaDataFactory", "jpa(Types=" +
                JPAHeader.class.getName() + ";" +
                JPAMailbox.class.getName() + ";" +
                AbstractJPAMailboxMembership.class.getName() + ";" +
                JPAMailboxMembership.class.getName() + ";" +
                AbstractJPAMessage.class.getName() + ";" +
                JPAMessage.class.getName() + ";" +
                JPAProperty.class.getName() + ";" +
                JPASubscription.class.getName() + ")");
        properties.put("openjpa.LockTimeout", locktimeout + "");
       
        entityManagerFactory = OpenJPAPersistence.getEntityManagerFactory(properties);
        JPACachingUidProvider uidProvider = new JPACachingUidProvider(entityManagerFactory);
        JPAMailboxSessionMapperFactory mf = new JPAMailboxSessionMapperFactory(entityManagerFactory);
        mailboxManager = new OpenJPAMailboxManager(mf, null, uidProvider);
        
        // Set the lock timeout via SQL because of a bug in openJPA
        // https://issues.apache.org/jira/browse/OPENJPA-1656
        setH2LockTimeout();
    
    }
    
    private void setH2LockTimeout() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();
        manager.createNativeQuery("SET DEFAULT_LOCK_TIMEOUT " + locktimeout).executeUpdate();
        manager.getTransaction().commit();
        manager.close();
    }
    
    @After
    public void tearDown() {
        MailboxSession session = mailboxManager.createSystemSession("test", new SimpleLog("Test"));
        try {
            mailboxManager.deleteEverything(session);
        } catch (MailboxException e) {
            e.printStackTrace();
        }
        session.close();
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return mailboxManager;
    }

}
