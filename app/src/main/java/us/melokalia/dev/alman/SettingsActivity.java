package us.melokalia.dev.alman;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
/**
 * Created by gpertea on 2/25/2017.
 */

public class SettingsActivity extends PreferenceActivity  {

    public static final String KEY_SRV_URL = "srv_url";
    public static final String KEY_BA_ENABLED = "ba_enabled";
    public static final String KEY_BA_USER = "ba_user";
    public static final String KEY_BA_PASS = "ba_pass";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new SettingsFragment()).commit();

        }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
        {
            @Override
            public void onCreate(final Bundle savedInstanceState)  {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.settings);
            }
            @Override
            public void onResume() {
                super.onResume();
                getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
                onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), "ba_enabled");
            }

            @Override
            public void onPause() {
                getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
                super.onPause();
            }

            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                if (key.equals("ba_enabled")) {
                    boolean isEnabled = sharedPreferences.getBoolean("ba_enabled", true);
                    Preference ba_user = findPreference("ba_user");
                    Preference ba_pass = findPreference("ba_pass");
                    if (ba_user!=null) ba_user.setEnabled(isEnabled);
                    if (ba_pass!=null) ba_pass.setEnabled(isEnabled);
                }
            }

        }

}
