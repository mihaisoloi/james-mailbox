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
package org.apache.james.mailbox.maildir.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.mail.Flags;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageRange.Type;
import org.apache.james.mailbox.SearchQuery;
import org.apache.james.mailbox.SearchQuery.Criterion;
import org.apache.james.mailbox.SearchQuery.NumericRange;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.maildir.MaildirMessageName;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.maildir.UidConstraint;
import org.apache.james.mailbox.maildir.mail.model.AbstractMaildirMessage;
import org.apache.james.mailbox.maildir.mail.model.LazyLoadingMaildirMessage;
import org.apache.james.mailbox.maildir.mail.model.MaildirMessage;
import org.apache.james.mailbox.store.SearchQueryIterator;
import org.apache.james.mailbox.store.UpdatedFlag;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;

public class MaildirMessageMapper extends NonTransactionalMapper implements MessageMapper<Integer> {

    private final MaildirStore maildirStore;
    private final int BUF_SIZE = 2048;

    public MaildirMessageMapper(MaildirStore  maildirStore) {
        this.maildirStore = maildirStore;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long copy(Mailbox<Integer> mailbox, MailboxMembership<Integer> original)
    throws MailboxException {
        MaildirMessage theCopy = new MaildirMessage(mailbox, (AbstractMaildirMessage) original);

        return add(mailbox, theCopy);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countMessagesInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long countMessagesInMailbox(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        File newFolder = folder.getNewFolder();
        File curFolder = folder.getCurFolder();
        File[] newFiles = newFolder.listFiles();
        File[] curFiles = curFolder.listFiles();
        if (newFiles == null || curFiles == null)
            throw new MailboxException("Unable to count messages in Mailbox " + mailbox,
                    new IOException("Not a valid Maildir folder: " + maildirStore.getFolderName(mailbox)));
        int count = newFiles.length + curFiles.length;
        return count;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#countUnseenMessagesInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long countUnseenMessagesInMailbox(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        File newFolder = folder.getNewFolder();
        File curFolder = folder.getCurFolder();
        String[] unseenMessages = curFolder.list(MaildirMessageName.FILTER_UNSEEN_MESSAGES);
        String[] newUnseenMessages = newFolder.list(MaildirMessageName.FILTER_UNSEEN_MESSAGES);
        if (newUnseenMessages == null || unseenMessages == null)
            throw new MailboxException("Unable to count unseen messages in Mailbox " + mailbox,
                    new IOException("Not a valid Maildir folder: " + maildirStore.getFolderName(mailbox)));
        int count = newUnseenMessages.length + unseenMessages.length;
        return count;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#delete(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public void delete(Mailbox<Integer> mailbox, MailboxMembership<Integer> message) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            folder.delete(message.getUid());
        } catch (IOException e) {
            throw new MailboxException("Unable to delete Message " + message + " in Mailbox " + mailbox, e);
        }
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public List<MailboxMembership<Integer>> findInMailbox(Mailbox<Integer> mailbox, MessageRange set)
    throws MailboxException {
        final List<MailboxMembership<Integer>> results;
        final long from = set.getUidFrom();
        final long to = set.getUidTo();
        final Type type = set.getType();
        switch (type) {
        default:
        case ALL:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, 0, -1);
            break;
        case FROM:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, from, -1);
            break;
        case ONE:
            results = findMessageInMailboxWithUID(mailbox, from);
            break;
        case RANGE:
            results = findMessagesInMailboxBetweenUIDs(mailbox, null, from, to);
            break;       
        }
        return results;
    }

    private List<MailboxMembership<Integer>> findMessageInMailboxWithUID(Mailbox<Integer> mailbox, long uid)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        MaildirMessageName messageName = null;
        try {
             messageName = folder.getMessageNameByUid(uid);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Message with uid " + uid + " in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        if (messageName != null) {
            messages.add(new LazyLoadingMaildirMessage(mailbox, uid, messageName));
        }
        return messages;
    }

