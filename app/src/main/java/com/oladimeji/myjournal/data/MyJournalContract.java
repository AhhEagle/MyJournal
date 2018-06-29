package com.oladimeji.myjournal.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the app
 */

public class MyJournalContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.

    private MyJournalContract(){}

    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.oladimeji.myjournal";
    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's)
     * For instance, content://com.oladimeji.myjournal/thoughts/ is a valid path for
     * this app
     */
    public static final String PATH_JOURNAL = "thoughts";

    /**
     * Inner class that defines constant values for the journals database table.
     * Each entry in the table represents a single thought.
     */
    public static final class MyJournalEntry implements BaseColumns{
        /**The content URI to access the data in the provider*/
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_JOURNAL);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of thoughts input .
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_JOURNAL;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single thought.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_JOURNAL;


        /** Name of database table for thoughts */
        public final static String TABLE_NAME = "thoughts";


        /**
         * Unique ID number for the thoughts (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Date the event occured.
         *
         * Type: TEXT
         */
        public final static String COLUMN_JOURNAL_DATE ="date";

        /**
         * The Time the event happened
         *
         * Type: TEXT
         */
        public final static String COLUMN_JOURNAL_TIME = "time";
        /**
         * The events that happened at that moment
         *
         * Type: TEXT
         */
        public final static String COLUMN_JOURNAL_THOUGHT = "thought";
    }
}
