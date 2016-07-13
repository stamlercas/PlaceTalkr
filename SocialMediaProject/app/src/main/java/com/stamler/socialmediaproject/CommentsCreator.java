package com.stamler.socialmediaproject;

import android.content.Intent;
import android.view.View;
import android.widget.ListView;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Chris on 4/6/2016.
 */
public class CommentsCreator extends ContentCreator {
    protected String postID;

    public CommentsCreator(BaseActivity mActivity, ListView list, String postID){
        super(mActivity, list);
        this.postID = postID;
    }

    //create individual hashmap from parameters
    //TODO: put into PostsCreator class. There will be a seperate function for createComments for the CommentCreator class
    private HashMap<String, String> createComment(String commentID, String userID, String content, String username, String time){
        HashMap<String, String> post = new HashMap<>();
        post.put("commentID", commentID);
        post.put("userID", userID);
        post.put("content", content);
        post.put("username", username);
        post.put("time", time);
        return post;
    }

    //called from anonymous inner class for the list
    public void getContent(int page)
    {
        getPost(jObj, postID, page, pageSize);
    }

    //gets the specific post along with the comments associated with it
    public void getPost(final JSONObject jObj, String postID, final int page, int pageSize) {
        pageSize = Integer.parseInt(sp.getString("limit", "25"));   //need to check if the limit has changed in the settings
        ServerRequests serverRequest = new ServerRequests(mActivity.getBaseContext());
        serverRequest.getIndividualPostsDataInBackground(jObj, postID, page, pageSize, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    //if no object is returned, then assume it did not connect to db
                    if (returnedJSONObject == null) {
                        mActivity.showErrorMessage("Sorry, the app could not connect.");
                    } else if (returnedJSONObject.getInt("success") == 1) {
                        endOfList = returnedJSONObject.getBoolean("EndOfList");
                        final int userID = returnedJSONObject.getJSONArray("post").getJSONObject(0).getInt("UserID");
                        //gets the object with the array in it, but only if its the first page
                        if (page == 0) {
                            JSONObject post = returnedJSONObject.getJSONArray("post").getJSONObject(0);
                            ((CommentsActivity)mActivity).displayPost(post);
                        }
                        JSONArray comments = returnedJSONObject.getJSONArray("comments");
                        //if page is 0, then the first posts are being displayed
                        if (page == 0) {
                            displayContent(mActivity.getBaseContext(),
                                    comments,
                                    R.layout.layout_comments,
                                    new String[]{"commentID", "userID", "username", "content", "time"},      //order matters HERE!
                                    new int[]{R.id.postID, R.id.userID, R.id.username, R.id.content, R.id.time});
                            start++;
                        } else {
                            addContent(comments);
                            start++;
                        }

                        //click on the post in the activity and it will take you to the profile activity, to view
                        //their profile details, but not sensitive details
                        ((CommentsActivity)mActivity).getPostsLayout().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(mActivity.getBaseContext(), ProfileActivity.class);
                                intent.putExtra("userID", String.valueOf(userID));
                                mActivity.startActivityForResult(intent, 100);
                            }
                        });

                    } else if (returnedJSONObject.getInt("success") == 0)
                        mActivity.showErrorMessage(returnedJSONObject.getString("message"));
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
                        if (returnedJSONObject.getInt("FirstToComment") == 1)
                            displayContent(mActivity.getBaseContext(), "Be the first one to comment here!"); //if there are no posts, display this message
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

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
                postList.add( createComment(childNode.getString("CommentID"),
                        childNode.getString("UserID"),
                        childNode.getString("Content"),
                        childNode.getString("Username"),
                        childNode.getString("Time")));
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        return postList;
    }
}
