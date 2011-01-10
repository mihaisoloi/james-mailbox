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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxExistsException;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.maildir.MaildirFolder;
import org.apache.james.mailbox.maildir.MaildirMessageName;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.maildir.mail.model.MaildirMailbox;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.transaction.NonTransactionalMapper;

public class MaildirMailboxMapper extends NonTransactionalMapper implements MailboxMapper<Integer> {

    /**
     * The {@link MaildirStore} the mailboxes reside in
     */
    private final MaildirStore maildirStore;
    
    /**
     * A request-scoped list of mailboxes in order to refer to them via id
     */
    private ArrayList<Mailbox<Integer>> mailboxCache = new ArrayList<Mailbox<Integer>>();
    
    public MaildirMailboxMapper(MaildirStore maildirStore) {
        this.maildirStore = maildirStore;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#delete(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public void delete(Mailbox<Integer> mailbox) throws MailboxException {
        String folderName = maildirStore.getFolderName(mailbox);
        File folder = new File(folderName);
        if (folder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                throw new MailboxException("Unable to delete Mailbox " + mailbox, e);
            }
        }
        else
            throw new MailboxNotFoundException(mailbox.getName());
    }

   
    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxByPath(org.apache.james.imap.api.MailboxPath)
     */
    public Mailbox<Integer> findMailboxByPath(MailboxPath mailboxPath)
            throws MailboxException, MailboxNotFoundException {
        String folder = maildirStore.getFolderName(mailboxPath);
        File f = new File(folder);
        if (!f.isDirectory())
            throw new MailboxNotFoundException(mailboxPath);
        Mailbox<Integer> mailbox = maildirStore.loadMailbox(f, mailboxPath);
        return cacheMailbox(mailbox);
    }
    
    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#findMailboxWithPathLike(org.apache.james.imap.api.MailboxPath)
     */
    public List<Mailbox<Integer>> findMailboxWithPathLike(MailboxPath mailboxPath)
            throws MailboxException {
        final Pattern searchPattern = Pattern.compile("[" + MaildirStore.maildirDelimiter + "]"
                + mailboxPath.getName().replace(".", "\\.").replace(MaildirStore.WILDCARD, ".*"));
        FilenameFilter filter = MaildirMessageName.createRegexFilter(searchPattern);
        File root = maildirStore.getMailboxRootForUser(mailboxPath.getUser());
        File[] folders = root.listFiles(filter);
        ArrayList<Mailbox<Integer>> mailboxList = new ArrayList<Mailbox<Integer>>();
        for (File folder : folders)
            if (folder.isDirectory()) {
                Mailbox<Integer> mailbox = maildirStore.loadMailbox(root, mailboxPath.getNamespace(), mailboxPath.getUser(), folder.getName());
                mailboxList.add(cacheMailbox(mailbox));
            }
        // INBOX is in the root of the folder
        if (Pattern.matches(mailboxPath.getName().replace(MaildirStore.WILDCARD, ".*"), MailboxConstants.INBOX)) {
            Mailbox<Integer> mailbox = maildirStore.loadMailbox(root, mailboxPath.getNamespace(), mailboxPath.getUser(), "");
            mailboxList.add(0, cacheMailbox(mailbox));
        }
        return mailboxList;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#hasChildren(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public boolean hasChildren(Mailbox<Integer> mailbox) throws MailboxException, MailboxNotFoundException {
        String searchString = mailbox.getName() + MaildirStore.maildirDelimiter + MaildirStore.WILDCARD;
        List<Mailbox<Integer>> mailboxes = findMailboxWithPathLike(
                new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), searchString));
        return (mailboxes.size() > 0);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#save(org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public void save(Mailbox<Integer> mailbox) throws MailboxException {
        try {
            Mailbox<Integer> originalMailbox = getCachedMailbox(mailbox.getMailboxId());
            MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
            if (originalMailbox.getName() != mailbox.getName()) {
                if (folder.exists())
                    throw new MailboxExistsException(mailbox.getName());
                
                MaildirFolder originalFolder = maildirStore.createMaildirFolder(originalMailbox);
                // renaming the INBOX means to move its contents to the new folder 
                if (originalMailbox.getName().equals(MailboxConstants.INBOX)) {
                    File inboxFolder = originalFolder.getRootFile();
                    File newFolder = folder.getRootFile();
                    if (!newFolder.mkdirs())
                        throw new MailboxException("Failed to saveMailbox " + mailbox);
                    originalFolder.getCurFolder().renameTo(folder.getCurFolder());
                    originalFolder.getNewFolder().renameTo(folder.getNewFolder());
                    originalFolder.getTmpFolder().renameTo(folder.getTmpFolder());
                    (new File(inboxFolder, MaildirFolder.UIDLIST_FILE)).renameTo(
                            (new File(newFolder, MaildirFolder.UIDLIST_FILE)));
                    (new File(inboxFolder, MaildirFolder.VALIDITY_FILE)).renameTo(
                            (new File(newFolder, MaildirFolder.VALIDITY_FILE)));
                    // recreate the INBOX folders, uidvalidity and uidlist will
                    // automatically be recreated later
                    originalFolder.getCurFolder().mkdir();
                    originalFolder.getNewFolder().mkdir();
                    originalFolder.getTmpFolder().mkdir();
                }
                else {
                    if (!originalFolder.getRootFile().renameTo(folder.getRootFile()))
                        throw new MailboxException("Failed to save Mailbox " + mailbox, 
                                new IOException("Could not rename folder " + originalFolder));
                }
            }
        } catch (MailboxNotFoundException e) {
            // it cannot be found and is thus new
            MaildirFolder folder = maildirStore.createMaildirFolder(mailbox);
            if (!folder.exists()) {
                boolean success = folder.getRootFile().mkdirs();
                if (!success)
                    throw new MailboxException("Failed to save Mailbox " + mailbox);
                success = folder.getCurFolder().mkdir();
                success = success && folder.getNewFolder().mkdir();
                success = success && folder.getTmpFolder().mkdir();
                if (!success)
                    throw new MailboxException("Failed to save Mailbox " + mailbox, new IOException("Needed folder structure can not be created"));

            }
            try {
                folder.setUidValidity(mailbox.getUidValidity());
            } catch (IOException ioe) {
                throw new MailboxException("Failed to save Mailbox " + mailbox, ioe);

            }
        }
        
    }

    /* (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.MailboxMapper#list()
     */
    public List<Mailbox<Integer>> list() throws MailboxException {
        
        File maildirRoot = maildirStore.getMaildirRoot();
        List<Mailbox<Integer>> mailboxList = new ArrayList<Mailbox<Integer>>();
        
       if (maildirStore.getMaildirLocation().endsWith("/" + MaildirStore.PATH_DOMAIN + "/" + MaildirStore.PATH_USER)) {
           File[] domains = maildirRoot.listFiles();
           for (File domain: domains) {
               File[] users = domain.listFiles();
               visitUsersForMailboxList(domain, users, mailboxList);
           }
           return mailboxList;
       }

       if ((maildirStore.getMaildirLocation().endsWith("/" + MaildirStore.PATH_USER)) && (maildirStore.getMaildirLocation().endsWith("/" + MaildirStore.PATH_FULLUSER))) {
           File[] users = maildirRoot.listFiles();
           visitUsersForMailboxList(null, users, mailboxList);
       }
       
       throw new UnsupportedOperationException("The MaildirLocation must end with /%domain/%user, /%user or /%fulluser.");
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.transaction.TransactionalMapper#endRequest()
     */
    public void endRequest() {
        mailboxCache.clear();
    }
    
    /**
     * Stores a copy of a mailbox in a cache valid for one request. This is to enable
     * referring to renamed mailboxes via id.
     * @param mailbox The mailbox to cache
     * @return The id of the cached mailbox
     */
    private Mailbox<Integer> cacheMailbox(Mailbox<Integer> mailbox) {
        mailboxCache.add(new MaildirMailbox(mailbox));
        int id = mailboxCache.size() - 1;
        ((MaildirMailbox) mailbox).setMailboxId(id);
        return mailbox;
    }
    
    /**
     * Retrieves a mailbox from the cache
     * @param mailboxId The id of the mailbox to retrieve
     * @return The mailbox
     * @throws MailboxNotFoundException If the mailboxId is not in the cache
     */
    private Mailbox<Integer> getCachedMailbox(Integer mailboxId) throws MailboxNotFoundException {
        if (mailboxId == null)
            throw new MailboxNotFoundException("null");
        try {
            return mailboxCache.get(mailboxId);
        } catch (IndexOutOfBoundsException e) {
            throw new MailboxNotFoundException(String.valueOf(mailboxId));
        }
    }
    
    private void visitUsersForMailboxList(File domain, File[] users, List<Mailbox<Integer>> mailboxList) throws MailboxException {
        
        String userName = null;
        
        for (File user: users) {
            
            // Special case for INBOX: Let's use the user's folder.

            MailboxPath inboxMailboxPath = MailboxPath.inbox(user.getName() + "@" + domain.getName());
            mailboxList.add(maildirStore.loadMailbox(user, inboxMailboxPath));
            
            // List all INBOX sub folders.
            
            File[] mailboxes = user.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith(".");
                }
            });
            
            for (File mailbox: mailboxes) {
                
                if (domain == null) {
                    userName = user.getName();
                }
                else {
                    userName = user.getName() + "@" + domain.getName();
                }
                
                MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, 
                        userName, 
                        mailbox.getName().substring(1));
                mailboxList.add(maildirStore.loadMailbox(mailbox, mailboxPath));

            }

        }
        
    }

}
