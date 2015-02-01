package uk.co.jackdh.tapchat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.net.Uri;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;


public class RecipientsActivity extends ActionBarActivity {

    public MenuItem mSendMenuItem;
    recipientsFragment recipFragment;
    protected Uri mMediaUri;
    protected String mFileType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipients);
        if (savedInstanceState == null) {
            recipFragment = new recipientsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, recipFragment)
                    .commit();
        }
        // might need to get intent from the Fragment.
        // No as it is passed to this class from main activity with the information such as
        // video or picture within the intent. It's like a hidden variable being passed.
        mMediaUri = getIntent().getData();
        mFileType = getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);



    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_send:
                ParseObject message = createMessage();
                if (message == null) {
                    //error
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.error_selecting_file)).setTitle(getString(R.string.sorry)).setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    send(message);
                    finish();
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void send(ParseObject message) {
        message.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    //success
                    Toast.makeText(RecipientsActivity.this, getString(R.string.success_message), Toast.LENGTH_LONG).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(getString(R.string.error_sending_message)).setTitle(getString(R.string.sorry)).setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    //finish finishes this activity and take them back to main.
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipients, menu);
        mSendMenuItem = menu.getItem(0);
        return true;
    }

    protected ParseObject createMessage() {
        ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGES);
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        message.put(ParseConstants.KEY_RECIPIENT_IDS, recipFragment.getRecipientIds());
        message.put(ParseConstants.KEY_FILE_TYPE, mFileType);

        byte[] fileBytes = FileHelper.getByteArrayFromFile(this, mMediaUri);
        if (fileBytes == null) {
            return null;
        } else {
            if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
                fileBytes = FileHelper.reduceImageForUpload(fileBytes);
            }

            String fileName = FileHelper.getFileName(this, mMediaUri, mFileType);
            ParseFile file = new ParseFile(fileName, fileBytes);
            message.put(ParseConstants.KEY_FILE, file);
            return message;
        }
    }


    @SuppressLint("ValidFragment")
    public class recipientsFragment extends ListFragment {

        public final String TAG = recipientsFragment.class.getSimpleName();

        protected List<ParseUser> mContacts;
        protected ParseRelation<ParseUser> mFriendsRelation;
        protected ParseUser mCurrentUser;

        private ListView mListView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            //TODO: Add spinner.
        }

        public ArrayList getRecipientIds() {
            ArrayList<String> recipientIds = new ArrayList<String>();
            for (int i =0; i< getListView().getCount(); i++) {
                if (getListView().isItemChecked(i)) {
                    recipientIds.add(mContacts.get(i).getObjectId());
                }
            }
            return recipientIds;

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_recipients, container, false);
            return rootView;
        }
        @Override
        public void onResume() {
            super.onResume();

            mCurrentUser = ParseUser.getCurrentUser();
            mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

            ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
            query.addAscendingOrder(ParseConstants.KEY_USERNAME);
            mFriendsRelation.getQuery().findInBackground( new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> parseUsers, ParseException e) {
                    //TODO: Add progress spinner.
                    if (e == null) {
                        mContacts = parseUsers;
                        String[] usernames = new String[mContacts.size()];
                        int i = 0;
                        for (ParseUser user : mContacts) {
                            usernames[i] = user.getUsername();
                            i++;

                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                getListView().getContext(),
                                android.R.layout.simple_list_item_checked,
                                usernames);
                        getListView().setAdapter(adapter);
                        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    } else {
                        Log.e(TAG, e.getMessage());
                        AlertDialog.Builder builder = new AlertDialog.Builder(getListView().getContext());
                        builder.setMessage(e.getMessage())
                                .setTitle(R.string.error_title)
                                .setPositiveButton(android.R.string.ok, null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            });
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            if (l.getCheckedItemCount() >0) {
                mSendMenuItem.setVisible(true);
            } else {
                mSendMenuItem.setVisible(false);
            }

        }

    }

}
