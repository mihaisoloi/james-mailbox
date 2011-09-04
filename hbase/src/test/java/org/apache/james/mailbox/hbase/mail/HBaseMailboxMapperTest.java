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
package org.apache.james.mailbox.hbase.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.james.mailbox.hbase.io.ChunkInputStream;
import org.apache.james.mailbox.hbase.io.ChunkOutputStream;
import java.util.logging.Level;
import org.apache.james.mailbox.MailboxException;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.hbase.HBaseClusterSingleton;
import org.apache.james.mailbox.hbase.HBaseMailboxSessionMapperFactory;
import org.apache.james.mailbox.hbase.mail.model.HBaseMailbox;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.apache.james.mailbox.hbase.HBaseUtils.*;
import static org.apache.james.mailbox.hbase.HBaseNames.*;

/**
 * HBaseMailboxMapper unit tests.
 * @author ieugen
 */
public class HBaseMailboxMapperTest {

    private static final Logger logger = Logger.getLogger("HBaseMailboxMapperTest");
    /** testing mini-cluster */
    private static Configuration conf;
    private static HBaseUidProvider uidProvider;
    private static HBaseModSeqProvider modSeqProvider;
    private static HBaseMailboxSessionMapperFactory mapperFactory;
    private static HBaseMailboxMapper mapper;
    private static List<HBaseMailbox> mailboxList;
    private static List<MailboxPath> pathsList;
    private static final int NAMESPACES = 5;
    private static final int USERS = 5;
    private static final int MAILBOXES = 5;
    private static final char SEPARATOR = '%';

    @BeforeClass
    public static void setUp() throws Exception {
        // start the test cluster 
        conf = HBaseClusterSingleton.build();
        HBaseClusterSingleton.resetTables(conf);
        uidProvider = new HBaseUidProvider(conf);
        modSeqProvider = new HBaseModSeqProvider(conf);
        mapperFactory = new HBaseMailboxSessionMapperFactory(conf, uidProvider, modSeqProvider);
        fillMailboxList();
        mapper = new HBaseMailboxMapper(conf);
        for (HBaseMailbox mailbox : mailboxList) {
            mapper.save(mailbox);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }

    private static void fillMailboxList() {
        mailboxList = new ArrayList<HBaseMailbox>();
        pathsList = new ArrayList<MailboxPath>();
        MailboxPath path;
        String name;
        for (int i = 0; i < NAMESPACES; i++) {
            for (int j = 0; j < USERS; j++) {
                for (int k = 0; k < MAILBOXES; k++) {
                    if (j == 3) {
                        name = "test" + SEPARATOR + "subbox" + k;
                    } else {
                        name = "mailbox" + k;
                    }
                    path = new MailboxPath("namespace" + i, "user" + j, name);
                    pathsList.add(path);
                    mailboxList.add(new HBaseMailbox(path, 13));
                }
            }
        }

        logger.log(Level.INFO, "Created test case with {0} mailboxes and {1} paths", new Object[]{mailboxList.size(), pathsList.size()});
    }

    private void addMailbox(HBaseMailbox mailbox) throws MailboxException {
        mailboxList.add(mailbox);
        pathsList.add(new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName()));
        mapper = new HBaseMailboxMapper(conf);
        mapper.save(mailbox);
        logger.log(Level.INFO, "Added new mailbox: {0} paths: {1}", new Object[]{mailboxList.size(), pathsList.size()});
    }

