package com.oladimeji.myjournal;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.support.v4.content.AsyncTaskLoader;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.oladimeji.myjournal.data.MyJournalContract.MyJournalEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>, MyJournalAdapter.ListItemClickListener {

    // Constants for logging and referring to a unique loader
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int THOUGHT_LOADER_ID = 0;

    //SignIn identifier for firebase authentication

    private static final int RC_SIGN_IN = 123;

    //Firebase Instance Variables

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // Member variables for the adapter and RecyclerView
    private MyJournalAdapter mAdapter;
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Get firebase instance
        mFirebaseAuth = FirebaseAuth.getInstance();



        // Set the RecyclerView to its corresponding view
        mRecyclerView = findViewById(R.id.recyclerViewThoughts);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        View emptyView =  findViewById(R.id.empty_view);

        //When no data is populated
        /*
        boolean emptyViewVisible = mRecyclerView.getAdapter().getItemCount() == 0;

        if (mRecyclerView.getAdapter() == null && emptyViewVisible){
            emptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);

        } else{
            mRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }*/



        // Set the layout for the RecyclerView to be a linear layout, which measures and
        // positions items within a RecyclerView into a linear list
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter and attach it to the RecyclerView
        mAdapter = new MyJournalAdapter(this, this);
        mRecyclerView.setAdapter(mAdapter);

        /*
         Add a touch helper to the RecyclerView to recognize when a user swipes to delete an item.
         An ItemTouchHelper enables touch behavior (like swipe and move) on each ViewHolder,
         and uses callbacks to signal when a user is performing these actions.
         */
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Here is where I implemented swipe to delete

                // the URI for the item to delete

                int id = (int) viewHolder.itemView.getTag();

                // Build appropriate uri with String row id appended
                String stringId = Integer.toString(id);
                Uri uri = MyJournalEntry.CONTENT_URI;
                uri = uri.buildUpon().appendPath(stringId).build();

                //  Delete a single row of data using a ContentResolver
                getContentResolver().delete(uri, null, null);

                //  Restart the loader to re-query for all thoughts after a deletion
                getSupportLoaderManager().restartLoader(THOUGHT_LOADER_ID, null, MainActivity.this);

            }
        }).attachToRecyclerView(mRecyclerView);

        //firebase Auth listening to weather you are signed in or not ..If you are signed in
        //you are allowed to access the app else you will be forced to sign in

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //User is signed in
                    mRecyclerView.setAdapter(mAdapter);
                } else {
                    List<AuthUI.IdpConfig> selectedProviders = new ArrayList<>();
                    selectedProviders.add(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(selectedProviders)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        // Setup FAB to open AddThoughtActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddThoughtActivity.class);
                startActivity(intent);
            }
        });

        //Setup the item click listener



        getSupportLoaderManager().initLoader(THOUGHT_LOADER_ID, null, this);

    }

    /**
     * This method is called after this activity has been paused or restarted.
     * Often, this is after new data has been inserted through an AddThoughtActivity,
     * so this restarts the loader to re-query the underlying data for any changes.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // re-queries for all thoughts
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        getSupportLoaderManager().restartLoader(THOUGHT_LOADER_ID, null, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                mRecyclerView.setAdapter(mAdapter);
                Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Signed Cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    /**
     * Instantiates and returns a new AsyncTaskLoader with the given ID.
     * This loader will return thought data as a Cursor or null if an error occurs.
     *
     * Implements the required callbacks to take care of loading data at all stages of loading.
     */
    @Override
    public android.support.v4.content.Loader onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(this) {

            // Initialize a Cursor, this will hold all the thought data
            Cursor mThoughtData = null;

            // onStartLoading() is called when a loader first starts loading data

            @Override
            protected void onStartLoading() {
                if (mThoughtData != null) {
                    // Delivers any previously loaded data immediately
                    deliverResult(mThoughtData);
                } else {
                    // Force a new load
                    forceLoad();
                }
            }

            // loadInBackground() performs asynchronous loading of data
            @Override
            public Cursor loadInBackground() {

                // Will implement to load data

                // Query and load all thought data in the background; sort by date
                // [Hint] use a try/catch block to catch any errors in loading data
                try {
                    return getContentResolver().query(MyJournalEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load data.");
                    e.printStackTrace();
                    return null;
                }
            }

            // deliverResult sends the result of the load, a Cursor, to the registered listener
            public void deliverResult(Cursor data) {
                mThoughtData = data;
                super.deliverResult(data);
            }
            };
    }


    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader The Loader that has finished.
     * @param data The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        // Update the data that the adapter uses to create ViewHolders
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader loader) {
        mAdapter.swapCursor(null);

    }


        @Override
    public void onListItemClick(int id) {
        //Creating new intent to go to {@link AddThoughtActivity}
        Intent intent = new Intent(MainActivity.this, AddThoughtActivity.class);
        // Form the content URI that represents the specific thought that was clicked on,
        // by appending the "id" (passed as input to this method) onto the
        // {@link MyJournalEntry#CONTENT_URI}.
        Uri currentThoughtUri = ContentUris.withAppendedId(MyJournalEntry.CONTENT_URI, id);


        // Set the URI on the data field of the intent
        intent.setData(currentThoughtUri);

        // Launch the {@link AddThoughtActivity} to display the data for the current pet.
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu options from the res/menu/menu_activity_activity.xml file
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    }
