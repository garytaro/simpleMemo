package com.taro.gary.simplememo;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Shunya on 15/02/03.
 */

public class FixPredFragment extends Fragment
        implements SensorEventListener {

    public final static String TAG = "FixPredFragment";
    protected final static double RAD2DEG = 180/Math.PI;
    String mFileName = "";

    private boolean mIsMagSensor;
    private boolean mIsAccSensor;
    private boolean mNotSave = false;
    private boolean mMvStart = false;
    private boolean mIsPressedVolume = false;
    private String mPreFix = "";
    private String mSufFix = "";

    private double mLrMaxDegree;
    private double mLrMinDegree;
    private double mUdMaxDegree;
    private double  mUdMinDegree;
    private long mSensorDelayMs = 5000;
    private String mSensorInterval = "UI";
    private boolean mDebFlg = false;
    private String[] mPredictionSentence = {"up", "right", "down", "left", "vup", "vdown"};

    private int mNumSample = 1000;
    private int mNumSampleAzi = 2000;
    private float[] mInRotationMatrix = new float[9];
    private float[] mOutRotationMatrix = new float[9];
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] mAttitude = new float[3];
    private float[] mAziPos = new float[3];
    private float[][] mPreviousAttitude = new float[mNumSample][3];
    private float[][] mPreviousAziPos = new float[mNumSample][2];
    private float[] mSumAttitude = new float[3];
    private float[] mSumAzi = new float[3];
    private int mMvCounter = 0;  //移動平均のカウンタ
    private int mMvCounterAzi = 0;  //移動平均のカウンタ
    private long mStartTime;
    private int mSlopeFlg = 0;  //傾き方向を決めるフラグ.北から時計周りに1~4, 音量上5, 下6
    private SensorManager mSensorManager;
    private float[] mAziIniPos = new float[2];
    private EditText mEtxtContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //設定の取得
        getPrefSetting();
        getSensorParam();
    }


    @Override
    public void onPause() {
        super.onPause();
        this.unregisterSensor();
        Log.v(TAG, "Puased");
    }


    public void initSensor(){
        mSensorManager = (SensorManager)getActivity().getSystemService(EditActivity.SENSOR_SERVICE);
        mEtxtContent = (EditText)getActivity().findViewById(R.id.eTxtContent);
        this.initSensorParam();
    }


    //センサのリスナ登録解除
    protected void unregisterSensor(){
        if (mIsMagSensor || mIsAccSensor) {
            mSensorManager.unregisterListener(this);
            mIsMagSensor = false;
            mIsAccSensor = false;
            Log.v(TAG, "sensor unregistered");
        }
//        mSensorManager.unregisterListener(this);
    }


    // センサの取得
    public void getSensor(){
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(this, sensor, getSensorFreq());
                mIsMagSensor = true;
            }

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(this, sensor, getSensorFreq());
                mIsAccSensor = true;
            }
        }
//        単体のセンサを取得する場合はこちらでおｋ
//        mSensorManager.registerListener(
//                this,
//                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(
//                this,
//                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
//                SensorManager.SENSOR_DELAY_GAME);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public boolean dispatchKeyEvent(KeyEvent e){
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.getActivity().dispatchKeyEvent(e);

        if (e.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP){
            if (mIsPressedVolume) {
                mSlopeFlg -= 1;
            }
            else{
                mSlopeFlg = 4;
                mIsPressedVolume = true;
            }
        }
        else if (e.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
            if (mIsPressedVolume) {
                mSlopeFlg += 1;
            }
            else{
                mSlopeFlg = 5;
                mIsPressedVolume = true;
            }
        }
        else {
            return super.getActivity().dispatchKeyEvent(e);
        }

        if (mSlopeFlg >= mPredictionSentence.length) {
            mSlopeFlg = 0;
        }
        else if (mSlopeFlg < 0) {
            mSlopeFlg = mPredictionSentence.length - 1;
        }

        this.unregisterSensor();
        EditText eTxtContent = (EditText)getActivity().findViewById(R.id.eTxtContent);
        eTxtContent.setText(mPreFix + mPredictionSentence[mSlopeFlg] + mSufFix);
        eTxtContent.setSelection(eTxtContent.getText().length());
        // trueを返さないと通常の音量ボタンが呼ばれる
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //傾き判定済ならreturnする
        //リスナの登録eTxtContent.setText解除後も何故か一度だけループが回るため
        if(mSlopeFlg!=0) return;
        if(new Date().getTime() < mStartTime + mSensorDelayMs) return;

        switch(event.sensor.getType()){
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mGravity = event.values.clone();
                break;
        }

        if(mGeomagnetic == null || mGravity == null) return;
        SensorManager.getRotationMatrix(mInRotationMatrix, null,
                mGravity, mGeomagnetic);

        //Debug時は基準軸を反転しない
        if (mDebFlg) {
            SensorManager.remapCoordinateSystem(mInRotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Y, mOutRotationMatrix);
        }
        else {
            // 基準軸を反転
            SensorManager.remapCoordinateSystem(mInRotationMatrix,
                    SensorManager.AXIS_MINUS_X, SensorManager.AXIS_Y, mOutRotationMatrix);
        }

        SensorManager.getOrientation(mOutRotationMatrix, mAttitude);

