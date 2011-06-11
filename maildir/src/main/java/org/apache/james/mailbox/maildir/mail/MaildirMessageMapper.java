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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageMetaData;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.UpdatedFlags;
import org.apache.james.mailbox.MessageRange.Type;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.maildir.MaildirMessageName;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.maildir.mail.model.AbstractMaildirMessage;
import org.apache.james.mailbox.maildir.mail.model.LazyLoadingMaildirMessage;
import org.apache.james.mailbox.maildir.mail.model.MaildirMessage;
import org.apache.james.mailbox.store.mail.AbstractMessageMapper;
import org.apache.james.mailbox.store.mail.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;

public class MaildirMessageMapper extends AbstractMessageMapper<Integer> {

    private final MaildirStore maildirStore;
    private final int BUF_SIZE = 2048;

    public MaildirMessageMapper(MailboxSession session,MaildirStore  maildirStore) {
        super(session);
        this.maildirStore = maildirStore;
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
    public void delete(Mailbox<Integer> mailbox, Message<Integer> message) throws MailboxException {
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
    public void findInMailbox(Mailbox<Integer> mailbox, MessageRange set, MailboxMembershipCallback<Integer> callback)
    throws MailboxException {
        final List<Message<Integer>> results;
        final long from = set.getUidFrom();
        final long to = set.getUidTo();
        final int batchSize = set.getBatchSize(); 
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
        
        if (batchSize > 0) {
            int i = 0;
            while (i * batchSize < results.size()) {
                callback.onMailboxMembers(results.subList(i * batchSize, (i + 1) * batchSize < results.size() ? (i + 1) * batchSize : results.size()));
                i++;
            }
        } else {
            callback.onMailboxMembers(results);
        }
    }

    private List<Message<Integer>> findMessageInMailboxWithUID(Mailbox<Integer> mailbox, long uid)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            MaildirMessageName messageName = folder.getMessageNameByUid(uid);
        
             ArrayList<Message<Integer>> messages = new ArrayList<Message<Integer>>();
             if (messageName != null) {
                 messages.add(new LazyLoadingMaildirMessage(mailbox, uid, messageName));
             }
             return messages;

        } catch (IOException e) {
            throw new MailboxException("Failure while search for Message with uid " + uid + " in Mailbox " + mailbox, e );
        }
    }

