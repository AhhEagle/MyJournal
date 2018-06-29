package com.oladimeji.myjournal;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.oladimeji.myjournal.data.MyJournalContract.MyJournalEntry;

import java.util.Calendar;

//Allows user to create a new thought or edit an existing one
public class AddThoughtActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the thought data loader
     */
    private static final int EXISTING_THOUGHT_LOADER = 0;

    /**
     * Content URI for the existing thought (null if it's a new thought)
     */
    private Uri mCurrentThoughtUri;

    /**
     * EditText field to enter the date of the thought
     */
    private EditText mDateEditText;

    /**
     * EditText field to enter the time of thought entered
     */
    private EditText mTimeEditText;

    /**
     * EditText field to enter the thoughts
     */
    private EditText mThoughtEditText;

    /**
     * Boolean flag that keeps track of whether the thought has been edited (true) or not (false)
     */
    private boolean mThoughtHasChanged = false;



    //DatePickerDialog Identifier
    DatePickerDialog datePickerDialog;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mThoughtHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
                    mThoughtHasChanged = true;
                    view.performClick();
                    return false;
        }
    };
    //A logic use to control the datePicker and TimePicker not to show more than once
    boolean mFirst = true;
    boolean mFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_thought);



        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new thought or editing an existing one.
        Intent intent = getIntent();
        mCurrentThoughtUri = intent.getData();

        // If the intent DOES NOT contain a thought content URI, then we know that we are
        // creating a new thought.
        if (mCurrentThoughtUri == null) {
            // This is a new thought, so change the app bar to say "Add a Thought"
            setTitle(getString(R.string.add_thoughts));
        } else {
            // Otherwise this is an existing thought, so change app bar to say "Edit Thought"
            setTitle(getString(R.string.edit_thought));

            // Initialize a loader to read the thought data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_THOUGHT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mDateEditText = findViewById(R.id.date_thought);
        mTimeEditText = findViewById(R.id.time_thought);
        mThoughtEditText = findViewById(R.id.thought_entered);

        //perform click event on edit text to show calendar picker
        mDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //calender class instance to get current date, month and year

                final Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);  // year selected
                int mMonth = c.get(Calendar.MONTH); // month selected
                int mDay = c.get(Calendar.DAY_OF_MONTH); //day selected
                //date picker dialog
                datePickerDialog = new DatePickerDialog(AddThoughtActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        //set day of month, month and year value in the edit text
                        mDateEditText.setText((month + 1) + "/" + dayOfMonth + "/" + year);
                    }
                }, mYear, mMonth, mDay);

                if (mFirst){
                    mFirst = false;
                    datePickerDialog.show();
                }


            }
        });

        //perform click event on edit text to show the time picker
        mTimeEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(AddThoughtActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        mTimeEditText.setText( selectedHour + ":" + selectedMinute);
                    }
                }, hour, minute, true);//Yes 24 hour time
                if (mFirstTime){
                    mFirstTime = false;
                    mTimePicker.setTitle("Select Time");
                    mTimePicker.show();
                }

            }
        });

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mDateEditText.setOnTouchListener(mTouchListener);
        mTimeEditText.setOnTouchListener(mTouchListener);
        mThoughtEditText.setOnTouchListener(mTouchListener);
    }

    //Get user input and save thoughts in the database.

    private void saveThought() {
        //Read from input fields
        //Use trim to eliminate leading or trailing white space
        String dateString = mDateEditText.getText().toString().trim();
        String timeString = mTimeEditText.getText().toString().trim();
        String thoughtsString = mThoughtEditText.getText().toString().trim();

        //Check if this is supposed to be a new thought to be entered
        //and check if all the fields are blank
        if (mCurrentThoughtUri == null &&
                TextUtils.isEmpty(dateString) && TextUtils.isEmpty(timeString) &&
                TextUtils.isEmpty(thoughtsString)) {
            // Since no fields were modified, we can return early without creating a new thought.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }
        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(MyJournalEntry.COLUMN_JOURNAL_DATE, dateString);
        values.put(MyJournalEntry.COLUMN_JOURNAL_TIME, timeString);
        values.put(MyJournalEntry.COLUMN_JOURNAL_THOUGHT, thoughtsString);

        // Determine if this is a new or existing thought by checking if mCurrentThoughtUri is null or not
        if (mCurrentThoughtUri == null) {
            // This is a NEW thought, so insert a new thought into the provider,
            // returning the content URI for the new thought.
            Uri newUri = getContentResolver().insert(MyJournalEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.error_save),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.save_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING thought, so update the thought with content URI: mCurrentThoughtUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentThoughtUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentThoughtUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.error_update),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.update_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu options from the res/menu/menu_add_thought
        //This add menu items to the app bar
        getMenuInflater().inflate(R.menu.menu_add_thought, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //Save Thought
                saveThought();
                //Exit activity
                finish();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the the hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mThoughtHasChanged) {
                    NavUtils.navigateUpFromSameTask(AddThoughtActivity.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(AddThoughtActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //This method is called when the back button is pressed.


    @Override
    public void onBackPressed() {
        // If the thought hasn't changed, continue with handling back button press
        if (!mThoughtHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the Addthought shows all pet attributes, define a projection that contains
        // all columns from the thought table
        String[] projection = {MyJournalEntry._ID,
        MyJournalEntry.COLUMN_JOURNAL_DATE,
        MyJournalEntry.COLUMN_JOURNAL_TIME,
        MyJournalEntry.COLUMN_JOURNAL_THOUGHT };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentThoughtUri,         // Query the content URI for the current thought
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (data == null || data.getCount() < 1) {
            return;
        }
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (data.moveToFirst()){
            // Find the columns of thought attributes that we're interested in
            int dateColumnIndex = data.getColumnIndex(MyJournalEntry.COLUMN_JOURNAL_DATE);
            int timeColumnIndex = data.getColumnIndex(MyJournalEntry.COLUMN_JOURNAL_TIME);
            int thoughtColumnIndex = data.getColumnIndex(MyJournalEntry.COLUMN_JOURNAL_THOUGHT);

            // Extract out the value from the Cursor for the given column index
            String date = data.getString(dateColumnIndex);
            String time = data.getString(timeColumnIndex);
            String thought = data.getString(thoughtColumnIndex);

            //Update the views on the screen with the values from the database
            mDateEditText.setText(date);
            mTimeEditText.setText(time);
            mThoughtEditText.setText(thought);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mDateEditText.setText("");
        mTimeEditText.setText("");
        mThoughtEditText.setText("");

    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
