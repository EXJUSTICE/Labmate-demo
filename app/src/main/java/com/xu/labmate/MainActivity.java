package com.xu.labmate;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import com.marytts.android.link.MaryLink;
import android.os.Vibrator;

//TODO 2408 Added base setup, permissions, and names of main methods.
//TODO 2408 added all of the background code and modules for pocketsphinx
//TODO 2608 Added proper dict and gram,attempting appending for editText
//TODO 2608 Added in android Mary
//TODO need to add in lights and vibration, and voice command tts!
//TODO also think properly about UI. needs to be seemless
//https://github.com/cmusphinx/pocketsphinx-android-demo/blob/master/app/src/main/java/edu/cmu/pocketsphinx/demo/PocketSphinxActivity.java
// https://cmusphinx.github.io/wiki/tutorialandroid/
//mainly https://www.vladmarton.com/pocketsphinx-continuous-speech-recognition-android-tutorial/
public class MainActivity extends AppCompatActivity implements
        RecognitionListener {
    /* We only need the keyphrase to start recognition, one menu with list of choices,
         and one word that is required for method switchSearch - it will bring recognizer
         back to listening for the keyphrase*/
    private static final String KWS_SEARCH = "wakeup";
    //TODOMENU ISNT A KEYWORD, THIS IS RATHER a link to the MENU GRAMMAR FILE!!!
    private static final String MENU_SEARCH = "menu";
    private static final String SAMPLE_ADD = "sample";
    //Since keyword searches only last a few syllables, we need to have an EDIT TEXT Here
    //That only stops when the user specifically callse one of the ENDSEARCH words
    private static final String DONE ="done";
    private static final String RESET = "reset";
    private static final String OVER = "over";
    //when app is listening, will respond to this keyword

    private static final String KEYPHRASE = "computer";
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    EditText debugtext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        debugtext = (EditText)findViewById(R.id.debugtext);
        runRecognizerSetup();



        FloatingActionButton speakbtn = (FloatingActionButton) findViewById(R.id.fab);
        speakbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaryLink.getInstance().startTTS(debugtext.getText().toString());
                if (MaryLink.getInstance() != null)
                    Snackbar.make(view, "Speaking <))", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

            }
        });

        FloatingActionButton clearbtn = (FloatingActionButton) findViewById(R.id.clearbutton);
        clearbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                debugtext.clearComposingText();
                //FAB may not be necessary here, as the app responds to wiords, but who knows!

            }
        });

        FloatingActionButton uploadbtn = (FloatingActionButton) findViewById(R.id.uploadbutton);
        uploadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


            }
        });


        checkPermissions();
        MaryLink.load(this);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        debugtext.setText("Initializing...");
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }
            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    System.out.println(result.getMessage());
                } else {
                    debugtext.setText("Initialization complete");
                    Toast.makeText(MainActivity.this,"Initialization complete",Toast.LENGTH_LONG);
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }
    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                // Disable this line if you don't want recognizer to save raw audio files to app's storage
                //.setRawLogDir(assetsDir)
                .getRecognizer();
        recognizer.addListener(this);
        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        // Create your custom grammar-based search
        //TODO we are using a custom grammar menu for testing
        File menuGrammar = new File(assetsDir, "labmate.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
    }


    public void beginRecord(){

    }

  //SWITCH BETWEEN KEYPHRASE OR MENU LISTENING
    //This is the key issue here

    //TODO READ UP https://stackoverflow.com/questions/41522815/how-to-setup-tresholds-to-spot-keywords-from-a-list-in-pocketsphinx-android

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            debugtext.setText("Standing by");
            switchSearch(MENU_SEARCH);
        } else if (text.equals("hello")) {
            System.out.println("Hello to you too!");
            debugtext.setText("Hello to you too!");
            switchSearch(MENU_SEARCH);
        } else if (text.equals("good morning")) {
            System.out.println("Good morning to you too!");
            debugtext.setText("Good Morning");
            switchSearch(MENU_SEARCH);
        } else if (text.equals("clear")){
            debugtext.clearComposingText();

        } else{
            debugtext.append(text);
            System.out.println(hypothesis.getHypstr());
            switchSearch(MENU_SEARCH);
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {

        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech(){
    }


    //RESET RECOGNIZER BACK TO KEYPHRASE LISTENING, OR LISTEN TO MENU OPTIONS AFTER END OF SPEECH
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    // SWITCH BETWEEN CONTINUOUS RECOGNITION OF KEYPHRASE, OR RECOGNITION OF MENU ITEMS WITH 10 SECONDS TIMEOUT
    private void switchSearch(String searchName) {
        recognizer.stop();
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }

    //IF THE 10 SECOND TIMEOUT IS FINISHED, SWITCH BACK TO KEYPHRASE RECOGNITION, AS NO MENU COMMAND WAS RECEIVED
    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }



    public void checkPermissions(){
        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
    }

    @Override
    public void onError(Exception error) {
        System.out.println(error.getMessage());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                //TODO MAKE THE TASK AND SET IT UP
                //new SetupTask(this).execute();
            } else {
                finish();
            }
        }
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


    //Destroying objects on app exit
    @Override
    public void onStop() {
        super.onStop();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }



}
