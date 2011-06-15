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
package org.apache.james.mailbox.maildir;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.james.mailbox.MailboxConstants;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxNotFoundException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.maildir.mail.model.MaildirMailbox;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.mail.model.Mailbox;

public class MaildirStore {

    public static final String PATH_USER = "%user";
    public static final String PATH_DOMAIN = "%domain";
    public static final String PATH_FULLUSER = "%fulluser";
    public static final String WILDCARD = "%";
    
    public static final String maildirDelimiter = ".";
    
    private String maildirLocation;
    
    private File maildirRootFile;
    private final MailboxPathLocker locker;
    
    /**
     * Construct a MaildirStore with a location. The location String
     * currently may contain the
     * %user,
     * %domain,
     * %fulluser
     * variables.
     * @param maildirLocation A String with variables
     * @param locker
     */
    public MaildirStore(String maildirLocation, MailboxPathLocker locker) {
        this.maildirLocation = maildirLocation;
        this.locker = locker;
    }
    
    public MaildirStore(String maildirLocation) {
        this(maildirLocation, new JVMMailboxPathLocker());
    }
    
    
    public String getMaildirLocation() {
        return maildirLocation;
    }
    /**
     * Create a {@link MaildirFolder} for a mailbox
     * @param mailbox
     * @return The MaildirFolder
     */
    public MaildirFolder createMaildirFolder(Mailbox<Integer> mailbox) {
        return new MaildirFolder(getFolderName(mailbox), new MailboxPath(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName()), locker);
    }

    /**
     * Creates a Mailbox object with data loaded from the file system
     * @param root The main maildir folder containing the mailbox to load
     * @param namespace The namespace to use
     * @param user The owner of this mailbox
     * @param folderName The name of the mailbox folder
     * @return The Mailbox object populated with data from the file system
     * @throws MailboxException If the mailbox folder doesn't exist or can't be read
     */
    public Mailbox<Integer> loadMailbox(File root, String namespace, String user, String folderName) throws MailboxException {
        String mailboxName = getMailboxNameFromFolderName(folderName);
        return loadMailbox(new File(root, folderName), new MailboxPath(namespace, user, mailboxName));
    }

    /**
     * Creates a Mailbox object with data loaded from the file system
     * @param mailboxPath The path of the mailbox
     * @return The Mailbox object populated with data from the file system
     * @throws MailboxNotFoundException If the mailbox folder doesn't exist
     * @throws MailboxException If the mailbox folder can't be read
     */
    public Mailbox<Integer> loadMailbox(MailboxPath mailboxPath)
    throws MailboxNotFoundException, MailboxException {
        MaildirFolder folder = new MaildirFolder(getFolderName(mailboxPath), mailboxPath, locker);

        if (!folder.exists())
            throw new MailboxNotFoundException(mailboxPath);
        return loadMailbox(folder.getRootFile(), mailboxPath);
    }

    /**
     * Creates a Mailbox object with data loaded from the file system
     * @param mailboxFile File object referencing the folder for the mailbox
     * @param mailboxPath The path of the mailbox
     * @return The Mailbox object populated with data from the file system
     * @throws MailboxException If the mailbox folder doesn't exist or can't be read
     */
    private Mailbox<Integer> loadMailbox(File mailboxFile, MailboxPath mailboxPath) throws MailboxException {
        long uidValidity;
        long lastUid;
        MaildirFolder folder = new MaildirFolder(mailboxFile.getAbsolutePath(), mailboxPath, locker);
        try {
            uidValidity = folder.getUidValidity();
            lastUid = folder.getLastUid();
            return new MaildirMailbox(mailboxPath, uidValidity, lastUid, folder.getHighestModSeq());

        } catch (IOException e) {
            throw new MailboxException("Unable to load Mailbox " + mailboxPath, e);
        }
    }
    
