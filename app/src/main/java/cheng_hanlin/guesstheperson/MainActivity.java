package cheng_hanlin.guesstheperson;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.id.button2;
import static android.R.id.button3;
import static android.R.string.no;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static android.support.v7.widget.AppCompatDrawableManager.get;

import com.loopj.android.http.*;
import cz.msebera.android.httpclient.Header;

//The "Guess the Person" game shows a name and 6 pictures.
// The player chooses the picture that matches the name.
// The game keeps a score of the number of questions correctly answered.
// There is a hint function that removes some pictures from the choices.
public class MainActivity extends AppCompatActivity {
    Button button0;
    Button hint;
    List<ImageView> imageViews = new ArrayList<>();
    ImageView imageView0;
    ImageView imageView1;
    ImageView imageView2;
    ImageView imageView3;
    ImageView imageView4;
    ImageView imageView5;
    TextView scoreText;
    int score = 0;
    int totalQuestions = 0;
    JSONArray jsonArray;
    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> imageURLs = new ArrayList<>();
    int chosenID = 0;
    int locationOfCorrectAnswer = 0;
    int numOfAnswerChoices = 6;
    ArrayList<Integer> answersIDList = new ArrayList<>();
    ArrayList<String> answerURLs = new ArrayList<>();
    String buttonText = "Loading new question.";
    boolean firstAnswer = true;

//    When the system starts, the system checks for previous state.
//    If there's previous state, the system simply updates UI with recovered data.
//    If not, the system makes API call to retrieve JSON data to create the question.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button0 = (Button) findViewById(R.id.button);
        hint = (Button) findViewById(R.id.hintButton);
        scoreText = (TextView) findViewById(R.id.textView0);
        imageView0 = (ImageView) findViewById(R.id.imageView0);
        imageViews.add(imageView0);
        imageView1 = (ImageView) findViewById(R.id.imageView1);
        imageViews.add(imageView1);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        imageViews.add(imageView2);
        imageView3 = (ImageView) findViewById(R.id.imageView3);
        imageViews.add(imageView3);
        imageView4 = (ImageView) findViewById(R.id.imageView4);
        imageViews.add(imageView4);
        imageView5 = (ImageView) findViewById(R.id.imageView5);
        imageViews.add(imageView5);
        setTitle("Guess the Person");

        if (savedInstanceState != null) {
            names = savedInstanceState.getStringArrayList("outNames");
            answerURLs = savedInstanceState.getStringArrayList("outAnswerURLs");
            imageURLs = savedInstanceState.getStringArrayList("outImageURLs");
            chosenID = savedInstanceState.getInt("outChosenID");
            locationOfCorrectAnswer = savedInstanceState.getInt("outLocationOfCorrectAnswer");
            answersIDList = savedInstanceState.getIntegerArrayList("outAnswersIDList");
            buttonText = savedInstanceState.getString("outButtonText");
            score = savedInstanceState.getInt("outScore");
            totalQuestions = savedInstanceState.getInt("outTotalQuestions");
            firstAnswer = savedInstanceState.getBoolean("outFirstAnswer");
            UpdateUI();
        } else {
            AsyncHttpClient client2 = new AsyncHttpClient();
            client2.get("https://willowtreeapps.com/api/v1.0/profiles/", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    super.onSuccess(statusCode, headers, response);
                    try {
                        jsonArray = new JSONArray(response.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    parseJSON();
                }
            });
        }
    }

//    When the JSON returned from API call is successful, the JSON  is parsed.
    public void parseJSON() {
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jo = jsonArray.getJSONObject(i);
                String name = jo.getString("firstName") + " " + jo.getString("lastName");
                names.add(name);
                String imageUrl = "http:" + jo.getJSONObject("headshot").getString("url"); //may get exception if no image for person
                imageURLs.add(imageUrl);
            } catch (JSONException e) {
                Log.d("debug", "JSON parsing exception");
                e.printStackTrace();
                //some ppl don't have associated images, so attach a non-descript image to the name
                imageURLs.add("http:http://images.ctfassets.net/3cttzl4i3k1h/5ZUiD3uOByWWuaSQsayAQ6/c630e7f851d5adb1876c118dc4811aed/featured-image-TEST1.png");
                continue;
            }
        }
        createNewQuestion();
    }

