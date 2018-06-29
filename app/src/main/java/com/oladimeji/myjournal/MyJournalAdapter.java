package com.oladimeji.myjournal;

/**
 * Created by Oladimeji on 6/27/2018.
 */

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.oladimeji.myjournal.data.MyJournalContract;

/**
 * This MyJournalAdapter creates and binds ViewHolders, that hold the date, time and thoughts entered,
 * to a RecyclerView to efficiently display data.
 */
public class MyJournalAdapter extends RecyclerView.Adapter<MyJournalAdapter.MyJournalViewHolder> {

   private Cursor mCursor;
   private Context mContext;



    // An on-lick handler that i defined to make it easy for editing of thoughts

    final private ListItemClickListener mOnClickListener;

    //Interface that receives onClick messages.

    public interface ListItemClickListener{
        void onListItemClick(int clickItemIndex);
    }
    /**
     * Constructor for the MyJournalAdapter that initializes the Context.
     *
     * @param mContext the current Context
     */
    public MyJournalAdapter(Context mContext, ListItemClickListener listener){
        this.mContext = mContext;
        mOnClickListener = listener;
    }

    /**
     * Called when ViewHolders are created to fill a RecyclerView.
     *
     * @return A new MyJournalViewHolder that holds the view for each task
     */

    @Override
    public MyJournalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.thought_layout, parent, false);
        return new MyJournalViewHolder(view);
    }

    /**
     * Called by the RecyclerView to display data at a specified position in the Cursor.
     *
     * @param holder The ViewHolder to bind Cursor data to
     * @param position The position of the data in the Cursor
     */
    @Override
    public void onBindViewHolder(MyJournalViewHolder holder, int position) {
        // Indices for the _id, date, time, thoughts columns
        int idIndex = mCursor.getColumnIndex(MyJournalContract.MyJournalEntry._ID);
        int dateIndex = mCursor.getColumnIndex(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_DATE);
        int timeIndex = mCursor.getColumnIndex(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_TIME);
        int thoughtsIndex = mCursor.getColumnIndex(MyJournalContract.MyJournalEntry.COLUMN_JOURNAL_THOUGHT);

        mCursor.moveToPosition(position); // get to the right location in the cursor

        //Determine the values of the wanted data
        final int id = mCursor.getInt(idIndex);
        String date = mCursor.getString(dateIndex);
        String time = mCursor.getString(timeIndex);
        String thoughts = mCursor.getString(thoughtsIndex);

        //Set Values
        holder.itemView.setTag(id);
        holder.dateView.setText(date);
        holder.timeView.setText(time);
        holder.thoughtView.setText(thoughts);



    }

    /**
     * Returns the number of items to display.
     */
    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    /**
     * When data changes and a re-query occurs, this function swaps the old Cursor
     * with a newly updated Cursor (Cursor c) that is passed in.
     */
    public Cursor swapCursor(Cursor c){
        // check if this cursor is the same as the previous cursor (mCursor)
        if (mCursor == c) {
            return null; // bc nothing has changed
        }
        Cursor temp = mCursor;
        this.mCursor = c; // new cursor value assigned

        //check if this is a valid cursor, then update the cursor
        if (c != null) {
            this.notifyDataSetChanged();
        }
        return temp;
    }

    //Inner class for creating ViewHolders
    class MyJournalViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        //Class variables for the date, time, thought TextViews
        TextView dateView;
        TextView timeView;
        TextView thoughtView;

        /**
         * Constructor for the MyJournalHolders.
         *
         * @param itemView The view inflated in onCreateViewHolder
         */

        public MyJournalViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.date);
            timeView = itemView.findViewById(R.id.time);
            thoughtView = itemView.findViewById(R.id.thoughts);
            //onClickListener on the View passed into the constructor (use 'this' as the OnClickListener)
            itemView.setOnClickListener(this);
        }

        /**
         * Called whenever a user clicks on an item in the list.
         * @param v The View that was clicked
         */

        @Override
        public void onClick(View v) {

            int clickedPosition = getAdapterPosition();
            //int id = mCursor.getColumnIndex(MyJournalContract.MyJournalEntry._ID);
            //Log.e("Thiss", String.valueOf(id));
            mCursor.moveToPosition(clickedPosition);
          int id =   mCursor.getInt(mCursor.getColumnIndex(MyJournalContract.MyJournalEntry._ID));
            mOnClickListener.onListItemClick(id);
            Log.e("neww", String.valueOf(id));
        }
    }
}
