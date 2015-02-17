package com.taro.gary.simplememo;

//import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Shunya on 15/01/02.
 */
public class EditActivity extends NavigationActivity {

    public final static String TAG = "editor";
    private static final String TAG_FRAGMENT = "FixPredFragment";
    String mFileName = "";

    private boolean mNotSave = false;
//    final float alpha = 0.5f;  //平均時の今回センサ値の重み


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //画面を点灯したままにする
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // タイトルと内容入力用の EditText を取得
        EditText eTxtTitle = (EditText)findViewById(R.id.eTxtTitle);
        EditText eTxtContent = (EditText)findViewById(R.id.eTxtContent);

        // メイン画面からの情報受け取り、EditTextに設定
        // （情報がない場合（新規作成の場合）は、設定しない）
        Intent intent = getIntent();
        String name = intent.getStringExtra("NAME");
        if (name != null) {
            mFileName = name;
            eTxtTitle.setText(intent.getStringExtra("TITLE"));
            eTxtContent.setText(intent.getStringExtra("CONTENT"));
        }

        getFragmentManager().beginTransaction()
                .add(new FixPredFragment(), TAG_FRAGMENT).commit();
    }

    @Override
    public void onSectionAttached(int number){
        super.onSectionAttached(number);
        // navigatino drawerで変更したsettingを取得
        FragmentManager fragmentManager = getFragmentManager();
        FixPredFragment fixPredFragment = (FixPredFragment) fragmentManager.findFragmentByTag(TAG_FRAGMENT);
        fixPredFragment.getPrefSetting();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // [削除] で画面を閉じるときは、保存しない
        if(mNotSave){
             return;
        }
        this.saveContent();
    }

    // メニュー生成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    // メニュー選択時の処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_del:
                // [削除] 選択処理
                if (!mFileName.isEmpty()) {
                    if (this.deleteFile(mFileName)) {
                        Toast.makeText(this, R.string.msg_del, Toast.LENGTH_SHORT).show();
                    }
                }
                // 保存せずに、画面を閉じる
                mNotSave = true;
                this.finish();
                break;
//            case R.id.action_save:
//                this.saveContent();
//                Toast.makeText(this, R.string.msg_save, Toast.LENGTH_LONG).show();
//                break;
            case R.id.action_sensorStart:
                FragmentManager fragmentManager = getFragmentManager();
                FixPredFragment fixPredFragment = (FixPredFragment) fragmentManager.findFragmentByTag(TAG_FRAGMENT);
                fixPredFragment.initSensor();

                // 証拠隠滅
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setHomeButtonEnabled(false);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //内容を保存
    public void saveContent(){
        // タイトル、内容を取得
        EditText eTxtTitle = (EditText)findViewById(R.id.eTxtTitle);
        EditText eTxtContent = (EditText)findViewById(R.id.eTxtContent);
        String title = eTxtTitle.getText().toString();
        String content = eTxtContent.getText().toString();

        // タイトル、内容が空白の場合、保存しない
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, R.string.msg_destruction, Toast.LENGTH_SHORT).show();
            return;
        }

        // ファイル名を生成  ファイル名 ： yyyyMMdd_HHmmssSSS.txt
        // （既に保存されているファイルは、そのままのファイル名とする）
        // TODO:タイトルとファイル名の一致
        if (mFileName.isEmpty()) {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.JAPAN);
            mFileName = sdf.format(date) + ".txt";
        }

        // 保存
        OutputStream out = null;
        PrintWriter writer = null;
        try{
            out = this.openFileOutput(mFileName, Context.MODE_PRIVATE);
            writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
            // タイトル書き込み
            writer.println(title);
            // 内容書き込み
            writer.print(content);
            writer.close();
            out.close();
        }catch(Exception e) {
            Toast.makeText(this, "File save error!", Toast.LENGTH_LONG).show();
            Log.v(TAG, "File save error!");
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        if (e.getKeyCode() != KeyEvent.KEYCODE_VOLUME_UP && e.getKeyCode() != KeyEvent.KEYCODE_VOLUME_DOWN){
            return super.dispatchKeyEvent(e);
        }

        try {
            FragmentManager fragmentManager = getFragmentManager();
            FixPredFragment fixPredFragment = (FixPredFragment) fragmentManager.findFragmentByTag(TAG_FRAGMENT);
            return fixPredFragment.dispatchKeyEvent(e);
        }
        catch( Exception exception ) {
            return super.dispatchKeyEvent(e);
        }
    }
}
