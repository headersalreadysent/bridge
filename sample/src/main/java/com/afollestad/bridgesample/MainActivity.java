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
import com.afollestad.bridge.annotations.Body;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    public static class Person {

        public Person() {
        }

        @Body
        public String name;
        @Body
        public int age;

        @Override
        public String toString() {
            return String.format("Name: %s, age: %d", name, age);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
        list.setAdapter(new MainAdapter());

        Bridge.config().logging(true);
        Bridge.get("https://gist.githubusercontent.com/afollestad/357e503f86c678b60e88/raw/859aae7d14ee118972c21101184906fbb9310f27/one.json")
                .asClass(Person.class, new ResponseConvertCallback<Person>() {
                    @Override
                    public void onResponse(@NonNull Response response, @Nullable Person object, @Nullable BridgeException e) {
                        if (e != null)
                            Log.d("BridgeSample1", "Error: " + e.getMessage());
                        else if(object != null)
                            Log.d("BridgeSample1", object.toString());
                    }
                });

        Bridge.get("https://gist.githubusercontent.com/afollestad/16b511ee12441ad688b3/raw/565e089302f4481c9014f12838a4b3a6157e6d8d/two.json")
                .asClass(Person.class, new ResponseConvertCallback<Person>() {
                    @Override
                    public void onResponse(@NonNull Response response, @Nullable Person object, @Nullable BridgeException e) {
                        if (e != null)
                            Log.d("BridgeSample2", "Error: " + e.getMessage());
                        else if(object != null)
                            Log.d("BridgeSample2", object.toString());
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}