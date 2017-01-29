package com.gamusicplayer.shakeel.gamusicplayer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.*;
import android.util.Log;


public class WriteToDatabase extends MainActivity
{
    public WriteToDatabase (){}

    public void dbWrite(int entryID, String songName, String songID, String spotifyID, int fitness, SQLiteDatabase db)
    {
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_ENTRY_ID, entryID);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SONG_NAME, songName);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SONG_ID, songID);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_SPOTIFY_ID, spotifyID);
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_FITNESS, fitness);

        long newRowID;
        newRowID = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, FeedReaderContract.FeedEntry.COLUMN_NAME_NULLABLE,values);

        Log.d("Write Success",String.valueOf(newRowID));
    }

    public void dbUpdate(int newFitness, int entryID, SQLiteDatabase db)
    {
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_FITNESS, newFitness);
        String valueAdded = values.getAsString(FeedReaderContract.FeedEntry.COLUMN_NAME_FITNESS);

        Log.d("Values", "Value added " + valueAdded);

        int noOfRows;
        noOfRows = db.update(FeedReaderContract.FeedEntry.TABLE_NAME, values, "entryID = " + String.valueOf(entryID), null);

        Log.d("Update",String.valueOf(noOfRows));
    }



    public String dbReadSongName(int position, Cursor c)
    {
        c.moveToPosition(position);
        String songName = c.getString(1);
        return songName;

    }

    public String dbReadSongID(int position, Cursor c)

    {
        c.moveToPosition(position);
        String songID = c.getString(2);
        return songID;

    }
    public String dbReadSpotifyID(int position, Cursor c)

    {
        c.moveToPosition(position);
        String spotifyID = c.getString(3);
        return spotifyID;

    }

    public int dbReadSongFitness(int position, Cursor c)

    {
        c.moveToPosition(position);
        int songFitness = c.getInt(4);
        return songFitness;

    }



}