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
import com.afollestad.bridgesample.conversion.Example;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private static String TEST_URL = "https://gist.githubusercontent.com/afollestad/97a52c5af3091a9d7c66/raw/263570b66b7c222d532a43204ed3c378f488cb33/Test1.json";
//    private static String TEST_URL = "https://gist.githubusercontent.com/afollestad/a21a8456423aec64a3b4/raw/ed5aa53e7153bb1136ed1c489a58923f58be963b/test4.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
        list.setAdapter(new MainAdapter());

        Bridge.get(TEST_URL)
                .asClass(Example.class, new ResponseConvertCallback<Example>() {
                    @Override
                    public void onResponse(@NonNull Response response, @Nullable Example object, @Nullable BridgeException e) {
                        Log.d("Test", "Test");

                        Bridge.post("http://requestb.in/ssdw1xss")
                                .body(object)
                                .request(new Callback() {
                                    @Override
                                    public void response(Request request, Response response, BridgeException e) {
                                        Log.d("Test", "Test");
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}