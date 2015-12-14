package com.afollestad.bridgesample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseConvertCallback;
import com.afollestad.bridge.conversion.JsonResponseConverter;
import com.afollestad.bridgesample.conversion.Person;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private static String TEST_URL = "https://gist.githubusercontent.com/afollestad/d72de6a32804b0f6e1e6/raw/80bab66f19980feec7672f38ecfe09a500dda3bb/user.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
        list.setAdapter(new MainAdapter());

        Bridge.config()
                .responseConverter("text/plain", new JsonResponseConverter());
        Bridge.get(TEST_URL)
                .asClass(Person.class, new ResponseConvertCallback<Person>() {
                    @Override
                    public void onResponse(@NonNull Response response, @Nullable Person object, @Nullable BridgeException e) {
                        Log.d("Test", "Test");
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}