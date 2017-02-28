package us.melokalia.dev.alman;

import android.net.NetworkInfo;
import android.support.annotation.IntDef;

/**
 * Sample interface containing bare minimum methods needed for an asynchronous task
 * to update the UI Context.
 * Created by gpertea on 2/23/17.
 */
class ProgressData {
  public int code;
  public String line;
    public ProgressData(int c, String l) {
        code=c;
        line=l;
    }
    public ProgressData(int c) {
        code=c;
    }

}

public interface DownloadCallback {
    interface Progress {
        int ERROR = -1;
        int CONNECT_SUCCESS = 0;
        int GET_INPUT_STREAM_SUCCESS = 1;
        int PROCESS_INPUT_STREAM_IN_PROGRESS = 2;
        int PROCESS_INPUT_STREAM_SUCCESS = 3;
    }

     // Indicates that the callback handler needs to update its appearance or information based on
     // the result of the task. Expected to be called from the main thread.
    void updateFromDownload(String result);


    // Get the device's active network status in the form of a NetworkInfo object.
    NetworkInfo getActiveNetworkInfo();

    // Indicate to callback handler any progress update.
    // @param progressCode must be one of the constants defined in DownloadCallback.Progress.
    // @param percentComplete must be 0-100.
    //void onProgressUpdate(int progressCode, int percentComplete);
    void onProgressUpdate(ProgressData progressData);

    void onLineRead(String line);

    // Indicates that the download operation has finished. This method is called even if the
    // download hasn't completed successfully.
    void finishDownloading();
}
