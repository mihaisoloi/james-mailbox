package org.apache.james.mailbox.hbase.store;

public enum MessageFields {
    NOT_FOUND((byte) 0),
    FLAGS_FIELD((byte) 1),
    BODY_FIELD((byte) 2),
    PREFIX_HEADER_FIELD((byte) 3),
    HEADERS_FIELD((byte) 4),
    TO_FIELD((byte) 5),
    CC_FIELD((byte) 6),
    FROM_FIELD((byte) 7),
    BCC_FIELD((byte) 8),
    BASE_SUBJECT_FIELD((byte) 9),
    SENT_DATE_FIELD((byte) 10),
    FIRST_FROM_MAILBOX_NAME_FIELD((byte) 11),
    FIRST_TO_MAILBOX_NAME_FIELD((byte) 12),
    FIRST_CC_MAILBOX_NAME_FIELD((byte) 13),
    FIRST_FROM_MAILBOX_DISPLAY_FIELD((byte) 14),
    FIRST_TO_MAILBOX_DISPLAY_FIELD((byte) 15);

    public final byte id;

    private MessageFields(byte id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }
}