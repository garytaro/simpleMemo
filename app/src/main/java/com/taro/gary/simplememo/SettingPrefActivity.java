package com.taro.gary.simplememo;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Shunya on 15/01/24.
 * 主にココを参照
 * http://blogand.stack3.net/archives/239
 */
public class SettingPrefActivity extends SettingSuperActivity {
    static public final String PREF_KEY_SENSOR_PARAM = "key_sensor_param";
    static public final String PREF_KEY_PRESET_PATTERN = "key_preset_pattern";
    static public final String PREF_KEY_FLG_CUSTOM = "key_flg_custom";
    //keyは1から始める
    static public final String[] PREF_KEY_CUSTOM = {"key_custom1","key_custom2","key_custom3","key_custom4","key_custom5","key_custom6"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // PrefFragmentの呼び出し
        getFragmentManager().beginTransaction().add(
                R.id.container, new SettingPrefActivity.PrefFragment()).commit();
    }

    // 設定画面のPrefFragmentクラス
    public static class PrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.setting_pref);

            // PreferenceScreenからのIntent
            PreferenceScreen prefSensorParam = (PreferenceScreen) findPreference(PREF_KEY_SENSOR_PARAM);
            prefSensorParam.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Activityの遷移
                    Intent nextActivity = new Intent(getActivity(), SetSensorParamActivity.class);
                    startActivity(nextActivity);
                    return true;
                }
            });

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
            ListPreference prefPresetPattern = (ListPreference) findPreference(PREF_KEY_PRESET_PATTERN);
            // entryは取得できないのでvalueで判定
            String predSentence = prefPresetPattern.getValue();
            if (predSentence.equals(getString(R.string.pred_custom_value))) {
                prefPresetPattern.setSummary(R.string.pred_custom);
            } else {
                prefPresetPattern.setSummary(predSentence);
            }

            for (int i = 0; i < PREF_KEY_CUSTOM.length; i++) {
                setCustomPredSummaryFraction(PREF_KEY_CUSTOM[i]);
            }
        }

        private void setCustomPredSummaryFraction(String key){
            EditTextPreference prefCustom = (EditTextPreference)findPreference(key);
            prefCustom.setSummary(prefCustom.getText());
        }

    }

}

