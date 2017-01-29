package com.gamusicplayer.shakeel.gamusicplayer;


import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.database.Cursor;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.util.Random;


public class MainActivity extends AppCompatActivity implements PlayerNotificationCallback, ConnectionStateCallback
{

    private static final String CLIENT_ID = "59109c44b5e841b4bfa4d4f55141819e";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";

    // Request code that will be passed together with authentication result to the onAuthenticationResult callback
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    private Player mPlayer;     // Spotify Player object

    private Button playButton;
    private Button skipNextButton;
    private TextView songName;
    private ProgressBar progressBar;
    private TextView[] songListViews;       // Actual text views of song table
    private TableRow[] songListRows;        // Collection of rows in the song table songList
    private TableLayout songList;

    private boolean paused = false;
    private boolean progressDone = false;


    public FeedReaderDbHelper mDbHelper;    // DbHelper object to help set up song database
    public SQLiteDatabase db;               // songDatabase
    public WriteToDatabase mReadWrite;      // Manages all reading and writing to and from the database
    public int[] entryIDs = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    public String[][] songInfo = {
            {
                    "All A Dream",  //0
                    "Pressure",          //1
                    "The Monster",  //2
                    "Lose Yourself", //3
                    "heartsigh",    //4
                    "push pull",    //5
                    "Crystallise",    //6
                    "Elements",      //7
                    "In The End",   //8
                    "Forgotten",    //9
                    "Hysteria",     //10
                    "New Born",     //11
                    "Piano Sonata", //12
                    "Fur Elise", //13
                    "Nuvole Bianche",       //14
                    "Ave Maria"      //15
            },
            {
                    "0000",  //0
                    "0001",  //1
                    "0010",  //2
                    "0011",  //3
                    "0100",  //4
                    "0101",  //5
                    "0110",  //6
                    "0111",  //7
                    "1000",  //8
                    "1001",  //9
                    "1010", //10
                    "1011", //11
                    "1100", //12
                    "1101", //13
                    "1110", //14
                    "1111", //15
            },
            {
                    "3KN80JbGYVhgEJcv3U0a27",  //0
                    "2gCP48sX8UFueGsusBDBZy",  //1
                    "5TLY55i7Mh5ses0mV5Vdx3",  //2
                    "7hLllX4WM5VXq4JG6h5YGK",  //3
                    "4OrLnFkB5Ebct9HNT5xU2i",  //4
                    "6pTfJ82DLrBoTTaKJrFr7R",  //5
                    "0z9UTMWoBnFsXBJSuOGZLf",  //6
                    "73LARGga47rEE3G7w4OOYk",  //7
                    "60a0Rd6pjrkxjPbaKzXjfq",  //8
                    "7kxM3UJwN6rHAW5kwdlAHK",  //9
                    "7xyYsOvq5Ec3P4fr6mM9fD", //10
                    "2VrJMuLt2m9HbifGrKWHqk", //11
                    "0rkajHs0Kcw73nFDlKmuBk", //12
                    "1QGN2Perfetpju3762mJsk", //13
                    "2lTcudVnztm9oWC4qJla7H", //14
                    "2kyEgPaAW8wdpvevPnkf0Z", //15
            },
    };
    public int[] songFitness = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    public Thread progressBarThread;        // Thread to allow progress bar to count in background

    private Song[] populationOfSongs;       // The current generation
    private Song[] newPopulationOfSongs;    // The generation currently being produced by the algorithm

    private int genIndex = 0;               // Current interation index of the algorithm
    private int maxGen = 10;                // Max number of iteratios of algorithm default 10
    private int populationSize = 6;         // Size of each generation, must be an even integer
    private int dbSize = 16;                // Number of elements in datavase
    private int songIDLength = 4;
    private int queueIndex = -1;            // Current place in spotify queue, initalised at -1 as it is immdieately incrememnted to 0 by spotify playback events
    private int trackEndCounter = 0;


