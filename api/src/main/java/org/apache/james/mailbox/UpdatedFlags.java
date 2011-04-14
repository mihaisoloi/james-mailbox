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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.UnsupportedOperationException;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

/**
 * Represent a Flag update for a message
 * 
 *
 */
public class UpdatedFlags implements Iterable<Flags.Flag>{

    private final long uid;
    private final Flags oldFlags;
    private final Flags newFlags;
    private final boolean[] modifiedFlags;

    public UpdatedFlags(long uid, Flags oldFlags, Flags newFlags) {
       this.uid = uid;
       this.oldFlags = oldFlags;
       this.newFlags = newFlags;
       this.modifiedFlags = new boolean[NUMBER_OF_SYSTEM_FLAGS];
       this.modifiedFlags[0] = isChanged(oldFlags, newFlags, Flags.Flag.ANSWERED);
       this.modifiedFlags[1] = isChanged(oldFlags, newFlags, Flags.Flag.DELETED);
       this.modifiedFlags[2] = isChanged(oldFlags, newFlags, Flags.Flag.DRAFT);
       this.modifiedFlags[3] = isChanged(oldFlags, newFlags, Flags.Flag.FLAGGED);
       this.modifiedFlags[4] = isChanged(oldFlags, newFlags, Flags.Flag.RECENT);
       this.modifiedFlags[5] = isChanged(oldFlags, newFlags, Flags.Flag.SEEN);
    }
    

    private static boolean isChanged(final Flags original, final Flags updated, Flags.Flag flag) {
        return original != null && updated != null && (original.contains(flag) ^ updated.contains(flag));
    }

    private static final Flags.Flag[] FLAGS = { Flags.Flag.ANSWERED, Flags.Flag.DELETED, Flags.Flag.DRAFT, Flags.Flag.FLAGGED, Flags.Flag.RECENT, Flags.Flag.SEEN };

    private static final int NUMBER_OF_SYSTEM_FLAGS = 6;

    
    /**
     * Return the old {@link Flags} for the message
     * 
     * @return oldFlags
     */
    public Flags getOldFlags() {
        return oldFlags;
    }
    
    /**
     * Return the new {@link Flags} for the message
     * 
     * @return newFlags
     */
    public Flags getNewFlags() {
        return newFlags;
    }
    
    /**
     * Return the uid for the message whichs {@link Flags} was updated
     * 
     * @return uid
     */
    public long getUid() {
        return uid;
    }
    
    private class FlagsIterator implements Iterator<Flag> {
        private int position;

        public FlagsIterator() {
            position = 0;
            nextPosition();
        }

        private void nextPosition() {
            if ((position < NUMBER_OF_SYSTEM_FLAGS) && (!modifiedFlags[position])) {
                position++;
                nextPosition();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return position < NUMBER_OF_SYSTEM_FLAGS;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#next()
         */
        public Flag next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final Flag result = FLAGS[position++];
            nextPosition();
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("Read only");
        }
    }

    /**
     * Gets an iterator for the system flags changed.
     * 
     * @return <code>Flags.Flag</code> <code>Iterator</code>, not null
     */

    public Iterator<Flags.Flag> iterator() {
        return new FlagsIterator();
    }
}
