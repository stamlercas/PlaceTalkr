package com.stamler.socialmediaproject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Chris on 2/17/2016.
 * This is an abstract class to serve as in imtermediary between the server requests and the activity. This class will hold a reference0
 * to the list and update the list and all its contents. Nothing in the activity will refer to editing the contents that are within
 * the list.  The activity will still be able to get the information from the list and do whatever is needed (for example, the main
 * acitivty will take the postID from a list element and take you to the comments section of the particular post).
 * This class will have 2 children, one for the posts and one for the comments.
 *
 */
public abstract class ContentCreator {

    protected ListView list;
    protected View footer;

    protected int start = 0;               //don't need anymore because of InfiniteScrollListener class
    protected boolean endOfList = false;
    protected int pageSize;
    protected boolean flag_loading = false;
    protected ArrayList<HashMap<String, String>> content;
    protected BaseActivity mActivity;
    protected JSONObject jObj;
    protected boolean firstToPost;
    protected ListAdapter adapter;

    protected SharedPreferences sp;

    public ContentCreator(BaseActivity activity, ListView list)
    {
        mActivity = activity;   //saving reference to activity to call functions from
        this.list = list;   //a reference to the list in the activity
        //footer will always be at bottom until end of list is reached
        footer = mActivity.getLayoutInflater().inflate(R.layout.list_footer, null);
        jObj = new JSONObject();

        sp = PreferenceManager.getDefaultSharedPreferences(mActivity.getBaseContext());
        pageSize = Integer.parseInt(sp.getString("limit", "25"));

        list.addFooterView(footer);

        //for dynamically adding items
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            int currentFirstVisibleItem = 0;
            int currentVisibleItemCount = 0;
            int totalItemCount = 0;
            int currentScrollState = 0;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                this.currentScrollState = scrollState;
                this.isScrollCompleted();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                this.currentFirstVisibleItem = firstVisibleItem;
                this.currentVisibleItemCount = visibleItemCount;
                this.totalItemCount = totalItemCount;
            }

            private void isScrollCompleted() {
                if (this.currentVisibleItemCount > 0 && this.currentScrollState == SCROLL_STATE_IDLE
                        && this.totalItemCount == (currentFirstVisibleItem + currentVisibleItemCount)) {
                    /*** In this way I detect if there's been a scroll which has completed ***/
                    /*** do the work for load more date! ***/
                    if (!endOfList) {       //don't load new posts if it is the all the posts have been loaded
                        if (!flag_loading) {
                            flag_loading = true;
                            getContent(start);   //this will abstract function to get the posts or comments
                                            //will call whatever method I have implemented in the class
                        }
                    }
                }
            }
        });
    }

    protected abstract void getContent(int page);
    //convert the json array into hashmap to place in listview
    protected abstract ArrayList<HashMap<String, String>> jsonToHashMap(JSONArray jArr);

    public void getContent() { getContent(start); }

    //display individual posts or comments
    public void displayContent(Context context, JSONArray jsonArray, int layout, String[] from, int[] to)
    {
            firstToPost = false;
            content = jsonToHashMap(jsonArray);
            adapter = new SimpleAdapter(
                    context,
                    content,
                    layout,
                    from,
                    to);
            list.setAdapter(adapter);
    }

    //this is for if there are no
    public void displayContent(Context context, String content)
    {
        firstToPost = true;
        ArrayList<HashMap<String, String>> hashmap = new ArrayList<>();
        HashMap<String, String> temp = new HashMap<>();
        temp.put("content", content);
        hashmap.add(temp);
        adapter = new SimpleAdapter(
                context,
                hashmap,
                android.R.layout.simple_list_item_1,
                new String[]{"content"},      //order matters HERE!
                new int[]{android.R.id.text1});
        list.setAdapter(adapter);
        list.removeFooterView(footer);  //if they are first, then no other content will need to be loaded
    }

    //add on the list of content
    public void addContent(JSONArray jsonArray)
    {
        firstToPost = false;
        content.addAll(jsonToHashMap(jsonArray));
        ((SimpleAdapter)adapter).notifyDataSetChanged();
        flag_loading = false;
    }

    //resets variables back to normal
    public void reset()
    {
        start = 0;
        endOfList = false;
        flag_loading = false;
        //
        list.removeFooterView(footer);
        list.addFooterView(footer);
    }

    public void resetAndGetContent()
    {
        reset();
        getContent(start);
    }

    public boolean getFirstToPost() { return firstToPost; }
}
