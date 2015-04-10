package com.example.cypher.securemessage.Manager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.example.cypher.securemessage.Beans.Contact;
import com.example.cypher.securemessage.Beans.Message;
import com.example.cypher.securemessage.Misc.Encrypter;

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

    private class Incoming extends AsyncTask<Void, Void, Message[]>
    {
        @Override
        protected Message[] doInBackground(Void... params)
        {
            return null;
        }

        @Override
        protected void onPostExecute(Message[] messages)
        {
            super.onPostExecute(messages);
        }
    }

    private class Outgoing extends AsyncTask<String, Void, Void>
    {
        private RSAPublicKey serverPubKey;

        public Outgoing()
        {
            Contact server = new Contact(context);
            server.getContact("Server");

            System.out.println(server);

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
                nameValuePairs.add(new BasicNameValuePair("1", encrypter.encrypt(params[0], serverPubKey)));
                //message
                System.out.println(params[1]);
                nameValuePairs.add(new BasicNameValuePair("2", params[1]));
                //to
                System.out.println(params[2]);
                nameValuePairs.add(new BasicNameValuePair("3", encrypter.encrypt(params[2], serverPubKey)));

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
}
