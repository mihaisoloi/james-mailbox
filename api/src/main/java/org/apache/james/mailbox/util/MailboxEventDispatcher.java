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

package org.apache.james.mailbox.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.UpdatedFlags;

/**
 * Helper class to dispatch {@link Event}'s to registerend MailboxListener
 */
public class MailboxEventDispatcher implements MailboxListener {

    private final Set<MailboxListener> listeners = new CopyOnWriteArraySet<MailboxListener>();

    /**
     * Remove all closed MailboxListener
     */
    private void pruneClosed() {
        final Collection<MailboxListener> closedListeners = new ArrayList<MailboxListener>();
        for (MailboxListener listener : listeners) {
            if (listener.isClosed()) {
                closedListeners.add(listener);
            }
        }
        if (!closedListeners.isEmpty()) {
            listeners.removeAll(closedListeners);
        }
    }

    /**
     * Add a MailboxListener to this dispatcher
     * 
     * @param mailboxListener
     */
    public void addMailboxListener(MailboxListener mailboxListener) {
        pruneClosed();
        listeners.add(mailboxListener);
    }

    /**
     * Should get called when a new message was added to a Mailbox. All
     * registered MailboxListener will get triggered then
     * 
     * @param uids
     * @param sessionId
     * @param path
     */
    public void added(MailboxSession session, Map<Long, Flags> uids, MailboxPath path) {
        pruneClosed();
        final AddedImpl added = new AddedImpl(session, path, uids);
        event(added);
    }

    /**
     * Should get called when a message was expunged from a Mailbox. All
     * registered MailboxListener will get triggered then
     * 
     * @param session
     * @param uids
     * @param path
     */
    public void expunged(final MailboxSession session, final List<Long> uids, MailboxPath path) {
        final ExpungedImpl expunged = new ExpungedImpl(session, path, uids);
        event(expunged);
    }

    /**
     * Should get called when the message flags were update in a Mailbox. All
     * registered MailboxListener will get triggered then
     * 
     * @param session
     * @param uids
     * @param path
     * @param original
     * @param updated
     */
    public void flagsUpdated(MailboxSession session, final List<Long> uids, final MailboxPath path, final List<UpdatedFlags> uflags) {
        final FlagsUpdatedImpl flags = new FlagsUpdatedImpl(session, path, uids, uflags);
        event(flags);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox
     * .MailboxListener.Event)
     */
    public void event(Event event) {
        List<MailboxListener> closed = new ArrayList<MailboxListener>();
        for (Iterator<MailboxListener> iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = iter.next();
            if (mailboxListener.isClosed() == false) {
                mailboxListener.event(event);
            } else {
                closed.add(mailboxListener);
            }
        }
        for (int i = 0; i < closed.size(); i++)
            listeners.remove(closed.get(i));
    }

    /**
     * Return the the count of all registered MailboxListener
     * 
     * @return count
     */
    public int count() {
        return listeners.size();
    }

    /**
     * Should get called when a Mailbox was renamed. All registered
     * MailboxListener will get triggered then
     * 
     * @param from
     * @param to
     * @param sessionId
     */
    public void mailboxRenamed(MailboxSession session, MailboxPath from, MailboxPath to) {
        event(new MailboxRenamedEventImpl(session, from, to));
    }

    private final static class AddedImpl extends MailboxListener.Added {

        private final List<Long> uids;
        private Map<Long, Flags> added;

        public AddedImpl(final MailboxSession session, final MailboxPath path, final Map<Long, Flags> added) {
            super(session, path);
            this.uids = new ArrayList<Long>(added.keySet());
            this.added = added;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getUids()
         */
        public List<Long> getUids() {
            return uids;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.Added#getFlags()
         */
        public Map<Long, Flags> getFlags() {
            return added;
        }
    }

    private final static class ExpungedImpl extends MailboxListener.Expunged {

        private final List<Long> uids;

        public ExpungedImpl(MailboxSession session, final MailboxPath path, final List<Long> uids) {
            super(session, path);
            this.uids = uids;
        }
        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getUids()
         */
        public List<Long> getUids() {
            return uids;
        }
    }

    private final static class FlagsUpdatedImpl extends MailboxListener.FlagsUpdated {

        private final List<Long> uids;


        private final List<UpdatedFlags> uFlags;

        public FlagsUpdatedImpl(MailboxSession session, final MailboxPath path, final List<Long> uids, final List<UpdatedFlags> uFlags) {
            super(session, path);
            this.uids = uids;

            this.uFlags = uFlags;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getUids()
         */
        public List<Long> getUids() {
            return uids;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.FlagsUpdated#getUpdatedFlags()
         */
        public List<UpdatedFlags> getUpdatedFlags() {
            return uFlags;
        }

    }

    /**
     * Should get called when a Mailbox was deleted. All registered
     * MailboxListener will get triggered then
     * 
     * @param session
     * @param path
     */
    public void mailboxDeleted(MailboxSession session, MailboxPath path) {
        final MailboxDeletion event = new MailboxDeletion(session, path);
        event(event);
    }

    /**
     * Should get called when a Mailbox was added. All registered
     * MailboxListener will get triggered then
     * 
     * @param session
     * @param path
     */
    public void mailboxAdded(MailboxSession session, MailboxPath path) {
        final MailboxAdded event = new MailboxAdded(session, path);
        event(event);
    }

    private static final class MailboxRenamedEventImpl extends MailboxListener.MailboxRenamed {
        private final MailboxPath newPath;

        public MailboxRenamedEventImpl(final MailboxSession session, final MailboxPath path, final MailboxPath newPath) {
            super(session, path);
            this.newPath = newPath;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.apache.james.mailbox.MailboxListener.MailboxRenamed#getNewPath()
         */
        public MailboxPath getNewPath() {
            return newPath;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public boolean isClosed() {
        return false;
    }
}
