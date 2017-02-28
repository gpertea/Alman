package us.melokalia.dev.alman;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by gpertea on 2/23/17.
 */

public class NetFragment extends Fragment {
    public static final String TAG = "NetworkFragment";

    private static final String URL_KEY = "UrlKey";
    private static final String URL_AUTH= "UrlAuth";

    private DownloadCallback mCallback;
    private DownloadTask mDownloadTask;
    private String mUrlString;
    private String mBasicAuth;

    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    /**
     * Trust every server - don't check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } }; //new X509TrustManager

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetFragment getInstance(FragmentManager fragmentManager, String url, String basicAuth) {
        // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
        // This is necessary because NetworkFragment might have a task that began running before
        // the config change and has not finished yet.
        // The NetworkFragment is recoverable via this method because it calls
        // setRetainInstance(true) upon creation.
        NetFragment networkFragment = (NetFragment) fragmentManager
                .findFragmentByTag(NetFragment.TAG);
        if (networkFragment == null) {
            networkFragment = new NetFragment();
            Bundle args = new Bundle();
            args.putString(URL_KEY, url);
            args.putString(URL_AUTH, basicAuth);
            networkFragment.setArguments(args);
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }
        return networkFragment;
    }

    public void updateArgs(String url, String basicAuth) {
        getArguments().putString(URL_KEY, url);
        getArguments().putString(URL_AUTH, basicAuth);
        mUrlString = url;
        mBasicAuth = basicAuth;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);
        //initialize always-trusting https connection
        trustAllHosts(); //install my lenient TrustManager

        mUrlString = getArguments().getString(URL_KEY);
        mBasicAuth = getArguments().getString(URL_AUTH);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        mCallback = (DownloadCallback)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity.
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask.
     */
    public void startDownload(String param) {
        cancelDownload();
        mDownloadTask = new DownloadTask();
        String urltxt=mUrlString;
        if (!param.isEmpty())
            urltxt+="?"+param;
        mDownloadTask.execute(urltxt);
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    public void cancelDownload() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
    }

    /**
     * Implementation of AsyncTask that runs a network operation on a background thread.
     */
    private class DownloadTask extends AsyncTask<String, ProgressData, DownloadTask.Result> {

        /**
         * Wrapper class that serves as a union of a result value and an exception. When the
         * download task has completed, either the result value or exception can be a non-null
         * value. This allows you to pass exceptions to the UI thread that were thrown during
         * doInBackground().
         */
        class Result {
            public String mResultValue;
            public Exception mException;
            public Result(String resultValue) {
                mResultValue = resultValue;
            }
            public Result(Exception exception) {
                mException = exception;
            }
        }

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected Result doInBackground(String... urls) {
            Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    int numlines=showLog(url);
                    /* String resultString = downloadUrl(url);
                    if (resultString != null) {
                        result = new Result(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                    */
                } catch(Exception e) {
                    result = new Result(e);
                }
            }
            return result;
        }

        /**
         * Send DownloadCallback a progress update.
         */
        @Override
        protected void onProgressUpdate(ProgressData... values) {
            super.onProgressUpdate(values);
            if (values.length >= 1) {
                mCallback.onProgressUpdate(values[0]);
            }
        }

        /**
         * Updates the DownloadCallback with the result.
         */

        @Override
        protected void onPostExecute(Result result) {
            /*if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.updateFromDownload(result.mException.getMessage());
                } else if (result.mResultValue != null) {
                    mCallback.updateFromDownload(result.mResultValue);
                }
                mCallback.finishDownloading();
            }*/
            //we don't use result at all
            if (mCallback != null)  mCallback.finishDownloading();
        }
        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(Result result) {
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * If the network request is successful, it returns the response body in String form. Otherwise,
         * it will throw an IOException.
         */
        private int showLog(URL url) throws IOException {
            InputStream stream = null;
            HttpsURLConnection connection = null;
            int result = 0; //number of lines read in loglines()
            try {
                connection = (HttpsURLConnection) url.openConnection();
                //make sure we don't choke on SSL certificates etc.
                connection.setHostnameVerifier(DO_NOT_VERIFY);
                // Timeout for reading InputStream arbitrarily set to 4s.
                connection.setReadTimeout(4000);
                // Timeout for connection.connect() arbitrarily set to 4s.
                connection.setConnectTimeout(4000);
                if (!mBasicAuth.isEmpty()) {
                    connection.setRequestProperty("Authorization", mBasicAuth);
                }
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
                publishProgress(new ProgressData(DownloadCallback.Progress.CONNECT_SUCCESS));
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                // Retrieve the response body as an InputStream.
                stream = connection.getInputStream();
                publishProgress(new ProgressData(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS));
                if (stream != null) {
                    // Converts Stream to String with max length of 1200.
                    result = readStream(stream);
                    publishProgress(new ProgressData(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS));
                }
            } finally {
                // Close Stream and disconnect HTTPS connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }

        /**
         * puts incoming lines into loglines ArrayList<String>
         *     returns number of lines read
         */
        private int readStream(InputStream stream) throws IOException {
            //String result = null;
            // Read InputStream using the UTF-8 charset.
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
            // Create temporary buffer to hold Stream data with specified max length.
            //char[] buffer = new char[maxLength];
            // Populate temporary buffer with Stream data.
            //int numChars = 0;
            int readCh = 0;
            /*
            while (numChars < maxLength && readSize != -1) {
                numChars += readSize;
                int pct = (100 * numChars) / maxLength;
                publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct);
                readSize = reader.read(buffer, numChars, buffer.length - numChars);
            }
            */
            StringBuilder line=new StringBuilder();
            int lineCount=0;
            while ((readCh=reader.read())>=0) {
                line.append((char)readCh);
                //numChars++;
                if (readCh=='\n') {
                    //end of line encountered
                    publishProgress(new ProgressData(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS,
                            line.toString()));
                    lineCount++;
                    line.setLength(0);

                }
            }
            if (line.length()>0) {
                //probably no newline character at the end
                publishProgress(new ProgressData(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS,
                        line.toString()));
            }
            /*
            if (numChars != -1) {
                // The stream was not empty.
                // Create String that is actual length of response body if actual length was less than
                // max length.
                numChars = Math.min(numChars, maxLength);
                result = new String(buffer, 0, numChars);
            }*/
            return lineCount;
        }
    }

}
