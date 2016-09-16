package com.afollestad.bridgesample;

import android.app.Activity;
import android.content.Intent;
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
import com.afollestad.bridge.Pipe;
import com.afollestad.bridge.ProgressCallback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            InputStream is;
            try {
                is = getContentResolver().openInputStream(resultData.getData());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            Bridge.post("http://requestb.in/12linl71")
                    .throwIfNotSuccess()
                    .body(Pipe.forStream(is, "image/*"))
                    .uploadProgress(new ProgressCallback() {
                        @Override
                        public void progress(Request request, int current, int total, int percent) {
                            Log.d("Test", "Test");
                        }
                    })
                    .request(new Callback() {
                        @Override
                        public void response(@NonNull Request request, @Nullable Response response, @Nullable BridgeException e) {
                            if (e != null) e.printStackTrace();
                            Log.d("Test", "Test");
                        }
                    });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 8));
//        list.setAdapter(new MainAdapter());

        performFileSearch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bridge.destroy();
    }
}