package com.example.ridize.microsoftgraphstarter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class GroupListActivity extends ListActivity {
    AuthenticationContext mAuthContext;
    private static final String GROUPS = "GROUPS";
    AlertDialog.Builder dialog;
    HashMap<String, String> groups;

    public static Intent makeIntent(HashMap<String, String> groups, Context context) {
        Intent intent = new Intent(context, GroupListActivity.class);
        intent.putExtra(GROUPS, groups);
        return intent;
    }

    @Override
    public void onCreate(Bundle icicle) { super.onCreate(icicle); setContentView(R.layout.activity_group_list);

        Intent intent = getIntent();
        groups = (HashMap<String, String>)intent.getSerializableExtra(GROUPS);

        if (groups.values().size() < 1) {
            dialog.setMessage("No groups on organization.");
            dialog.show();
            return;
        }

        ArrayList<String> groupNames = new ArrayList(groups.keySet());

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                groupNames));
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        ArrayList<String> groupNames = new ArrayList(groups.keySet());
        String g = groupNames.get((int) id);
        getLuckyOne(groups.get(g));

    }

    public void getLuckyOne(String groupId) {
        final String  id = groupId;
        AuthenticationContext mAuthContext;
        try {
            mAuthContext = new AuthenticationContext(GroupListActivity.this,
                    ServiceConsts.AUTHORITY_URL, false);
            mAuthContext.acquireToken(GroupListActivity.this,
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
                                        chooseAndShow(json.getJSONArray("value"));
                                        // Log.v("RESULT", json.toString());
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
                            http.execute("https://graph.microsoft.com/v1.0/groups/" + id +
                                    "/members");
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

    private void chooseAndShow(JSONArray values) {
        ArrayList<String> members = new ArrayList<String>();

        for (int i=0; i< values.length(); i++) {
            try {
                JSONObject item = values.getJSONObject(i);
                String name = item.getString("displayName");
                members.add(name);
            } catch (Throwable t) {

            }
        }

        Random randomGenerator = new Random();
        int index = randomGenerator.nextInt(members.size());
        String winner = members.get(index);

        dialog = new AlertDialog.Builder(GroupListActivity.this);
        dialog.setTitle("The Dreamed Volunteer is");
        dialog.setCancelable(false);
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.create();
        dialog.setMessage(winner);
        dialog.show();
    }

}