//            センサー値に対しローパスフィルタ
//            前回値に対し重み付け平均をとり、今回値とする→本来良好な手法だが、感度が高すぎて無理
//        for(int i=0; i <=2; i++){
//            mAttitude[i] = (1-alpha)*mPreviousAttitude[i] + alpha*mAttitude[i];
//            mPreviousAttitude[i] = mAttitude[i];
//        }

        // mNumSampleの窓幅で移動平均を取る
        // mNumSample*サンプリング間隔/2だけの群遅延が発生する
        for(int i=1; i<=2; i++){  //azimuthは別計算
            mSumAttitude[i] = mSumAttitude[i] + mAttitude[i] - mPreviousAttitude[mMvCounter][i];
            mPreviousAttitude[mMvCounter][i] = mAttitude[i];
            mAttitude[i] = mSumAttitude[i] / mNumSample;
        }
        if(mMvCounter == mNumSample -1) mMvCounter = 0;
        else mMvCounter++;

        // 移動平均時に+180と-180で振動するため、単位円上の座標で取得
        // TODO:回転行列を直接利用できないか
        mAziPos[0] = (float)Math.cos(mAttitude[0]);
        mAziPos[1] = (float)Math.sin(mAttitude[0]);
        // azimuthは振動が大きいため、別のサンプル数で実施する
        for(int i=0; i<=1; i++){
            mSumAzi[i] = mSumAzi[i] + mAziPos[i] - mPreviousAziPos[mMvCounterAzi][i];
            mPreviousAziPos[mMvCounterAzi][i] = mAziPos[i];
            mAziPos[i] = mSumAzi[i] / mNumSampleAzi;
        }
        if(mMvCounterAzi == mNumSampleAzi -1) mMvCounterAzi = 0;
        else mMvCounterAzi++;

        if(!mMvStart){
            if(mMvCounterAzi == mNumSampleAzi -1){
                mMvStart = true;
                mAziIniPos[0] = mAziPos[0];
                mAziIniPos[1] = mAziPos[1];
                Log.v(TAG, "moving average started!");
            }
            else return;
        }

        double pitch = mAttitude[1]*RAD2DEG;
        double roll = mAttitude[2]*RAD2DEG;

        if (mDebFlg) {
            double rotation = calcRotation( mAziIniPos[0], mAziIniPos[1], mAziPos[0], mAziPos[1] )*RAD2DEG;
            String sensor_val_inf = "rotation:" + Integer.toString((int) (rotation))
                    + "\npitch:" + Integer.toString((int) (pitch))
                    + "\nroll :" + Integer.toString((int) (roll))
                    + "\nIniPos : " + Float.toString(mAziIniPos[0]) + ", " + Float.toString(mAziIniPos[1])
                    + "\nPos    : " + Float.toString(mAziPos[0]) + ", " + Float.toString(mAziPos[1])
                    + "\nazimuth: " + Double.toString(mAttitude[0]*RAD2DEG);
            mEtxtContent.setText(sensor_val_inf);
            return;
        }

        if(pitch < -mUdMinDegree && pitch > -mUdMaxDegree){
            Log.v(TAG, "up!!!");
            mSlopeFlg=0;
        }
        else if (pitch > mUdMinDegree && pitch  < mUdMaxDegree){
            Log.v(TAG, "down!!!");
            mSlopeFlg=2;
        }
        else if (roll < -mLrMinDegree && roll > -mLrMaxDegree){
            Log.v(TAG, "right!!!");
            mSlopeFlg=1;
        }
        else if (roll > mLrMinDegree && roll < mLrMaxDegree){
            Log.v(TAG, "left!!!");
            mSlopeFlg=3;
        }
