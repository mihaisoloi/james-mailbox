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

package org.apache.james.mailbox;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Mailbox session.
 */
public interface MailboxSession {
  
    /**
     * Gets the session ID.
     * 
     * @return session id
     */
    public long getSessionId();

    /**
     * Is this session open?
     * 
     * @return true if the session is open, false otherwise
     */
    public boolean isOpen();

    /**
     * Closes this session.
     */
    public void close();
    
    /**
     * Gets the logger for this session context.
     * @return not null
     */
    public Log getLog();
    
    /**
     * Gets the user executing this session.
     * @return not null
     */
    public User getUser();
    
    /**
     * A mailbox user.
     * Useful for specialist mailbox implementation.
     */
    public interface User {
        /**
         * Gets the name of the user.
         * @return not null
         */
        public String getUserName();
        
        /**
         * Return the Password for the logged in user
         * 
         * @return password
         */
        public String getPassword();
        
        /**
         * Gets acceptable localisation for this user in preference order.
         * When localising a phrase, each <code>Locale</code> should be 
         * tried in order until an appropriate translation is obtained.
         * 
         * @return not null, when empty the default local should be used
         */
        public List<Locale> getLocalePreferences();
    }
    
    /**
     * Gets the <a href='http://www.isi.edu/in-notes/rfc2342.txt' rel='tag'>RFC 2342</a> 
     * personal namespace for the current session.
     * Note that though servers may offer multiple personal namespaces,
     * support is not offered through this API. This decision may be revised if reasonable
     * use cases emerge.
     * @return Personal Namespace, not null
     */
    public String getPersonalSpace();
    
    /**
     * Gets the <a href='http://www.isi.edu/in-notes/rfc2342.txt' rel='tag'>RFC 2342</a> 
     * other users namespace for the current session.
     * Note that though servers may offer multiple other users namespaces,
     * support is not offered through this API. This decision may be revised if reasonable
     * use cases emerge.
     * @return Other Users Namespace 
     * or null when there is non available
     */
    public String getOtherUsersSpace();
    
    /**
     * Iterates the <a href='http://www.isi.edu/in-notes/rfc2342.txt' rel='tag'>RFC 2342</a>
     * Shared Namespaces available for the current session.
     * @return not null though possibly empty
     */
    public Collection<String> getSharedSpaces();
    
    
    /**
     * Return the stored attributes for this {@link MailboxSession}.
     * 
     * @return attributes
     */
    public Map<Object,Object> getAttributes();
    
    /**
     * Return server side, folder path separator
     * @return path separator
     */
    public char getPathDelimiter();
}
