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
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;

public class RegisterActivity extends AppCompatActivity {

    protected EditText editUsername, editEmail, editFName, editLName, editPassword, editConfirmPassword;
    protected Button btnRegister;
    protected JSONObject jObj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editUsername = (EditText)findViewById(R.id.editUsername);
        editEmail = (EditText)findViewById(R.id.editEmail);
        editFName = (EditText)findViewById(R.id.editFName);
        editLName = (EditText)findViewById(R.id.editLName);
        editPassword = (EditText)findViewById(R.id.editPassword);
        editConfirmPassword = (EditText)findViewById(R.id.editConfirmPassword);

        btnRegister = (Button)findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editUsername.getText().toString();
                String email = editEmail.getText().toString();
                String fname = editFName.getText().toString();
                String lname = editLName.getText().toString();
                String password = editPassword.getText().toString();
                String confirm = editConfirmPassword.getText().toString();

                if (!password.equals(confirm))
                    Toast.makeText(getBaseContext(), "Password does not match.", Toast.LENGTH_SHORT).show();
                else if (username.length() < 6)
                    showErrorMessage("Username must be 6 or more characters");
                else if (email.equals(""))
                    showErrorMessage("Email cannot be empty.");
                else if (password.equals(""))
                    showErrorMessage("Password must contain at least 1 character.  That's it, none of that must be 20 character, " +
                            "have one uppercase letter, and have the blood of a virgin bullshit.");
                else
                {
                    //hashing
                    try {
                        password = HashText.sha1(password);
                    } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
                    User user = new User(fname, lname, email, username, password);
                    registerUser(user);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showErrorMessage(String msg) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RegisterActivity.this);
        dialogBuilder.setMessage(msg);
        dialogBuilder.setPositiveButton("Ok", null);
        dialogBuilder.show();
    }

    private void registerUser(User user)
    {
        ServerRequests serverRequest = new ServerRequests(this);
        serverRequest.storeUserDataInBackground(user, jObj, new GetJSONObjectCallBack() {
            @Override
            public void done(JSONObject returnedJSONObject) {
                try {
                    if (returnedJSONObject == null)
                        showErrorMessage("Sorry, the app could not connect. Make sure no fields are empty.");
                    else if (returnedJSONObject.getInt("success") == 1) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                    else if (returnedJSONObject.getInt("success") == 0)
                        showErrorMessage(returnedJSONObject.getString("message"));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
