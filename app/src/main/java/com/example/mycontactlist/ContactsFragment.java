package com.example.mycontactlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;


public class ContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    /*
    * Defines an array that contains column names to move from
    * the Cursor to the ListView.
    */
    @SuppressLint("InlinedApi")
    private final static String[] FROM_COLUMNS = {
            Email.ADDRESS
    };
    /*
     * Defines an array that contains resource ids for the layout views
     * that get the Cursor column contents. The id is pre-defined in
     * the Android framework, so it is prefaced with "android.R.id"
     */
    private final static int[] TO_IDS = {
            android.R.id.text1
    };
    // Define global mutable variables
    // Define a ListView object
    ListView mContactsList;

    private CursorAdapter mCursorAdapter;

    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION =
            {
                    Email._ID,
                    Email.ADDRESS,
                    Email.TYPE,
                    Email.LABEL,
                    Contacts._ID,
                    Contacts.LOOKUP_KEY,
                    Contacts.PHOTO_THUMBNAIL_URI
            };

    // Defines the text expression
    @SuppressLint("InlinedApi")
    private static final String SELECTION =
            Email.ADDRESS + " LIKE ?" +
                    " AND " +
                    Data.MIMETYPE + " = " +
                    "'" + Email.CONTENT_ITEM_TYPE + "'";

    private static final String SORT_ORDER = Email.TYPE + " ASC ";

    // Defines a variable for the search string
    private String mSearchString;
    // Defines the array to hold values that replace the ?
    private String[] mSelectionArgs = { mSearchString };

    // Empty public constructor, required by the system
    public ContactsFragment() {
    }

    // A UI Fragment must inflate its View
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.contacts_list_view,
                container, false);
    }

    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSearchString = "ro"; // "ro of Taro/Jiro/Saburo "

        // Gets the ListView from the View list of the parent activity
        mContactsList =
                (ListView) getActivity().findViewById(R.id.contacts_fragment);
        /*
         * Instantiates the subclass of
         * CursorAdapter
         */
        mCursorAdapter =
                new ContactsAdapter(getActivity());
        // Sets the adapter for the ListView
        mContactsList.setAdapter(mCursorAdapter);

        // Set the item click listener to be the current fragment.
        mContactsList.setOnItemClickListener(this);

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            loadView();
        }
    }

    public void loadView() {
        // Initializes the loader
        getLoaderManager().initLoader(0, null, ContactsFragment.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                loadView();
            } else {
                Toast.makeText(getActivity(), "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /*
         * Makes search string into pattern and
         * stores it in the selection array
         */
        mSelectionArgs[0] = "%" + mSearchString + "%";
        // Starts the query
        return new CursorLoader(
                getActivity(),
                Data.CONTENT_URI,
                PROJECTION,
                SELECTION,
                mSelectionArgs,
                SORT_ORDER
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Put the result Cursor in the adapter for the ListView
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Delete the reference to the existing Cursor
        mCursorAdapter.swapCursor(null);
    }

    public static final int REQUEST_EDIT_CONTACT = 2;

    @Override
    public void onItemClick(AdapterView<?> parent, View item, int position, long rowID) {
        Cursor mCursor = mCursorAdapter.getCursor();

        // Moves to the Cursor row corresponding to the ListView item that was clicked
        mCursor.moveToPosition(position);

        /*
         * Once the user has selected a contact to edit,
         * this gets the contact's lookup key and _ID values from the
         * cursor and creates the necessary URI.
         */
        // Gets the lookup key column index
        int mLookupKeyIndex = mCursor.getColumnIndex(Contacts.LOOKUP_KEY);
        // Gets the lookup key value
        String mCurrentLookupKey = mCursor.getString(mLookupKeyIndex);
        // Gets the _ID column index
        int mIdIndex = mCursor.getColumnIndex(Contacts._ID);
        long mCurrentId = mCursor.getLong(mIdIndex);
        Uri mSelectedContactUri =
                Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);
        // Creates a new Intent to edit a contact
        Intent editIntent = new Intent(Intent.ACTION_EDIT);

        /*
         * Sets the contact URI to edit, and the data type that the
         * Intent must match
         */
        editIntent.setDataAndType(mSelectedContactUri,Contacts.CONTENT_ITEM_TYPE);

        // Sets the special extended data for navigation
        editIntent.putExtra("finishActivityOnSaveCompleted", true);

        // Sends the Intent
        startActivityForResult(editIntent, REQUEST_EDIT_CONTACT);
    }

    private class ContactsAdapter extends CursorAdapter {
        private LayoutInflater mInflater;


        public ContactsAdapter(Context context) {
            super(context, null, 0);

            /*
             * Gets an inflater that can instantiate
             * the ListView layout from the file.
             */
            mInflater = LayoutInflater.from(context);
        }

        /**
         * Defines a class that hold resource IDs of each item layout
         * row to prevent having to look them up each time data is
         * bound to a row.
         */
        private class ViewHolder {
            TextView email;
            QuickContactBadge quickcontact;
        }

        @Override
        public View newView(
                Context context,
                Cursor cursor,
                ViewGroup viewGroup) {
            /* Inflates the item layout. Stores resource IDs in a
             * in a ViewHolder class to prevent having to look
             * them up each time bindView() is called.
             */
            final View itemView =
                    mInflater.inflate(
                            R.layout.contacts_list_item,
                            viewGroup,
                            false
                    );
            final ViewHolder holder = new ViewHolder();
            holder.email =
                    (TextView) itemView.findViewById(R.id.text1);
            holder.quickcontact =
                    (QuickContactBadge)
                            itemView.findViewById(R.id.quickbadge);
            itemView.setTag(holder);
            return itemView;
        }

        @Override
        public void bindView(
                View view,
                Context context,
                Cursor cursor) {

            final ViewHolder holder = (ViewHolder) view.getTag();
            final String photoUri = cursor.getString(6); // PHOTO_THUMBNAIL_URI

            final String email = cursor.getString(1); // ADDRESS
            holder.email.setText(email);

            // Gets the lookup key column index
            int mLookupKeyIndex = mCursor.getColumnIndex(Contacts.LOOKUP_KEY);
            // Gets the _ID column index
            int mIdIndex = mCursor.getColumnIndex(Contacts._ID);

            /*
             * Generates a contact URI for the QuickContactBadge.
             */
            final Uri contactUri = Contacts.getLookupUri(
                    cursor.getLong(mIdIndex),
                    cursor.getString(mLookupKeyIndex));
            holder.quickcontact.assignContactUri(contactUri);
            if(photoUri != null) {
                holder.quickcontact.setImageURI(Uri.parse(photoUri));
            }
        }
    }

}