package com.amazonaws.demo.polly;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.translate.AmazonTranslateAsyncClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

import java.io.IOException;
import java.net.URL;


public class MainActivity extends Activity {
    private static final String TAG = "PollyDemo";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    View progressBar;
    MediaPlayer mediaPlayer;
    private AmazonPollyPresigningClient client;
    private EditText sampleTextEditText;
    private Button playButton;
    private ImageButton defaultTextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ");

        playButton = findViewById(R.id.readButton);
        progressBar = findViewById(R.id.voicesProgressBar);
        defaultTextButton = findViewById(R.id.defaultTextButton);
        sampleTextEditText = findViewById(R.id.sampleText);

        progressBar.setVisibility(View.VISIBLE);

        initPollyClient();
        setupNewMediaPlayer();
        setupSampleTextEditText();
        setupDefaultTextButton();
    }


    void initPollyClient() {
        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                // Create a client that supports generation of presigned URLs.
                client = new AmazonPollyPresigningClient(AWSMobileClient.getInstance());
                Log.d(TAG, "onResult: Created polly pre-signing client");


            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "onError: Initialization error", e);
            }
        });
    }

    void setupSampleTextEditText() {
        sampleTextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                defaultTextButton.setVisibility(sampleTextEditText.getText().toString().isEmpty() ?
                        View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sampleTextEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                playButton.performClick();
                return false;
            }
        });
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                playButton.setEnabled(true);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                playButton.setEnabled(true);
                return false;
            }
        });
    }



    void setupDefaultTextButton() {
        defaultTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleTextEditText.setText(null);
            }
        });
    }

    public void speechRequest(String word) {
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set text to synthesize.
                        .withText(word)
                        // Set voice selected by the user.
                        .withVoiceId("Miguel")
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

        Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

        // Create a media player to play the synthesized audio stream.
        if (mediaPlayer.isPlaying()) {
            setupNewMediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
        }

        // Start the playback asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();
    }

    public void playVoice(View view) {
        playButton.setEnabled(false);
        String textToRead = sampleTextEditText.getText().toString();

        AWSCredentials awsCredentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAIUMBDEU25R2PYY2Q";
            }

            @Override
            public String getAWSSecretKey() {
                return "6fRYbyM8XITfxRuV4iXDgBVSafQmEyLOCkP5xCbX";
            }
        };



        AmazonTranslateAsyncClient translateAsyncClient = new AmazonTranslateAsyncClient(awsCredentials);
        TranslateTextRequest translateTextRequest = new TranslateTextRequest()
                .withText(textToRead)
                .withSourceLanguageCode("auto")
                .withTargetLanguageCode("es");

        translateAsyncClient.translateTextAsync(translateTextRequest, new AsyncHandler<TranslateTextRequest, TranslateTextResult>() {
            @Override
            public void onError(Exception e) {
                Log.e(LOG_TAG, "Error occurred in translating the text: " + e.getLocalizedMessage());



            }

            @Override
            public void onSuccess(TranslateTextRequest request, TranslateTextResult translateTextResult) {

                Log.d(LOG_TAG, "Original Text: " + request.getText());
                Log.d(LOG_TAG, "Translated Text: " + translateTextResult.getTranslatedText());

                String word = translateTextResult.getTranslatedText();
                speechRequest(word);

            }
        });
    }
}
