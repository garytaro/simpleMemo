package com.taro.gary.simplememo;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            loadPredFromJson();
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
                if (key.equals("key_preset_pattern")){
                    loadPredFromJson();
                }
                else{
                    setSummaryFraction();
                }
            }
        };

        // Fraction の Summary を設定
        private void setSummaryFraction() {
            ListPreference prefPresetPattern = (ListPreference) findPreference(PREF_KEY_PRESET_PATTERN);

            // valueからentryのidを取得、entryを呼び出し
            String predSentence = prefPresetPattern.getValue();
            int listId = prefPresetPattern.findIndexOfValue(predSentence);
            CharSequence[] entries = prefPresetPattern.getEntries();
            String entry = (String)entries[listId];
            prefPresetPattern.setSummary(entry);

            ArrayList<String> predList = new ArrayList<>();
            for (int i = 0; i < PREF_KEY_CUSTOM.length; i++) {
                setCustomPredSummaryFraction(PREF_KEY_CUSTOM[i]);
                String predText = ((EditTextPreference)findPreference(PREF_KEY_CUSTOM[i])).getText();
                predList.add(i, predText);
            }

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            // ArrayListをjsonに変換、シリアライズしてsharedpreferencesに書き込み
            Array2Pref.array2pref(pref, entry, predList);
        }

        // 選択したcaseに対応するvalueをjsonから読み出す
        private void loadPredFromJson(){
            ListPreference prefPresetPattern = (ListPreference) findPreference(PREF_KEY_PRESET_PATTERN);
            String predSentence = prefPresetPattern.getValue();
            int listId = prefPresetPattern.findIndexOfValue(predSentence);
            CharSequence[] entries = prefPresetPattern.getEntries();
            String entry = (String)entries[listId];

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            ArrayList<String> predList = Array2Pref.pref2array(pref, entry);
            if (predList==null){
                String predChars = pref.getString(PREF_KEY_PRESET_PATTERN, "empty,empty,empty,empty,empty,empty");
                String[] strAray = predChars.split(",");
                for(int i=0; i<PREF_KEY_CUSTOM.length; i++) {
                    ((EditTextPreference) findPreference(PREF_KEY_CUSTOM[i])).setText(strAray[i]);
                }
            }
            else if (predList.size()==PREF_KEY_CUSTOM.length) {
                for (int i = 0; i < PREF_KEY_CUSTOM.length; i++) {
                    ((EditTextPreference) findPreference(PREF_KEY_CUSTOM[i])).setText(predList.get(i));
                }
            }
            else{
                for(int i=0; i<PREF_KEY_CUSTOM.length; i++) {
                    ((EditTextPreference) findPreference(PREF_KEY_CUSTOM[i])).setText("empty");
                }
            }
            pref.edit().putString("key_preset_entry", entry).apply();
        }

        private void setCustomPredSummaryFraction(String key){
            EditTextPreference prefCustom = (EditTextPreference)findPreference(key);
            prefCustom.setSummary(prefCustom.getText());
        }

    }

}

