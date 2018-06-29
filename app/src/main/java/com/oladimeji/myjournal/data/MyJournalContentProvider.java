package com.oladimeji.myjournal.data;

/**
 * Created by Oladimeji on 6/26/2018.
 */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.oladimeji.myjournal.data.MyJournalContract.MyJournalEntry;
/**
 * {@link ContentProvider} for MyJournal app.
 */
public class MyJournalContentProvider extends ContentProvider {

    /**
     * URI matcher code for the content URI for the thoughts table
     */
    private static final int THOUGHTS = 100;
    /**
     * URI matcher code for the content URI for a single thought in the thoughts table
     */
    private static final int THOUGHTS_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passes into the constructor represents the code to return for the root URI
     * Its common to use NO_MATCH as the input for this case.
     */

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //Static Initiator, This is run the first time anything is called from this class.
    static {
        //The calls to addURI() go here, for all of the content URI patterns that the provider
        //should recognize. All paths added to the UriMatcher have a corresponding code to return.
        //when a match is found

        //The content URI of the form "content://com.oladimeji.myjournal/thoughts" will map to the
        //integer code {@link #THOUGHTS}. This URI is used to provide access to MULTIPLE rows
        //of the thoughts table
        sUriMatcher.addURI(MyJournalContract.CONTENT_AUTHORITY, MyJournalContract.PATH_JOURNAL, THOUGHTS);
        //The content URI of the form "content://com.oladimeji.myjournal/thoughts/#" will map to the
        //integer code {@link #THOUGHTS}. This URI is used to provide access to ONE single row
        //of the thoughts table.

        //In this case, the # wildcard is used where "#" can be substituted for an integer.
        //for example "contents://com.exmple.android.pets/pets/3" matches
        sUriMatcher.addURI(MyJournalContract.CONTENT_AUTHORITY, MyJournalContract.PATH_JOURNAL + "/#", THOUGHTS_ID);
    }

    /*Database helper object*/
    private MyJournalDbHelper mdbHelper;

    /**
     * Initialize the provider and the database helper object
     */

    @Override
    public boolean onCreate() {
        mdbHelper = new MyJournalDbHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection,  String selection, String[] selectionArgs,  String sortOrder) {
       // Get readable database
        SQLiteDatabase database = mdbHelper.getReadableDatabase();

        //This cursor will hold the result of the query
        Cursor cursor;
        //Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match){
            case THOUGHTS:
                //for the THOUGHTS code, query the thoughts table directly with the given
                //projection, selection, selection arguments and sort order, The cursor
                //could contain multiple rows at the thoughts table.
                cursor = database.query(MyJournalEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case THOUGHTS_ID:
                //for the THOUGHTS_ID code, extract out the ID from the URI
                //for an example URI such as "content://com.oladimeji.MyJournal/thoughts/2",
                //the selection will be "_id" and the selection argument will be a
                //string array containing the actual ID of 2 in this case.
                //
                //for every "2" in the selection, we need to have an element in the selection
                //arguments that will fill in the "7" since we have 1 question mark in the
                //selection, we have 1 String in the selection arguments string array.

                selection = MyJournalEntry._ID + "=?";
                selectionArgs = new String[]{ String.valueOf(ContentUris.parseId(uri)) };

                //This will perform a query on the thoughts table where thr _id equals 2 to return a
                //Cursor containing that row of the table.
                cursor = database.query(MyJournalEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("cannot query unknown UI " + uri);
        }
        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case THOUGHTS:
                return  MyJournalEntry.CONTENT_LIST_TYPE;
            case THOUGHTS_ID:
                return MyJournalEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case THOUGHTS:
                return insertThought(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a thought into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */

    private Uri insertThought(Uri uri, ContentValues values){
        // Check that the date value is not null
        String date = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_DATE);
        if (date == null){
            throw new IllegalArgumentException("Date is required");
        }
        // Check that the time value is not null
        String time = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_TIME);
        if (time == null){
            throw new IllegalArgumentException("Date is required");
        }
        // Check that the thought value is not null
        String thought = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_THOUGHT);
        if (thought == null){
            throw new IllegalArgumentException("Date is required");
        }

        // Get writable database
        SQLiteDatabase database = mdbHelper.getWritableDatabase();

        // Insert the new thought/event with the given values
        long id = database.insert(MyJournalEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed.
        if (id == -1) {
            return null;
        }
        //Notify all listeners that the data has changed for the thought content URI
        // uri: content://com.oladimeji.myjournal/thoughts
        getContext().getContentResolver().notifyChange(uri, null);

        // return the new URI with the ID  (of the newly inserted row) appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mdbHelper.getWritableDatabase();

        //Track the number of rows that were deleted.
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case THOUGHTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(MyJournalEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case THOUGHTS_ID:
                // Delete a single row given by the ID in the URI
                selection = MyJournalEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(MyJournalEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
      final int match = sUriMatcher.match(uri);
      switch (match){
          case THOUGHTS:
              return updateThought(uri, values, selection, selectionArgs);

          case THOUGHTS_ID:
              // For the THOUGHTS_ID code, extract out the ID from the URI,
              // so we know which row to update. Selection will be "_id=?" and selection
              // arguments will be a String array containing the actual ID.
              selection = MyJournalEntry._ID + "=?";
              selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
              return updateThought(uri, values, selection, selectionArgs);
          default:
              throw new IllegalArgumentException("Update is not supported for " + uri);

      }
    }

    /*
     * Update thoughts in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more thoughts).
     * Return the number of rows that were successfully updated.
     */
    private int updateThought(Uri uri, ContentValues values, String selection, String[] selectionArgs){
       //If the {@link MyJournalEntry#COLUMN_JOURNAL_DATE} key is present,
        // Check that the date value is not null
        if (values.containsKey(MyJournalEntry.COLUMN_JOURNAL_DATE)){
            String date = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_DATE);
            if (date == null){
                throw new IllegalArgumentException("Date is required");
            }
        }
        //If the {@link MyJournalEntry#COLUMN_JOURNAL_TIME} key is present,
        // Check that the time value is not null
        if (values.containsKey(MyJournalEntry.COLUMN_JOURNAL_TIME)){
            String time = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_TIME);
            if (time == null){
                throw new IllegalArgumentException("Time is required");
            }
        }

        //If the {@link MyJournalEntry#COLUMN_JOURNAL_THOUGHT} key is present,
        // Check that the thought value is not null
        if (values.containsKey(MyJournalEntry.COLUMN_JOURNAL_THOUGHT)){
            String thought = values.getAsString(MyJournalEntry.COLUMN_JOURNAL_THOUGHT);
            if (thought == null){
                throw new IllegalArgumentException("Kindly enter your thoughts");
            }
        }
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }
        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mdbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(MyJournalEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

}
