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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MaildirFolder {

    /**
     * The logger.
     */
    private Log log = LogFactory.getLog(MaildirFolder.class.getName());

    public static final String VALIDITY_FILE = "james-uidvalidity";
    public static final String UIDLIST_FILE = "james-uidlist";
    public static final String CUR = "cur";
    public static final String NEW = "new";
    public static final String TMP = "tmp";
    
    private File rootFolder;
    private File curFolder;
    private File newFolder;
    private File tmpFolder;
    private File uidFile;
    
    private long lastUid = -1;
    private int messageCount = 0;
    private long uidValidity = -1;
    
    /**
     * Representation of a maildir folder containing the message folders
     * and some special files
     * @param absPath The absolute path of the mailbox folder
     */
    public MaildirFolder(String absPath) {
        log.debug("=====> absPath=" + absPath + " - " + this.hashCode());
        this.rootFolder = new File(absPath);
        this.curFolder = new File(rootFolder, CUR);
        this.newFolder = new File(rootFolder, NEW);
        this.tmpFolder = new File(rootFolder, TMP);
        this.uidFile = new File(rootFolder, UIDLIST_FILE);
    }
    
    /**
     * Returns the {@link File} of this Maildir folder.
     * @return
     */
    public File getRootFile() {
        return rootFolder;
    }
    
    /**
     * Locks the uidList file and if it has retrieved the lock, returns it.
     * Make sure to call unlockUidList() in a finally block afterwards.
     * @return The locked uidList
     */
    public File lockUidList() {
        FileLock.lock(uidFile);
        return uidFile;
    }
    
    /**
     * Unlocks the uidList file if it has been locked before.
     */
    public void unlockUidList() {
        FileLock.unlock(uidFile);
    }
    
    /**
     * Tests whether the directory belonging to this {@link MaildirFolder} exists 
     * @return true if the directory belonging to this {@link MaildirFolder} exists ; false otherwise 
     */
    public boolean exists() {
        return rootFolder.isDirectory() && curFolder.isDirectory() && newFolder.isDirectory() && tmpFolder.isDirectory();
    }
    
    /**
     * Checks whether the folder's contents have been changed after
     * the uidfile has been created.
     * @return true if the contents have been changed.
     */
    private boolean isModified() {
        long uidListModified = uidFile.lastModified();
        long curModified = curFolder.lastModified();
        long newModified = newFolder.lastModified();
        // because of bad time resolution of file systems we also check "equals"
        if (curModified >= uidListModified || newModified >= uidListModified) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns the ./cur folder of this Maildir folder.
     * @return
     */
    public File getCurFolder() {
        return curFolder;
    }
    
    /**
     * Returns the ./new folder of this Maildir folder.
     * @return
     */
    public File getNewFolder() {
        return newFolder;
    }
    
    /**
     * Returns the ./tmp folder of this Maildir folder.
     * @return
     */
    public File getTmpFolder() {
        return tmpFolder;
    }
    
    /**
     * Returns the nextUid value and increases it.
     * @return nextUid
     */
    private long getNextUid() {
        return ++lastUid;
    }
    
    /**
     * Returns the last uid used in this mailbox
     * @return lastUid
     * @throws IOException
     */
    public long getLastUid() throws IOException {
        if (lastUid == -1) {
            readLastUid();
        }
        return lastUid;
    }

    /**
     * Read the lastUid of the given mailbox from the file system.
     * @throws IOException if there are problems with the uidList file
     */
    private void readLastUid() throws IOException {
        File uidList = lockUidList();
        try {
            if (!uidList.exists())
                createUidFile();
            FileReader fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            reader.close();
            fileReader.close();
            if (line != null)
                readUidListHeader(line);
        }
        finally {
            unlockUidList();
        }
    }
    
    /**
     * Returns the uidValidity of this mailbox
     * @return
     * @throws IOException
     */
    public long getUidValidity() throws IOException {
        if (uidValidity == -1)
            uidValidity = readUidValidity();
        return uidValidity;
    }
    
    /**
     * Sets the uidValidity for this mailbox and writes it to the file system
     * @param uidValidity
     * @throws IOException
     */
    public void setUidValidity(long uidValidity) throws IOException {
        saveUidValidity(uidValidity);
        this.uidValidity = uidValidity;
    }

    /**
     * Read the uidValidity of the given mailbox from the file system.
     * If the respective file is not yet there, it gets created and
     * filled with a brand new uidValidity.
     * @return The uidValidity
     * @throws IOException if there are problems with the validity file
     */
    private long readUidValidity() throws IOException {
        File validityFile = new File(rootFolder, VALIDITY_FILE);
        if (!validityFile.exists()) {
            return resetUidValidity();
        }
        FileInputStream fis = new FileInputStream(validityFile);
        InputStreamReader isr = new InputStreamReader(fis);
        char[] uidValidity = new char[20];
        int len = isr.read(uidValidity);
        isr.close();
        fis.close();
        return Long.parseLong(String.valueOf(uidValidity, 0, len).trim());
    }

    /**
     * Save the given uidValidity to the file system
     * @param uidValidity
     * @throws IOException
     */
    private void saveUidValidity(long uidValidity) throws IOException {
        File validityFile = new File(rootFolder, VALIDITY_FILE);
        validityFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(validityFile);
        fos.write(String.valueOf(uidValidity).getBytes());
        fos.close();
    }
    
    /**
     * Sets and returns a new uidValidity for this folder.
     * @return the new uidValidity
     * @throws IOException
     */
    private long resetUidValidity() throws IOException {
        // using the timestamp as uidValidity
        long timestamp = System.currentTimeMillis();
        setUidValidity(timestamp);
        return timestamp;
    }
    
    /**
     * Searches the uid list for a certain uid and returns the according {@link MaildirMessageName}
     * @param uid The uid to search for
     * @return The {@link MaildirMessageName} that belongs to the uid
     * @throws IOException If the uidlist file cannot be found or read
     */
    public MaildirMessageName getMessageNameByUid(Long uid) throws IOException, FileNotFoundException {
        File uidList = lockUidList();
        MaildirMessageName messageName = null;
        try {
            FileReader fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            String uidString = String.valueOf(uid);
            String line = reader.readLine(); // the header
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    int gap = line.indexOf(" ");
                    if (line.substring(0, gap).equals(uidString)) {
                        fileReader.close();
                        messageName = new MaildirMessageName(this, line.substring(gap + 1));
                        break;
                    }
                }
            }
            fileReader.close();
        } finally {
            unlockUidList();
        }
        return messageName;
    }
    
    /**
     * Reads all uids between the two boundaries from the folder and returns them as
     * a sorted map together with their corresponding {@link MaildirMessageName}s.
     * @param from The lower uid limit
     * @param to The upper uid limit. <code>-1</code> disables the upper limit
     * @return a {@link Map} whith all uids in the given range and associated {@link MaildirMessageName}s
     * @throws IOException if there is a problem with the uid list file
     */
    public SortedMap<Long, MaildirMessageName> getUidMap(long from, long to)
    throws IOException, FileNotFoundException {
        SortedMap<Long, MaildirMessageName> uidMap = null;
        File uidList = lockUidList();
        try {
            if (uidList.isFile()) {
                if (isModified()) {
                    try {
                        uidMap = truncateMap(updateUidFile(), from, to);
                    } catch (FileNotFoundException e) {
                        // weird case if someone deleted the uidlist after checking its
                        // existence and before trying to update it.
                        uidMap = truncateMap(createUidFile(), from, to);
                    }
                }
                else {
                    // the uidList is up to date
                    uidMap = new TreeMap<Long, MaildirMessageName>(readUidFile(from, to));
                }
            }
            else {
                // the uidList does not exist
                uidMap = truncateMap(createUidFile(), from, to);
            }
        } finally {
            unlockUidList();
        }
        return uidMap;
    }
    
    public SortedMap<Long, MaildirMessageName> getUidMap(FilenameFilter filter, long from, long to)
    throws FileNotFoundException, IOException {
        SortedMap<Long, MaildirMessageName> allUids = getUidMap(from, to);
        SortedMap<Long, MaildirMessageName> filteredUids = new TreeMap<Long, MaildirMessageName>();
        for (Entry<Long, MaildirMessageName> entry : allUids.entrySet()) {
            if (filter.accept(null, entry.getValue().getFullName()))
                filteredUids.put(entry.getKey(), entry.getValue());
        }
        return filteredUids;
    }
    
    /**
     * Reads all uids from the uid list file which match the given filter
     * and returns as many of them as a sorted map as the limit specifies.
     * @param filter The file names of all returned items match the filter. 
     * The dir argument to {@link FilenameFilter}.accept(dir, name) will always be null.
     * @param limit The number of items; a limit smaller then 1 disables the limit
     * @return A {@link Map} with all uids and associated {@link MaildirMessageName}s
     * @throws IOException if there is a problem with the uid list file
     */
    public SortedMap<Long, MaildirMessageName> getUidMap(FilenameFilter filter, int limit) throws IOException {
        SortedMap<Long, MaildirMessageName> allUids = getUidMap(0, -1);
        SortedMap<Long, MaildirMessageName> filteredUids = new TreeMap<Long, MaildirMessageName>();
        int theLimit = limit;
        if (limit < 1)
            theLimit = allUids.size();
        int counter = 0;
        for (Entry<Long, MaildirMessageName> entry : allUids.entrySet()) {
            if (counter >= theLimit)
                break;
            if (filter.accept(null, entry.getValue().getFullName())) {
                filteredUids.put(entry.getKey(), entry.getValue());
                counter++;
            }
        }
        return filteredUids;
    }
    
    /**
     * Creates a map of recent messages.
     * @return A {@link Map} with all uids and associated {@link MaildirMessageName}s of recent messages
     * @throws IOException If there is a problem with the uid list file
     */
    public SortedMap<Long, MaildirMessageName> getRecentMessages() throws IOException {
        String[] recentFiles = getNewFolder().list();
        LinkedList<String> lines = new LinkedList<String>();
        int theLimit = recentFiles.length;
        SortedMap<Long, MaildirMessageName> recentMessages = new TreeMap<Long, MaildirMessageName>();
        File uidList = lockUidList();
        try {
            if (!uidList.isFile()) {
                uidList.createNewFile();
                String[] curFiles = curFolder.list();
                String[] newFiles = newFolder.list();
                messageCount = curFiles.length + newFiles.length;
                String[] allFiles = (String[]) ArrayUtils.addAll(curFiles, newFiles);
                for (String file : allFiles)
                    lines.add(String.valueOf(getNextUid()) + " " + file);
                PrintWriter pw = new PrintWriter(uidList);
                pw.println(createUidListHeader());
                for (String line : lines)
                    pw.println(line);
                pw.close();
            }
            else {
                FileReader fileReader = new FileReader(uidList);
                BufferedReader reader = new BufferedReader(fileReader);
                String line = reader.readLine();
                // the first line in the file contains the next uid and message count
                while ((line = reader.readLine()) != null)
                    lines.add(line);
            }
            int counter = 0;
            String line;
            while (counter < theLimit) {
                // walk backwards as recent files are supposedly recent
                try {
                    line = lines.removeLast();
                } catch (NoSuchElementException e) {
                    break; // the list is empty
                }
                if (!line.equals("")) {
                    int gap = line.indexOf(" ");
                    Long uid = Long.valueOf(line.substring(0, gap));
                    String name = line.substring(gap + 1, line.length());
                    for (String recentFile : recentFiles) {
                        if (recentFile.equals(name)) {
                            recentMessages.put(uid, new MaildirMessageName(this, recentFile));
                            counter++;
                            break;
                        }
                    }
                }
            }
        } finally {
            unlockUidList();
        }
        return recentMessages;
    }
    
    
    /**
     * Creates and returns a uid map (uid -> {@link MaildirMessageName}) and writes it to the disk
     * @return The uid map
     * @throws IOException
     */
    private Map<Long, MaildirMessageName> createUidFile() throws IOException {
        Map<Long, MaildirMessageName> uidMap = null;
        File uidList = lockUidList();
        try {
            uidList.createNewFile();
            lastUid = 0;
            String[] curFiles = curFolder.list();
            String[] newFiles = newFolder.list();
            messageCount = curFiles.length + newFiles.length;
            uidMap = new HashMap<Long, MaildirMessageName>(messageCount);
            String[] allFiles = (String[]) ArrayUtils.addAll(curFiles, newFiles);
            for (String file : allFiles)
                uidMap.put(getNextUid(), new MaildirMessageName(this, file));
            uidMap = new TreeMap<Long, MaildirMessageName>(uidMap);
            PrintWriter pw = new PrintWriter(uidList);
            pw.println(createUidListHeader());
            for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet())
                pw.println(String.valueOf(entry.getKey()) + " " + entry.getValue().getFullName());
            pw.close();
        } finally {
            unlockUidList();
        }
        return uidMap;
    }
    
    private Map<Long, MaildirMessageName> updateUidFile() throws FileNotFoundException, IOException {
        Map<Long, MaildirMessageName> uidMap = null;
        File uidList = lockUidList();
        String[] curFiles = curFolder.list();
        String[] newFiles = newFolder.list();
        messageCount = curFiles.length + newFiles.length;
        uidMap = new HashMap<Long, MaildirMessageName>(messageCount);
        HashMap<String, Long> reverseUidMap = new HashMap<String, Long>(messageCount);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            // the first line in the file contains the next uid and message count
            if (line != null)
                readUidListHeader(line);
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    int gap = line.indexOf(" ");
                    Long uid = Long.valueOf(line.substring(0, gap));
                    String name = line.substring(gap + 1, line.length());
                    reverseUidMap.put(stripMetaFromName(name), uid);
                }
            }
            fileReader.close();
            String[] allFiles = (String[]) ArrayUtils.addAll(curFiles, newFiles);
            for (String file : allFiles) {
                MaildirMessageName messageName = new MaildirMessageName(this, file);
                Long uid = reverseUidMap.get(messageName.getBaseName());
                if (uid == null)
                    uid = getNextUid();
                uidMap.put(uid, messageName);
            }
            uidMap = new TreeMap<Long, MaildirMessageName>(uidMap);
            PrintWriter pw = new PrintWriter(uidList);
            pw.println(createUidListHeader());
            for (Entry<Long, MaildirMessageName> entry : uidMap.entrySet())
                pw.println(String.valueOf(entry.getKey()) + " " + entry.getValue().getFullName());
            pw.close();
        } finally {
            unlockUidList();
        }
        return uidMap;
    }
    
    private Map<Long, MaildirMessageName> readUidFile(long from, long to) throws IOException, FileNotFoundException {
        Map<Long, MaildirMessageName> uidMap = null; 
        File uidList = lockUidList();
        try {
            FileReader fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            // the first line in the file contains the next uid and message count
            if (line != null)
                readUidListHeader(line);
            uidMap = new HashMap<Long, MaildirMessageName>(messageCount);
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) {
                    int gap = line.indexOf(" ");
                    Long uid = Long.valueOf(line.substring(0, gap));
                    if (uid >= from) {
                        if (to != -1 && uid > to)
                            break;
                        String name = line.substring(gap + 1, line.length());
                        uidMap.put(uid, new MaildirMessageName(this, name));
                    }
                }
            }
            fileReader.close();
        } finally {
            unlockUidList();
        }
        messageCount = uidMap.size();
        return uidMap;
    }
    
    /**
     * Sorts the given map and returns a subset which is constricted by a lower and an upper limit.
     * @param source The source map
     * @param from The lower limit
     * @param to The upper limit; <code>-1</code> disables the upper limit.
     * @return The sorted subset
     */
    private SortedMap<Long, MaildirMessageName> truncateMap(Map<Long, MaildirMessageName> source, long from, long to) {
        TreeMap<Long, MaildirMessageName> sortedMap;
        if (source instanceof TreeMap<?, ?>) sortedMap = (TreeMap<Long, MaildirMessageName>) source;
        else sortedMap = new TreeMap<Long, MaildirMessageName>(source);
        if (to != -1)
            return sortedMap.subMap(from, to + 1);
        return sortedMap.tailMap(from);
    }
    
    /**
     * Parses the header line in uid list files.
     * The format is: version lastUid messageCount (e.g. 1 615 273)
     * @param line The raw header line
     * @throws IOException
     */
    private void readUidListHeader(String line) throws IOException {
        int gap1 = line.indexOf(" ");
        int version = Integer.valueOf(line.substring(0, gap1));
        if (version != 1)
            throw new IOException("Cannot read uidlists with versions other than 1.");
        int gap2 = line.indexOf(" ", gap1 + 1);
        lastUid = Long.valueOf(line.substring(gap1 + 1, gap2));
        messageCount = Integer.valueOf(line.substring(gap2 + 1, line.length()));
    }
    
    /**
     * Creates a line to put as a header in the uid list file.
     * @return the line which ought to be the header
     */
    private String createUidListHeader() {
        return "1 " + String.valueOf(lastUid) + " " + String.valueOf(messageCount);
    }
    
    /**
     * Takes the name of a message file and returns only the base name.
     * @param fileName The name of the message file
     * @return the file name without meta data, the unmodified name if it doesn't have meta data
     */
    public static String stripMetaFromName(String fileName) {
        int end = fileName.indexOf(",S="); // the size
        if (end == -1)
            end = fileName.indexOf(":2,"); // the flags
        if (end == -1)
            return fileName; // there is no meta data to strip
        return fileName.substring(0, end);
    }

    /**
     * Appends a message to the uidlist and returns its uid.
     * @param name The name of the message's file
     * @return The uid of the message
     * @throws IOException
     */
    public long appendMessage(String name) throws IOException {
        File uidList = lockUidList();
        long uid = -1;
        try {
            if (uidList.isFile()) {
                FileReader fileReader = new FileReader(uidList);
                BufferedReader reader = new BufferedReader(fileReader);
                String line = reader.readLine();
                // the first line in the file contains the next uid and message count
                if (line != null)
                    readUidListHeader(line);
                ArrayList<String> lines = new ArrayList<String>(messageCount);
                while ((line = reader.readLine()) != null)
                    lines.add(line);
                uid = getNextUid();
                lines.add(String.valueOf(uid) + " " + name);
                messageCount++;
                fileReader.close();
                PrintWriter writer = new PrintWriter(uidList);
                writer.println(createUidListHeader());
                for (String entry : lines)
                    writer.println(entry);
                writer.close();
            }
            else {
                // create the file
                uidList.createNewFile();
                String[] curFiles = curFolder.list();
                String[] newFiles = newFolder.list();
                messageCount = curFiles.length + newFiles.length;
                ArrayList<String> lines = new ArrayList<String>(messageCount);
                String[] allFiles = (String[]) ArrayUtils.addAll(curFiles, newFiles);
                for (String file : allFiles) {
                    long theUid = getNextUid();
                    lines.add(String.valueOf(theUid) + " " + file);
                    // the listed names already include the message to append
                    if (file.equals(name))
                        uid = theUid;
                }
                PrintWriter pw = new PrintWriter(uidList);
                pw.println(createUidListHeader());
                for (String line : lines)
                    pw.println(line);
                pw.close();
            }
        } finally {
            unlockUidList();
        }
        if (uid == -1)
            throw new FileNotFoundException(name);
        return uid;
    }

    /**
     * Updates an entry in the uid list.
     * @param uid
     * @param messageName
     * @throws IOException
     */
    public void update(long uid, String messageName) throws IOException {
        File uidList = lockUidList();
        try {
            FileReader fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            readUidListHeader(line);
            ArrayList<String> lines = new ArrayList<String>(messageCount);
            while ((line = reader.readLine()) != null) {
                if (uid == Long.valueOf(line.substring(0, line.indexOf(" "))))
                    line = String.valueOf(uid) + " " + messageName;
                lines.add(line);
            }
            fileReader.close();
            PrintWriter writer = new PrintWriter(uidList);
            writer.println(createUidListHeader());
            for (String entry : lines)
                writer.println(entry);
            writer.close();
        } finally {
            unlockUidList();
        }
    }
    
    /**
     * Retrieves the file belonging to the given uid, deletes it and updates
     * the uid list.
     * @param uid The uid of the message to delete
     * @return The {@link MaildirMessageName} of the deleted message
     * @throws IOException If the file cannot be deleted of there is a problem with the uid list
     */
    public MaildirMessageName delete(long uid) throws IOException {
        MaildirMessageName deletedMessage = null;
        File uidList = lockUidList();
        try {
            FileReader fileReader = new FileReader(uidList);
            BufferedReader reader = new BufferedReader(fileReader);
            readUidListHeader(reader.readLine());
            ArrayList<String> lines = new ArrayList<String>(messageCount-1);
            String line;
            while ((line = reader.readLine()) != null) {
                int gap = line.indexOf(" ");
                if (uid == Long.valueOf(line.substring(0, line.indexOf(" ")))) {
                    deletedMessage = new MaildirMessageName(this, line.substring(gap + 1, line.length()));
                    messageCount--;
                }
                else {
                    lines.add(line);
                }
            }
            fileReader.close();
            if (deletedMessage != null) {
                if (!deletedMessage.getFile().delete())
                    throw new IOException("Cannot delete file " + deletedMessage.getFile().getAbsolutePath());
                PrintWriter writer = new PrintWriter(uidList);
                writer.println(createUidListHeader());
                for (String entry : lines)
                    writer.println(entry);
                writer.close();
            }
        } finally {
            unlockUidList();
        }
        return deletedMessage;
    }
    
    /** 
     * The absolute path of this folder.
     */
    @Override
    public String toString() {
        return getRootFile().getAbsolutePath();
    }
    
}