    private List<MailboxMembership<Integer>> findMessagesInMailboxBetweenUIDs(Mailbox<Integer> mailbox,
            FilenameFilter filter, long from, long to) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap = null;
        try {
            if (filter != null)
                uidMap = folder.getUidMap(filter, from, to);
            else
                uidMap = folder.getUidMap(from, to);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet()) {
            messages.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
        }
        return messages;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findMarkedForDeletionInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    public List<MailboxMembership<Integer>> findMarkedForDeletionInMailbox(Mailbox<Integer> mailbox, MessageRange set) throws MailboxException {
        List<MailboxMembership<Integer>> results = new ArrayList<MailboxMembership<Integer>>();
        final long from = set.getUidFrom();
        final long to = set.getUidTo();
        final Type type = set.getType();
        switch (type) {
        default:
        case ALL:
            results = findMessagesInMailbox(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, -1);
            break;
        case FROM:
            results = findMessagesInMailboxBetweenUIDs(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, from, -1);
            break;
        case ONE:
            results = findDeletedMessageInMailboxWithUID(mailbox, from);
            break;
        case RANGE:
            results = findMessagesInMailboxBetweenUIDs(mailbox, MaildirMessageName.FILTER_DELETED_MESSAGES, from, to);
            break;       
        }
        return results;
    }

