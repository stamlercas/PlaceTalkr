package com.stamler.socialmediaproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    protected Button btnLogin;
    protected EditText editUsername, editPassword;
    protected UserLocalStore userLocalStore;
    protected TextView txtRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsername = (EditText)findViewById(R.id.editUsername);
        editPassword = (EditText)findViewById(R.id.editPassword);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        userLocalStore = new UserLocalStore(this);      //pass in context
        txtRegister = (TextView)findViewById(R.id.txtRegister);
        //listeners
        txtRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to register page
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editUsername.getText().toString();
                String password = editPassword.getText().toString();
                //hashing should be done now before server request
                try {
                    password = HashText.sha1(password);
                } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

                User user = new User(username, password);

                //userLocalStore.storeUserData(user);
                //userLocalStore.setUserLoggedIn(true);

                authenticate(user);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    private void authenticate(User user)
    {
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.fetchUserDataInBackground(user, new GetUserCallBack() {
            @Override
            public void done(User returnedUser) {
                if (returnedUser == null)
                    showErrorMessage();
                else
                    logUserIn(returnedUser);
            }
        });
    }

    private void showErrorMessage() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(LoginActivity.this);
        dialogBuilder.setMessage("Incorrect user details ");
        dialogBuilder.setPositiveButton("Ok", null);
        dialogBuilder.show();
    }

    private void logUserIn(User returnedUser) {
        userLocalStore.clearUserData();
        userLocalStore.storeUserData(returnedUser);
        userLocalStore.setUserLoggedIn(true);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
