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
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseConvertCallback;
import com.afollestad.bridge.conversion.JsonResponseConverter;
import com.afollestad.bridgesample.conversion.SimplePerson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

//    @SuppressWarnings("FieldCanBeLocal")
//    private static String TEST_URL = "https://gist.githubusercontent.com/afollestad/b2ff13a08239b74d25c5/raw/725403b7cad2d42662006ec727c050cbbfe400e6/users.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
        list.setAdapter(new MainAdapter());

        Bridge.config()
                .responseConverter("text/plain", new JsonResponseConverter());

//        SimplePerson[] people = new SimplePerson[]{
//                new SimplePerson("Jeffrey Follestad", 42),
//                new SimplePerson("Natalie Micheal", 41)
//        };

//        List<SimplePerson> friends = new ArrayList<>();
//        SimplePerson tony = new SimplePerson("Anthony Cole", 18);
//        friends.add(tony);
//        SimplePerson waverly = new SimplePerson("Waverly Moua", 18);
//        waverly.otherFriend = tony;
//        friends.add(waverly);

        Bridge.get("https://gist.githubusercontent.com/afollestad/c667d71f413586d6546b/raw/7c7b3a340f93056c9c7e6160118d1753f3dd0a1b/test.json")
                .asClass(SimplePerson.class, new ResponseConvertCallback<SimplePerson>() {
                    @Override
                    public void onResponse(@NonNull Response response, @Nullable SimplePerson object, @Nullable BridgeException e) {
                        Log.d("TEST", "Test");
                    }
                });

//        Bridge.get(TEST_URL)
//                .asClassArray(SimplePerson.class, new ResponseConvertCallback<SimplePerson[]>() {
//                    @Override
//                    public void onResponse(@NonNull Response response, @Nullable SimplePerson[] objects, @Nullable BridgeException e) {
//                        Log.d("Test", "Test");
//                    }
//                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}