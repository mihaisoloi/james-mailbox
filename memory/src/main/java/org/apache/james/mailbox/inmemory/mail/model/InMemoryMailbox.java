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

package org.apache.james.mailbox.inmemory.mail.model;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Mailbox data which is stored only in memory.
 */
public class InMemoryMailbox implements Mailbox<Long> {

    private final long id;
    private final long uidValidity;
    private final AtomicLong nextUid;
    private String namespace;
    private String user;
    private String name;
    
    public InMemoryMailbox(final long id, final MailboxPath path, final long uidValidity) {
        super();
        this.nextUid = new AtomicLong(0);
        this.id = id;
        this.namespace = path.getNamespace();
        this.user = path.getUser();
        this.name = path.getName();
        this.uidValidity = uidValidity;
    }

    public void consumeUid() {
        nextUid.incrementAndGet();
    }

    public long getLastUid() {
        return nextUid.get();
    }

    public Long getMailboxId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getLastKnownUid()
     */
    public long getLastKnownUid() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Mailbox#getHighestKnownModSeq()
     */
    public long getHighestKnownModSeq() {
        return 0;
    }
}
