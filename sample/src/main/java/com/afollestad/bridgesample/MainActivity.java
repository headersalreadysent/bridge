package com.afollestad.bridgesample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;

import org.json.JSONObject;

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

        Bridge.client().config()
                .validators(new ResponseValidator() {
                    @Override
                    public boolean validate(@NonNull Response response) throws Exception {
                        JSONObject json = response.asJsonObject();
                        return json.getBoolean("success");
                    }

                    @NonNull
                    @Override
                    public String id() {
                        return "custom-validator";
                    }
                });

        ResponseValidator validator = new ResponseValidator() {
            @Override
            public boolean validate(@NonNull Response response) throws Exception {
                JSONObject json = response.asJsonObject();
                return json.getBoolean("success");
            }

            @NonNull
            @Override
            public String id() {
                return "custom-validator";
            }
        };
        try {
            JSONObject response = Bridge.client()
                    .get("http://www.someurl.com/api/test")
                    .validators(validator)
                    .asJsonObject();
        } catch (BridgeException e) {
            if (e.reason() == BridgeException.REASON_RESPONSE_VALIDATOR) {
                // Validator threw an Exception OR returned false
            }
        }

        Bridge.client()
                .get("http://www.someurl.com/api/test")
                .validators(new ResponseValidator() {
                    @Override
                    public boolean validate(@NonNull Response response) throws Exception {
                        JSONObject json = response.asJsonObject();
                        return json.getBoolean("success");
                    }

                    @NonNull
                    @Override
                    public String id() {
                        return "custom-validator";
                    }
                })
                .request(new Callback() {
                    @Override
                    public void response(Request request, Response response, BridgeException e) {
                        // Use validated response
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.client().cancelAll();
    }
}
