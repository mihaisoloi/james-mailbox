/**
 * **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one * or more
 * contributor license agreements. See the NOTICE file * distributed with this
 * work for additional information * regarding copyright ownership. The ASF
 * licenses this file * to you under the Apache License, Version 2.0 (the *
 * "License"); you may not use this file except in compliance * with the
 * License. You may obtain a copy of the License at * *
 * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable
 * law or agreed to in writing, * software distributed under the License is
 * distributed on an * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY *
 * KIND, either express or implied. See the License for the * specific language
 * governing permissions and limitations * under the License. *
 * **************************************************************
 */
package org.apache.james.mailbox.hbase;

import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOXES;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOXES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MAILBOX_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_META_CF;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGES_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGE_DATA_BODY;
import static org.apache.james.mailbox.hbase.HBaseNames.MESSAGE_DATA_HEADERS;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTIONS;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTIONS_TABLE;
import static org.apache.james.mailbox.hbase.HBaseNames.SUBSCRIPTION_CF;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that will creates a single instance of HBase MiniCluster.
 */
public final class HBaseClusterSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseClusterSingleton.class);
    private static final HBaseTestingUtility htu = new HBaseTestingUtility();
    private static HBaseClusterSingleton cluster = null;
    private MiniHBaseCluster hbaseCluster;
    private Configuration conf;

    public static synchronized HBaseClusterSingleton build()
            throws RuntimeException {
        LOG.info("Retrieving cluster instance.");
        if (cluster == null) {
            cluster = new HBaseClusterSingleton();
        }
        return cluster;
    }

    private HBaseClusterSingleton() throws RuntimeException {
        HTableDescriptor desc = null;
        HColumnDescriptor hColumnDescriptor = null;
        try {
            hbaseCluster = htu.startMiniCluster();
            htu.createTable(MAILBOXES_TABLE, MAILBOX_CF);
            htu.createTable(MESSAGES_TABLE, new byte[][]{MESSAGES_META_CF,
                        MESSAGE_DATA_HEADERS, MESSAGE_DATA_BODY});
            htu.createTable(SUBSCRIPTIONS_TABLE, SUBSCRIPTION_CF);
            
            conf = hbaseCluster.getConfiguration();
        } catch (Exception e) {
            throw new RuntimeException("Error starting MiniCluster ", e);
        } finally {
            if (hbaseCluster != null) {
                // add a shutdown hook for shuting down the minicluster.
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            hbaseCluster.shutdown();
                        } catch (IOException e) {
                            throw new RuntimeException("Exception shuting down cluster.");
                        }
                    }
                }));
            }
        }
    }

    public Configuration getConf() {
        return conf;
    }

    public void truncateTable(String tableName) {
        LOG.info("Truncating table!");
        try {
            htu.truncateTable(Bytes.toBytes(tableName));
        } catch (IOException ex) {
            LOG.info("Exception truncating table {}", tableName, ex);
        }
    }

    public void clearTables() {
        clearTable(MAILBOXES);
        clearTable(MESSAGES);
        clearTable(SUBSCRIPTIONS);
    }

    /**
     * Delete all rows from specified table.
     *
     * @param tableName
     */
    public void clearTable(String tableName) {
        HTable table = null;
        ResultScanner scanner = null;
        try {
            table = new HTable(conf, tableName);
            Scan scan = new Scan();
            scan.setCaching(1000);
            scanner = table.getScanner(scan);
            Result result;
            while ((result = scanner.next()) != null) {
                Delete delete = new Delete(result.getRow());
                table.delete(delete);
            }
        } catch (IOException ex) {
            LOG.info("Exception clearing table {}", tableName);
        } finally {
            IOUtils.closeStream(scanner);
            // TODO Temporary commented, was not compiling.
//            IOUtils.closeStream(table);
        }
    }
}
