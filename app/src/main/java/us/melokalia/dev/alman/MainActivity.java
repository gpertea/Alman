package us.melokalia.dev.alman;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Sample Activity demonstrating how to connect to the HTTPS and fetch data
 * HTML. It uses a Fragment that encapsulates the network operations on an AsyncTask.
 */


public class MainActivity extends AppCompatActivity implements DownloadCallback {
        //implements DownloadCallback {
    // Reference to the TextView showing fetched data, so we can clear it with a button
    // as necessary.
    private TextView mDataText;
    private ArrayList<String> loglines;
    private String mUrl;
    private String mBasicAuth;
    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetFragment mNetworkFragment;

    // Boolean telling us whether a download is in progress, so we don't trigger overlapping
    // downloads with consecutive button clicks.
    private boolean mDownloading = false;

    protected void updateSettings() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mUrl = settings.getString(SettingsActivity.KEY_SRV_URL, "");
        String bauth="";
        if (settings.getBoolean(SettingsActivity.KEY_BA_ENABLED, false)) {
            String ba_user = settings.getString(SettingsActivity.KEY_BA_USER, "");
            String ba_pass = settings.getString(SettingsActivity.KEY_BA_PASS, "");
            bauth = ba_user.concat(":").concat(ba_pass);
            mBasicAuth = "Basic " + Base64.encodeToString(bauth.getBytes(), Base64.NO_WRAP);
        }
        else {
            mBasicAuth = "";
        }
    }

    protected void checkSettings() {
        String prev_url=mUrl;
        String prev_auth=mBasicAuth;
        updateSettings();
        if (prev_url!=mUrl || prev_auth!=mBasicAuth) {
            mNetworkFragment.updateArgs(mUrl, mBasicAuth);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataText = (TextView) findViewById(R.id.data_text);
        mDataText.setMovementMethod(new ScrollingMovementMethod());
        loglines=new ArrayList<String>();
        mNetworkFragment = NetFragment.getInstance(getSupportFragmentManager(),
                mUrl, mBasicAuth);
    }

    @Override
    //onResume() is execute after onCreate() and onStart() as the application comes to the foreground
    public void onResume() {
        super.onResume();
        checkSettings();
        startDownload("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Clear the text and cancel download.
            /*case R.id.clear_action:
                finishDownloading();
                mDataText.setText("");
                return true;*/
            case R.id.disarm_action:
                startDownload("cmd=disarm");
                return true;
            case R.id.arm_action:
                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to arm the system?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //finally do what was asked
                                startDownload("cmd=arm");
                            }

                        })
                        .setNegativeButton(" No ", null)
                        .show();
                return true;
            case R.id.settings_action:
                //Toast.makeText(this, "Showing Settings", Toast.LENGTH_SHORT).show();
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                return true;

        }
        return false;
    }
    protected void fetchLog(View v) {
        mDataText.setText("");
        startDownload("");
    }

    protected void startDownload(String params) {
        //checkSettings();
        if (mUrl.isEmpty()) {
            mDataText.setText("Error: No server URL provided!\nCheck Settings.");
            return;
        }
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            loglines.clear();
            //Toast.makeText(this, mUrl, Toast.LENGTH_LONG).show();
            mNetworkFragment.startDownload(params);
            mDownloading = true;
        }
    }

    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            mDataText.setText(result);
        } else {
            mDataText.setText(getString(R.string.connection_error));
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
    }

    @Override
    public void onProgressUpdate(ProgressData pd) {
        switch(pd.code) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                mDataText.setText("Progress: ERROR!");
                break;
            case Progress.CONNECT_SUCCESS:
                mDataText.setText("Progress: CONNECT_SUCCESS!");
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                mDataText.setText("Progress: GET_INPUT_STREAM_SUCCESS!");
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                //mDataText.setText("" + percentComplete + "%");
                onLineRead(pd.line);
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }

    @Override
    public void onLineRead(String line) {
        loglines.add(line);
        StringBuilder lines=new StringBuilder();
        for (String l : loglines) {
            lines.append(l);
        }
        mDataText.setText(lines.toString());
    }
}
