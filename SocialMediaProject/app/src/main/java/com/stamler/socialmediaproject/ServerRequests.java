package com.stamler.socialmediaproject;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.location.places.Place;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by Chris on 11/17/2015.
 */
public class ServerRequests {
    ProgressDialog progressDialog;
    public final static int CONNECTION_TIMEOUT = 1000 * 15;
    public final static String SERVER_ADDRESS = "http://cisprod.clarion.edu/~s_castamler/";
    public final static String SUCCESS = "success";

    protected int postID;

    public ServerRequests(Context context)
    {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please Wait...");
    }

    //for individual posts need to know postID
    public ServerRequests(Context context, int postID)
    {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please Wait...");

        this.postID = postID;
    }

    //used to register
    public void storeUserDataInBackground(User user, JSONObject jObj, GetJSONObjectCallBack callBack)
    {
        progressDialog.show();
        new StoreUserDataAsyncTask(user, jObj, callBack).execute();
    }

    //used to login
    public void fetchUserDataInBackground(User user, GetUserCallBack callBack)
    {
        progressDialog.show();
        new FetchUserDataAsyncTask(user, callBack).execute();
    }

    //used to get posts for main activity
    public void getPostsDataInBackground(JSONObject jObj, Place place, int page, int pageSize, GetJSONObjectCallBack callBack)
    {
        //progressDialog.show();    Don't need to show, replaced with footerView
        new getPostsDataInBackground(jObj, place, page, pageSize, callBack).execute();
    }

    //gets individual post details along with comments
    public void getIndividualPostsDataInBackground(JSONObject jObj, String postID, int page, int pageSize, GetJSONObjectCallBack callBack)
    {
        //progressDialog.show();
        new getIndividualPostsDataInBackground(jObj, postID, page, pageSize, callBack).execute();
    }

    public void postDataInBackground(User user, String post, JSONObject jObj, Place place, GetJSONObjectCallBack callBack)
    {
        progressDialog.show();
        new postDataInBackground(user, post, jObj, place, callBack).execute();
    }

    public void commentDataInBackground(User user, String comment, String postID, JSONObject jObj, GetJSONObjectCallBack callBack)
    {
        progressDialog.show();
        new commentDataInBackground(user, comment, postID, jObj, callBack).execute();
    }

    public void getUserDataInBackground(User user, JSONObject jObj, GetJSONObjectCallBack callBack)
    {
        progressDialog.show();
        new getUserDataInBackground(user, jObj, callBack).execute();
    }

    public class StoreUserDataAsyncTask extends AsyncTask<Void, Void, JSONObject>
    {
        User user;
        JSONObject jObj;
        GetJSONObjectCallBack callBack;
        public StoreUserDataAsyncTask(User user, JSONObject jObj, GetJSONObjectCallBack callBack)
        {
            this.user = user;
            this.jObj = jObj;
            this.callBack = callBack;
        }