    private List<Message<Integer>> findMessagesInMailboxBetweenUIDs(Mailbox<Integer> mailbox,
            FilenameFilter filter, long from, long to) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        SortedMap<Long, MaildirMessageName> uidMap = null;
        try {
            if (filter != null)
                uidMap = folder.getUidMap(filter, from, to);
            else
                uidMap = folder.getUidMap(from, to);
            
            ArrayList<Message<Integer>> messages = new ArrayList<Message<Integer>>();
            for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet()) {
                messages.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
            }
            return messages;
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
       
    }


    private List<Message<Integer>> findMessagesInMailbox(Mailbox<Integer> mailbox,
            FilenameFilter filter, int limit) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            SortedMap<Long, MaildirMessageName> uidMap = folder.getUidMap(filter, limit);
            
            ArrayList<Message<Integer>> filtered = new ArrayList<Message<Integer>>(uidMap.size());
            for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet())
                filtered.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
            return filtered;
        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
       
    }

    private List<Message<Integer>> findDeletedMessageInMailboxWithUID(
            Mailbox<Integer> mailbox, long uid) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            MaildirMessageName messageName = folder.getMessageNameByUid(uid);
             ArrayList<Message<Integer>> messages = new ArrayList<Message<Integer>>();
             if (MaildirMessageName.FILTER_DELETED_MESSAGES.accept(null, messageName.getFullName())) {
                 messages.add(new LazyLoadingMaildirMessage(mailbox, uid, messageName));
             }
             return messages;

        } catch (IOException e) {
            throw new MailboxException("Failure while search for Messages in Mailbox " + mailbox, e );
        }
       
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findRecentMessagesInMailbox(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public List<Message<Integer>> findRecentMessagesInMailbox(Mailbox<Integer> mailbox)
    throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            SortedMap<Long, MaildirMessageName> recentMessageNames = folder.getRecentMessages();
            
            List<Message<Integer>> recentMessages = new ArrayList<Message<Integer>>(recentMessageNames.size());
            for (Entry<Long, MaildirMessageName> entry : recentMessageNames.entrySet())
                recentMessages.add(new LazyLoadingMaildirMessage(mailbox, entry.getKey(), entry.getValue()));
            return recentMessages;
        } catch (IOException e) {
            throw new MailboxException("Failure while search recent messages in Mailbox " + mailbox, e );
        }
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#findFirstUnseenMessageUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public Long findFirstUnseenMessageUid(Mailbox<Integer> mailbox)
    throws MailboxException {
        List<Message<Integer>> result = findMessagesInMailbox(mailbox, MaildirMessageName.FILTER_UNSEEN_MESSAGES, 1);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0).getUid();
        }
    }


    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#endRequest()
     */
    public void endRequest() {
        // not used
        
    }

    /**
     * Call {@link #calculateHigestModSeq(Mailbox)} as we do no caching here
     */
    public long getHighestModSeq(Mailbox<Integer> mailbox) throws MailboxException {
        return calculateHigestModSeq(mailbox);
    }

    /**
     * Call {@link #calculateLastUid(Mailbox)} as we do no caching here
     */
    public long getLastUid(Mailbox<Integer> mailbox) throws MailboxException {
        return calculateLastUid(mailbox);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#calculateHigestModSeq(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long calculateHigestModSeq(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            return folder.getHighestModSeq();
        } catch (IOException e) {
            throw new MailboxException("Unable to get highest mod-seq for mailbox " + mailbox, e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#calculateLastUid(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long calculateLastUid(Mailbox<Integer> mailbox) throws MailboxException {
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        try {
            return folder.getLastUid();
        } catch (IOException e) {
            throw new MailboxException("Unable to get last uid for mailbox " + mailbox, e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#copy(org.apache.james.mailbox.store.mail.model.Mailbox, long, long, org.apache.james.mailbox.store.mail.model.Message)
     */
    protected MessageMetaData copy(Mailbox<Integer> mailbox, long uid, long modSeq, Message<Integer> original) throws MailboxException {
        MaildirMessage theCopy = new MaildirMessage(mailbox, (AbstractMaildirMessage) original);
        return save(mailbox, theCopy);        
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.AbstractMessageMapper#expungeMarkedForDeletion(org.apache.james.mailbox.store.mail.model.Mailbox, org.apache.james.mailbox.MessageRange)
     */
    protected Map<Long, MessageMetaData> expungeMarkedForDeletion(Mailbox<Integer> mailbox, MessageRange set) throws MailboxException {
        List<Message<Integer>> results = new ArrayList<Message<Integer>>();
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
        Map<Long, MessageMetaData> uids = new HashMap<Long, MessageMetaData>();
        for (int i = 0; i < results.size(); i++) {
            Message<Integer> m = results.get(i);
            long uid = m.getUid();
            uids.put(uid, new SimpleMessageMetaData(m));
            delete(mailbox, m);
        }
        
        return uids;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.AbstractMessageMapper#save(org.apache
     * .james.mailbox.store.mail.model.Mailbox,
     * org.apache.james.mailbox.store.mail.model.Message)
     */
    protected MessageMetaData save(Mailbox<Integer> mailbox, Message<Integer> message) throws MailboxException {
        AbstractMaildirMessage maildirMessage = (AbstractMaildirMessage) message;
        MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
        long uid = 0;
        // a new message
        // save file to "tmp" folder
        File tmpFolder = folder.getTmpFolder();
        // The only case in which we could get problems with clashing names
        // is if the system clock
        // has been set backwards, then the server is restarted with the
        // same pid, delivers the same
        // number of messages since its start in the exact same millisecond
        // as done before and the
        // random number generator returns the same number.
        // In order to prevent this case we would need to check ALL files in
        // all folders and compare
        // them to this message name. We rather let this happen once in a
        // billion years...
        MaildirMessageName messageName = MaildirMessageName.createUniqueName(folder, message.getFullContentOctets());
        File messageFile = new File(tmpFolder, messageName.getFullName());
        FileOutputStream fos = null;
        InputStream input = null;
        try {
            messageFile.createNewFile();
            fos = new FileOutputStream(messageFile);
            input = message.getFullContent();
            byte[] b = new byte[BUF_SIZE];
            int len = 0;
            while ((len = input.read(b)) != -1)
                fos.write(b, 0, len);
        } catch (IOException ioe) {
            throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, ioe);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
            }
        }
        File newMessageFile = null;
        // delivered via SMTP, goes to ./new without flags
        if (maildirMessage.isRecent()) {
            messageName.setFlags(message.createFlags());
            newMessageFile = new File(folder.getNewFolder(), messageName.getFullName());
            // System.out.println("save new recent " + message + " as " +
            // newMessageFile.getName());
        }
        // appended via IMAP (might already have flags etc, goes to ./cur
        // directly)
        else {
            messageName.setFlags(message.createFlags());
            newMessageFile = new File(folder.getCurFolder(), messageName.getFullName());
            // System.out.println("save new not recent " + message + " as "
            // + newMessageFile.getName());
        }
        try {
            FileUtils.moveFile(messageFile, newMessageFile);
        } catch (IOException e) {
            // TODO: Try copy and delete
            throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e);
        }
        try {
            uid = folder.appendMessage(newMessageFile.getName());
            maildirMessage.setUid(uid);
            maildirMessage.setModSeq(newMessageFile.lastModified());
            return new SimpleMessageMetaData(message);
        } catch (IOException e) {
            throw new MailboxException("Failure while save Message " + message + " in Mailbox " + mailbox, e);
        }

    }

    /**
     * Do nothing as maildir store the uid and modseq everytime by it own
     */
    protected void saveSequences(Mailbox<Integer> mailbox, long lastUid, long highestModSeq) throws MailboxException {
        // Nothing todo as maildir does its own sequence-keeping
        
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#begin()
     */
    protected void begin() throws MailboxException {
        //nothing todo
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#commit()
     */
    protected void commit() throws MailboxException {
        //nothing todo
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#rollback()
     */
    protected void rollback() throws MailboxException {
        //nothing todo
    }
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MessageMapper#updateFlags(org.apache.james.mailbox.store.mail.model.Mailbox, javax.mail.Flags, boolean, boolean, org.apache.james.mailbox.MessageRange)
     */
    public Iterator<UpdatedFlags> updateFlags(final Mailbox<Integer> mailbox, final Flags flags, final boolean value, final boolean replace, final MessageRange set) throws MailboxException {
        final List<UpdatedFlags> updatedFlags = new ArrayList<UpdatedFlags>();
        final MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);

        findInMailbox(mailbox, set, new MailboxMembershipCallback<Integer>() {

            public void onMailboxMembers(List<Message<Integer>> members) throws MailboxException {
                for (final Message<Integer> member : members) {
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
                        // System.out.println("save existing " + message +
                        // " as " + messageFile.getName());
                        messageName.setFlags(maildirMessage.createFlags());
                        // this automatically moves messages from new to cur if
                        // needed
                        String newMessageName = messageName.getFullName();

                        File newMessageFile;
                        
                        // See MAILBOX-57
                        if (newFlags.contains(Flag.RECENT)) {
                            // message is recent so save it in the new folder
                            newMessageFile = new File(folder.getNewFolder(), newMessageName);
                        } else {
                            newMessageFile = new File(folder.getCurFolder(), newMessageName);
                        }
                        long modSeq;
                        // if the flags don't have change we should not try to move the file
                        if (newMessageFile.equals(messageFile) == false) {
                            FileUtils.moveFile(messageFile, newMessageFile );
                            modSeq = newMessageFile.lastModified();

                        } else {
                            modSeq = messageFile.lastModified();
                        } 
                        maildirMessage.setModSeq(modSeq);
                        
                        updatedFlags.add(new UpdatedFlags(member.getUid(), modSeq, originalFlags, newFlags));

                        long uid = maildirMessage.getUid();
                        folder.update(uid, newMessageName);
                    } catch (IOException e) {
                        throw new MailboxException("Failure while save Message " + member + " in Mailbox " + mailbox, e);
                    }

                }

            }
        });
        
        return updatedFlags.iterator();       
        
    }

}
