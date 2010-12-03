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

package org.apache.james.mailbox.torque;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.util.MailboxEventDispatcher;

/**
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class UidChangeTracker implements MailboxConstants {

    private final MailboxEventDispatcher eventDispatcher;

    private final TreeMap<Long, Flags> cache;

    private long lastUid;

    private MailboxPath path;

    public UidChangeTracker(long lastUid, MailboxPath path) {
        this.lastUid = lastUid;
        eventDispatcher = new MailboxEventDispatcher();
        cache = new TreeMap<Long, Flags>();
        this.path = path;
    }

    public synchronized void expunged(final Collection<Long> uidsExpunged) {
        for (Long uid:uidsExpunged) {
            cache.remove(uid);
            eventDispatcher.expunged(uid, 0, path);
        }
    }

    /**
     * Indicates that the flags on the given messages may have been updated.
     * 
     * @param messageFlags
     *            flags
     * @param sessionId
     *            id of the session upating the flags
     */
    public synchronized void flagsUpdated(SortedMap<Long,Flags> newFlagsByUid, Map<Long,Flags> originalFlagsByUid, long sessionId) {
        if (newFlagsByUid != null) {
            for(Map.Entry<Long, Flags> entry:newFlagsByUid.entrySet()) {
                final Long uid = entry.getKey();
                final Flags newFlags = entry.getValue();
                final Flags cachedFlags = cache.get(uid);
                final Flags lastFlags;
                if (cachedFlags == null) {
                    lastFlags = originalFlagsByUid.get(uid);
                } else {
                    lastFlags = cachedFlags;
                }
                if (!newFlags.equals(lastFlags)) {
                    eventDispatcher.flagsUpdated(uid, sessionId, path, lastFlags, newFlags);
                }
                cache.put(uid, newFlags);
            }
        }
    }

    public synchronized void found(MessageRange range, final Map<Long, Flags> flagsByIndex, long sessionId) {
        final Set<Long> expectedSet = getSubSet(range);
        for (Map.Entry<Long, Flags> entry:flagsByIndex.entrySet()) {
            final Long uid = entry.getKey();
            if (expectedSet.contains(uid)) {
                expectedSet.remove(uid);
            }
            final Flags flags = entry.getValue();
            found(uid, flags, sessionId);
        }

        for (Iterator<Long> iter = expectedSet.iterator(); iter.hasNext();) {
            long uid = ((Long) iter.next()).longValue();
            eventDispatcher.expunged(uid, sessionId, path);
        }
    }

    public synchronized void found(final Long uid, final Flags flags, long sessionId) {
        if (flags != null) {
            final Flags cachedFlags = cache.get(uid);
            if (cachedFlags == null || !flags.equals(cachedFlags)) {
                eventDispatcher.flagsUpdated(uid, sessionId, path, cachedFlags, flags);
            }
        }
        if (uid > lastUid) {
            eventDispatcher.added(uid, sessionId, path);
            lastUid = uid;
        }
        cache.put(uid, flags);
    }

    private SortedSet<Long> getSubSet(MessageRange range) {
        final Long rangeStartLong = range.getUidFrom();
        if (range.getUidTo() > 0) {
            final long nextUidAfterRange = range.getUidTo() + 1;
            final Long nextUidAfterRangeLong = new Long(nextUidAfterRange);
            final SortedMap<Long, Flags> subMap = cache.subMap(rangeStartLong,
                    nextUidAfterRangeLong);
            final Set<Long> keySet = subMap.keySet();
            return new TreeSet<Long>(keySet);
        } else {
            return new TreeSet<Long>(cache.tailMap(rangeStartLong).keySet());
        }
    }
    
    public void addMailboxListener(MailboxListener listener) {
        eventDispatcher.addMailboxListener(listener);
    }
    
    public void mailboxDeleted(long sessionId) {
        eventDispatcher.mailboxDeleted(sessionId, path);
    }

    public void reportRenamed(MailboxPath to, long sessionId) {
        eventDispatcher.mailboxRenamed(path, to, sessionId);
        path = to;
    }
}
