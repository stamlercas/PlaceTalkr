package com.stamler.socialmediaproject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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

public class MainActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener {

    //UserLocalStore userLocalStore;

    protected ListView list;
    //protected JSONObject jObj;
    //protected ListAdapter adapter;
    protected ImageButton btnPost;
    protected TextView txtPost;

    private GoogleApiClient mGoogleApiClient;
    private Place place;
    private ArrayList<Place> places;

    private boolean alreadyLoaded = false;          //flag to stop refreshing

    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView nvDrawer;
    protected TextView txtHeader;

    protected PostsCreator posts;

    /* REFACTORED
    protected int start = 0;               //don't need anymore because of InfiniteScrollListener class
    protected boolean endOfList = false;
    protected static final int pageSize = 25;
    protected boolean flag_loading = false;
    protected ArrayList<HashMap<String, String>> posts;*/

    protected View footer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.setDrawerListener(drawerToggle);
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        setupDrawerContent(nvDrawer);
        txtHeader = (TextView)findViewById(R.id.txtHeader);

        //instantiate google api client
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        places = new ArrayList<>();


        /* REFACTORED
        //check if user is logged in
        userLocalStore = new UserLocalStore(this);
        if (userLocalStore.getLoggedInUser() == null)
        {
            finish();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }*/

        //posts = new ArrayList<>(); REFACTORED

        //jObj = new JSONObject();  REFACTORED
        txtPost = (TextView)findViewById(R.id.txtPost);
        list = (ListView)findViewById(R.id.list);
        /* REFACTORED
        //footer will always be at bottom until end of list is reached
        footer = getLayoutInflater().inflate(R.layout.list_footer, null);
        list.addFooterView(footer);*/

