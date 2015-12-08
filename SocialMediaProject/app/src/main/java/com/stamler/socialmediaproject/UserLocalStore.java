package com.stamler.socialmediaproject;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Chris on 11/17/2015.
 */
public class UserLocalStore {
    public static final String SP_NAME = "userDetails";

    SharedPreferences userLocalDatabase;

    //needs context
    public UserLocalStore(Context context)
    {
        userLocalDatabase = context.getSharedPreferences(SP_NAME, 0);
    }

    public void storeUserData(User user)
    {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.putInt("userID", user.getUserID());
        userLocalDatabaseEditor.putString("fname", user.getFname());
        userLocalDatabaseEditor.putString("lname", user.getLname());
        userLocalDatabaseEditor.putString("email", user.getEmail());
        userLocalDatabaseEditor.putString("username", user.getUsername());
        userLocalDatabaseEditor.putString("password", user.getPassword());
        userLocalDatabaseEditor.commit();
    }

    public void setUserLoggedIn(boolean loggedIn)
    {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.putBoolean("loggedIn", loggedIn);
        userLocalDatabaseEditor.commit();
    }

    public void clearUserData()
    {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.clear();
        userLocalDatabaseEditor.commit();
    }

    public User getLoggedInUser()
    {
        if (userLocalDatabase.getBoolean("loggedIn", false) == false)
            return null;

        int userID = userLocalDatabase.getInt("userID", -1);
        String fname = userLocalDatabase.getString("fname", "");
        String lname = userLocalDatabase.getString("lname", "");
        String email = userLocalDatabase.getString("email", "");
        String username = userLocalDatabase.getString("username", "");
        String password = userLocalDatabase.getString("password", "");

        User user = new User(userID, fname, lname, email, username, password);
        return user;
    }
}
