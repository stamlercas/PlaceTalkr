package com.stamler.socialmediaproject;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class ProfileActivity extends BaseActivity {

    protected TextView txtUsername, txtScore, numPosts, numComments, txtFullName, mostPopPlace;
    protected JSONObject jObj;

    protected int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userID = Integer.parseInt(getIntent().getStringExtra("userID"));

        txtUsername = (TextView)findViewById(R.id.txtUsername);
        txtScore = (TextView)findViewById(R.id.txtScore);
        numPosts = (TextView)findViewById(R.id.txtNumPosts);
        numComments = (TextView)findViewById(R.id.txtNumComments);
        txtFullName = (TextView)findViewById(R.id.txtFullName);
        mostPopPlace = (TextView)findViewById(R.id.mostPopPlace);
        jObj = new JSONObject();
    }

    public void onStart()
    {
        super.onStart();
        getUserInfo(jObj, userID);
        setTitle(userLocalStore.getLoggedInUser().getUsername());
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id)
        {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void getUserInfo(JSONObject jObj, int userID)
    {
        ServerRequests serverRequests = new ServerRequests(this);
        serverRequests.getUserDataInBackground(userID, jObj, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObj) {
                try {
                    if (returnedJSONObj == null)
                        showErrorMessage("Sorry, the app couldn't connect.");
                    else if (returnedJSONObj.getInt("success") == 0)
                        showErrorMessage(returnedJSONObj.getString("message"));
                    else if (returnedJSONObj.getInt("success") == 1)
                        displayInfo(returnedJSONObj.getJSONArray("UserInfo").getJSONObject(0));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    protected void displayInfo(JSONObject jObj)
    {
        try {
            txtUsername.setText(jObj.getString("Username"));
            txtScore.setText("Score: " + jObj.getString("Score"));
            numPosts.setText("Number of Posts: " + jObj.getString("NumberOfPosts"));
            numComments.setText("Number of Comments: " + jObj.getString("NumberOfComments"));
            if (userID == userLocalStore.getLoggedInUser().getUserID())     //do not display full name unless they are looking
                                                                            //at their own details
                txtFullName.setText(jObj.getString("FirstName") + " " + jObj.getString("LastName"));
            mostPopPlace.setText("Most Popular Place: " + jObj.getString("MostPopularPlace"));
        } catch(JSONException e) {
            e.printStackTrace();
        }


    }

}
