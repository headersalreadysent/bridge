package com.afollestad.bridgesample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseConvertCallback;
import com.afollestad.bridgesample.conversion.Person;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
        list.setAdapter(new MainAdapter());

Bridge.get("https://www.someurl.com/test.js")
    .asClass(Person.class, new ResponseConvertCallback<Person>() {
        @Override
        public void onResponse(@NonNull Response response, @Nullable Person object, @Nullable BridgeException e) {
            // Use response object
        }
    });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}