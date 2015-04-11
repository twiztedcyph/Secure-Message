package com.example.cypher.securemessage.Manager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.example.cypher.securemessage.Activities.ViewMessagesActivity;
import com.example.cypher.securemessage.Beans.Contact;
import com.example.cypher.securemessage.Beans.Message;
import com.example.cypher.securemessage.Misc.Encrypter;
import com.example.cypher.securemessage.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by Cypher on 10/04/2015.
 */
public class NetManager
{
    private Context context;
    private HttpClient httpClient;
    private HttpPost httpPost;
    private List<NameValuePair> nameValuePairs;
    private HttpResponse response;
    private StatusLine statusLine;
    private Encrypter encrypter;
    private NotificationCompat.Builder notification;
    private Uri notificationSound;
    private LocalBroadcastManager broadcaster;
    public static final String RESULT = "com.example.cypher.securemessage.MESSAGERECIEVED";

    private static final int uniqueID = 564564;
    private final String TAG = "securemessage";

    public NetManager(Context context)
    {
        this.context = context;
    }

    public Contact getServerKey() throws ExecutionException, InterruptedException
    {
        ServerKey serverKey = new ServerKey();
        serverKey.execute();
        return serverKey.get();
    }

    public String registerMe(String[] myInfo) throws ExecutionException, InterruptedException
    {
        Registration registration = new Registration();
        registration.execute(myInfo);
        return registration.get();
    }

    public Contact getContact(String contactUsername) throws
    ExecutionException,
    InterruptedException
    {
        PubKey pubKey = new PubKey();
        pubKey.execute(contactUsername);

        return pubKey.get();
    }

    public void sendMessage(String[] message)
    {
        Outgoing outgoing = new Outgoing();
        outgoing.execute(message);
    }

    public Integer checkMessages(String myUsername) throws ExecutionException, InterruptedException
    {
        Incoming incoming = new Incoming();
        incoming.execute(myUsername);
        return incoming.get();
    }

    private class Incoming extends AsyncTask<String, Void, Integer>
    {
        private RSAPublicKey serverPubKey;

        public Incoming()
        {
            Contact server = new Contact(context);
            server.getContact("Server");

            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(server.get_key());
                serverPubKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
            {
                e.printStackTrace();
            }
            broadcaster = LocalBroadcastManager.getInstance(context);
        }

        @Override
        protected Integer doInBackground(String... params)
        {
            Integer count = 0;
            try
            {
                String result = "";
                encrypter = new Encrypter();
                httpClient = new DefaultHttpClient();
                httpPost = new HttpPost("http://twizted.co.uk:8080/MessageServerTwo/Auth");

                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("0", "2"));
                nameValuePairs.add(new BasicNameValuePair("1", encrypter.encrypt(params[0], serverPubKey)));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                response = httpClient.execute(httpPost);
                statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    System.out.println("OK");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    result = out.toString();
                    out.close();
                }

                if (!result.isEmpty())
                {
                    String[] messages = result.split("\\r?\\n");
                    Message m = null;
                    Contact c = new Contact(context);
                    Intent intent = new Intent(context, ViewMessagesActivity.class);

                    for (String s : messages)
                    {
                        count++;
                        String[] split = s.split("%%");
                        String contact_username = split[0];
                        String messageText = split[1].trim();
                        Timestamp t = new Timestamp(Long.valueOf(split[3]));

                        c.getContact(contact_username);

                        if (c.get_id() == 0)
                        {
                            int id = 0;

                            c.set_username(contact_username);
                            //id = regContact(c);
                            intent.putExtra("contactID", id);
                            m = new Message(id, false, encrypter.decrypt(messageText, encrypter.getMyPrivKey()), t.toString());
                            m.setContext(context);
                            m.persist();
                        }else
                        {
                            intent.putExtra("contactID", c.get_id());
                            m = new Message(c.get_id(), false, encrypter.decrypt(messageText, encrypter.getMyPrivKey()), t.toString());
                            m.setContext(context);
                            m.persist();
                        }

                    }


                    sendResult("new");
                    notification = new NotificationCompat.Builder(context);
                    notification.setAutoCancel(true);

                    notification.setSmallIcon(R.mipmap.ic_launcher);
                    notification.setTicker("Secure message received");
                    notification.setWhen(System.currentTimeMillis());
                    notification.setContentTitle(c.get_username());
                    notification.setContentText(m.get_messageContent());

                    notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    notification.setSound(notificationSound);

                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    notification.setContentIntent(pendingIntent);

                    //Build and issue notification.
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(uniqueID, notification.build());
                }

            } catch (NoSuchPaddingException | InvalidKeyException |
                    NoSuchProviderException | BadPaddingException |
                    IllegalBlockSizeException | NoSuchAlgorithmException |
                    IOException | CertificateException |
                    UnrecoverableEntryException | KeyStoreException e)
            {
                e.printStackTrace();
            }