    /**
     * Inserts the user name parts in the general maildir location String
     * @param user The user to get the root for.
     * @return The name of the folder which contains the specified user's mailbox
     */
    public String userRoot(String user) {
        String path = maildirLocation.replace(PATH_FULLUSER, user);
        String[] userParts = user.split("@");
        String userName = user;
        if (userParts.length == 2) {
            userName = userParts[0];
            // At least the domain part should not handled in a case-sensitive manner
            // See MAILBOX-58
            path = path.replace(PATH_DOMAIN, userParts[1].toLowerCase(Locale.US));
        }
        path = path.replace(PATH_USER, userName);
        return path;
    }
    
    /**
     * The main maildir folder containing all mailboxes for one user
     * @param user The user name of a mailbox
     * @return A File object referencing the main maildir folder
     * @throws MailboxException If the folder does not exist or is no directory
     */
    public File getMailboxRootForUser(String user) throws MailboxException {
        String path = userRoot(user);
        File root = new File(path);
        if (!root.isDirectory())
            throw new MailboxException("Unable to load Mailbox for user " + user);
        return root;
    }
    
    /**
     * Return a File which is the root of all Maidirs.
     * The returned maidirRootFile is lazilly constructured.
     * 
     * @return maidirRootFile
     */
    public File getMaildirRoot() {
        if (maildirRootFile == null) {
            String maildirRootLocation = maildirLocation.replaceAll(PATH_FULLUSER, "");
            maildirRootLocation = maildirRootLocation.replaceAll(PATH_DOMAIN, "");
            maildirRootLocation = maildirRootLocation.replaceAll(PATH_USER, "");
            maildirRootFile = new File(maildirRootLocation);
        }        
        return maildirRootFile;
    }

    /**
     * Transforms a folder name into a mailbox name
     * @param folderName The name of the mailbox folder
     * @return The complete (namespace) name of a mailbox
     */
    public String getMailboxNameFromFolderName(String folderName) {
        String mName;
        if (folderName.equals("")) mName = MailboxConstants.INBOX;
        else
        // remove leading dot
            mName = folderName.substring(1);
        // they are equal, anyways, this might change someday...
        //if (maildirDelimiter != MailboxConstants.DEFAULT_DELIMITER_STRING)
        //    mName = mName.replace(maildirDelimiter, MailboxConstants.DEFAULT_DELIMITER_STRING);
        return mName;
    }
    
    /**
     * Get the absolute name of the folder for a specific mailbox
     * @param namespace The namespace of the mailbox
     * @param user The user of the mailbox
     * @param name The name of the mailbox
     * @return
     */
    public String getFolderName(String namespace, String user, String name) {
        String root = userRoot(user);
        // if INBOX => location == maildirLocation
        if (name.equals(MailboxConstants.INBOX))
            return root;
        StringBuffer folder = new StringBuffer(root);
        if (!root.endsWith(File.pathSeparator))
            folder.append(File.separator);
        folder.append(".");
        folder.append(name);
        return folder.toString();
    }
    
    /**
     * Get the absolute name of the folder for a specific mailbox
     * @param mailbox The mailbox
     * @return The absolute path to the folder containing the mailbox
     */
    public String getFolderName(Mailbox<Integer> mailbox) {
        return getFolderName(mailbox.getNamespace(), mailbox.getUser(), mailbox.getName());
    }
    
    /**
     * Get the absolute name of the folder for a specific mailbox
     * @param mailboxPath The MailboxPath
     * @return The absolute path to the folder containing the mailbox
     */
    public String getFolderName(MailboxPath mailboxPath) {
        return getFolderName(mailboxPath.getNamespace(), mailboxPath.getUser(), mailboxPath.getName());
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.UidProvider#nextUid(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    public long nextUid(MailboxSession session, Mailbox<Integer> mailbox) throws MailboxException {
        try {
            return createMaildirFolder(mailbox).getLastUid() +1;
        } catch (MailboxException e) {
            throw new MailboxException("Unable to generate next uid", e);
        }
    }
}
