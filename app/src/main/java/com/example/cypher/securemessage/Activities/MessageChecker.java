package com.example.cypher.securemessage.Activities;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.example.cypher.securemessage.Manager.NetManager;

import java.util.concurrent.ExecutionException;

public class MessageChecker extends Service
{
    private final String TAG = "securemessage";
    private SharedPreferences prefs;

    public MessageChecker()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        prefs = getSharedPreferences(TAG, MODE_PRIVATE);
        final String me = prefs.getString("username", "");
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    NetManager netManager = new NetManager(getApplicationContext());
                    try
                    {
                        netManager.checkMessages(me);
                    } catch (ExecutionException | InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    try
                    {
                        Thread.sleep(30000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}
