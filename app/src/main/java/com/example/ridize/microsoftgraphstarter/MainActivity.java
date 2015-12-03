package com.example.ridize.microsoftgraphstarter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    AuthenticationContext mAuthContext;
    TextView txtMessage;
    AlertDialog.Builder dialog;
    ProgressBar spinner;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get controls
        txtMessage = (TextView)findViewById(R.id.txtMessage);
        spinner = (ProgressBar)findViewById(R.id.spinner);

        //setup dialog
        dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Error");
        dialog.setCancelable(false);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.create();

        //initiate login
        try {
            mAuthContext = new AuthenticationContext(MainActivity.this,
                    ServiceConsts.AUTHORITY_URL, false);
            mAuthContext.acquireToken(MainActivity.this,
                    ServiceConsts.RESOURCE_ID,
                    ServiceConsts.CLIENT_ID,
                    ServiceConsts.REDIRECT_URL, "",
                    new AuthenticationCallback<AuthenticationResult>() {
                        @Override
                        public void onSuccess(AuthenticationResult result) {
                            //make a call to the Microsoft Graph
                            HttpTask http = new HttpTask(result);
                            final AuthenticationResult authResult = result;
                            http.setTaskHandler(new HttpTask.HttpTaskHandler() {
                                @Override
                                public void taskSuccessful(JSONObject json) {
                                    //bind the txtMessage control
                                    try {
                                        // String name = json.getString("displayName");
                                        // txtMessage.setText("Hello " + name);
                                        HashMap<String, String> groups =
                                                getGroups(json.getJSONArray("value"));

                                        Intent i = GroupListActivity.makeIntent(groups,
                                                getApplicationContext());
                                        startActivity(i);
                                        finish();
                                        //hide spinner
                                        spinner.setVisibility(View.INVISIBLE);
                                    }
                                    catch (Exception ex) {

                                    }
                                }

                                @Override
                                public void taskFailed() {
                                    dialog.setMessage("REST call failed");
                                    dialog.show();
                                }
                            });
                            http.execute("https://graph.microsoft.com/v1.0/groups");
                        }

                        @Override
                        public void onError(Exception exc) {
                            dialog.setMessage(exc.getMessage());
                            dialog.show();
                        }
                    });
        }
        catch (Throwable t) {
            dialog.setMessage(t.getMessage());
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mAuthContext != null)
            mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    private HashMap<String, String> getGroups(JSONArray values) {
        HashMap<String, String> result = new HashMap<String, String>();
        if (values.length() < 1) {
            dialog.setMessage("No groups on organization.");
            dialog.show();
        }

        for (int i=0; i< values.length(); i++) {
            try {
                JSONObject item = values.getJSONObject(i);
                String name = item.getString("displayName");
                String id = item.getString("id");
                result.put(name, id);
            } catch (Throwable t) {

            }
        }
        return result;
    }
}
