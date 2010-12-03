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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Built-in file locks (e.g. FileChannel.lock()) are not thread-safe
 * meaning that the files are only locked against access outside of the
 * vm, not against other threads.
 * This class can synchronize access to the same files using one
 * {@link ReentrantLock} per file.
 * 
 * Its very important to call the {@link #unlock(File)} method
 * in a finally block to not risk a dead lock.
 * 
 * Be aware that this class will keep one {@link ReentrantLock} per {@link File} in memory. 
 * Maybe some kind of cleanup should be done here 
 *
 */
public final class FileLock {

    private static final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<String, ReentrantLock>();
    
    /**
     * Obtains a {@link Lock} for the given file. It will block if the lock for the file is
     * already held by some other thread.
     * 
     * @param file The file to lock
     */
    public static void lock(File file) {
        String key = getKey(file);
        fileLocks.putIfAbsent(key, new ReentrantLock());
        fileLocks.get(key).lock();
    }
    
    /**
     * Unlock the previous obtained {@link Lock} for the given file.
     * 
     * @param file The file to unlock
     */
    public static void unlock(File file) {
        String key = getKey(file);

        ReentrantLock lock = fileLocks.get(key);
        if (lock != null) {
            lock.unlock();
        }
    }
    
    private static String getKey(File file) {
    	return file.getAbsoluteFile().getAbsolutePath();
    }
}
