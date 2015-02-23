package com.taro.gary.simplememo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shunya on 15/02/22.
 * 配列とpreferenceのやりとりを行うメソッド集
 */
public class Array2Pref {
    public static boolean array2pref(SharedPreferences pref, String key, Object mydata) {
        try {
            Gson gson = new Gson();
            pref.edit().putString(key, gson.toJson(mydata)).apply();
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<String> pref2array(SharedPreferences pref, String key) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(pref.getString(key,"default"),
                    new TypeToken<List<String>>(){}.getType());
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