//    The question is created using parsed JSON data.
//    A name and 6 pictures are randomly picked.
    public void createNewQuestion() {
        hint.setVisibility(View.VISIBLE);
        firstAnswer = true;
        totalQuestions++;
        answersIDList.clear();
        answerURLs.clear();
        Random random = new Random();
        chosenID = random.nextInt(names.size()); //range 0 to size-1
        locationOfCorrectAnswer = random.nextInt(numOfAnswerChoices); //range 0 to 5
        int nonChosenID;
        Log.d("debug", names.size() + " " + imageURLs.size());
        for (int i = 0; i < numOfAnswerChoices; i++) {
            imageViews.get(i).setVisibility(View.VISIBLE);
            if (i == locationOfCorrectAnswer) {
                answerURLs.add(imageURLs.get(chosenID));
                answersIDList.add(chosenID);
            } else {
                nonChosenID = random.nextInt(names.size());
                while (answersIDList.contains(nonChosenID) || nonChosenID == chosenID) {
                    nonChosenID = random.nextInt(names.size());
                }
                answersIDList.add(nonChosenID);
                answerURLs.add(imageURLs.get(nonChosenID));
            }
        }
        UpdateUI();
    }

//    After the question is created, or after returning from the previous state,
//    the screen UI is updated (the pictures, buttons, texts, etc.).
    public void UpdateUI() {
        scoreText.setText("Score: " + String.valueOf(score) + "/" + String.valueOf(totalQuestions));
        for (int i = 0; i < answerURLs.size(); i++) {
            try {
                ImageDownloader imageTask = new ImageDownloader();
                Bitmap image = imageTask.execute(answerURLs.get(i)).get();
                imageViews.get(i).setImageBitmap(image);
            } catch (Exception e) {
                Log.d("debug", "error fetching image at URL " + answerURLs.get(i));
                e.printStackTrace();
            }
        }
        if (buttonText.equals("Loading new question."))
            buttonText = "Who is " + names.get(chosenID) + "?";
        button0.setText(buttonText);
    }

//    onClick callback for when user clicked on a picture when picking an answer
    public void personChosen(View view) {
        if (view.getTag().toString().equals(Integer.toString(locationOfCorrectAnswer))) {
            buttonText = "Correct!\nThis is " + names.get(chosenID) + ".  >>click NEXT";
            button0.setText(buttonText);
            if (firstAnswer == true) {
                score++;
                scoreText.setText("Score: " + String.valueOf(score) + "/" + String.valueOf(totalQuestions));
                firstAnswer = false;
            }
        } else {
            int unChosenID = answersIDList.get(Integer.valueOf(view.getTag().toString()));
            buttonText = "Wrong!\nThis is " + names.get(unChosenID) + ".  >>click NEXT";
            button0.setText(buttonText);
            if (firstAnswer == true) {
                firstAnswer = false;
            }
        }
    }

//    onClick callback for moving to next question
    public void buttonClick(View view) {
        if (firstAnswer == false) {
            buttonText = "Loading new question.";
            button0.setText(buttonText);
            createNewQuestion();
        }
    }

//    onClick callback for the "Hint" button.
//    Two pictues of wrong choices are removed.
    public void hintClick(View view) {
        if (hint.getVisibility() == View.GONE) {
            return;
        }
        Random random = new Random();
        int dropOut1 = random.nextInt(numOfAnswerChoices);
        while (dropOut1 == locationOfCorrectAnswer) {
            dropOut1 = random.nextInt(numOfAnswerChoices);
        }
        int dropOut2 = random.nextInt(numOfAnswerChoices);
        while (dropOut2 == locationOfCorrectAnswer || dropOut1 == dropOut2) {
            dropOut2 = random.nextInt(numOfAnswerChoices);
        }
        imageViews.get(dropOut1).setVisibility(View.GONE);
        imageViews.get(dropOut2).setVisibility(View.GONE);
        hint.setVisibility(View.GONE);
    }

//    Save the current state, so the game can reload properly, like after a screen rotation.
//    The Bundle outState will pass info to the Bundle savedInstanceState in onCreate().
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("outNames", names);
        outState.putStringArrayList("outAnswerURLs", answerURLs);
        outState.putStringArrayList("outImageURLs", imageURLs);
        outState.putInt("outChosenID", chosenID);
        outState.putInt("outLocationOfCorrectAnswer", locationOfCorrectAnswer);
        outState.putIntegerArrayList("outAnswersIDList", answersIDList);
        outState.putString("outButtonText", buttonText);
        outState.putInt("outScore", score);
        outState.putInt("outTotalQuestions", totalQuestions);
        outState.putBoolean("outFirstAnswer", firstAnswer);
    }

}