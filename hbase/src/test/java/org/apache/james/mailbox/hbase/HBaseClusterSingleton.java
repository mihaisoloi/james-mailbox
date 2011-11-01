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

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import static org.apache.james.mailbox.hbase.HBaseNames.*;

/**
 * Class that will creates a single connection to a HBaseCluster.
 */
public class HBaseClusterSingleton {

    private static HBaseClusterSingleton cluster = null;
    private MiniHBaseCluster hbaseCluster;
    private Configuration conf;
    /** Set this to false if you wish to test it against a real cluster.
     * In that case you should provide the configuration file for the real
     * cluster on the classpath. 
     */
    public static boolean useMiniCluster = true;

    public static synchronized Configuration build() throws Exception {
        if (cluster == null) {
            cluster = new HBaseClusterSingleton(useMiniCluster);
        }
        return cluster.getConf();
    }

    public HBaseClusterSingleton(boolean useMiniCluster) throws Exception {
        if (useMiniCluster) {
            HBaseTestingUtility htu = new HBaseTestingUtility();
            try {
                hbaseCluster = htu.startMiniCluster();
                conf = hbaseCluster.getConfiguration();
            } catch (IOException e) {
                throw new Exception("Error starting MiniCluster ", e);
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
        } else {
            conf = HBaseConfiguration.create();
        }
    }

    public Configuration getConf() {
        return conf;
    }

    public static void resetTables(Configuration conf) throws Exception {
        HBaseAdmin hbaseAdmin = new HBaseAdmin(conf);
        if (hbaseAdmin.tableExists(MAILBOXES_TABLE)) {
            hbaseAdmin.disableTable(MAILBOXES_TABLE);
            hbaseAdmin.deleteTable(MAILBOXES_TABLE);
        }
        if (hbaseAdmin.tableExists(MESSAGES_TABLE)) {
            hbaseAdmin.disableTable(MESSAGES_TABLE);
            hbaseAdmin.deleteTable(MESSAGES_TABLE);
        }
        if (hbaseAdmin.tableExists(SUBSCRIPTIONS_TABLE)) {
            hbaseAdmin.disableTable(SUBSCRIPTIONS_TABLE);
            hbaseAdmin.deleteTable(SUBSCRIPTIONS_TABLE);
        }
    }
}
