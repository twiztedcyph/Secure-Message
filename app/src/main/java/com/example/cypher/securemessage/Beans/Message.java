package com.example.cypher.securemessage.Beans;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.cypher.securemessage.Misc.DbCon;

import java.sql.SQLOutput;
import java.util.ArrayList;

public class Message
{
    private int _id, _contactID;
    private boolean _fromME;
    private String _messageContent;
    private String _timeStamp;
    private Context context;

    public Message(Context context)
    {
        this.context = context;
    }

    public Message(int id, int contactID, boolean fromME, String messageContent)
    {
        this._id = id;
        this._contactID = contactID;
        this._fromME = fromME;
        this._messageContent = messageContent;
    }

    public Message(int contactID, boolean fromME, String messageContent, String _timeStamp)
    {
        this._contactID = contactID;
        this._fromME = fromME;
        this._messageContent = messageContent;
        this._timeStamp = _timeStamp;
    }

    public void persist()
    {
        try
        {
            System.out.println("SAVING MESSAGE");
            DbCon dbCon = new DbCon(context, null);
            SQLiteDatabase con = dbCon.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DbCon.COLUMN_CONTACT_ID, _contactID);
            values.put(DbCon.COLUMN_FROM_ME, (_fromME) ? 1 : 0);
            values.put(DbCon.COLUMN_MESSAGE, this._messageContent);
            values.put(DbCon.COLUMN_TIMESTAMP, this._timeStamp);

            con.insert(DbCon.TABLE_MESSAGES, null, values);
            con.close();
            dbCon.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Message[] getAll(int _contactID)
    {
        ArrayList<Message> resultsArrayList = new ArrayList<>();
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        String query = "SELECT * FROM " + DbCon.TABLE_MESSAGES + " WHERE " + DbCon.COLUMN_CONTACT_ID + " = " + _contactID + ";";

        Cursor cursor = con.rawQuery(query, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            int id = cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_MESSAGE_ID));
            int fromID = cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_CONTACT_ID));
            boolean fromMe = (cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_FROM_ME)) == 1);
            String message = cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_MESSAGE));

            resultsArrayList.add(new Message(id, fromID, fromMe, message));
            cursor.moveToNext();
        }
        con.close();
        cursor.close();
        dbCon.close();

        Message results[] = new Message[resultsArrayList.size()];
        resultsArrayList.toArray(results);

        return results;
    }

    public void delete()
    {
        DbCon con = new DbCon(context, null);
        SQLiteDatabase db = con.getWritableDatabase();


        String delStatement = "DELETE FROM " + DbCon.TABLE_MESSAGES +
                " WHERE " + DbCon.COLUMN_MESSAGE_ID + " = " +
                this._id + ";";

        db.execSQL(delStatement);

        db.close();
        con.close();
    }

    //Delete all messages...? Do I need something to do that??? Not at the moment...

    public int get_id()
    {
        return _id;
    }

    public int get_contactID()
    {
        return _contactID;
    }

    public void set_contactID(int contactID)
    {
        this._contactID = contactID;
    }

    public boolean is_fromME()
    {
        return _fromME;
    }

    public void set_fromME(boolean fromME)
    {
        this._fromME = fromME;
    }

    public String get_messageContent()
    {
        return _messageContent;
    }

    public String get_timeStamp()
    {
        return _timeStamp;
    }

    public void set_timeStamp(String _timeStamp)
    {
        this._timeStamp = _timeStamp;
    }

    public void set_messageContent(String messageContent)
    {
        this._messageContent = messageContent;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    @Override
    public String toString()
    {
        return "_id = " + _id +
                "\n_contactID = " + _contactID +
                "\n_fromME = " + _fromME +
                "\n_messageContent = " + _messageContent +
                "\n_timeStamp = " + _timeStamp;
    }
}
