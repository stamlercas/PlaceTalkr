package com.stamler.socialmediaproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class CommentsActivity extends BaseActivity {

    protected String postID;
    //protected JSONObject jObj;
    protected TextView content, username, time;

    //protected ListAdapter adapter;
    protected ListView list;

    protected ImageButton btnSubmit;
    protected EditText txtSubmit;

    protected UserLocalStore userLocalStore;

    protected RelativeLayout postsLayout;

    protected CommentsCreator comments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        postID = getIntent().getStringExtra("postID");

        userLocalStore = new UserLocalStore(this);
        if (userLocalStore.getLoggedInUser() == null)
        {
            finish();
            startActivity(new Intent(CommentsActivity.this, LoginActivity.class));
        }

        content = (TextView)findViewById(R.id.content);
        username = (TextView)findViewById(R.id.username);
        time = (TextView)findViewById(R.id.time);

        list = (ListView)findViewById(R.id.list);

        btnSubmit = (ImageButton)findViewById(R.id.btnSubmit);
        txtSubmit = (EditText)findViewById(R.id.txtSubmit);

        postsLayout = (RelativeLayout)findViewById(R.id.postLayout);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send post to the server
                if (txtSubmit.getText().length() != 0)
                    comment(postID, txtSubmit.getText().toString(), userLocalStore.getLoggedInUser());
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int userID = Integer.parseInt(((TextView) view.findViewById(R.id.userID)).getText().toString());
                Intent intent = new Intent(CommentsActivity.this, ProfileActivity.class);
                intent.putExtra("userID", String.valueOf(userID));
                startActivityForResult(intent, 100);
            }
        });
    }

    public void onStart()
    {
        super.onStart();
        if (authenticate()) {
            //getPost(jObj, postID);
            comments = new CommentsCreator(CommentsActivity.this, list, postID);
            comments.getContent();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id)
        {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                break;
            case(R.id.action_refresh):
                finish();
                startActivity(getIntent());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void comment(String postID, String comment, User user)
    {
        JSONObject jObj = new JSONObject();
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.commentDataInBackground(user, comment, postID, jObj, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObj) {
                try {
                    if (returnedJSONObj.getInt("success") != 1)
                        showErrorMessage(returnedJSONObj.getString("message"));
                    else    //refresh activity to see post sent
                    {
                        //do not want to refresh intent, may give other place
                        //and you want to clear the edittext too
                        comments.resetAndGetContent();
                        txtSubmit.setText("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /* REFACTORED
    public void getPost(JSONObject jObj, String postID) {
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.getIndividualPostsDataInBackground(jObj, postID, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    //if no object is returned, then assume it did not connect to db
                    if (returnedJSONObject == null) {
                        showErrorMessage("Sorry, the app could not connect.");
                    } else if (returnedJSONObject.getInt("success") == 1) {
                        //gets the object with the array in it
                        JSONObject post = returnedJSONObject.getJSONArray("post").getJSONObject(0);
                        displayPost(post);
                        JSONArray comments = returnedJSONObject.getJSONArray("comments");
                        displayComments(comments);
                    } else if (returnedJSONObject.getInt("success") == 0)
                        showErrorMessage(returnedJSONObject.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }*/

    //display individual posts
    public void displayPost(JSONObject post)
    {
        try {
            content.setText(post.getString("Content"));
            username.setText(post.getString("Username"));
            time.setText(post.getString("Time"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /*

    public void displayComments(JSONArray comments)
    {
        if (comments == null || comments.length() == 0)
        {
            ArrayList<HashMap<String, String>> hashmap = new ArrayList<>();
            HashMap<String, String> temp = new HashMap<>();
            temp.put("content", "No comments have been made.");
            hashmap.add(temp);
            adapter = new SimpleAdapter(
                    this,
                    hashmap,
                    android.R.layout.simple_list_item_1,
                    new String[]{"content"},      //order matters HERE!
                    new int[]{android.R.id.text1});
            list.setAdapter(adapter);
        }
        else {
            adapter = new SimpleAdapter(
                    this,
                    jsonToHashMap(comments),
                    R.layout.layout_comments,
                    new String[]{"commentID", "username", "content", "time"},      //order matters HERE!
                    new int[]{R.id.postID, R.id.username, R.id.content, R.id.time});
            list.setAdapter(adapter);
        }
    }

    //convert the json array into hashmap to place in listview
    public ArrayList<HashMap<String, String>> jsonToHashMap(JSONArray jArr)
    {
        ArrayList<HashMap<String, String>> postList = new ArrayList<>();
        JSONObject childNode = new JSONObject();    //for objects within array to place in list
        for (int i = 0; i < jArr.length(); i++)
        {
            try {
                childNode = jArr.getJSONObject(i);
                postList.add( createPost(childNode.getString("CommentID"),
                        childNode.getString("Content"),
                        childNode.getString("Username"),
                        childNode.getString("Time")));
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return postList;
    }

    //create individual hashmap from parameters
    private HashMap<String, String>createPost(String commentID, String content, String username, String time){
        HashMap<String, String> post = new HashMap<>();
        post.put("postID", postID);
        post.put("content", content);
        post.put("username", username);
        post.put("time", time);
        return post;
    } */

    //make sure user is logged into application
    public boolean authenticate()
    {
        if(userLocalStore.getLoggedInUser() == null)
        {
            startActivity(new Intent(CommentsActivity.this, LoginActivity.class));
            return false;
        }
        return true;
    }

    public RelativeLayout getPostsLayout()
    {
        return postsLayout;
    }

}