            return count;
        }

        @Override
        protected void onPostExecute(Integer integer)
        {
            Log.i(TAG, "Check messages returned: " + integer);
            return;
        }
    }

    private class Outgoing extends AsyncTask<String, Void, Void>
    {
        private RSAPublicKey serverPubKey;

        public Outgoing()
        {
            Contact server = new Contact(context);
            server.getContact("Server");
            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(server.get_key());
                serverPubKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(String... params)
        {
            try
            {
                encrypter = new Encrypter();
                httpClient = new DefaultHttpClient();
                httpPost = new HttpPost("http://twizted.co.uk:8080/MessageServerTwo/Auth");

                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("0", "4"));
                //from
                System.out.println(params[0]);
                nameValuePairs.add(new BasicNameValuePair("1", encrypter.encrypt(params[0],
                                                                                 serverPubKey)));
                //message
                System.out.println(params[1]);
                nameValuePairs.add(new BasicNameValuePair("2", params[1]));
                //to
                System.out.println(params[2]);
                nameValuePairs.add(new BasicNameValuePair("3", encrypter.encrypt(params[2],
                                                                                 serverPubKey)));


                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                response = httpClient.execute(httpPost);
                statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                }

            } catch (IOException | IllegalBlockSizeException |
                    NoSuchProviderException | BadPaddingException |
                    NoSuchPaddingException | InvalidKeyException |
                    NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            Log.i(TAG, "Message sent.");
            return;
        }
    }

    private class ServerKey extends AsyncTask<Void, Void, Contact>
    {
        @Override
        protected Contact doInBackground(Void... params)
        {
            Contact server = new Contact(context);
            try
            {
                httpClient = new DefaultHttpClient();
                httpPost = new HttpPost("http://twizted.co.uk:8080/MessageServerTwo/Auth");
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("0", "1"));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                response = httpClient.execute(httpPost);
                statusLine = response.getStatusLine();

                String serverKey;
                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    serverKey = out.toString();
                    out.close();
                    server.set_username("Server");
                    server.set_key(Base64.decode(serverKey, Base64.NO_WRAP | Base64.NO_PADDING));
                    server.persist();
                }

            } catch (IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
            return server;
        }

        @Override
        protected void onPostExecute(Contact contact)
        {
            Log.i(TAG, "Server key obtained.");
            return;
        }
    }

    private class Registration extends AsyncTask<String, Void, String>
    {
        private RSAPublicKey serverPubKey;

        public Registration()
        {
            Contact server = new Contact(context);
            server.getContact("Server");

            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(server.get_key());
                serverPubKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... params)
        {
            String result = "";
            try
            {
                encrypter = new Encrypter();
                byte[] pubKey = encrypter.getMyPubKey().getEncoded();

                String username = params[0];
                String password = params[1];
                String key = Base64.encodeToString(pubKey, Base64.NO_WRAP | Base64.NO_PADDING);

                httpPost = new HttpPost("http://twizted.co.uk:8080/MessageServerTwo/Auth");
                nameValuePairs = new ArrayList<>();

                nameValuePairs.add(new BasicNameValuePair("0", "0"));
                nameValuePairs.add(new BasicNameValuePair("1", encrypter.encrypt(username,
                                                                                 serverPubKey)));
                nameValuePairs.add(new BasicNameValuePair("2", encrypter.encrypt(password,
                                                                                 serverPubKey)));
                nameValuePairs.add(new BasicNameValuePair("4", key));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                response = httpClient.execute(httpPost);
                statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    result = username;
                }

            } catch (CertificateException | IOException | KeyStoreException |
                    NoSuchAlgorithmException | UnrecoverableEntryException |
                    NoSuchProviderException | BadPaddingException |
                    IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e)
            {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s)
        {
            Log.i(TAG, "Registration complete.");
            return;
        }
    }

    private class PubKey extends AsyncTask<String, Void, Contact>
    {
        private RSAPublicKey serverPubKey;

        public PubKey()
        {
            Contact server = new Contact(context);
            server.getContact("Server");

            try
            {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(server.get_key());
                serverPubKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        protected Contact doInBackground(String... params)
        {
            try
            {
                encrypter = new Encrypter();

                httpClient = new DefaultHttpClient();
                httpPost = new HttpPost("http://twizted.co.uk:8080/MessageServerTwo/Auth");
                nameValuePairs = new ArrayList<>();

                nameValuePairs.add(new BasicNameValuePair("0", "3"));
                nameValuePairs.add(new BasicNameValuePair("1", encrypter.encrypt(params[0], serverPubKey)));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                response = httpClient.execute(httpPost);
                statusLine = response.getStatusLine();

                String result = "";
                if (statusLine.getStatusCode() == HttpStatus.SC_OK)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    result = out.toString();
                    out.close();
                }

                if (!result.isEmpty() && !result.equals("0"))
                {
                    Contact c = new Contact(context);
                    c.set_username(params[0]);
                    c.set_key(Base64.decode(result, Base64.NO_PADDING | Base64.NO_WRAP));
                    c.persist();
                    return c;
                }
            } catch (NoSuchPaddingException | InvalidKeyException |
                    NoSuchProviderException | BadPaddingException |
                    IllegalBlockSizeException | NoSuchAlgorithmException |
                    IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Contact contact)
        {
            Log.i(TAG, "Contact details retrieved and saved.");
            return;
        }
    }

    private void sendResult(String message)
    {
        Intent intent = new Intent(RESULT);
        if (message != null)
        {
            intent.putExtra("IMMMESSAGE", message);
        }
        broadcaster.sendBroadcast(intent);
    }
}
