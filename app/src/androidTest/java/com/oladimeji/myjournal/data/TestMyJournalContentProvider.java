package com.oladimeji.myjournal.data;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TestMyJournalContentProvider {
    /* Context used to access various parts of the system */
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    /**
     * Because I annotate this method with the @Before annotation, this method will be called
     * before every single method with an @Test annotation. We want to start each test clean, so we
     * delete all entries in the thoughts directory to do so.
     */
    @Before
    public void setUp() {
        /* Use MyJournalDbHelper to get access to a writable database */
        MyJournalDbHelper dbHelper = new MyJournalDbHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(MyJournalContract.MyJournalEntry.TABLE_NAME, null, null);
    }

    /**
     * This test checks to make sure that the content provider is registered correctly in the
     * AndroidManifest file. If it fails, you should check the AndroidManifest to see if you've
     * added a <provider/> tag and that you've properly specified the android:authorities attribute.
     */

    @Test
    public void testProviderRegistry() {

        /*
         * A ComponentName is an identifier for a specific application component, such as an
         * Activity, ContentProvider, BroadcastReceiver, or a Service.
         *
         * Two pieces of information are required to identify a component: the package (a String)
         * it exists in, and the class (a String) name inside of that package.
         *
         * We will use the ComponentName for our ContentProvider class to ask the system
         * information about the ContentProvider, specifically, the authority under which it is
         * registered.
         */
        String packageName = mContext.getPackageName();
        String myJournalProviderClassName = MyJournalContentProvider.class.getName();
        ComponentName componentName = new ComponentName(packageName,  myJournalProviderClassName);

        try {

            /*
             * Get a reference to the package manager. The package manager allows us to access
             * information about packages installed on a particular device. In this case, we're
             * going to use it to get some information about our ContentProvider under test.
             */
            PackageManager pm = mContext.getPackageManager();

            /* The ProviderInfo will contain the authority, which is what we want to test */
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            String actualAuthority = providerInfo.authority;
            String expectedAuthority = packageName;

            /* Make sure that the registered authority matches the authority from the Contract */
            String incorrectAuthority =
                    "Error: MyJournalContentProvider registered with authority: " + actualAuthority +
                            " instead of expected authority: " + expectedAuthority;
            assertEquals(incorrectAuthority,
                    actualAuthority,
                    expectedAuthority);

        } catch (PackageManager.NameNotFoundException e) {
            String providerNotRegisteredAtAll =
                    "Error: MyJournalContentProvider not registered at " + mContext.getPackageName();
            /*
             * This exception is thrown if the ContentProvider hasn't been registered with the
             * manifest at all. If this is the case, you need to double check your
             * AndroidManifest file
             */
            fail(providerNotRegisteredAtAll);
        }
    }
    /**
     * Tests inserting a single row of data via a ContentResolver
     */
    @Test
    public void testInsert() {

        /* Create values to insert */
        ContentValues myJournalValues = new ContentValues();
        myJournalValues .put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_DATE, "6/28/2018");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_TIME, "12:20");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_THOUGHT, "My thinking");

        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver myJournalObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                MyJournalContract.MyJournalEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                myJournalObserver);


        Uri uri = contentResolver.insert(MyJournalContract.MyJournalEntry.CONTENT_URI, myJournalValues);


        Uri expectedUri = ContentUris.withAppendedId(MyJournalContract.MyJournalEntry.CONTENT_URI, 1);

        String insertProviderFailed = "Unable to insert item through Provider";
        assertEquals(insertProviderFailed, uri, expectedUri);

        /*
         * If this fails, it's likely you didn't call notifyChange in your insert method from
         * your ContentProvider.
         */
        myJournalObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(myJournalObserver);
    }


    //================================================================================
    // Test Query (for thoughts directory)
    //================================================================================


    /**
     * Inserts data, then tests if a query for the thoughts directory returns that data as a Cursor
     */
    @Test
    public void testQuery() {

        /* Get access to a writable database */
        MyJournalDbHelper dbHelper = new MyJournalDbHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues myJournalValues = new ContentValues();
        myJournalValues .put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_DATE, "6/28/2018");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_TIME, "12:20");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_THOUGHT, "My thinking");


        /* Insert ContentValues into database and get a row ID back */
        long thoughtsRowId = database.insert(
                /* Table to insert values into */
                MyJournalContract.MyJournalEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                myJournalValues);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, thoughtsRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        /* Perform the ContentProvider query */
        Cursor myJournalCursor = mContext.getContentResolver().query(
                MyJournalContract.MyJournalEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Sort order to return in Cursor */
                null);


        String queryFailed = "Query failed to return a valid Cursor";
        assertTrue(queryFailed, myJournalCursor  != null);

        /* We are done with the cursor, close it now. */
        myJournalCursor .close();
    }


    //================================================================================
    // Test Delete (for a single item)
    //================================================================================


    /**
     * Tests deleting a single row of data via a ContentResolver
     */
    @Test
    public void testDelete() {
        /* Access writable database */
        MyJournalDbHelper helper = new MyJournalDbHelper(InstrumentationRegistry.getTargetContext());
        SQLiteDatabase database = helper.getWritableDatabase();

         /* Create values to insert */
        ContentValues myJournalValues = new ContentValues();
        myJournalValues .put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_DATE, "6/28/2018");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_TIME, "12:20");
        myJournalValues.put(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_THOUGHT, "My thinking");


        /* Insert ContentValues into database and get a row ID back */
        long thoughtRowId = database.insert(
                /* Table to insert values into */
                MyJournalContract.MyJournalEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                myJournalValues);

        /* Always close the database when you're through with it */
        database.close();

        String insertFailed = "Unable to insert into the database";
        assertTrue(insertFailed, thoughtRowId != -1);


        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver thoughtObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (tasks) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                MyJournalContract.MyJournalEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                thoughtObserver);



        /* The delete method deletes the previously inserted row with id = 1 */
        Uri uriToDelete =  MyJournalContract.MyJournalEntry.CONTENT_URI.buildUpon().appendPath("1").build();
        int thoughtsDeleted = contentResolver.delete(uriToDelete, null, null);

        String deleteFailed = "Unable to delete item in the database";
        assertTrue(deleteFailed, thoughtsDeleted != 0);

        /*
         * If this fails, it's likely you didn't call notifyChange in your delete method from
         * your ContentProvider.
         */
        thoughtObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver( thoughtObserver);
    }



}
