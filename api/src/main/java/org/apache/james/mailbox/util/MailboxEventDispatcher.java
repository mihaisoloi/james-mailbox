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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;

/**
 * Helper class to dispatch {@link Event}'s to registerend MailboxListener 
 * 
 *
 */
public class MailboxEventDispatcher implements MailboxListener {

    private final Set<MailboxListener> listeners = new CopyOnWriteArraySet<MailboxListener>();

    /**
     * Remove all closed MailboxListener 
     */
    private void pruneClosed() {
        final Collection<MailboxListener> closedListeners = new ArrayList<MailboxListener>();
        for (MailboxListener listener:listeners) {
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
     * Should get called when a new message was added to a Mailbox. All registered MailboxListener will get triggered then
     * 
     * @param uid
     * @param sessionId
     * @param path
     */
    public void added(MailboxSession session, long uid, MailboxPath path) {
        pruneClosed();
        final AddedImpl added = new AddedImpl(session, path, uid);
        event(added);
    }

    /**
     * Should get called when a message was expunged from a Mailbox. All registered MailboxListener will get triggered then
     * 
     * @param session
     * @param uid
     * @param path
     */
    public void expunged(final MailboxSession session, final long uid, MailboxPath path) {
        final ExpungedImpl expunged = new ExpungedImpl(session, path, uid);
        event(expunged);
    }

    /**
     * Should get called when the message flags were update in a Mailbox. All registered MailboxListener will get triggered then
     *
     * @param session
     * @param uid
     * @param path
     * @param original
     * @param updated
     */
    public void flagsUpdated(MailboxSession session, final long uid, final MailboxPath path,
            final Flags original, final Flags updated) {
        final FlagsUpdatedImpl flags = new FlagsUpdatedImpl(session, path, uid,
                original, updated);
        event(flags);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#event(org.apache.james.mailbox.MailboxListener.Event)
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
     * Should get called when a Mailbox was renamed. All registered MailboxListener will get triggered then
     * 
     * @param from
     * @param to
     * @param sessionId
     */
    public void mailboxRenamed(MailboxSession session, MailboxPath from, MailboxPath to) {
        event(new MailboxRenamedEventImpl(session, from, to));
    }

    private final static class AddedImpl extends MailboxListener.Added {

        private final long subjectUid;

        public AddedImpl(final MailboxSession session, final MailboxPath path, final long subjectUid) {
            super(session, path);
            this.subjectUid = subjectUid;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getSubjectUid()
         */
        public long getSubjectUid() {
            return subjectUid;
        }
    }

    private final static class ExpungedImpl extends MailboxListener.Expunged {

        private final long subjectUid;

        public ExpungedImpl(MailboxSession session, final MailboxPath path, final long subjectUid) {
            super(session, path);
            this.subjectUid = subjectUid;
        }

        public long getSubjectUid() {
            return subjectUid;
        }
    }

    private final static class FlagsUpdatedImpl extends
            MailboxListener.FlagsUpdated {

        private static final boolean isChanged(final Flags original,
                final Flags updated, Flags.Flag flag) {
            return original != null && updated != null
                    && (original.contains(flag) ^ updated.contains(flag));
        }

        private static final Flags.Flag[] FLAGS = { Flags.Flag.ANSWERED,
                Flags.Flag.DELETED, Flags.Flag.DRAFT, Flags.Flag.FLAGGED,
                Flags.Flag.RECENT, Flags.Flag.SEEN };

        private static final int NUMBER_OF_SYSTEM_FLAGS = 6;

        private final long subjectUid;

        private final boolean[] modifiedFlags;

        private final Flags newFlags;

        public FlagsUpdatedImpl(MailboxSession session, final MailboxPath path, final long subjectUid,
                final Flags original, final Flags updated) {
            this(session, path, subjectUid, updated, isChanged(original, updated,
                    Flags.Flag.ANSWERED), isChanged(original, updated,
                    Flags.Flag.DELETED), isChanged(original, updated,
                    Flags.Flag.DRAFT), isChanged(original, updated,
                    Flags.Flag.FLAGGED), isChanged(original, updated,
                    Flags.Flag.RECENT), isChanged(original, updated,
                    Flags.Flag.SEEN));
        }

        public FlagsUpdatedImpl(MailboxSession session, final MailboxPath path, final long subjectUid,
                final Flags newFlags, boolean answeredUpdated,
                boolean deletedUpdated, boolean draftUpdated,
                boolean flaggedUpdated, boolean recentUpdated,
                boolean seenUpdated) {
            super(session, path);
            this.subjectUid = subjectUid;
            this.modifiedFlags = new boolean[NUMBER_OF_SYSTEM_FLAGS];
            this.modifiedFlags[0] = answeredUpdated;
            this.modifiedFlags[1] = deletedUpdated;
            this.modifiedFlags[2] = draftUpdated;
            this.modifiedFlags[3] = flaggedUpdated;
            this.modifiedFlags[4] = recentUpdated;
            this.modifiedFlags[5] = seenUpdated;
            this.newFlags = newFlags;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.MessageEvent#getSubjectUid()
         */
        public long getSubjectUid() {
            return subjectUid;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.FlagsUpdated#flagsIterator()
         */
        public Iterator<Flag> flagsIterator() {
            return new FlagsIterator();
        }

        /*
         * (non-Javadoc)
         * @see org.apache.james.mailbox.MailboxListener.FlagsUpdated#getNewFlags()
         */
        public Flags getNewFlags() {
            return newFlags;
        }

        private class FlagsIterator implements Iterator<Flag> {
            private int position;

            public FlagsIterator() {
                position = 0;
                nextPosition();
            }

            private void nextPosition() {
                if (position < NUMBER_OF_SYSTEM_FLAGS) {
                    if (!modifiedFlags[position]) {
                        position++;
                        nextPosition();
                    }
                }
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return position < NUMBER_OF_SYSTEM_FLAGS;
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#next()
             */
            public Flag next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final Flag result = FLAGS[position++];
                nextPosition();
                return result;
            }

            /*
             * (non-Javadoc)
             * @see java.util.Iterator#remove()
             */
            public void remove() {
                throw new UnsupportedOperationException("Read only");
            }
        }
    }


    /**
     * Should get called when a Mailbox was deleted. All registered MailboxListener will get triggered then
     *
     * @param session
     * @param path
     */
    public void mailboxDeleted(MailboxSession session, MailboxPath path) {
        final MailboxDeletion event = new MailboxDeletion(
                session, path);
        event(event);
    }

    /**
     * Should get called when a Mailbox was added. All registered MailboxListener will get triggered then
     *
     * @param session
     * @param path
     */
    public void mailboxAdded(MailboxSession session, MailboxPath path) {
        final MailboxAdded event = new MailboxAdded(
                session, path);
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
         * @see org.apache.james.mailbox.MailboxListener.MailboxRenamed#getNewPath()
         */
        public MailboxPath getNewPath() {
            return newPath;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MailboxListener#isClosed()
     */
    public boolean isClosed() {
        return false;
    }
}