        btnPost = (ImageButton)findViewById(R.id.btnPost);
        //listeners
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //get individual post and display comments if not first to post in location
                if (!posts.getFirstToPost()) {    //TODO: if the view being selected is the footer view, then it will null obj
                    String postID = ((TextView) view.findViewById(R.id.postID)).getText().toString();
                    Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
                    intent.putExtra("postID", postID);
                    startActivityForResult(intent, 100);
                }
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

    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    public void onStart()
    {
        super.onStart();
        txtHeader.setText(userLocalStore.getLoggedInUser().getUsername());
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

    /* REFACTORED
    //because java doesn't support default parameters....
    public void getPosts(JSONObject jObj, Place place)
    {
        getPosts(jObj, place, 0, pageSize);
    }

    //return true to tell when its done loading
    public boolean getPosts(JSONObject jObj, Place place, final int page, int pageSize) {
        //list.setAdapter(null);
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.getPostsDataInBackground(jObj, place, page, pageSize, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    //if no object is returned, then assume it did not connect to db
                    if (returnedJSONObject == null && !endOfList) {
                        showErrorMessage("Sorry, the app could not connect.");
                        txtPost.setEnabled(false);
                    } else if (returnedJSONObject.getInt("success") == 1) {
                        endOfList = returnedJSONObject.getBoolean("EndOfList");
                        //gets the object with the array in it
                        //set the action bar title to name
                        setTitle(returnedJSONObject.getString("PlaceName").replaceAll("\\\\",""));
                        JSONArray jsonArray = returnedJSONObject.getJSONArray("posts");
                        //if page is 0, then the first posts are being displayed
                        if (page == 0)
                        {
                            displayPosts(jsonArray);
                            start++;
                        }
                        else {
                            addPosts(jsonArray);
                            start++;
                        }
                    } else if (returnedJSONObject.getInt("success") == 0)
                        showErrorMessage(returnedJSONObject.getString("message"));
                    //check to see if should remove footer
                    if (endOfList)
                        list.removeFooterView(footer);
                    //then check to see if footer is removed, if its not the end of the list, since you could pick a different location
                    //and start loading posts from the first page
                    else if (!endOfList && list.getFooterViewsCount() == 0)
                        list.addFooterView(footer);
                } catch (JSONException e) {
                    e.printStackTrace();
                    try {       //make sure the JSONException is because there have been no posts made yet
                        //have to put a try catch inside a catch...
                        if (returnedJSONObject.getInt("FirstToPost") == 1)
                            displayPosts(null);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        return true;
    }

    //display individual posts
    public void displayPosts(JSONArray jsonArray)
    {
        if (jsonArray == null || jsonArray.length() == 0)
        {
            firstToPost = true;
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
            firstToPost = false;
            posts = jsonToHashMap(jsonArray);
            adapter = new SimpleAdapter(
                    this,
                    posts,
                    R.layout.layout_posts,
                    new String[]{"postID", "username", "content", "time"},      //order matters HERE!
                    new int[]{R.id.postID, R.id.username, R.id.content, R.id.time});
            list.setAdapter(adapter);
        }
    }

    public void addPosts(JSONArray jsonArray)
    {
        firstToPost = false;
        posts.addAll( jsonToHashMap(jsonArray) );
        ((SimpleAdapter)adapter).notifyDataSetChanged();
        flag_loading = false;
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
                                        childNode.getString("Content").replaceAll("\\\\",""),
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
    }*/

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
                        //posts = new PostsCreator(MainActivity.this, list, place);
                        posts.resetAndGetContent(place);
                        txtPost.setText("");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void pickDifferentPlace()
    {
        final ArrayList<Place> temp = this.places;
        CharSequence[] places = new CharSequence[this.places.size()];
        for (int i = 0; i < places.length; i++) {
            places[i] = this.places.get(i).getName();
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Where are you?");
        dialogBuilder.setItems(places, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //start = 0;    REFACTORED
                place = temp.get(item);
                posts.resetAndGetContent(temp.get(item));
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
            case (android.R.id.home):
                mDrawer.openDrawer(GravityCompat.START);
                break;
            /*
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
                */
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
                PlaceLikelihood nearest = null;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    if (nearest != null) {
                        if (placeLikelihood.getLikelihood() > nearest.getLikelihood()) {   //short circuting
                            nearest = placeLikelihood;
                            place = nearest.getPlace();
                        }
                    }
                    //if its null, then the first place is the most likely
                    else
                        nearest = placeLikelihood;
                    //maybe this isn't a good idea..
                    //if (placeLikelihood.getLikelihood() > 0.00000)
                    places.add(placeLikelihood.getPlace());
                    Log.i("GOOGLE PLACES", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                }
                //ADD TEST LOCATION:
                //  this loation can be joined by anyone from anywhere
                places.add(new Place() {
                    @Override
                    public String getId() {
                        return "1";
                    }

                    @Override
                    public List<Integer> getPlaceTypes() {
                        return null;
                    }

                    @Override
                    public CharSequence getAddress() {
                        return null;
                    }

                    @Override
                    public Locale getLocale() {
                        return null;
                    }

                    @Override
                    public CharSequence getName() {
                        return "Test Location";
                    }

                    @Override
                    public LatLng getLatLng() {
                        return new LatLng(0, 0);
                    }

                    @Override
                    public LatLngBounds getViewport() {
                        return null;
                    }

                    @Override
                    public Uri getWebsiteUri() {
                        return null;
                    }

                    @Override
                    public CharSequence getPhoneNumber() {
                        return null;
                    }

                    @Override
                    public float getRating() {
                        return 0;
                    }

                    @Override
                    public int getPriceLevel() {
                        return 0;
                    }

                    @Override
                    public Place freeze() {
                        return null;
                    }

                    @Override
                    public boolean isDataValid() {
                        return false;
                    }
                });
                //got a nullpointerexception
                if (nearest != null || nearest.getPlace() != null) {
                    Log.i("GOOGLE PLACES", String.format("Place " +
                            nearest.getPlace().getName() + " " + nearest.getPlace().getId()));
                    place = nearest.getPlace();
                    //if (place != null)
                    //setTitle(place.getName());
                    //start = 0;
                    posts = new PostsCreator(MainActivity.this, list, place);
                    posts.resetAndGetContent(nearest.getPlace());
                }
                else
                    showErrorMessage("Sorry, the app could not find your location. Try refreshing.");
            }
            //likelyPlaces.release();
        });
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.navProfile:
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("userID", String.valueOf(userLocalStore.getLoggedInUser().getUserID()));
                startActivityForResult(intent, 100);
                break;
            case R.id.navNotHere:
                pickDifferentPlace();
                break;
            case R.id.navSettings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.navLogout:
                userLocalStore.clearUserData();
                userLocalStore.setUserLoggedIn(false);
                finish();
                startActivity(getIntent());
                break;
        }
        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true);
        //setTitle(menuItem.getTitle());    //no need
        mDrawer.closeDrawers();
        menuItem.setChecked(false);
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

    public void setTxtPost(boolean value)
    {
        txtPost.setEnabled(value);
    }
}
