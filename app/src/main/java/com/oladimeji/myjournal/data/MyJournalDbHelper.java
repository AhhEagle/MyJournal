package com.oladimeji.myjournal.data;

/**
 * Created by Oladimeji on 6/26/2018.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.oladimeji.myjournal.data.MyJournalContract.MyJournalEntry;

/**
 * Database helper for MyJournal app. Manages database creation and version management.
 */
public class MyJournalDbHelper  extends SQLiteOpenHelper{

    /** Name of the database file */
    private static final String DATABASE_NAME = "journal.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link MyJournalDbHelper}.
     *
     * @param context of the app
     */
    public MyJournalDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the thoughts table
      final String CREATE_TABLE = "CREATE TABLE " + MyJournalEntry.TABLE_NAME + " ("
              + MyJournalEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
              + MyJournalEntry.COLUMN_JOURNAL_DATE + " TEXT NOT NULL, "
              + MyJournalEntry.COLUMN_JOURNAL_TIME + " TEXT NOT NULL, "
              + MyJournalEntry.COLUMN_JOURNAL_THOUGHT + " TEXT NOT NULL );";

       db.execSQL(CREATE_TABLE);

    }
    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MyJournalEntry.TABLE_NAME);
    }
}
