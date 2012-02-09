/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.acl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRight;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRights;
import org.apache.james.mailbox.model.MailboxACL.NameType;

import com.sun.mail.mbox.Mailbox;

/**
 * An implementation which works with the union of the rights granted to the
 * applicable identifiers. Inspired by RFC 4314 Section 2.
 * 
 * In
 * {@link UnionMailboxACLResolver#listRights(String, org.apache.james.mailbox.MailboxACLResolver.GroupMembershipResolver, MailboxACL, String, boolean)}
 * all applicable negative and non-negative rights are union-ed separately and
 * the result is computed afterwards with
 * <code>nonNegativeUnion.except(negativeUnion)</code>.
 * 
 * Allows for setting dictinct global ACL for users' mailboxes on one hand and
 * group (a.k.a shared) mailboxes on the other hand. E.g. the zero parameter
 * constructor uses full rights for user mailboxes and
 * full-except-administration rights for group mailboxes.
 * 
 */
public class UnionMailboxACLResolver implements MailboxACLResolver {
    public static final MailboxACL DEFAULT_GLOBAL_GROUP_ACL = SimpleMailboxACL.OWNER_FULL_EXCEPT_ADMINISTRATION_ACL;

    /**
     * Nothing else than full rights for the owner.
     */
    public static final MailboxACL DEFAULT_GLOBAL_USER_ACL = SimpleMailboxACL.OWNER_FULL_ACL;

    private final MailboxACL groupGlobalACL;
    /**
     * Stores global ACL which is merged with ACL of every mailbox when
     * computing
     * {@link #rightsOf(String, org.apache.james.mailbox.MailboxACLResolver.GroupMembershipResolver, Mailbox)}
     * and
     * {@link #hasRight(String, Mailbox, MailboxACLRight, org.apache.james.mailbox.MailboxACLResolver.GroupMembershipResolver)}
     * .
     */
    private final MailboxACL userGlobalACL;

    /**
     * Creates a new instance of UnionMailboxACLResolver with
     * {@link #DEFAULT_GLOBAL_USER_ACL} as {@link #userGlobalACL} and
     * {@link #DEFAULT_GLOBAL_USER_ACL} as {@link #groupGlobalACL}.
     */
    public UnionMailboxACLResolver() {
        super();
        this.userGlobalACL = DEFAULT_GLOBAL_USER_ACL;
        this.groupGlobalACL = DEFAULT_GLOBAL_GROUP_ACL;
    }

    /**
     * Creates a new instance of UnionMailboxACLResolver with the given
     * globalACL.
     * 
     * @param groupGlobalACL
     * 
     * @param globalACL
     *            see {@link #userGlobalACL}, cannot be null.
     * @throws NullPointerException
     *             when globalACL is null.
     */
    public UnionMailboxACLResolver(MailboxACL userGlobalACL, MailboxACL groupGlobalACL) {
        super();
        if (userGlobalACL == null) {
            throw new NullPointerException("Missing userGlobalACL.");
        }
        if (groupGlobalACL == null) {
            throw new NullPointerException("Missing groupGlobalACL.");
        }
        this.userGlobalACL = userGlobalACL;
        this.groupGlobalACL = groupGlobalACL;
    }

