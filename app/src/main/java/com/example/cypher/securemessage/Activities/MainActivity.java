package com.example.cypher.securemessage.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.cypher.securemessage.Beans.Contact;
import com.example.cypher.securemessage.Manager.NetManager;
import com.example.cypher.securemessage.Misc.Encrypter;
import com.example.cypher.securemessage.R;

import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity
{
    private NetManager netManager;
    private Encrypter encrypter;
    private SharedPreferences prefs;
    protected static ListAdapter customListAdapter;
    protected static ListView contactListView;
    private TextView meDisplay;
    private final String TAG = "securemessage";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume()
    {
        meDisplay = (TextView) findViewById(R.id.myNameDisplay);
        super.onResume();
        prefs = getSharedPreferences(TAG, MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true))
        {
            //First run of the app.
            firstRun();
        }
        meDisplay.setText(prefs.getString("username", ""));

        refreshList();

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Contact c = (Contact) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this,
                                           ViewMessagesActivity.class);
                intent.putExtra("contactID", c.get_id());
                intent.putExtra("contactUsername", c.get_username());
                intent.putExtra("contactKey", c.get_key());
                intent.putExtra("myusername", prefs.getString("username", ""));
                startActivity(intent);
            }
        });

        contactListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                                           View view,
                                           int position,
                                           long id)
            {
                Contact c = (Contact) parent.getItemAtPosition(position);
                c.setContext(MainActivity.this);
                c.deleteContact();
                refreshList();
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    public void addNewContactClicked(View view)
    {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.contact_input, null);
        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText contUserInput = (EditText) promptsView
                .findViewById(R.id.contactUsernameInput);

        alertDialogBuilder
                .setCancelable(false)
                .setTitle("Enter the contact's username:")
                .setPositiveButton("OK",
                                   new DialogInterface.OnClickListener()
                                   {
                                       public void onClick(DialogInterface dialog, int id)
                                       {
                                           String username = contUserInput.getText().toString();
                                           netManager = new NetManager(MainActivity.this);
                                           try
                                           {
                                               Contact c = netManager.getContact(username);
                                               refreshList();
                                           } catch (ExecutionException | InterruptedException e)
                                           {
                                               e.printStackTrace();
                                           }

                                       }
                                   })
                .setNegativeButton("Cancel",
                                   new DialogInterface.OnClickListener()
                                   {
                                       public void onClick(DialogInterface dialog, int id)
                                       {
                                           dialog.cancel();
                                       }
                                   });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void refreshList()
    {
        customListAdapter = new CustomContactAdapter(this, new Contact(this).getContacts());
        contactListView = (ListView) findViewById(R.id.contactListView);
        contactListView.setAdapter(customListAdapter);
    }

    private void firstRun()
    {
        try
        {
            final SharedPreferences.Editor editor = prefs.edit();
            //Generate my public and private key.
            encrypter = new Encrypter();
            encrypter.genKeys(this, "myKey");

            //Get the server's public key.
            netManager = new NetManager(this);
            Contact c = netManager.getServerKey();

            //Register as a user on the server.
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.user_pass_input, null);
            AlertDialog.Builder alertDialogBuilder;
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(promptsView);

            final EditText usernameInput = (EditText) promptsView
                    .findViewById(R.id.usernameInput);
            final EditText passwordInput = (EditText) promptsView
                    .findViewById(R.id.passwordInput);

            alertDialogBuilder
                    .setCancelable(false)
                    .setTitle("Enter a username and password:")
                    .setPositiveButton("OK",
                                       new DialogInterface.OnClickListener()
                                       {
                                           public void onClick(DialogInterface dialog, int id)
                                           {
                                               String[] myInfo = {usernameInput.getText().toString(),
                                                       passwordInput.getText().toString()};
                                               editor.putString("username", myInfo[0]);
                                               editor.putString("password", myInfo[1]);

                                               editor.putBoolean("firstrun", false);
                                               editor.commit();
                                               Log.i(TAG, "(LOCAL) Saved me.");

                                               try
                                               {
                                                   meDisplay.setText(netManager.registerMe(myInfo));
                                               } catch (ExecutionException | InterruptedException e)
                                               {
                                                   e.printStackTrace();
                                               }
                                           }
                                       })
                    .setNegativeButton("Cancel",
                                       new DialogInterface.OnClickListener()
                                       {
                                           public void onClick(DialogInterface dialog, int id)
                                           {
                                               dialog.cancel();
                                           }
                                       });
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

        } catch (Exception e)
        {
            Log.e(TAG, "ERROR");
            e.printStackTrace();
        }
    }

    private class CustomContactAdapter extends ArrayAdapter
    {

        public CustomContactAdapter(Context context, Object[] objects)
        {
            super(context, R.layout.custom_contact_list, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View customContactView = layoutInflater.inflate(R.layout.custom_contact_list, parent,
                                                            false);

            Contact c = (Contact) getItem(position);

            TextView nameDisplay = (TextView) customContactView.findViewById(
                    R.id.contactNameDisplay);

            nameDisplay.setText(c.get_username());

            return customContactView;
        }
    }
}