    private List<MailboxMembership<Integer>> findMessagesInMailbox(Mailbox<Integer> mailbox,
            FilenameFilter filter, int limit) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap = null;
        try {
            uidMap = folder.getUidMap(filter, limit);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> filtered = new ArrayList<MailboxMembership<Integer>>(uidMap.size());
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet())
            filtered.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
        return filtered;
    }

    private List<MailboxMembership<Integer>> findDeletedMessageInMailboxWithUID(
            Mailbox<Integer> mailbox, long uid) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        MaildirMessageName messageName = null;
        try {
             messageName = folder.getMessageNameByUid(uid);
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
        ArrayList<MailboxMembership<Integer>> messages = new ArrayList<MailboxMembership<Integer>>();
        if (MaildirMessageName.FILTER_DELETED_MESSAGES.accept(null, messageName.getFullName())) {
            messages.add(new LazyLoadingMaildirMessage(mailbox, uid, messageName));
        }
        return messages;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public List<MailboxMembership<Integer>> findRecentMessagesInMailbox(Mailbox<Integer> mailbox)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> recentMessageNames;
        try {
            recentMessageNames = folder.getRecentMessages();
        } catch (IOException e) {
            throw new MailboxException("Failure while search recent messages in Mailbox " + mailbox, e );
        }
        List<MailboxMembership<Integer>> recentMessages = new ArrayList<MailboxMembership<Integer>>(recentMessageNames.size());
        for (Entry<Long, MaildirMessageName> entry : recentMessageNames.entrySet())
            recentMessages.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
        return recentMessages;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Integer> mailbox)
    throws MailboxException {
        List<MailboxMembership<Integer>> result = findMessagesInMailbox(mailbox, MaildirMessageName.FILTER_UNSEEN_MESSAGES, 1);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0).getUid();
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#add(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.store.mail.model.MailboxMembership)
     */
    public long add(Mailbox<Integer> mailbox, MailboxMembership<Integer> message)
    throws MailboxException {
        AbstractMaildirMessage maildirMessage = (AbstractMaildirMessage) message;
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        long uid = 0;
        // a new message
        if (maildirMessage.isNew()) {
            // save file to "tmp" folder
            File tmpFolder = folder.getTmpFolder();
            // The only case in which we could get problems with clashing names is if the system clock
            // has been set backwards, then the server is restarted with the same pid, delivers the same
            // number of messages since its start in the exact same millisecond as done before and the
            // random number generator returns the same number.
            // In order to prevent this case we would need to check ALL files in all folders and compare
            // them to this message name. We rather let this happen once in a billion years...
            MaildirMessageName messageName = MaildirMessageName.createUniqueName(folder,
                    message.getMessage().getFullContentOctets());
            File messageFile = new File(tmpFolder, messageName.getFullName());
            FileOutputStream fos = null;
            try {
                messageFile.createNewFile();
                fos = new FileOutputStream(messageFile);
                InputStream input = message.getMessage().getFullContent();
                byte[] b = new byte[BUF_SIZE];
                int len = 0;
                while ((len = input.read(b)) != -1)
                    fos.write(b, 0, len);
            }
            catch (IOException ioe) {
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, ioe );
            }
            finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File newMessageFile = null;
            // delivered via SMTP, goes to ./new without flags
            if (maildirMessage.isRecent()) {
                messageName.setFlags(message.createFlags());
                newMessageFile = new File(folder.getNewFolder(), messageName.getFullName());
                //System.out.println("save new recent " + message + " as " + newMessageFile.getName());
            }
            // appended via IMAP (might already have flags etc, goes to ./cur directly)
            else {
                messageName.setFlags(message.createFlags());
                newMessageFile = new File(folder.getCurFolder(), messageName.getFullName());
                //System.out.println("save new not recent " + message + " as " + newMessageFile.getName());
            }
            try {
                FileUtils.moveFile(messageFile, newMessageFile);
            } catch (IOException e) {
                System.err.println(newMessageFile);
                // TODO: Try copy and delete
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e );
            }
            try {
                uid = folder.appendMessage(newMessageFile.getName());
            } catch (IOException e) {
                throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e );
            }
        } else {
            throw new MailboxException("Message already exists!");
        }
        
        return uid;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#searchMailbox(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.SearchQuery)
     */
    public Iterator<Long> searchMailbox(Mailbox<Integer> mailbox, SearchQuery query)
    throws MailboxException {
        final List<Criterion> criteria = query.getCriterias();
        boolean range = false;
        int rangeLength = -1;
        UidConstraint constraint = new UidConstraint();
        
        if (criteria.size() == 1) {
            final Criterion firstCriterion = criteria.get(0);
            if (firstCriterion instanceof SearchQuery.UidCriterion) {
                final SearchQuery.UidCriterion uidCriterion = (SearchQuery.UidCriterion) firstCriterion;
                final NumericRange[] ranges = uidCriterion.getOperator().getRange();
                rangeLength = ranges.length;

                for (int i = 0; i < ranges.length; i++) {
                    final long low = ranges[i].getLowValue();
                    final long high = ranges[i].getHighValue();

                    if (low == Long.MAX_VALUE) {
                        constraint.lessOrEquals(high);
                        range = true;
                    } else if (low == high) {
                        constraint.equals(low);
                        range = false;
                    } else {
                        constraint.between(low, high);
                        range = true;
                    }
                }
            }
        }
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap;
        try {
            uidMap = folder.getUidMap(0, -1);
        } catch (IOException e) {
            throw new MailboxException("Failure while search in Mailbox " + mailbox, e );
        }
        LinkedList<MailboxMembership<?>> messages = new LinkedList<MailboxMembership<?>>();
        for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet()) {
            //System.out.println("check " + entry.getKey());
            if (constraint.isAllowed(entry.getKey())) {
                //System.out.println("allow " + entry.getKey());
                messages.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
                // Check if we only need to fetch 1 message, if so we can set a limit to speed up things
                if (rangeLength == 1 && range == false) break;
            }
        }
        return new SearchQueryIterator(messages.iterator(), query);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#endRequest()
     */
    public void endRequest() {
        // not used
        
    }


    public Iterator<UpdatedFlag> updateFlags(Mailbox<Integer> mailbox, Flags flags, boolean value, boolean replace, MessageRange set) throws MailboxException {
        final List<UpdatedFlag> updatedFlags = new ArrayList<UpdatedFlag>();
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);

        final List<MailboxMembership<Integer>> members = findInMailbox(mailbox, set);
        for (final MailboxMembership<Integer> member:members) {
            Flags originalFlags = member.createFlags();
            if (replace) {
                member.setFlags(flags);
            } else {
                Flags current = member.createFlags();
                if (value) {
                    current.add(flags);
                } else {
                    current.remove(flags);
                }
                member.setFlags(current);
            }
            Flags newFlags = member.createFlags();
            
            try {
                AbstractMaildirMessage maildirMessage = (AbstractMaildirMessage) member;
                MaildirMessageName messageName = folder.getMessageNameByUid(maildirMessage.getUid());
                File messageFile = messageName.getFile();
                //System.out.println("save existing " + message + " as " + messageFile.getName());
                messageName.setFlags(maildirMessage.createFlags());
                // this automatically moves messages from new to cur if needed
                String newMessageName = messageName.getFullName();
                messageFile.renameTo(new File(folder.getCurFolder(), newMessageName));
                long uid = maildirMessage.getUid();
                folder.update(uid, newMessageName);
            } catch (IOException e) {
                throw new MailboxException("Failure while save Message " + member + " in Mailbox " + mailbox, e );
            }
            
            updatedFlags.add(new UpdatedFlag(member.getUid(),originalFlags, newFlags));
        }
        
        return updatedFlags.iterator();       
        
    }
    
  
}