    /**
     * Tells whether the given {@link MailboxACLEntryKey} is applicable for the
     * given user. If the given key is a group key, the given
     * {@link GroupMembershipResolver} is used to find out if the given user is
     * a member of the key's group. If the given key is an "owner" key, it is
     * effectively handled as if it was a {@code resourceOwner} key. To avoid
     * clash between user and group names, {@code resourceOwnerIsGroup} must
     * state explicitly if the given {@code resourceOwner} is a group.
     * 
     * @param key
     * @param user
     * @param groupMembershipResolver
     * @param resourceOwner
     * @param resourceOwnerIsGroup
     * @return
     */
    protected boolean applies(MailboxACLEntryKey key, String user, GroupMembershipResolver groupMembershipResolver, String resourceOwner, boolean resourceOwnerIsGroup) {
        final String keyName = key.getName();
        final NameType keyNameType = key.getNameType();
        if (MailboxACL.SpecialName.anybody.name().equals(keyName)) {
            /* this works also for unauthenticated users */
            return true;
        } else if (user != null) {
            /* Authenticated users */
            if (MailboxACL.SpecialName.authenticated.name().equals(keyName)) {
                return true;
            } else if (MailboxACL.SpecialName.owner.name().equals(keyName)) {
                return (!resourceOwnerIsGroup && user.equals(resourceOwner)) || (resourceOwnerIsGroup && groupMembershipResolver.isMember(user, resourceOwner));
            } else if (MailboxACL.NameType.user.equals(keyNameType)) {
                return keyName.equals(user);
            } else if (MailboxACL.NameType.group.equals(keyNameType)) {
                return groupMembershipResolver.isMember(user, keyName);
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.MailboxACLResolver#applyGlobalACL(org.apache
     * .james.mailbox.MailboxACL, boolean)
     */
    @Override
    public MailboxACL applyGlobalACL(MailboxACL resourceACL, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
        return resourceOwnerIsGroup ? resourceACL.union(groupGlobalACL) : resourceACL.union(userGlobalACL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxACLResolver#hasRight(java.
     * lang.String, org.apache.james.mailbox.store.mail.MailboxACLResolver.
     * GroupMembershipResolver,
     * org.apache.james.mailbox.MailboxACL.MailboxACLRight,
     * org.apache.james.mailbox.MailboxACL, java.lang.String)
     */
    @Override
    public boolean hasRight(String requestUser, GroupMembershipResolver groupMembershipResolver, MailboxACLRight right, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
        boolean result = false;
        Map<MailboxACLEntryKey, MailboxACLRights> entries = resourceOwnerIsGroup ? groupGlobalACL.getEntries() : userGlobalACL.getEntries();
        if (entries != null) {
            for (Iterator<Map.Entry<MailboxACLEntryKey, MailboxACLRights>> it = entries.entrySet().iterator(); it.hasNext();) {
                final Entry<MailboxACLEntryKey, MailboxACLRights> entry = it.next();
                final MailboxACLEntryKey key = entry.getKey();
                if (applies(key, requestUser, groupMembershipResolver, resourceOwner, resourceOwnerIsGroup) && entry.getValue().contains(right)) {
                    if (key.isNegative()) {
                        return false;
                    } else {
                        result = true;
                    }
                }
            }
        }

        if (resourceACL != null) {
            entries = resourceACL.getEntries();
            if (entries != null) {
                for (Iterator<Map.Entry<MailboxACLEntryKey, MailboxACLRights>> it = entries.entrySet().iterator(); it.hasNext();) {
                    final Entry<MailboxACLEntryKey, MailboxACLRights> entry = it.next();
                    final MailboxACLEntryKey key = entry.getKey();
                    if (applies(key, requestUser, groupMembershipResolver, resourceOwner, resourceOwnerIsGroup) && entry.getValue().contains(right)) {
                        if (key.isNegative()) {
                            return false;
                        } else {
                            result = true;
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.MailboxACLResolver#rightsOf(java.
     * lang.String, org.apache.james.mailbox.store.mail.MailboxACLResolver.
     * GroupMembershipResolver, org.apache.james.mailbox.MailboxACL,
     * java.lang.String)
     */
    @Override
    public MailboxACL.MailboxACLRights listRights(String user, GroupMembershipResolver groupMembershipResolver, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
        MailboxACL.MailboxACLRights[] positiveNegativePair = { SimpleMailboxACL.NO_RIGHTS, SimpleMailboxACL.NO_RIGHTS };

        listRights(user, groupMembershipResolver, userGlobalACL.getEntries(), resourceOwner, resourceOwnerIsGroup, positiveNegativePair);

        if (resourceACL != null) {
            listRights(user, groupMembershipResolver, resourceACL.getEntries(), resourceOwner, resourceOwnerIsGroup, positiveNegativePair);
        }

        return positiveNegativePair[0].except(positiveNegativePair[1]);
    }

    /**
     * What needs to be done for both global ACL and the given mailboxe's ACL.
     * 
     * @param requestUser
     * @param groupMembershipResolver
     * @param entries
     * @param resourceOwner
     * @param resourceOwnerIsGroup
     * @param positiveNegativePair
     * @throws UnsupportedRightException
     */
    private void listRights(String requestUser, GroupMembershipResolver groupMembershipResolver, final Map<MailboxACLEntryKey, MailboxACLRights> entries, String resourceOwner, boolean resourceOwnerIsGroup, MailboxACL.MailboxACLRights[] positiveNegativePair) throws UnsupportedRightException {
        if (entries != null) {
            for (Iterator<Map.Entry<MailboxACLEntryKey, MailboxACLRights>> it = entries.entrySet().iterator(); it.hasNext();) {
                final Entry<MailboxACLEntryKey, MailboxACLRights> entry = it.next();
                final MailboxACLEntryKey key = entry.getKey();
                if (applies(key, requestUser, groupMembershipResolver, resourceOwner, resourceOwnerIsGroup)) {
                    if (key.isNegative()) {
                        positiveNegativePair[1] = positiveNegativePair[1].union(entry.getValue());
                    } else {
                        positiveNegativePair[0] = positiveNegativePair[0].union(entry.getValue());
                    }
                }
            }
        }
    }

}
