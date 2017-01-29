package com.gamusicplayer.shakeel.gamusicplayer;

import java.sql.Struct;

/**
 * Created by Shak on 20/01/2016.
 * Song class - defines a Song object that has an entryId, name, binary ID, Spotify ID and Fitness.
 */
public class Song
{
    int EntryID;
    String Name;
    String ID;
    String SpotifyID;
    int Fitness;


    public Song(int entryID,String name,String id, String spotifyid,int fitness)
    {
        EntryID = entryID;
        Name = name;
        ID = id;
        SpotifyID = spotifyid;
        Fitness = fitness;
    }



}