    private int currentSongFitness = 0;     // Fitness of the current song being assessed
    private float sumFitness;               // Sum of the fitness values of a generation
    private float crossProbability = 0.7F;  // Probablity that crossover occurs, default 70%
    private float mutationProbability = 0.2F;   // Probablity that bit mutation occurs, default 20%

    private  Random rnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Progress Bar Thread - increments progress bar once every second while the song is not paused
        progressBarThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(!progressDone)
                {
                    while (!paused)
                    {
                        progressBar.incrementProgressBy(1);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        //Generate Spotify login authentication activity
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        //Open Spotify login activity
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        mDbHelper = new FeedReaderDbHelper(this);   //Create new database helper
        Log.d("dbSuccess", "mDbHelper success");
        db = mDbHelper.getWritableDatabase();       //Create new database using helper
        mReadWrite = new WriteToDatabase();         //Create new object to control reading and writing to database

        rnd = new Random();

        //INITIALISNG UI
        songName = (TextView)findViewById(R.id.songName);
        paused = false;
        songListViews = new TextView[populationSize];
        songListRows = new TableRow[populationSize/2];
        songList = (TableLayout)findViewById(R.id.songList);
        for(int i = 0; i < songListRows.length; i++)
        {
            songListRows[i] = new TableRow(this);
            songList.addView(songListRows[i]);
        }
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBarThread.start();

        initialiseDb();         //Fill DB with song's and their info stored in the SongInfo arrays
        populationOfSongs = generateSongs();       // Generate initial selection of songs
        newPopulationOfSongs = new Song[populationSize];   //Assign space for next generation of songs
        for(int i = 0; i < populationSize; i++)
        {
            newPopulationOfSongs[i] = new Song(0,null,"0000", "0000000000000000000000",0);
        }

    }

    /** Carries forward a single iteration of the algorithm, makes the produced generation of songs become
     * the current generation of songs
     */
    public void GAIteration()
    {
        if(genIndex < maxGen)
        {
            Log.d("Generation","function started");
            generation();
            Log.d("Generation", "success");
            genIndex++;     // Increment generation index forward
            populationOfSongs = newPopulationOfSongs;
        }
    }

    // When the Spotify Login authentication activity recieves a results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("MainActivity", "result code: " + resultCode);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN)    // If the response is correct then carry on
            {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);       //Create new Config
                //Create new player
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver()
                {
                    @Override
                    public void onInitialized(Player player)
                    {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);

                        GAIteration();  // Begin the first iteration of the algorithm

                        // Define play and skipNext buttons, clicking them causes them to affect the player appropriately
                        skipNextButton = (Button)findViewById(R.id.skipNextButton);
                        skipNextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mPlayer.skipToNext();
                            }
                        });

                        playButton = (Button)findViewById(R.id.playButton);
                        playButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v)
                            {
                                if(!paused)
                                {
                                    mPlayer.pause();
                                    paused = true;      // Causes progressBarThread to pause
                                }
                                else
                                {
                                    mPlayer.resume();
                                    paused = false;     // Resumes progressBar Thread
                                }
                            }
                        });

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }


    public void generation()
    {
        int mate1,mate2;    // Indexes of both song's that will be selected to crossover
        String[] newSongIDs;

        Cursor dbNewSongCursor =  db.query(FeedReaderContract.FeedEntry.TABLE_NAME, null, null, null, null, null, null);    // Declare new database cursor that queries by name
        //Loop through the current generation
        for(int j = 0; j < populationSize;)
        {
            mate1 = select(populationSize,populationOfSongs);      // Biased selection of two mates based on fitness
            mate2 = select(populationSize,populationOfSongs);
            newSongIDs = crossover(populationOfSongs[mate1].ID, populationOfSongs[mate2].ID, songIDLength, crossProbability, mutationProbability);  // Crossover of those songs
            newPopulationOfSongs[j].ID = newSongIDs[0];            //Assign both generated songs to the new generation of songs
            newPopulationOfSongs[j+1].ID = newSongIDs[1];
            j += 2;                                                //Increment to next pair of songs
        }

        // Loop through next generation
        for(int k = 0; k < populationSize; k++)
        {
            int newEntryIDValue = decode(newPopulationOfSongs[k].ID, songIDLength);                 // Decode the entryID of each new song from their new binary IDs
            newPopulationOfSongs[k].Name = mReadWrite.dbReadSongName(newEntryIDValue, dbNewSongCursor);     // Assign information from database to song array items
            newPopulationOfSongs[k].EntryID = newEntryIDValue;
            newPopulationOfSongs[k].SpotifyID = mReadWrite.dbReadSpotifyID(newEntryIDValue, dbNewSongCursor);
            newPopulationOfSongs[k].Fitness = mReadWrite.dbReadSongFitness(newEntryIDValue,dbNewSongCursor);

            Log.d("URI",newPopulationOfSongs[k].SpotifyID);

            mPlayer.queue("spotify:track:" + newPopulationOfSongs[k].SpotifyID);                    // Add the new song to the Spotify queue

            //UPDATE UI
            songListViews[k] = new TextView(this);                                                  // Create new textView for each song in generation
            songListViews[k].setText(newPopulationOfSongs[k].Name + " " + newPopulationOfSongs[k].Fitness); // Add name of song and it's fitness to table
            if(!(k % 2 == 0))   // If k is odd
            {
                int j = (k-1)/2;
                songListRows[j].addView(songListViews[k]);  // Add song as the second of a pair of songs in the correct row
            }
            else    // If k is even
            {
                songListViews[k].setPadding(0,0,230,0);
                int j = k/2;
                songListRows[j].addView(songListViews[k]);  // Add song as the first of the pair of songs in the correct row
            }

            Log.d("generation" + String.valueOf(genIndex), newPopulationOfSongs[k].Name + " " + newPopulationOfSongs[k].ID + " " + newPopulationOfSongs[k].SpotifyID + " " + newPopulationOfSongs[k].Fitness + " " + newPopulationOfSongs[k].EntryID);


        }
        dbNewSongCursor.close();    //Close cursor

    }


    public int select(int popSize, Song[] songPopulation)
    {
        sumFitness = 0;     //Reset sumFitness to 0

        int j;              // Index to return
        int index = 0;      // Selected index

        float fRand;        // Random marker for this selection
        float fCumulative[] = new float[popSize];   // Array that accumlates the fitness of the generation
        float oldVal;
        float newVal;

        //Loop through generation
        for(int i = 0; i < popSize; i++)
        {
           sumFitness = sumFitness + songPopulation[i].Fitness; //Sum the fitness
        }
        Log.d("sumFitness", "Sum of Fitness = " + sumFitness);

        float r = randomFloat();    //Generate random float between 0-1

        fRand = sumFitness* r;      //Set marker to random fraction of sumFitness
        Log.d("fRand", String.valueOf(fRand));

        for( j = 0; j < popSize; j++)
        {

            if(j==0)
            {
                fCumulative[0] = songPopulation[j].Fitness; //For first song, cumulative fitness is just the song fitness
                continue;
            }
            else
                oldVal = fCumulative[j-1];                  // Record the value of the previous song as the old value
            newVal = songPopulation[j].Fitness;             // Record the value of the current song as the new value

            fCumulative[j] = oldVal + newVal;               // The current cumulative fitness is the sum

            // If the random marker lies between any two indexes of cumulative fitness, set that index to be selected
            if(fRand >= fCumulative[j-1] && fRand < fCumulative[j])
            {
               index = j;
            }

        }

        j = index;
        if(j == populationSize) // To prevent arrayOutOfBounds exceptions
            j -= 1;
        Log.d("jSelect",String.valueOf(j));
        return j;
    }

    public String[] crossover(String parent1ID,String parent2ID,int songIDLength,float pCross,float pMutation)
    {
        int j;  // Current index
        int crossIndex; // Index at which crossover occurs
        String child1ID = "0000";   // Target child Strings
        String child2ID = "0000";

        String[] childrenOut = new String[2];   // Children strings to return as a pair

        char[] cParent1ID = parent1ID.toCharArray();    //Convert all strings to char arrays
        char[] cParent2ID = parent2ID.toCharArray();
        char[] cChild1ID = child1ID.toCharArray();
        char[] cChild2ID = child2ID.toCharArray();
        if(flip(pCross))    // If a flip biased by percentage of crossover is true
        {
            crossIndex = randomInt(1,songIDLength-1);   //Select crossover index randomly between 1 and songID length
        }
        else
            crossIndex = songIDLength;                  // No crossover

        Log.d("crossIndex", String.valueOf(crossIndex));

        //Loop up to the crossIndex
        for(j = 0; j < crossIndex; j++)
        {
            cChild1ID[j] =  mutation(cParent1ID[j], pMutation); //Match values before crossIndex with that of the same parent, allowing for a small chance mutation to occur
            cChild2ID[j] =  mutation(cParent2ID[j], pMutation);

        }
        if(crossIndex != songIDLength) //If crossover can occur
        {
            //Loop from crossIndex to end of string
            for(j = crossIndex; j < songIDLength; j++)
            {
                //with chance of bit mutation
                cChild1ID[j] =  mutation(cParent2ID[j], pMutation); //Match parent2[j] values with child1[j]
                cChild2ID[j] =  mutation(cParent1ID[j], pMutation); //Match parent2[j] values with child1[j]

            }
        }

        //Obtain strings of newly created IDs
        child1ID = String.valueOf(cChild1ID);
        child2ID = String.valueOf(cChild2ID);
        Log.d("crossover",child1ID + " " + child2ID);

        childrenOut[0] = child1ID;  // Assign strings to array so they can be returned
        childrenOut[1] = child2ID;

        return childrenOut;

    }

    //Decodes a binary string into an int
    public int decode(String songID,int idLength)
    {
        int accum = 0;  // Int accumulation of the binary number
        int powerOf2 = 1;   //Current power of 2 being represented

        char[] cSongID = songID.toCharArray();  //Convert string to char array to loop through
        for(int c = idLength-1; c >= 0; c--)    // Loop backwards from first bit to last
        {
            if(cSongID[c] == '1')               //If bit is 1, add current power of 2 to accumulation and increase power of 2
            {
                accum += powerOf2;
                powerOf2 *= 2;
            }
            else
                powerOf2 *=2;                   //Otherwise just increase power of 2
        }

        return accum;
    }


    public int randomInt(int low, int high)
    {
        int i;
        if(low >= high)
            i = low;
        else
        {
            i = Math.round(rnd.nextFloat()*(high-low+1)+low);
            if(i > high)
                i = high;
        }
        return i;
    }
    public float randomFloat()
    {
        float i = 0;
        while(i < 0.5F)
        {
            i = rnd.nextFloat();
        }
        return i;
    }

    public char mutation(char allele, float pMutation)
    {
        boolean mutate = flip(pMutation);
        //Log.d("Allele",String.valueOf(allele));
        char a;
        if(mutate)
        {
            if(allele == '0')
            {
                allele = '1';
            }
            else
                allele = '0';
        }
        a = allele;
        return a;
    }

    public boolean flip(float probability)
    {
        boolean f;
        if(probability == 1.0)
        {
            f = true;
        }
        else
            f = (rnd.nextFloat() <= probability);
        return f;
    }


    public void initialiseDb()
    {
        Cursor dbInitialCursor;
        dbInitialCursor = db.query(FeedReaderContract.FeedEntry.TABLE_NAME, null, null, null, null, null, null);

        if(dbInitialCursor.getCount()==0)
        {
            //For each database entry
            for (int i = 0; i < dbSize; i++)
            {
                mReadWrite.dbWrite(entryIDs[i],songInfo[0][i],songInfo[1][i], songInfo[2][i], songFitness[i],db);   //Write song info to db
            }
        }

        dbInitialCursor.close();
    }

    // Method to update a song's fitness value in the database
    public int updateDbFitness(int newFitness,int entryID, SQLiteDatabase db)
    {
        mReadWrite.dbUpdate(newFitness,entryID, db);
        Cursor dbCursor = db.query(FeedReaderContract.FeedEntry.TABLE_NAME,null,null,null,null,null,null);
        String ID = mReadWrite.dbReadSongID(entryID, dbCursor);             //DEBUGGING
        String name = mReadWrite.dbReadSongName(entryID, dbCursor);
        int Fitness = mReadWrite.dbReadSongFitness(entryID, dbCursor);

        dbCursor.close();
        Log.d("Update", ID + " " + name + " " + String.valueOf(Fitness));
        return Fitness;
    }

    public Song[] generateSongs()
    {
        Song[] songs = new Song[populationSize];
        Cursor dbCursor = db.query(FeedReaderContract.FeedEntry.TABLE_NAME, null, null, null, null, null, null);
        for(int i = 0; i < populationSize; i++)
        {
            int w = rnd.nextInt(populationSize);
            songs[i] = new Song(w,mReadWrite.dbReadSongName(w, dbCursor), mReadWrite.dbReadSongID(w, dbCursor),mReadWrite.dbReadSpotifyID(w,dbCursor), mReadWrite.dbReadSongFitness(w,dbCursor));
            Log.d("Initial Population",songs[i].Name + " " + songs[i].EntryID + " " + songs[i].ID + " " + songs[i].Fitness);
        }

        dbCursor.close();
        return songs;
    }

    public void assessFitness(int duration, int position)
    {
        currentSongFitness = (int)(((double)position/(double)duration)*100);    // Takes percentage of song played as fitness value
        Log.d("CFitness",String.valueOf(currentSongFitness));
        newPopulationOfSongs[queueIndex].Fitness = updateDbFitness(currentSongFitness,newPopulationOfSongs[queueIndex].EntryID,db); //Update fitness value in database
        Log.d("popFitnessUpdate",String.valueOf(newPopulationOfSongs[queueIndex].Fitness));
        songListViews[queueIndex].setText(newPopulationOfSongs[queueIndex].Name + " " + newPopulationOfSongs[queueIndex].Fitness ); // Update UI

    }



    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name() + " " + playerState.trackUri);

        switch (eventType) {
            // Handle event type as necessary
            case SKIP_NEXT:
                //When song is skipped
                Log.d("Duration on Next", String.valueOf(playerState.durationInMs));
                Log.d("Position on Next", String.valueOf(playerState.positionInMs));
                assessFitness(playerState.durationInMs, playerState.positionInMs);  //Assess the fitness of current song
                Log.d("queueIndex",String.valueOf(queueIndex));
                break;
            case END_OF_CONTEXT:
                //When queue is complete, iterate GA
                Log.d("GAThread","Sucessful iteration");
                //Update UI
                for(int i = 0; i < songListRows.length; i++)
                {
                    songListRows[i].removeAllViews();
                }
                queueIndex = -2;    // Reset queue index, queue index will be incremented by 2 during initialisation
                GAIteration();      // Call next GA interation
                break;


            case TRACK_END:
                trackEndCounter++;
                if(trackEndCounter == 2)        //Song was played to 100%
                {
                    assessFitness(100,100);
                    Log.d("queueIndex",String.valueOf(queueIndex));
                    trackEndCounter = 0;
                }
                break;
            case TRACK_CHANGED:
                queueIndex++;       //When track is change, increment queue index to change app state to next song
                break;

            case TRACK_START:
                trackEndCounter = 0;
                //When a song starts, update UI to match with song
                songName.setText(populationOfSongs[queueIndex].Name);
                progressBar.setMax(millisToSec(playerState.durationInMs));
                Log.d("Duration in seconds", "" + millisToSec(playerState.durationInMs));
                progressBar.setProgress(0);
                break;

            default:
                break;
        }
    }

    public int millisToSec(long millis)
    {
        millis /= 1000;
        return (int)millis;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}


