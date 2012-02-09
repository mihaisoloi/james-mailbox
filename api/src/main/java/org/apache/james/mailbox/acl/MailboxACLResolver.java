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

import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRight;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRights;

/**
 * Implements the interpretation of ACLs.
 * 
 * From RFC4314: <cite>It is possible for multiple identifiers in an access
 * control list to apply to a given user. For example, an ACL may include rights
 * to be granted to the identifier matching the user, one or more
 * implementation-defined identifiers matching groups that include the user,
 * and/or the identifier "anyone". How these rights are combined to determine
 * the users access is implementation defined. An implementation may choose,
 * for example, to use the union of the rights granted to the applicable
 * identifiers. An implementation may instead choose, for example, to use only
 * those rights granted to the most specific identifier present in the ACL. A
 * client can determine the set of rights granted to the logged-in user for a
 * given mailbox name by using the MYRIGHTS command. </cite>
 * 
 */
public interface MailboxACLResolver {

    /**
     * Applies global ACL to the given <code>resourceACL</code>. From RFC 4314:
     * An implementation [...] MAY force rights to always or never be granted to
     * particular identifiers.
     * 
     * @param resourceACL
     * @param resourceOwnerIsGroup
     * @return
     * @throws UnsupportedRightException
     */
    public abstract MailboxACL applyGlobalACL(MailboxACL resourceACL, boolean resourceOwnerIsGroup) throws UnsupportedRightException;

    /**
     * Tells whether the given user has the given right granted on the basis of
     * the given resourceACL. Global ACL (if there is any) should be applied
     * within this method.
     * 
     * @param requestUser
     *            the user for whom the given right is tested, possibly
     *            <code>null</code> when there is no authenticated user in the
     *            given context.
     * @param groupMembershipResolver
     *            this resolver is used when checking whether any group rights
     *            contained in resourceACL are applicable for the requestUser.
     * @param right
     *            the right which will be proven to apply for the given
     *            requestUser.
     * @param resourceACL
     *            the ACL defining the access right for the resource in
     *            question.
     * @param resourceOwner
     *            this user name is used as a replacement for the "owner" place
     *            holder in the resourceACL.
     * @param resourceOwnerIsGroup
     *            true if the resourceOwner is a group of users, false
     *            otherwise.
     * @return true if the given user has the given right for the given
     *         resource; false otherwise.
     * @throws UnsupportedRightException
     */
    boolean hasRight(String requestUser, GroupMembershipResolver groupMembershipResolver, MailboxACLRight right, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException;

    /**
     * Computes the rights which apply to the given user and resource. Global ACL (if there is any) should be applied
     * within this method.
     * 
     * @param requestUser
     *            the user for whom the rights are computed, possibly
     *            <code>null</code> when there is no authenticated user in the
     *            given context.
     * @param groupMembershipResolver
     *            this resolver is used when checking whether any group rights
     *            contained in resourceACL are applicable for the requestUser.
     * @param resourceACL
     *            the ACL defining the access right for the resource in
     *            question.
     * @param resourceOwner
     *            this user name is used as a replacement for the "owner" place
     *            holder in the resourceACL.
     * @param resourceOwnerIsGroup
     *            true if the resourceOwner is a group of users, false
     *            otherwise.
     * @return the rights applicable for the given user and resource.
     * @throws UnsupportedRightException
     */
    public abstract MailboxACLRights listRights(String requestUser, GroupMembershipResolver groupMembershipResolver, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException;

}