        protected JSONObject doInBackground(Void... params)
        {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("FirstName", user.getFname()));
            dataToSend.add(new BasicNameValuePair("LastName", user.getLname()));
            dataToSend.add(new BasicNameValuePair("Username", user.getUsername()));
            dataToSend.add(new BasicNameValuePair("Password", user.getPassword()));     //should already have had hashing
            dataToSend.add(new BasicNameValuePair("Email", user.getEmail()));

            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "register.php");

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));       //attach data to request
                HttpResponse httpResponse = client.execute(post);           //retrieve json to know if successful or not

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);

                JSONObject jObject = new JSONObject(result);
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    public class FetchUserDataAsyncTask extends AsyncTask<Void, Void, User> {
        User user;
        GetUserCallBack userCallBack;

        public FetchUserDataAsyncTask(User user, GetUserCallBack userCallBack) {
            this.user = user;
            this.userCallBack = userCallBack;
        }

        protected User doInBackground(Void... params)
        {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("Username", user.getUsername()));
            dataToSend.add(new BasicNameValuePair("Password", user.getPassword()));     //should have had hashing already

            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "login.php");

            User returnedUser = null;

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));
                HttpResponse httpResponse = client.execute(post);

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);

                JSONObject jObject = new JSONObject(result);

                if (jObject.getInt(SUCCESS) == 1) {
                    JSONArray userObj = jObject.getJSONArray("user");       //gets the object with the array in it
                    JSONArray jsonArr = userObj.getJSONArray(0);             //the array has another object
                    JSONObject jsonUser = jsonArr.getJSONObject(0);         //get the actual object i need

                    if (jsonUser.length() != 0) {
                        Log.v("happened", "2");
                        int userID = jsonUser.getInt("UserID");
                        String fname = jsonUser.getString("FirstName");
                        String lname = jsonUser.getString("LastName");
                        String email = jsonUser.getString("Email");

                        returnedUser = new User(userID, fname, lname, email, user.getUsername(), user.getPassword());
                    }
                }
                }catch(Exception e){
                    e.printStackTrace();
                }

            return returnedUser;
        }

        protected void onPostExecute(User returnedUser)
        {
            progressDialog.dismiss();
            userCallBack.done(returnedUser);
            super.onPostExecute(returnedUser);
        }
    }

    //get the posts from server to display
    public class getPostsDataInBackground extends AsyncTask<Void, Void, JSONObject> {
        JSONObject jObj;
        Place place;
        GetJSONObjectCallBack callBack;
        int page, pageSize;

        public getPostsDataInBackground(JSONObject jObj, Place place, int page, int pageSize, GetJSONObjectCallBack callBack)
        {
            this.jObj = jObj;
            this.place = place;
            this.callBack = callBack;
            this.page = page;
            this.pageSize = pageSize;
        }

        protected JSONObject doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("PlaceID", place.getId()));
            dataToSend.add(new BasicNameValuePair("Latitude", String.valueOf(place.getLatLng().latitude)));
            dataToSend.add(new BasicNameValuePair("Longitude", String.valueOf(place.getLatLng().longitude)));
            dataToSend.add(new BasicNameValuePair("Name", String.valueOf(place.getName())));

            dataToSend.add(new BasicNameValuePair("Address", String.valueOf(place.getAddress())));
            dataToSend.add(new BasicNameValuePair("PhoneNumber", String.valueOf(place.getPhoneNumber())));
            dataToSend.add(new BasicNameValuePair("WebsiteUri", String.valueOf(place.getWebsiteUri())));

            dataToSend.add(new BasicNameValuePair("Page", String.valueOf(page)));
            dataToSend.add(new BasicNameValuePair("PageSize", String.valueOf(pageSize)));
            for (int i = 0; i < dataToSend.size(); i++)
                Log.i(dataToSend.get(i).getName(), dataToSend.get(i).getValue());

            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "get_all_posts.php");

            JSONObject jObject = null;

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));
                HttpResponse httpResponse = client.execute(post);

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);
                Log.i("Result: ", result);

                jObject = new JSONObject(result);
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    public class getIndividualPostsDataInBackground extends AsyncTask<Void, Void, JSONObject>
    {
        JSONObject jObj;
        GetJSONObjectCallBack callBack;
        String postID;
        int page, pageSize;

        public getIndividualPostsDataInBackground(JSONObject jObj, String postID, int page, int pageSize, GetJSONObjectCallBack callBack)
        {
            this.jObj = jObj;
            this.callBack = callBack;
            this.postID = postID;
            this.page = page;
            this.pageSize = pageSize;
        }

        protected JSONObject doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("PostID", postID));
            dataToSend.add(new BasicNameValuePair("Page", String.valueOf(page)));
            dataToSend.add(new BasicNameValuePair("PageSize", String.valueOf(pageSize)));

            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "get_post.php");

            JSONObject jObject = null;

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));
                HttpResponse httpResponse = client.execute(post);

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);

                jObject = new JSONObject(result);
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    public class postDataInBackground extends AsyncTask<Void, Void, JSONObject>
    {
        JSONObject jObj;
        GetJSONObjectCallBack callBack;

        User user;
        String post;
        Place place;

        public postDataInBackground(User user, String post, JSONObject jObj, Place place, GetJSONObjectCallBack callBack)
        {
            this.jObj = jObj;
            this.callBack = callBack;
            this.user = user;
            this.post = post;
            this.place = place;
        }

        protected JSONObject doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("Content", post));
            dataToSend.add(new BasicNameValuePair("UserID", String.valueOf(user.getUserID())));
            dataToSend.add(new BasicNameValuePair("PlaceID", String.valueOf(place.getId())));


            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "create_post.php");

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));       //attach data to request
                HttpResponse httpResponse = client.execute(post);           //retrieve json to know if successful or not

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);

                JSONObject jObject = new JSONObject(result);
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    public class commentDataInBackground extends AsyncTask<Void, Void, JSONObject>
    {
        JSONObject jObj;
        GetJSONObjectCallBack callBack;

        User user;
        String comment, postID;

        public commentDataInBackground(User user, String comment, String postID, JSONObject jObj, GetJSONObjectCallBack callBack)
        {
            this.jObj = jObj;
            this.callBack = callBack;
            this.user = user;
            this.comment = comment;
            this.postID = postID;
        }

        protected JSONObject doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("Content", comment));
            dataToSend.add(new BasicNameValuePair("UserID", String.valueOf(user.getUserID())));
            dataToSend.add(new BasicNameValuePair("PostID", postID));


            HttpParams httpRequestParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpRequestParams,
                    CONNECTION_TIMEOUT);

            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "create_comment.php");

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));       //attach data to request
                HttpResponse httpResponse = client.execute(post);           //retrieve json to know if successful or not

                HttpEntity entity = httpResponse.getEntity();
                String result = EntityUtils.toString(entity);

                JSONObject jObject = new JSONObject(result);
                return jObject;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    public class getUserDataInBackground extends AsyncTask<Void, Void, JSONObject>
    {
        JSONObject jObj;
        GetJSONObjectCallBack callBack;

        User user;

        public getUserDataInBackground(User user, JSONObject jObj, GetJSONObjectCallBack callBack)
        {
            this.jObj = jObj;
            this.callBack = callBack;
            this.user = user;
        }

        protected JSONObject doInBackground(Void... params) {
            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("UserID", String.valueOf(user.getUserID())));

            return simpleDoInBackground(dataToSend, SERVER_ADDRESS + "get_user_info.php");
        }

        protected void onPostExecute(JSONObject returnedJSONObject)
        {
            progressDialog.dismiss();
            callBack.done(returnedJSONObject);
            super.onPostExecute(returnedJSONObject);
        }
    }

    private JSONObject simpleDoInBackground(ArrayList<NameValuePair> dataToSend, String url)
    {
        HttpParams httpRequestParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpRequestParams,
                CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpRequestParams,
                CONNECTION_TIMEOUT);

        HttpClient client = new DefaultHttpClient(httpRequestParams);
        HttpPost post = new HttpPost(url);

        try {
            post.setEntity(new UrlEncodedFormEntity(dataToSend));       //attach data to request
            HttpResponse httpResponse = client.execute(post);           //retrieve json to know if successful or not

            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);

            JSONObject jObject = new JSONObject(result);
            return jObject;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
