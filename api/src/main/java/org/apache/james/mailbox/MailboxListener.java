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

package org.apache.james.mailbox;

import java.util.Iterator;

import javax.mail.Flags;


/**
 * Listens to <code>Mailbox</code> events.
 * Note that listeners may be removed asynchronously.
 * When {@link #isClosed()} returns true,
 * the listener may be removed from the mailbox by
 * the dispatcher.
 */
public interface MailboxListener {

    /**
     * Informs this listener about the given event.
     * @param event not null
     */
    void event(final Event event);
    
    /**
     * Is this listener closed?
     * Closed listeners may be unsubscribed. 
     * 
     * Be aware that if the listener is marked as close it will not get any events passed anymore!
     * 
     * @return true when closed,
     * false when open
     */
    boolean isClosed();
    
    /**
     * A mailbox event.
     */
    public class Event {
        private final long sessionId;
        private final MailboxPath path;
        public Event(final long sessionId, final MailboxPath path) {
            this.sessionId = sessionId;
            this.path = path;
        }
        
        /**
         * Gets the id of the session which the event.
         * 
         * @return session id
         */
        public long getSessionId() {
            return sessionId;
        }
        
        /**
         * Return the path of the Mailbox this event belongs to. 
         * 
         * @return path
         */
        public MailboxPath getMailboxPath() {
            return path;
        }
    }

    /**
     * Indicates that mailbox has been deleted.
     */
    public class MailboxDeletion extends Event {

        public MailboxDeletion(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }
    }
    
    
    /**
     * Indicates that a mailbox has been renamed.
     */
    public abstract class MailboxRenamed extends Event {
        public MailboxRenamed(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }
        /**
         * Gets the new name for this mailbox.
         * 
         * @return name, not null
         */
        public abstract MailboxPath getNewPath();
    }

    /**
     * A mailbox event related to a message.
     */
    public abstract class MessageEvent extends Event {

        public MessageEvent(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }

        /**
         * Gets the message UID for the subject of this event.
         * 
         * @return message uid
         */
        public abstract long getSubjectUid();
    }

    public abstract class Expunged extends MessageEvent {

        public Expunged(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }
    }

    /**
     * A mailbox event related to updated flags
     * 
     *
     */
    public abstract class FlagsUpdated extends MessageEvent {

        public FlagsUpdated(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }

        /**
         * Gets new flags for this message.
         */
        public abstract Flags getNewFlags();

        /**
         * Gets an iterator for the system flags changed.
         * 
         * @return <code>Flags.Flag</code> <code>Iterator</code>, not null
         */
        public abstract Iterator<Flags.Flag> flagsIterator();
    }

    /**
     * A mailbox event related to added message
     * 
     *
     */
    public abstract class Added extends MessageEvent {

        public Added(long sessionId, MailboxPath path) {
            super(sessionId, path);
        }
    }

}
