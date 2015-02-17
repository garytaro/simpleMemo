package com.taro.gary.simplememo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

/**
 * Created by Shunya on 15/01/26.
 */
public class SetSensorParamActivity extends SettingSuperActivity {
    static public final String PREF_KEY_MV_NUM_SAMPLE = "key_mv_num_sample";
    static public final String PREF_KEY_SENSOR_FREQ   = "key_sensor_freq";
    static public final String PREF_KEY_SENSOR_DELAY  = "key_sensor_delay";
    static public final String PREF_KEY_LR_MAX_DEGREE = "key_lr_max_degree";
    static public final String PREF_KEY_LR_MIN_DEGREE = "key_lr_min_degree";
    static public final String PREF_KEY_UD_MAX_DEGREE = "key_ud_max_degree";
    static public final String PREF_KEY_UD_MIN_DEGREE = "key_ud_min_degree";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // PrefFragmentの呼び出し
        getFragmentManager().beginTransaction().add(
                R.id.container, new PrefFragment()).commit();
    }

    // 設定画面のPrefFragmentクラス
    public static class PrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.setting_sensor_param);

            // Summary を設定
            setSummaryFraction();
        }

        // 設定値が変更されたときのリスナーを登録
        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(listener);
        }

        // 設定値が変更されたときのリスナー登録を解除
        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.unregisterOnSharedPreferenceChangeListener(listener);
        }

        // 設定変更時に、Summaryを更新
        private SharedPreferences.OnSharedPreferenceChangeListener listener
                = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                setSummaryFraction();
            }
        };

        // Fraction の Summary を設定
        private void setSummaryFraction() {
            EditTextPreference prefNumSample = (EditTextPreference)findPreference(PREF_KEY_MV_NUM_SAMPLE);
            prefNumSample.setSummary(prefNumSample.getText());
            ListPreference prefPresetPattern = (ListPreference)findPreference(PREF_KEY_SENSOR_FREQ);
            prefPresetPattern.setSummary(prefPresetPattern.getEntry());
            EditTextPreference prefDelay = (EditTextPreference)findPreference(PREF_KEY_SENSOR_DELAY);
            prefDelay.setSummary(prefDelay.getText());            
            EditTextPreference prefLrMax = (EditTextPreference)findPreference(PREF_KEY_LR_MAX_DEGREE);
            prefLrMax.setSummary(prefLrMax.getText());
            EditTextPreference prefLrMin = (EditTextPreference)findPreference(PREF_KEY_LR_MIN_DEGREE);
            prefLrMin.setSummary(prefLrMin.getText());
            EditTextPreference prefUdMax = (EditTextPreference)findPreference(PREF_KEY_UD_MAX_DEGREE);
            prefUdMax.setSummary(prefUdMax.getText());
            EditTextPreference prefUdMin = (EditTextPreference)findPreference(PREF_KEY_UD_MIN_DEGREE);
            prefUdMin.setSummary(prefUdMin.getText());
        }

        private int checkNumSample(){
            EditTextPreference prefNumSample = (EditTextPreference)findPreference(PREF_KEY_MV_NUM_SAMPLE);
            int numSample = Integer.parseInt(prefNumSample.getText());
            if (numSample < 2 || numSample > 1000){
                Toast.makeText(getActivity(),"Input 1 ~ 1000", Toast.LENGTH_LONG).show();
                numSample = 50;
            }
            return(numSample);
        }
    }

}
