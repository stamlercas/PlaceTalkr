package com.stamler.socialmediaproject;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Chris on 4/4/2016.
 * This class will be specifically for handling post content in the main activity. It will not be responsible for finding the place.
 * That will be the responsibility of the Main Activity, and the Main Activity will be responsible for passing on the place to
 * the PostsCreator.
 */
public class PostsCreator extends ContentCreator {
    protected Place place;

    //you can't post without a place, so the constructor should instantiate the place object
    public PostsCreator(BaseActivity mActivity, ListView list, Place place){
        super(mActivity, list);
        this.place = place;
    }

    //create individual hashmap from parameters
    //TODO: put into PostsCreator class. There will be a seperate function for createComments for the CommentCreator class
    private HashMap<String, String>createPost(String postID, String content, String username, String time){
        HashMap<String, String> post = new HashMap<>();
        post.put("postID", postID);
        post.put("content", content);
        post.put("username", username);
        post.put("time", time);
        return post;
    }

    //called from anonymous inner class for the list
    public void getContent(int page)
    {
        getPosts(jObj, place, page, pageSize);
    }

    //return true to tell when its done loading
    public boolean getPosts(JSONObject jObj, Place place, final int page, int pageSize) {
        //list.setAdapter(null);
        ServerRequests serverRequest = new ServerRequests(mActivity.getBaseContext());
        serverRequest.getPostsDataInBackground(jObj, place, page, pageSize, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    //if no object is returned, then assume it did not connect to db
                    if (returnedJSONObject == null && !endOfList) {
                        mActivity.showErrorMessage("Sorry, the app could not connect.");
                        ((MainActivity) mActivity).setTxtPost(false);
                    } else if (returnedJSONObject.getInt("success") == 1) {
                        endOfList = returnedJSONObject.getBoolean("EndOfList");
                        //gets the object with the array in it
                        //set the action bar title to name
                        mActivity.setTitle(returnedJSONObject.getString("PlaceName").replaceAll("\\\\", ""));
                        JSONArray jsonArray = returnedJSONObject.getJSONArray("posts");
                        //if page is 0, then the first posts are being displayed
                        if (page == 0) {
                            displayContent(mActivity.getBaseContext(),
                                    jsonArray,
                                    R.layout.layout_posts,
                                    new String[]{"postID", "username", "content", "time"},      //order matters HERE!
                                    new int[]{R.id.postID, R.id.username, R.id.content, R.id.time});
                            ;
                            start++;
                        } else {
                            addContent(jsonArray);
                            start++;
                        }
                    } else if (returnedJSONObject.getInt("success") == 0)
                        mActivity.showErrorMessage(returnedJSONObject.getString("message"));
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
                            displayContent(mActivity.getBaseContext(), "Be the first one to post here!"); //if there are no posts, display this message
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        return true;
    }

    protected ArrayList<HashMap<String, String>> jsonToHashMap(JSONArray jArr)
    {
        ArrayList<HashMap<String, String>> contentList = new ArrayList<>();
        JSONObject childNode = new JSONObject();    //for objects within array to place in list
        for (int i = 0; i < jArr.length(); i++)
        {
            try {
                childNode = jArr.getJSONObject(i);
                contentList.add( createPost(childNode.getString("PostID"),
                        childNode.getString("Content").replaceAll("\\\\",""),
                        childNode.getString("Username"),
                        childNode.getString("Time")));
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return contentList;
    }

    //just sets the place before content is loaded, so only one function needs to be called from activity
    public void resetAndGetContent(Place place)
    {
        setPlace(place);
        super.resetAndGetContent();
    }

    public void setPlace(Place place)
    {
        this.place = place;
    }
}
