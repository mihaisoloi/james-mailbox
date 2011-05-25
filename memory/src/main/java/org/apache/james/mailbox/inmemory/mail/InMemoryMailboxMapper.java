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
package org.apache.james.mailbox.inmemory.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.inmemory.mail.model.InMemoryMailbox;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;

public class InMemoryMailboxMapper implements MailboxMapper<Long> {
    
    private static final int INITIAL_SIZE = 128;
    private final Map<Long, InMemoryMailbox> mailboxesById;

    public InMemoryMailboxMapper() {
        mailboxesById = new ConcurrentHashMap<Long, InMemoryMailbox>(INITIAL_SIZE);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#delete(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<Long> mailbox) throws MailboxException {
        mailboxesById.remove(mailbox.getMailboxId());
    }

    public void deleteAll() throws MailboxException {
        mailboxesById.clear();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByName(java.lang.String)
     */
    public synchronized Mailbox<Long> findMailboxByPath(MailboxPath path) throws MailboxException, MailboxNotFoundException {
        Mailbox<Long> result = null;
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            MailboxPath mp = new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName());
            if (mp.equals(path)) {
                result = mailbox;
                break;
            }
        }
        if (result == null) {
            throw new MailboxNotFoundException(path);
        } else {
            return result;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithNameLike(java.lang.String)
     */
    public List<Mailbox<Long>> findMailboxWithPathLike(MailboxPath path) throws MailboxException {
        final String regex = path.getName().replace("%", ".*");
        List<Mailbox<Long>> results = new ArrayList<Mailbox<Long>>();
        for (final InMemoryMailbox mailbox:mailboxesById.values()) {
            if (mailbox.getName().matches(regex)) {
                results.add(mailbox);
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public void save(Mailbox<Long> mailbox) throws MailboxException {
        mailboxesById.put(mailbox.getMailboxId(), (InMemoryMailbox) mailbox);
    }

    /**
     * Do nothing
     */
    public void endRequest() {
        // Do nothing
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public boolean hasChildren(Mailbox<Long> mailbox, char delimiter) throws MailboxException,
            MailboxNotFoundException {
        String mailboxName = mailbox.getName() + delimiter;
        for (final InMemoryMailbox box:mailboxesById.values()) {
            if (box.getName().startsWith(mailboxName)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    public List<Mailbox<Long>> list() throws MailboxException {
        return new ArrayList<Mailbox<Long>>(mailboxesById.values());
    }

    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }
    
}
