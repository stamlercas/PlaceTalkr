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

public class ProfileActivity extends AppCompatActivity {

    protected TextView txtUsername, txtScore, numPosts, numComments, txtFullName, mostPopPlace;
    protected JSONObject jObj;
    protected UserLocalStore userLocalStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        txtUsername = (TextView)findViewById(R.id.txtUsername);
        txtScore = (TextView)findViewById(R.id.txtScore);
        numPosts = (TextView)findViewById(R.id.txtNumPosts);
        numComments = (TextView)findViewById(R.id.txtNumComments);
        txtFullName = (TextView)findViewById(R.id.txtFullName);
        mostPopPlace = (TextView)findViewById(R.id.mostPopPlace);
        jObj = new JSONObject();

        userLocalStore = new UserLocalStore(this);
    }

    public void onStart()
    {
        super.onStart();
        getUserInfo(jObj, userLocalStore.getLoggedInUser());
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

    protected void getUserInfo(JSONObject jObj, User user)
    {
        ServerRequests serverRequests = new ServerRequests(this);
        serverRequests.getUserDataInBackground(user, jObj, new GetJSONObjectCallBack() {
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
            txtFullName.setText(jObj.getString("FirstName") + " " + jObj.getString("LastName"));
            mostPopPlace.setText("Most Popular Place: " + jObj.getString("MostPopularPlace"));
        } catch(JSONException e) {
            e.printStackTrace();
        }


    }

    private void showErrorMessage(String msg) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ProfileActivity.this);
        dialogBuilder.setMessage(msg);
        dialogBuilder.setPositiveButton("Ok", null);
        dialogBuilder.show();
    }

}