    /**
     * Test of findMailboxByPath method, of class HBaseMailboxMapper.
     */
    @Test
    public void testFindMailboxByPath() throws Exception {
        System.out.println("findMailboxByPath");
        HBaseMailbox mailbox;
        for (MailboxPath path : pathsList) {
            System.out.println("Searching for " + path);
            mailbox = (HBaseMailbox) mapper.findMailboxByPath(path);
            assertEquals(path, new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName()));
        }
    }

    /**
     * Test of findMailboxWithPathLike method, of class HBaseMailboxMapper.
     */
    @Test
    public void testFindMailboxWithPathLike() throws Exception {
        System.out.println("findMailboxWithPathLike");
        MailboxPath path = pathsList.get(pathsList.size() / 2);

        List<Mailbox<UUID>> result = mapper.findMailboxWithPathLike(path);
        assertEquals(1, result.size());

        int start = 3;
        int end = 7;
        MailboxPath newPath;

        for (int i = start; i < end; i++) {
            newPath = new MailboxPath(path);
            newPath.setName(i + newPath.getName() + " " + i);
            // test for paths with null user 
            if (i % 2 == 0) {
                newPath.setUser(null);
            }
            addMailbox(new HBaseMailbox(newPath, 1234));
        }
        result = mapper.findMailboxWithPathLike(path);
        assertEquals(end - start + 1, result.size());
    }

    /**
     * Test of list method, of class HBaseMailboxMapper.
     */
    @Test
    public void testList() throws Exception {
        System.out.println("list");
        List<Mailbox<UUID>> result = mapper.list();
        assertEquals(mailboxList.size(), result.size());

    }

    /**
     * Test of save method, of class HBaseMailboxMapper.
     */
    @Test
    public void testSave() throws Exception {
        System.out.println("save and mailboxFromResult");
        HTable mailboxes = new HTable(conf, MAILBOXES_TABLE);

        HBaseMailbox mlbx = mailboxList.get(mailboxList.size() / 2);

        Get get = new Get(mailboxRowKey(mlbx.getMailboxId()));
        // get all columns for the DATA column family
        get.addFamily(Bytes.toBytes("DATA"));

        Result result = mailboxes.get(get);
        HBaseMailbox newValue = (HBaseMailbox) mailboxFromResult(result);
        assertEquals(mlbx, newValue);
        assertEquals(mlbx.getUser(), newValue.getUser());
        assertEquals(mlbx.getName(), newValue.getName());
        assertEquals(mlbx.getNamespace(), newValue.getNamespace());
        assertEquals(mlbx.getMailboxId(), newValue.getMailboxId());
        assertEquals(mlbx.getLastUid(), newValue.getLastUid());
        assertEquals(mlbx.getUidValidity(), newValue.getUidValidity());
        assertEquals(mlbx.getHighestModSeq(), newValue.getHighestModSeq());
        assertArrayEquals(mailboxRowKey(mlbx.getMailboxId()), mailboxRowKey(newValue.getMailboxId()));
    }

    /**
     * Test of delete method, of class HBaseMailboxMapper.
     */
    @Test
    public void testDelete() throws Exception {
        System.out.println("delete");
        // delete last 5 mailboxes from mailboxList
        int offset = 5;
        int notFoundCount = 0;

        Iterator<HBaseMailbox> iterator = mailboxList.subList(mailboxList.size() - offset, mailboxList.size()).iterator();

        while (iterator.hasNext()) {
            HBaseMailbox mailbox = iterator.next();
            mapper.delete(mailbox);
            iterator.remove();
            MailboxPath path = new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName());
            pathsList.remove(path);
            logger.log(Level.INFO, "Removing mailbox: {0}", path);
            try {
                mapper.findMailboxByPath(path);
            } catch (MailboxNotFoundException e) {
                logger.log(Level.INFO, "Succesfully removed {0}", mailbox);
                notFoundCount++;
            }
        }
        assertEquals(offset, notFoundCount);
        assertEquals(mailboxList.size(), mapper.list().size());
    }

    /**
     * Test of hasChildren method, of class HBaseMailboxMapper.
     */
    @Test
    public void testHasChildren() throws Exception {
        System.out.println("hasChildren");
        String oldName;
        for (MailboxPath path : pathsList) {
            HBaseMailbox mailbox = new HBaseMailbox(path, 12455);
            oldName = mailbox.getName();
            if (path.getUser().equals("user3")) {
                mailbox.setName("test");
            }
            boolean result = mapper.hasChildren(mailbox, SEPARATOR);
            mailbox.setName(oldName);
            if (path.getUser().equals("user3")) {
                assertTrue(result);
            } else {
                assertFalse(result);
            }

        }
    }

    /**
     * Test of deleteAllMemberships method, of class HBaseMailboxMapper.
     */
//    @Test
    public void testDeleteAllMemberships() {
        System.out.println("deleteAllMemberships");
        fail("Not yet implemented");
    }

    /**
     * Test of deleteAllMailboxes method, of class HBaseMailboxMapper.
     */
    @Test
    public void testDeleteAllMailboxes() throws MailboxException {
        System.out.println("deleteAllMailboxes");
        mapper.deleteAllMailboxes();
        assertEquals(0, mapper.list().size());
        fillMailboxList();
    }

    @Test
    public void chunkStream() throws IOException {
        System.out.println("Checking ChunkOutpuStream and ChunkInputStream");
        final String original = "This is a proper test for the HBase ChunkInputStream and"
                + "ChunkOutputStream. This text must be larger than the chunk size so we write"
                + "and read more then one chunk size. I think that a few more lore ipsum lines"
                + "will be enough."
                + "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor "
                + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis "
                + "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
                + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu "
                + "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa"
                + " qui officia deserunt mollit anim id est laborum";
        byte[] data = Bytes.toBytes(original);
        // we make the column size = 10 bytes
        ChunkOutputStream out = new ChunkOutputStream(conf,
                MESSAGES_TABLE, MESSAGE_DATA_BODY, Bytes.toBytes("10"), 10);
        ChunkInputStream in = new ChunkInputStream(conf,
                MESSAGES_TABLE, MESSAGE_DATA_BODY, Bytes.toBytes("10"));
        //create the stream
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(data.length);
        int b;
        while ((b = bin.read()) != -1) {
            out.write(b);
        }
        out.close();
        while ((b = in.read()) != -1) {
            bout.write(b);
        }
        String s = bout.toString();
        assertTrue(original.equals(s));
    }
}
