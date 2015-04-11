package com.example.cypher.securemessage.Beans;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.cypher.securemessage.Misc.DbCon;

import java.util.ArrayList;
import java.util.Arrays;

public class Contact
{
    private int _id;
    private String _username;
    private byte[] _key;
    private Context context;

    public Contact(Context context)
    {
        this.context = context;
    }

    public Contact(int id, String name, byte[] key)
    {
        this._id = id;
        this._username = name;
        this._key = key;
    }

    public Contact(String name, byte[] key)
    {
        this._username = name;
        this._key = key;
    }

    public void persist()
    {
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DbCon.COLUMN_USERNAME, this._username);
        contentValues.put(DbCon.COLUMN_KEY, this._key);

        con.insert(DbCon.TABLE_CONTACTS, null, contentValues);
        con.close();
        dbCon.close();
    }

    public void getContact(String contact_username)
    {
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        String query = "SELECT * FROM " + DbCon.TABLE_CONTACTS + " WHERE " + DbCon.COLUMN_USERNAME + " = '" + contact_username + "';";

        Cursor cursor = con.rawQuery(query, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            if (cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME)) != null)
            {
                this._id = cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_ID));
                this._username = cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME));
                this._key = cursor.getBlob(cursor.getColumnIndex(DbCon.COLUMN_KEY));
                cursor.moveToNext();
            }
        }
        con.close();
        cursor.close();
        dbCon.close();
    }

    public void getContact(int contact_id)
    {
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        String query = "SELECT * FROM " + DbCon.TABLE_CONTACTS + " WHERE " + DbCon.COLUMN_ID + " = '" + contact_id + "';";

        Cursor cursor = con.rawQuery(query, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            if (cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME)) != null)
            {
                this._id = cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_ID));
                this._username = cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME));
                this._key = cursor.getBlob(cursor.getColumnIndex(DbCon.COLUMN_KEY));
                cursor.moveToNext();
            }
        }
        con.close();
        cursor.close();
        dbCon.close();
    }

    public Contact[] getContacts()
    {
        ArrayList<Contact> contactList = new ArrayList<>();
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        String query = "SELECT * FROM " +
                DbCon.TABLE_CONTACTS + " WHERE 1;";

        Cursor cursor = con.rawQuery(query, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            if (cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME)) != null)
            {
                if (!cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME)).equals("Server"))
                {
                    contactList.add(new Contact(cursor.getInt(cursor.getColumnIndex(DbCon.COLUMN_ID)),
                                                cursor.getString(cursor.getColumnIndex(DbCon.COLUMN_USERNAME)),
                                                cursor.getBlob(cursor.getColumnIndex(DbCon.COLUMN_KEY))));
                }
                cursor.moveToNext();
            }
        }
        con.close();
        cursor.close();
        dbCon.close();
        Contact[] contactArray = new Contact[contactList.size()];
        contactList.toArray(contactArray);

        return contactArray;
    }

    public void deleteContact()
    {
        DbCon dbCon = new DbCon(context, null);
        SQLiteDatabase con = dbCon.getWritableDatabase();

        String delStatement = "DELETE FROM " +
                DbCon.TABLE_CONTACTS + " WHERE " +
                DbCon.COLUMN_ID + " = " +
                this._id + ";";

        con.execSQL(delStatement);
        con.close();
        dbCon.close();
    }

    public Contact(String _username)
    {
        this._username = _username;
    }

    public int get_id()
    {
        return _id;
    }

    public String get_username()
    {
        return _username;
    }

    public void set_username(String _username)
    {
        this._username = _username;
    }

    public byte[] get_key()
    {
        return _key;
    }

    public void set_key(byte[] _key)
    {
        this._key = _key;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    @Override
    public String toString()
    {
        return "_id = " + _id +
                "\n_username = " + _username +
                "\n_key = " + Arrays.toString(_key);
    }
}
