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
package org.apache.james.mailbox.store.mail;

import com.netflix.curator.framework.CuratorFramework;
import java.io.Closeable;
import java.io.IOException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * ZooKeepr based implementation of a distribuited sequential UID generator.
 */
public class ZooUidProvider<E> implements UidProvider<E>, Closeable {

    /** Inject the curator client using srping */
    private final CuratorFramework client;

    public ZooUidProvider(CuratorFramework client) {
        this.client = client;
        client.start();
    }

    @Override
    public long nextUid(MailboxSession session,
                        Mailbox<E> mailbox) throws MailboxException {
        if (client.isStarted()) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            throw new IllegalStateException("Curator client is closed.");
        }
    }

    @Override
    public long lastUid(MailboxSession session,
                        Mailbox<E> mailbox) throws MailboxException {
        if (client.isStarted()) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            throw new IllegalStateException("Curator client is closed.");
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}