//        else if (rotation > 90){
//            Log.v(TAG, "turn left!!!");
//            return;
//        }
//        else if (rotation > -90){
//            Log.v(TAG, "turn right!!!");
//            return;
//        }
        else return;

        this.unregisterSensor();
        mEtxtContent.setText(mPreFix + mPredictionSentence[mSlopeFlg] + mSufFix);
        mEtxtContent.setSelection(mEtxtContent.getText().length());
    }


    public void getPrefSetting(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String predSentence = pref.getString("key_preset_pattern","empty,empty,empty,empty,empty,empty");
        String predEntry = pref.getString("key_preset_entry","Direction");

        ArrayList<String> predList = Array2Pref.pref2array(pref, predEntry);
        if(predList!=null){
            for (int i=0; i < mPredictionSentence.length ; i++) {
                mPredictionSentence[i] = predList.get(i);
            }
        }
        else{
            String[] strAray = predSentence.split(",");
            if(strAray.length!=mPredictionSentence.length) {
                Toast.makeText(getActivity(),"Invalid Pre-set Prediction",Toast.LENGTH_SHORT).show();
                return;
            }
            for(int i=0; i<strAray.length; i++){
                mPredictionSentence[i] = strAray[i];
            }
        }
    }

    public void getSensorParam(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSensorInterval = pref.getString(SetSensorParamActivity.PREF_KEY_SENSOR_FREQ, "UI");
        mSensorDelayMs = Long.parseLong(pref.getString(SetSensorParamActivity.PREF_KEY_SENSOR_DELAY, "5"))*1000;
        mNumSample = Integer.parseInt(pref.getString(SetSensorParamActivity.PREF_KEY_MV_NUM_SAMPLE, "10"));
        mNumSampleAzi = mNumSample*2;

        mLrMaxDegree = Double.parseDouble(pref.getString(SetSensorParamActivity.PREF_KEY_LR_MAX_DEGREE, "90"));
        mLrMinDegree = Double.parseDouble(pref.getString(SetSensorParamActivity.PREF_KEY_LR_MIN_DEGREE, "30"));
        mUdMaxDegree = Double.parseDouble(pref.getString(SetSensorParamActivity.PREF_KEY_UD_MAX_DEGREE, "20"));
        mUdMinDegree = Double.parseDouble(pref.getString(SetSensorParamActivity.PREF_KEY_UD_MIN_DEGREE, "90"));
        Log.v(TAG,String.valueOf(mSensorDelayMs));
    }


    public int getSensorFreq(){
        switch( mSensorInterval ) {
            case "FASTEST":
                return SensorManager.SENSOR_DELAY_FASTEST;
            case "GAME":
                return SensorManager.SENSOR_DELAY_GAME;
            case "UI":
                return SensorManager.SENSOR_DELAY_UI;
            case "NORMAL":
                return SensorManager.SENSOR_DELAY_NORMAL;
            default:
                break;
        }
        return SensorManager.SENSOR_DELAY_UI;
    }


    public double calcRotation(float x1, float y1, float x2, float y2){
        float fact;

        //端末上向きで反時計周りを正とする
        if ( x1*y2 - x2*y1 > 0) fact = -1;
        else fact = 1;

        double innerProd = (x1*x2 + y1*y2);
        double scalar = Math.sqrt(x1*x1 + y1*y1) * Math.sqrt(x2*x2 + y2*y2);

        double rotation = Math.acos(innerProd / scalar)*fact;
//        double rotation = Math.acos(innerProd / scalar);
//        Log.v(TAG, Double.toString(x1)+","+Double.toString(y1)
//                +","+Double.toString(x2)+","+Double.toString(y2)
//                +","+Double.toString(rotation*RAD2DEG)+","+Double.toString(fact));
        return rotation;
    }


    // センサ判定用のパラメータを初期化
    public void initSensorParam(){
        //センサー初期化・登録
        this.getSensor();
        Toast.makeText(getActivity(), "Sensor Starts in " + String.valueOf(mSensorDelayMs / 1000) + " sec",
                Toast.LENGTH_SHORT).show();
        Log.v(TAG, "sensor registered");

        mIsPressedVolume = false;
        mSlopeFlg = 0;
        mMvCounter = 0;
        mMvCounterAzi = 0;
        mSumAttitude[0] = 0.0f;
        mSumAttitude[1] = 0.0f;
        mSumAttitude[2] = 0.0f;
        mSumAzi[0] = 0.0f;
        mSumAzi[1] = 0.0f;
        mMvStart = false;
        for (int i=0; i<mNumSample; i++){
            mPreviousAttitude[i][0]=0.0f;
            mPreviousAttitude[i][1]=0.0f;
            mPreviousAttitude[i][2]=0.0f;
        }
        for (int i=0; i<mNumSampleAzi; i++){
            mPreviousAziPos[i][0]=0.0f;
            mPreviousAziPos[i][1]=0.0f;
        }
        mStartTime = new Date().getTime();

        // 結果表示のprefix,suffixを本文から読み取り
        String[] strAry = mEtxtContent.getText().toString().split("\n");
        mPreFix = "";
        mSufFix = "";
        if (strAry.length>=2){
            mPreFix = strAry[0];
            mSufFix = strAry[1];
        }
        else if (strAry.length==1){
            mPreFix = strAry[0];
        }
//                eTxtContent.setText("");

        mDebFlg = false;
        if (mPreFix.equals("deb")){
            mDebFlg = true;
            mPreFix = "";
            mSufFix = "";
            Toast.makeText(getActivity(), "Debug Mode Start", Toast.LENGTH_LONG).show();
        }

    }
}
