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

package org.apache.james.mailbox.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageMetaData;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Helper class to dispatch {@link Event}'s to registerend MailboxListener
 */
public class MailboxEventDispatcher<Id> implements MailboxListener {

    private final Set<MailboxListener> listeners = new CopyOnWriteArraySet<MailboxListener>();

    

    /**
     * Add a MailboxListener to this dispatcher
     * 
     * @param mailboxListener
     */
    public void addMailboxListener(MailboxListener mailboxListener) {
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
    public void added(MailboxSession session, SortedMap<Long, MessageMetaData> uids, Mailbox<Id> mailbox) {
        final AddedImpl added = new AddedImpl(session, mailbox, uids);
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
    public void expunged(final MailboxSession session,  Map<Long, MessageMetaData> uids, Mailbox<Id> mailbox) {
        final ExpungedImpl expunged = new ExpungedImpl(session, mailbox, uids);
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
    public void flagsUpdated(MailboxSession session, final List<Long> uids, final Mailbox<Id> mailbox, final List<UpdatedFlags> uflags) {
        final FlagsUpdatedImpl flags = new FlagsUpdatedImpl(session, mailbox, uids, uflags);
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
            mailboxListener.event(event);
           
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
    public void mailboxRenamed(MailboxSession session, MailboxPath from, Mailbox<Id> to) {
        event(new MailboxRenamedEventImpl(session, from, to));
    }

    public final class AddedImpl extends MailboxListener.Added {

        private SortedMap<Long, MessageMetaData> added;
        private final Mailbox<Id> mailbox;

        public AddedImpl(final MailboxSession session, final Mailbox<Id> mailbox, final SortedMap<Long, MessageMetaData> added) {
            super(session, new StoreMailboxPath<Id>(mailbox));
            this.added = added;
            this.mailbox = mailbox;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getUids()
         */
        public List<Long> getUids() {
            return new ArrayList<Long>(added.keySet());
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.Added#getMetaData(long)
         */
        public MessageMetaData getMetaData(long uid) {
            return added.get(uid);
        }
        
        public Mailbox<Id> getMailbox() {
            return mailbox;
        }
    }

    public final class ExpungedImpl extends MailboxListener.Expunged {

        private final Map<Long, MessageMetaData> uids;
        private final Mailbox<Id> mailbox;

        public ExpungedImpl(MailboxSession session, final Mailbox<Id> mailbox, final  Map<Long, MessageMetaData> uids) {
            super(session,  new StoreMailboxPath<Id>(mailbox));
            this.uids = uids;
            this.mailbox = mailbox;
        }
        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getUids()
         */
        public List<Long> getUids() {
            return new ArrayList<Long>(uids.keySet());
        }
        
        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.Expunged#getMetaData(long)
         */
        public MessageMetaData getMetaData(long uid) {
            return uids.get(uid);
        }
        
        public Mailbox<Id> getMailbox() {
            return mailbox;
        }
    }

    public final class FlagsUpdatedImpl extends MailboxListener.FlagsUpdated {

        private final List<Long> uids;

        private final Mailbox<Id> mailbox;

        private final List<UpdatedFlags> uFlags;

        public FlagsUpdatedImpl(MailboxSession session, final Mailbox<Id> mailbox, final List<Long> uids, final List<UpdatedFlags> uFlags) {
            super(session, new StoreMailboxPath<Id>(mailbox));
            this.uids = uids;
            this.uFlags = uFlags;
            this.mailbox = mailbox;
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
        
        public Mailbox<Id> getMailbox() {
            return mailbox;
        }

    }

    public final class MailboxDeletionImpl extends MailboxDeletion {

        private final Mailbox<Id> mailbox;

        public MailboxDeletionImpl(MailboxSession session, Mailbox<Id> mailbox) {
            super(session, new StoreMailboxPath<Id>(mailbox));
            this.mailbox = mailbox;
        }
        
        
        public Mailbox<Id> getMailbox() {
            return mailbox;
        }

    }
    
    public final class MailboxAddedImpl extends MailboxAdded {

        private final Mailbox<Id> mailbox;

        public MailboxAddedImpl(MailboxSession session, Mailbox<Id> mailbox) {
            super(session,  new StoreMailboxPath<Id>(mailbox));
            this.mailbox = mailbox;
        }
        
        
        public Mailbox<Id> getMailbox() {
            return mailbox;
        }

    }
    /**
     * Should get called when a Mailbox was deleted. All registered
     * MailboxListener will get triggered then
     * 
     * @param session
     * @param path
     */
    public void mailboxDeleted(MailboxSession session, Mailbox<Id> mailbox) {
        final MailboxDeletion event = new MailboxDeletionImpl(session, mailbox);
        event(event);
    }

    /**
     * Should get called when a Mailbox was added. All registered
     * MailboxListener will get triggered then
     * 
     * @param session
     * @param path
     */
    public void mailboxAdded(MailboxSession session, Mailbox<Id> mailbox) {
        final MailboxAdded event = new MailboxAddedImpl(session, mailbox);
        event(event);
    }

    public final class MailboxRenamedEventImpl extends MailboxListener.MailboxRenamed {
        private final MailboxPath newPath;
        private final Mailbox<Id> newMailbox;

        public MailboxRenamedEventImpl(final MailboxSession session, final MailboxPath oldPath, final Mailbox<Id> newMailbox) {
            super(session, oldPath);
            this.newPath = new StoreMailboxPath<Id>(newMailbox);
            this.newMailbox = newMailbox;
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
        
        public Mailbox<Id> getNewMailbox() {
            return newMailbox;
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
