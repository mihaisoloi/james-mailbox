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

package org.apache.james.mailbox.hbase;

import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.hbase.mail.HBaseModSeqProvider;
import org.apache.james.mailbox.hbase.mail.HBaseUidProvider;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.LoggerFactory;

/**
 *
 * 
 */
public class HBaseMailboxSessionMapperFactoryTest {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(HBaseMailboxSessionMapperFactoryTest.class);
    private static Configuration conf;

    @BeforeClass
    public static void setUpClass() throws Exception {
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        
    }

     /**
     * Test of createMessageMapper method, of class HBaseMailboxSessionMapperFactory.
     */
    @Test
    public void testCreateMessageMapper() throws Exception {
        System.out.println("createMessageMapper");
        MailboxSession session = null;
        HBaseMailboxSessionMapperFactory instance = new HBaseMailboxSessionMapperFactory(conf, null , null);
        MessageMapper<UUID> messageMapper = instance.createMessageMapper(session); 
        assertNotNull(messageMapper);
        assertTrue(messageMapper instanceof MessageMapper);
    }

    /**
     * Test of createMailboxMapper method, of class HBaseMailboxSessionMapperFactory.
     */
    @Test
    public void testCreateMailboxMapper() throws Exception {
        System.out.println("createMailboxMapper");
        MailboxSession session = null;
        HBaseMailboxSessionMapperFactory instance = new HBaseMailboxSessionMapperFactory(conf, null , null);
        MailboxMapper<UUID> mailboxMapper = instance.createMailboxMapper(session); 
        assertNotNull(mailboxMapper);
        assertTrue(mailboxMapper instanceof MailboxMapper);
    }

    /**
     * Test of createSubscriptionMapper method, of class HBaseMailboxSessionMapperFactory.
     */
    @Test
    public void testCreateSubscriptionMapper() throws Exception {
        System.out.println("createSubscriptionMapper");
        MailboxSession session = null;
        HBaseMailboxSessionMapperFactory instance = new HBaseMailboxSessionMapperFactory(conf, null , null);
        SubscriptionMapper subscriptionMapper = instance.createSubscriptionMapper(session); 
        assertNotNull(subscriptionMapper);
        assertTrue(subscriptionMapper instanceof SubscriptionMapper);
    }

    /**
     * Test of getModSeqProvider method, of class HBaseMailboxSessionMapperFactory.
     */
    @Test
    public void testGetModSeqProvider() {
        System.out.println("getModSeqProvider");
        ModSeqProvider<UUID> expResult = new HBaseModSeqProvider(conf);
        HBaseMailboxSessionMapperFactory instance = new HBaseMailboxSessionMapperFactory(conf, null , expResult);
        ModSeqProvider<UUID> result = instance.getModSeqProvider();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUidProvider method, of class HBaseMailboxSessionMapperFactory.
     */
    @Test
    public void testGetUidProvider() {
        System.out.println("getUidProvider");
        UidProvider<UUID> expResult = new HBaseUidProvider(conf);
        HBaseMailboxSessionMapperFactory instance = new HBaseMailboxSessionMapperFactory(conf, expResult , null);        
        UidProvider<UUID> result = instance.getUidProvider();
        assertEquals(expResult, result);
    }
    
}
