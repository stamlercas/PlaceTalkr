package com.stamler.socialmediaproject;

/**
 * Created by Chris on 11/17/2015.
 */
public class User {
    private String fname, lname, email, username, password;
    private int userID;

    public User(int userID, String fname, String lname, String email,
                String username, String password)
    {
        this.userID = userID;
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public User (String fname, String lname, String email,
                 String username, String password)
    {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public User(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getUserID() {
        return userID;
    }
}
