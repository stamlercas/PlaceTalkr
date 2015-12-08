package com.stamler.socialmediaproject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener {

    UserLocalStore userLocalStore;

    protected ListView list;
    protected JSONObject jObj;
    protected ListAdapter adapter;
    protected Button btnPost;
    protected TextView txtPost;

    private GoogleApiClient mGoogleApiClient;
    private Place place;
    private ArrayList<Place> places;

    private boolean alreadyLoaded = false;          //flag to stop refreshing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //instantiate google api client
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        places = new ArrayList<>();


        //check if user is logged in
        userLocalStore = new UserLocalStore(this);
        if (userLocalStore.getLoggedInUser() == null)
        {
            finish();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }

        jObj = new JSONObject();
        txtPost = (TextView)findViewById(R.id.txtPost);
        list = (ListView)findViewById(R.id.list);
        btnPost = (Button)findViewById(R.id.btnPost);
        //listeners
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO: get individual post and display comments
                String postID = ((TextView) view.findViewById(R.id.postID)).getText().toString();
                Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
                intent.putExtra("postID", postID);
                startActivityForResult(intent, 100);
            }
        });
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send post to the server
                if (txtPost.getText().length() != 0)
                    post(txtPost.getText().toString(), userLocalStore.getLoggedInUser());
            }
        });
    }

    public void onStart()
    {
        super.onStart();
        if( mGoogleApiClient != null )
            mGoogleApiClient.connect();
        //authenticate user and get all the posts to display
        //using the current place
        if (authenticate() && !alreadyLoaded) {
            getCurrentPlaceAndPost();
            alreadyLoaded = true;
        }
    }

    public void onStop()
    {
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    public void getPosts(JSONObject jObj, Place place) {
        list.setAdapter(null);
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.getPostsDataInBackground(jObj, place, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    //if no object is returned, then assume it did not connect to db
                    if (returnedJSONObject == null) {
                        showErrorMessage("Sorry, the app could not connect.");
                        txtPost.setEnabled(false);
                    } else if (returnedJSONObject.getInt("success") == 1) {
                        //gets the object with the array in it
                        //set the action bar title to name
                        setTitle(returnedJSONObject.getString("PlaceName"));
                        JSONArray jsonArray = returnedJSONObject.getJSONArray("posts");
                        displayPosts(jsonArray);
                    } else if (returnedJSONObject.getInt("success") == 0)
                        showErrorMessage(returnedJSONObject.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    try {       //make sure the JSONException is because there have been no posts made yet
                                //have to put a try catch inside a catch...
                        if (returnedJSONObject.getInt("FirstToPost") == 1)
                            displayPosts(null);
                    } catch (JSONException ex) { ex.printStackTrace(); }
                }
            }
        });

    }

    //display individual posts
    public void displayPosts(JSONArray jsonArray)
    {
        if (jsonArray == null || jsonArray.length() == 0)
        {
            ArrayList<HashMap<String, String>> hashmap = new ArrayList<>();
            HashMap<String, String> temp = new HashMap<>();
            temp.put("content", "Be the first one to post here!");
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
                    jsonToHashMap(jsonArray),
                    R.layout.layout_posts,
                    new String[]{"postID", "username", "content", "time"},      //order matters HERE!
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
                postList.add( createPost(childNode.getString("PostID"),
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
    private HashMap<String, String>createPost(String postID, String content, String username, String time){
        HashMap<String, String> post = new HashMap<>();
        post.put("postID", postID);
        post.put("content", content);
        post.put("username", username);
        post.put("time", time);
        return post;
    }

    //make sure user is logged into application
    public boolean authenticate()
    {
        if(userLocalStore.getLoggedInUser() == null)
        {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return false;
        }
        return true;
    }

    public void post(String post, User user)
    {
        JSONObject jObj = new JSONObject();
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.postDataInBackground(user, post, jObj, place, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObj) {
                try {
                    if (returnedJSONObj.getInt("success") != 1)
                        showErrorMessage(returnedJSONObj.getString("message"));
                    else    //refresh activity to see post sent
                    {
                        //do not want to refresh intent, may give other place
                        //and you want to clear the edittext too
                        getPosts(new JSONObject(), place);
                        txtPost.setText("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showErrorMessage(String msg) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setMessage(msg);
        dialogBuilder.setPositiveButton("Ok", null);
        dialogBuilder.show();
    }

    protected void pickDifferentPlace()
    {
        final ArrayList<Place> temp = this.places;
        CharSequence[] places = new CharSequence[this.places.size()];
        for (int i = 0; i < places.length; i++)
        {
            places[i] = this.places.get(i).getName();
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Where are you?");
        dialogBuilder.setItems(places, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                place = temp.get(item);
                getPosts(jObj, temp.get(item));
            }
        });
        //create it and show
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case(R.id.action_settings):
                break;
            case(R.id.action_logout):
                userLocalStore.clearUserData();
                userLocalStore.setUserLoggedIn(false);
                finish();
                startActivity(getIntent());
                break;
            case (R.id.action_findPlace):
                pickDifferentPlace();
                break;
            case(R.id.action_refresh):
                finish();
                startActivity(getIntent());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //finds the current place and places it into the place object
    //can get posts from db using place id
    public void getCurrentPlaceAndPost()
    {
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                PlaceLikelihood nearest = new PlaceLikelihood() {
                    @Override
                    public float getLikelihood() {
                        return 0;
                    }

                    @Override
                    public Place getPlace() {
                        return null;
                    }

                    @Override
                    public PlaceLikelihood freeze() {
                        return null;
                    }

                    @Override
                    public boolean isDataValid() {
                        return false;
                    }
                };
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    if (placeLikelihood.getLikelihood() > nearest.getLikelihood()) {
                        nearest = placeLikelihood;
                        place = nearest.getPlace();
                    }
                    places.add(placeLikelihood.getPlace());
                    Log.i("GOOGLE PLACES", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                }
                //got a nullpointerexception
                if (nearest != null) {
                    Log.i("GOOGLE PLACES", String.format("Place " +
                            nearest.getPlace().getName() + " " + nearest.getPlace().getId()));
                    place = nearest.getPlace();
                    //if (place != null)
                    //setTitle(place.getName());
                    getPosts(jObj, nearest.getPlace());
                }
                else
                    showErrorMessage("Sorry, the app could not find your location. Try refreshing.");
            }
            //likelyPlaces.release();
        });
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void setPlace(Place place)
    {
        this.place = place;
    }
}
