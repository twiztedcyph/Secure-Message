package com.example.cypher.securemessage.Activities;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.cypher.securemessage.Beans.Message;
import com.example.cypher.securemessage.Manager.NetManager;
import com.example.cypher.securemessage.Misc.Encrypter;
import com.example.cypher.securemessage.R;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ViewMessagesActivity extends ActionBarActivity
{
    private Encrypter encrypter;
    public static ListAdapter customListAdapter;
    public static ListView messageListView;
    private final String TAG = "securemessage";
    private String contactName, myName;
    private int contactID;
    private byte[] contactKey;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_messages);
        encrypter = new Encrypter();
        Bundle extras = getIntent().getExtras();

        if (extras != null)
        {
            contactID = extras.getInt("contactID", 0);
            contactName = extras.getString("contactUsername");
            contactKey = extras.getByteArray("contactKey");
            myName = extras.getString("myusername");

        }

        if (contactID > 0)
        {
            refreshList();

            messageListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
            {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id)
                {
                    Message m = (Message) parent.getItemAtPosition(position);
                    m.setContext(ViewMessagesActivity.this);
                    m.delete();
                    refreshList();
                    return true;
                }
            });
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_messages, menu);
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

    private void refreshList()
    {
        customListAdapter = new CustomMessageAdapter(this, new Message(this).getAll(contactID));
        messageListView = (ListView) findViewById(R.id.messageListView);
        messageListView.setAdapter(customListAdapter);
    }

    public void messageSendButtonClicked(View view)
    {
        EditText messageInput = (EditText) findViewById(R.id.messageInput);
        String messageText = messageInput.getText().toString();
        if (!messageText.isEmpty())
        {
            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(contactKey);
                RSAPublicKey recipPublicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

                String[] message = {myName, encrypter.encrypt(messageText, recipPublicKey), contactName};
                NetManager netManager = new NetManager(this);
                netManager.sendMessage(message);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException |
                    IllegalBlockSizeException | InvalidKeyException |
                    NoSuchProviderException | NoSuchPaddingException |
                    BadPaddingException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class CustomMessageAdapter extends ArrayAdapter
    {

        public CustomMessageAdapter(Context context, Object[] objects)
        {
            super(context, R.layout.custom_message_list, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View customContactView = layoutInflater.inflate(R.layout.custom_message_list, parent, false);

            Message m = (Message) getItem(position);

            TextView nameDisplay = (TextView) customContactView.findViewById(R.id.messageNameDisplay);
            TextView messageDisplay = (TextView) customContactView.findViewById(R.id.messageMessageDisplay);

            System.out.println("IS FROM ME: " + m.is_fromME());

            if (m.is_fromME())
            {
                customContactView.setBackgroundColor(Color.parseColor("#00FFCC"));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                params.weight = 1.0f;
                params.gravity = Gravity.RIGHT;
                nameDisplay.setLayoutParams(params);
                nameDisplay.setText("Me");

                try
                {
                    messageDisplay.setLayoutParams(params);
                    messageDisplay.setText(encrypter.decrypt(m.get_messageContent(), encrypter.getMyPrivKey()));
                } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                        InvalidKeyException | IllegalBlockSizeException |
                        BadPaddingException | CertificateException |
                        IOException | KeyStoreException |
                        UnrecoverableEntryException |
                        NoSuchProviderException e)
                {
                    Log.i(TAG, "An error has occurred");
                    e.printStackTrace();
                }
            }
            else
            {
                customContactView.setBackgroundColor(Color.parseColor("#CC99FF"));
                nameDisplay.setText(contactName);
                messageDisplay.setText(m.get_messageContent());
            }
            return customContactView;
        }
    }
}
