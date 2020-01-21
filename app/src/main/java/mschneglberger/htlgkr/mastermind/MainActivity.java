package mschneglberger.htlgkr.mastermind;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    int[] alphabet;
    int codeLength;
    boolean doubleAllowed;
    int guessRound;
    char correctPositionSign;
    char correctCodeElementSign;
    String currentCode = "";
    List<String> currentGuesses;

    ArrayAdapter<String> adapter;
    ListView monitor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readAssets();
        monitor = findViewById(R.id.listView_monitor);

        Button showSettings = findViewById(R.id.button_ShowSettings);
        showSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> listForAdapter = new ArrayList<>();
                listForAdapter.add("alphabet");
                listForAdapter.add(Arrays.toString(alphabet));
                listForAdapter.add("codeLength");
                listForAdapter.add(String.valueOf(codeLength));
                listForAdapter.add("doubleAllowed");
                listForAdapter.add(String.valueOf(doubleAllowed));
                listForAdapter.add("guessRound");
                listForAdapter.add(String.valueOf(guessRound));
                listForAdapter.add("correctPositionSign");
                listForAdapter.add(String.valueOf(correctPositionSign));
                listForAdapter.add("correctCodeElementSign");
                listForAdapter.add(String.valueOf(correctCodeElementSign));
                listForAdapter.add("START NEW GAME");

                adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, listForAdapter);

                monitor.setAdapter(adapter);
            }
        });

        Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveClicked();
            }
        });

        Button loadButton = findViewById(R.id.button_load);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadClicked();
            }
        });

        Button submit = findViewById(R.id.button_submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitClicked(view);
            }
        });



        monitor.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                if(adapterView.getAdapter().getItem(pos).toString().equals("START NEW GAME")){
                    startNewGame();
                }
            }
        });

        startNewGame();

    }


    public void saveClicked(){
        String strToFile = "<saveState>\n";
        strToFile += "\t<code>" + currentCode + "</code>\n";

        for(int i = 0; i < adapter.getCount(); i++){
            strToFile += "\t<guess" + i + ">\n";

            String[] parts1 = adapter.getItem(i).split(" \\| ");
            strToFile += "\t\t<userInput>" + TextUtils.join(", ", parts1[0].split("")).substring(2) + "</userInput>\n";
            strToFile += "\t\t<result>" + parts1[1] + "</result>\n";
            strToFile += "\t</guess" + i + ">\n";
        }
        strToFile += "</saveState>";




        String filePath = getApplicationContext().getFilesDir().getPath().toString() + "/saveState.txt";
//        File outputF = new File(filePath);
        Log.d(TAG, "saveClicked: file Path: " + filePath);

        File outputF = new File(filePath);

        try {
            outputF.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputF))) {
            writer.write(strToFile);
            writer.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadClicked(){
        String filePath = getApplicationContext().getFilesDir().getPath().toString() + "/saveState.txt";
//        File outputF = new File(filePath);
        Log.d(TAG, "saveClicked: file Path: " + filePath);

        File outputF = new File(filePath);

        try {
            outputF.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(BufferedReader reader = new BufferedReader(new FileReader(outputF))) {

            String fullFile = "";

            String line = reader.readLine();
            while(line != null){
                fullFile += line;
                line = reader.readLine();
            }
            Log.d(TAG, "loadClicked: Loaded new specs");


            currentCode = fullFile.split("<code>")[1].split("</code>")[0];
            Log.d(TAG, "loadClicked: new code: " + currentCode);

            fullFile = fullFile.replaceAll("\t", "");
            fullFile = fullFile.replaceAll("\n", "");

            int counter = 0;
            while(true){

                if(fullFile.contains("<guess" + counter + ">")){
                    String[] tempParts = fullFile.split("<guess" + counter + ">")[1].split("</guess" + counter + ">")[0].split("</userInput><result>");

                    String userInput = tempParts[0].substring(11).replaceAll(", ", "");
                    String result = tempParts[1].substring(0,tempParts[1].length()-9);

                    Log.d(TAG, "loadClicked: UserInput#" + counter + ": " + userInput);
                    Log.d(TAG, "loadClicked: Result#" + counter + ": " + result);

                    String fullLine = userInput + " | " + result;

                    adapter.add(fullLine);
                    adapter.notifyDataSetChanged();
                }
                else{
                    break;
                }
                counter++;
            }




        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }





    public void submitClicked(View view){
        EditText guessField = findViewById(R.id.editText_nextGuess);
        String guess = guessField.getText().toString();

        String returnValue = "";
        if(currentCode.equals(guess)){
            returnValue = "SOLVED";
        }
        else if(!inputValid(guess)){
            returnValue = "Invalid Input!";
        }
        else{
            //+ richtige zoi, richtige stö
            //- richtige zoi, foische stö

            String[] partsCode = currentCode.split("");
            String[] partsGuess = guess.split("");

            for(int i = 1; i < partsCode.length; i++){
                if(partsCode[i].equals(partsGuess[i])){
                    Log.d(TAG, "submitClicked: currentCodePart:" + partsCode[i] + "|currentPartsGuess:" + partsGuess[i]);
                    returnValue += correctPositionSign;
                }
                else{
                    if(Arrays.asList(partsGuess).contains(partsCode[i])){
                        returnValue += correctCodeElementSign;
                    }
                }
            }

            returnValue = returnValue.replace("-","") + "" + returnValue.replace("+","");



        }


        Toast.makeText(getApplicationContext(),"Rueckgabe: " + returnValue, Toast.LENGTH_LONG).show();



        String listItemString = guess + " | " + returnValue;
        adapter.add(listItemString);
        adapter.notifyDataSetChanged();
    }

    public boolean inputValid(String input){
        if(!(input.length() == codeLength)){
            return false;
        }
        try{
            int i = Integer.valueOf(input);
        }
        catch(ClassCastException | NumberFormatException e){
            return false;
        }
        return true;
    }


    public void startNewGame(){
        Toast.makeText(getApplicationContext(),"NEW GAME STARTED", Toast.LENGTH_LONG).show();

        try{
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
        catch(NullPointerException e){
            adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, new ArrayList<String>());
            monitor.setAdapter(adapter);
        }

        currentGuesses = new ArrayList<>();

        String code = "";
        for(int i = 0; i < codeLength; i++){
            code += getRandom(alphabet);
        }

        Log.d(TAG, "startNewGame: CODE: " + code);

        currentCode = code;


    }

    private static String getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return String.valueOf(array[rnd]);
    }

    private void readAssets() {
        // better store assetfilename as String constant!
        InputStream in = getInputStreamForAsset("config.conf");
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = bin.readLine()) != null) {
                line = line.replaceAll(" ", "");
                Log.d(TAG, "line: " + line);
                String[] parts = line.split("=");
                switch (parts[0]){
                    case "alphabet":
                        String[] newArr = parts[1].split(",");
                        this.alphabet = new int[newArr.length];
                        for(int i = 0; i < newArr.length; i++){
                            this.alphabet[i] = Integer.valueOf(newArr[i]);
                        }
                        break;
                    case "codeLength":
                        codeLength = Integer.valueOf(parts[1]);
                        break;
                    case "doubleAllowed":
                        doubleAllowed = Boolean.valueOf(parts[1]);
                        break;
                    case "guessRound":
                        guessRound = Integer.valueOf(parts[1]);
                        break;
                    case "correctPositionSign":
                        correctPositionSign = parts[1].charAt(0);
                        break;
                    case "correctCodeElementSign":
                        correctCodeElementSign = parts[1].charAt(0);
                        break;
                }

            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private InputStream getInputStreamForAsset(String filename) {
        // tries to open Stream on Assets. If fails, returns null
        Log.d(TAG, "getInputStreamForAsset: " + filename);
        AssetManager assets = getAssets();
        try {
            return assets.open(filename);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }

}
