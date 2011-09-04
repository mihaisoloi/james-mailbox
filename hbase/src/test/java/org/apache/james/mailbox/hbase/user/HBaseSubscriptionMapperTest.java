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
package org.apache.james.mailbox.hbase.user;

import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.store.user.model.impl.SimpleSubscription;
import org.apache.james.mailbox.store.user.model.Subscription;
import org.apache.james.mailbox.SubscriptionException;
import org.apache.james.mailbox.hbase.HBaseMailboxSessionMapperFactory;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.james.mailbox.hbase.HBaseNames.*;

/**
 *
 * @author ieugen
 */
public class HBaseSubscriptionMapperTest {
    //TODO: move tests to a TestSuite so we reduce time by avoiding miniCluster restarts

    private static final Logger logger = Logger.getLogger("HBaseSubscriptionMapperTest");
    /** testing mini-cluster */
    private static Configuration conf;
    private static HBaseMailboxSessionMapperFactory mapperFactory;
    private static HBaseSubscriptionMapper mapper;
    private static Map<String, List<SimpleSubscription>> subscriptionList;
    private static final int USERS = 5;
    private static final int MAILBOXES = 5;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // start the test cluster 
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
        mapperFactory = new HBaseMailboxSessionMapperFactory(conf, null, null);
        mapper = new HBaseSubscriptionMapper(conf);
        fillSubscriptionList();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private static void fillSubscriptionList() throws SubscriptionException {
        System.out.println("Creating subscription list");
        SimpleSubscription subscription;
        String user, mailbox;
        subscriptionList = new HashMap<String, List<SimpleSubscription>>();
        for (int i = 0; i < USERS; i++) {
            user = "user" + i;
            List<SimpleSubscription> mailboxes = new ArrayList<SimpleSubscription>();
            subscriptionList.put(user, mailboxes);

            for (int j = 0; j < MAILBOXES; j++) {
                if (j == 0) {
                    mailbox = "INBOX";
                } else {
                    mailbox = "BOX" + j;
                }
                if ((i % 2 == 0) && (j > 0)) {
                    continue;
                }
                subscription = new SimpleSubscription(user, mailbox);
                mailboxes.add(subscription);
                mapper.save(subscription);
                System.out.println("Adding subscription " + subscription);
            }
        }
    }

    /**
     * Test of findMailboxSubscriptionForUser method, of class HBseSubscriptionMapper.
     */
    @Test
    public void testFindMailboxSubscriptionForUser() throws Exception {
        System.out.println("findMailboxSubscriptionForUser");

        SimpleSubscription fake1 = new SimpleSubscription("user1", "FAKEBOX");
        SimpleSubscription fake2 = new SimpleSubscription("fakeUser", "INBOX");

        for (String user : subscriptionList.keySet()) {
            System.out.println("Searching for all subscriptions for user: " + user);
            for (SimpleSubscription subscription : subscriptionList.get(user)) {
                Subscription result = mapper.findMailboxSubscriptionForUser(user, subscription.getMailbox());
                assertEquals(subscription.getMailbox(), result.getMailbox());
                assertEquals(subscription.getUser(), result.getUser());
            }
        }
        assertNull(mapper.findMailboxSubscriptionForUser(fake1.getUser(), fake1.getMailbox()));
        assertNull(mapper.findMailboxSubscriptionForUser(fake2.getUser(), fake2.getMailbox()));
    }

    /**
     * Test of save method, of class HBaseSubscriptionMapper.
     */
    @Test
    public void testSave() throws Exception {
        System.out.println("save");
        HTable subscriptions = new HTable(mapperFactory.getClusterConfiguration(), SUBSCRIPTIONS_TABLE);

        for (String user : subscriptionList.keySet()) {
            Get get = new Get(Bytes.toBytes(user));
            get.addFamily(SUBSCRIPTION_CF);
            Result result = subscriptions.get(get);
            for (Subscription subscription : subscriptionList.get(user)) {
                assertTrue(result.containsColumn(SUBSCRIPTION_CF, Bytes.toBytes(subscription.getMailbox())));
            }
        }
        subscriptions.close();
    }

    /**
     * Test of findSubscriptionsForUser method, of class HBaseSubscriptionMapper.
     */
    @Test
    public void testFindSubscriptionsForUser() throws Exception {
        System.out.println("findSubscriptionsForUser");
        SimpleSubscription fake1 = new SimpleSubscription("user1", "FAKEBOX");
        SimpleSubscription fake2 = new SimpleSubscription("fakeUser", "INBOX");
        for (String user : subscriptionList.keySet()) {
            System.out.println("Searching for all subscriptions for user: " + user);
            List<Subscription> foundSubscriptions = mapper.findSubscriptionsForUser(user);
            assertEquals(subscriptionList.get(user).size(), foundSubscriptions.size());
            // TODO: patch Subscription to implement equals
            //assertTrue(subscriptionList.get(user).containsAll(foundSubscriptions));
            //assertTrue(foundSubscriptions.containsAll(subscriptionList.get(user)));
            //assertFalse(foundSubscriptions.contains(fake1));
            //assertFalse(foundSubscriptions.contains(fake2));
        }
        //TODO: check what value we should return in case of no subscriptions: null or empty list
        assertEquals(mapper.findSubscriptionsForUser(fake2.getMailbox()).size(), 0);

    }

    /**
     * Test of delete method, of class HBaseSubscriptionMapper.
     */
    @Test
    public void testDelete() throws Exception {
        System.out.println("delete");
        HTable subscriptions = new HTable(mapperFactory.getClusterConfiguration(), SUBSCRIPTIONS_TABLE);

        for (String user : subscriptionList.keySet()) {
            System.out.println("Deleting subscriptions for user: " + user);
            for (SimpleSubscription subscription : subscriptionList.get(user)) {
                System.out.println("Deleting subscription : " + subscription);
                mapper.delete(subscription);
                Get get = new Get(Bytes.toBytes(subscription.getUser()));
                Result result = subscriptions.get(get);
                assertFalse(result.containsColumn(SUBSCRIPTION_CF, Bytes.toBytes(subscription.getMailbox())));
            }
        }
        subscriptions.close();
        fillSubscriptionList();
    }
}
