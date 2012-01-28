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

package org.apache.james.mailbox;

import org.apache.james.mailbox.MailboxACL.NameType;
import org.apache.james.mailbox.MailboxACL.SpecialName;
import org.apache.james.mailbox.SimpleMailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.SimpleMailboxACL.SimpleMailboxACLEntryKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Palaga
 */
public class UnionMailboxACLResolverTest {

    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";

    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";

    private MailboxACL anybodyRead;
    private MailboxACL anybodyReadNegative;
    private UnionMailboxACLResolver anyoneReadListGlobal;
    private MailboxACL authenticatedRead;
    private UnionMailboxACLResolver authenticatedReadListWriteGlobal;
    private MailboxACL authenticatedReadNegative;
    private MailboxACL group1Read;
    private MailboxACL group1ReadNegative;
    private SimpleGroupMembershipResolver groupMembershipResolver;
    private UnionMailboxACLResolver negativeGroup2FullGlobal;
    private UnionMailboxACLResolver noGlobals;
    private UnionMailboxACLResolver ownerFullGlobal;
    private MailboxACL ownerRead;
    private MailboxACL ownerReadNegative;
    private MailboxACL user1Read;
    private MailboxACL user1ReadNegative;

    @Before
    public void setUp() throws Exception {
        MailboxACL acl = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACL.AUTHENTICATED_KEY, SimpleMailboxACL.FULL_RIGHTS) });
        authenticatedReadListWriteGlobal = new UnionMailboxACLResolver(acl, acl);
        acl = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACL.ANYBODY_KEY, new Rfc4314Rights("rl")) });
        anyoneReadListGlobal = new UnionMailboxACLResolver(acl, acl);
        acl = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACL.OWNER_KEY, SimpleMailboxACL.FULL_RIGHTS) });
        ownerFullGlobal = new UnionMailboxACLResolver(acl, acl);
        noGlobals = new UnionMailboxACLResolver(SimpleMailboxACL.EMPTY, SimpleMailboxACL.EMPTY);
        acl = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(new SimpleMailboxACLEntryKey(GROUP_2, NameType.group, true), SimpleMailboxACL.FULL_RIGHTS) });
        negativeGroup2FullGlobal = new UnionMailboxACLResolver(acl, new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(new SimpleMailboxACLEntryKey(GROUP_2, NameType.group, true), SimpleMailboxACL.FULL_RIGHTS) }));

        groupMembershipResolver = new SimpleGroupMembershipResolver();
        groupMembershipResolver.addMembership(GROUP_1, USER_1);
        groupMembershipResolver.addMembership(GROUP_2, USER_2);

        user1Read = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createUser(USER_1), new Rfc4314Rights("r")) });
        user1ReadNegative = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createUser(USER_1, true), new Rfc4314Rights("r")) });

        group1Read = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createGroup(GROUP_1), new Rfc4314Rights("r")) });
        group1ReadNegative = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createGroup(GROUP_1, true), new Rfc4314Rights("r")) });

        anybodyRead = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), new Rfc4314Rights("r")) });
        anybodyReadNegative = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody, true), new Rfc4314Rights("r")) });

        authenticatedRead = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), new Rfc4314Rights("r")) });
        authenticatedReadNegative = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated, true), new Rfc4314Rights("r")) });

        ownerRead = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), new Rfc4314Rights("r")) });
        ownerReadNegative = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner, true), new Rfc4314Rights("r")) });

    }

    @Test
    public void testAppliesNullUser() throws UnsupportedRightException {

        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_1), null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_2), null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_1), null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_2), null, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), null, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), null, groupMembershipResolver, USER_1, false));
    }

    @Test
    public void testAppliesUser() throws UnsupportedRightException {
        /* requester unequal to owner */
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_1), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_2), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_1), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_2), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), USER_1, groupMembershipResolver, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), USER_1, groupMembershipResolver, USER_1, false));

        /* requester unequal to owner user */
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_1), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_2), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_1), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_2), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), USER_1, groupMembershipResolver, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), USER_1, groupMembershipResolver, USER_2, false));

        /* requester member of owner group */
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_1), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_2), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_1), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_2), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), USER_1, groupMembershipResolver, GROUP_1, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), USER_1, groupMembershipResolver, GROUP_1, true));

        /* requester not member of owner group */
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_1), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createUser(USER_2), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_1), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createGroup(GROUP_2), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.anybody), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertTrue(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.authenticated), USER_1, groupMembershipResolver, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.applies(SimpleMailboxACLEntryKey.createSpecial(SpecialName.owner), USER_1, groupMembershipResolver, GROUP_2, true));

    }

    @Test
    public void testHasRightNullUser() throws UnsupportedRightException {

        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));
        
        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

    }

    @Test
    public void testHasRightNullUserGlobals() throws UnsupportedRightException {
        Assert.assertTrue(anyoneReadListGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, SimpleMailboxACL.EMPTY, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, SimpleMailboxACL.EMPTY, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, SimpleMailboxACL.EMPTY, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(null, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, SimpleMailboxACL.EMPTY, USER_2, false));
    }
    

    @Test
    public void testHasRightUserSelfOwner() throws UnsupportedRightException {

        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_1, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_1, false));
        
        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_1, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_1, false));

    }
    

    @Test
    public void testHasRightUserNotOwner() throws UnsupportedRightException {

        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_2, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_2, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_2, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_2, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, USER_2, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_2, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_2, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_2, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_2, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_2, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, USER_2, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_2, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_2, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_2, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_2, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_2, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, USER_2, false));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_2, false));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_2, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_2, false));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_2, false));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_2, false));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_2, false));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, USER_2, false));
        
        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_2, false));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_2, false));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_2, false));
        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_2, false));

        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_2, false));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_2, false));

        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_2, false));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_2, false));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, USER_2, false));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, USER_2, false));

    }
    @Test
    public void testHasRightUserMemberOfOwnerGroup() throws UnsupportedRightException {

        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_1, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_1, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_1, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_1, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_1, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_1, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_1, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_1, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_1, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_1, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_1, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_1, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_1, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_1, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_1, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_1, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_1, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_1, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_1, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_1, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_1, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_1, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_1, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_1, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_1, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_1, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_1, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_1, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_1, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_1, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_1, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_1, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_1, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_1, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_1, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_1, true));
        
        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_1, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_1, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_1, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_1, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_1, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_1, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_1, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_1, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_1, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_1, true));

    }    
    
    
    @Test
    public void testHasRightUserNotMemberOfOwnerGroup() throws UnsupportedRightException {

        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_2, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_2, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_2, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_2, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_2, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_2, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_2, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1Read, GROUP_2, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, user1ReadNegative, GROUP_2, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_2, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_2, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_2, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_2, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_2, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_2, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_2, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1Read, GROUP_2, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, group1ReadNegative, GROUP_2, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_2, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_2, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_2, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_2, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_2, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_2, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_2, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyRead, GROUP_2, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, anybodyReadNegative, GROUP_2, true));

        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_2, true));
        Assert.assertFalse(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_2, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_2, true));
        Assert.assertFalse(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_2, true));

        Assert.assertTrue(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_2, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_2, true));

        Assert.assertTrue(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_2, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_2, true));

        Assert.assertTrue(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedRead, GROUP_2, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, authenticatedReadNegative, GROUP_2, true));
        
        
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_2, true));
        Assert.assertTrue(anyoneReadListGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_2, true));

        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_2, true));
        Assert.assertTrue(authenticatedReadListWriteGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_2, true));

        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_2, true));
        Assert.assertFalse(ownerFullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_2, true));

        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_2, true));
        Assert.assertFalse(noGlobals.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_2, true));

        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerRead, GROUP_2, true));
        Assert.assertFalse(negativeGroup2FullGlobal.hasRight(USER_1, groupMembershipResolver, Rfc4314Rights.r_Read_RIGHT, ownerReadNegative, GROUP_2, true));

    }

}
