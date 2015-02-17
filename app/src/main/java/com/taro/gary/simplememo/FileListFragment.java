package com.taro.gary.simplememo;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Shunya on 15/02/05.
 */
public class FileListFragment extends ListFragment{
    // ListView　用アダプタ
    SimpleAdapter mAdapter = null;
    // ListView に設定するデータ
    List<Map<String, String>> mList = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // ListView 用アダプタのリストを生成
        mList = new ArrayList<Map<String, String>>();

        // ListView 用アダプタを生成
        mAdapter = new SimpleAdapter(
                getActivity(),
                mList,
                android.R.layout.simple_list_item_2,
                new String [] {"title", "content"},
                new int[] {android.R.id.text1, android.R.id.text2}
        );

        // ListView にアダプターをセット
        setListAdapter(mAdapter);

        // ListView のアイテム選択イベント
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(
                    AdapterView<?> parent, View view, int pos, long id) {
                // 編集画面に渡すデータをセットし、表示
                Intent intent = new Intent(getActivity(), EditActivity.class);
                intent.putExtra("NAME", mList.get(pos).get("filename"));
                intent.putExtra("TITLE", mList.get(pos).get("title"));
                intent.putExtra("CONTENT", mList.get(pos).get("content"));
                startActivity(intent);
            }
        });

//        // ListView をコンテキストメニューに登録
        registerForContextMenu(getListView());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.custom_list, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // ListView 用アダプタのデータをクリア
        mList.clear();

        // アプリの保存フォルダ内のファイル一覧を取得
        String savePath = getActivity().getFilesDir().getPath().toString();
        File[] files = new File(savePath).listFiles();
        // ファイル名の降順でソート
//        Arrays.sort(files, Collections.reverseOrder());
        // 最終更新日時でソート。要commons.io
//        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        // テキストファイル(*.txt)を取得し、ListView用アダプタのリストにセット
        for (int i=0; i<files.length; i++) {
            String fileName = files[i].getName();
            if (files[i].isFile() && fileName.endsWith(".txt")) {
                String title = null;
                String content = null;
                //　ファイルを読み込み
                try {
                    // ファイルオープン
                    InputStream in = getActivity().openFileInput(fileName);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    // タイトル（1行目）を読み込み
                    title = reader.readLine();
                    // 内容（2行目以降）を読み込み
                    char[] buf = new char[(int)files[i].length()];
                    int num = reader.read(buf);
                    content = new String(buf, 0, num);
                    // ファイルクローズ
                    reader.close();
                    in.close();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "File read error!", Toast.LENGTH_LONG).show();
                }

                // ListView用のアダプタにデータをセット
                Map<String, String> map = new HashMap<String, String>();
                map.put("filename", fileName);
                map.put("title", title);
                map.put("content", content);
                mList.add(map);
            }
        }
        // ListView のデータ変更を表示に反映
        mAdapter.notifyDataSetChanged();
    }


    // コンテキストメニュー作成処理
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        getActivity().getMenuInflater().inflate(R.menu.context_main, menu);
    }

    // コンテキストメニュー選択処理
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        switch(item.getItemId()) {
            case R.id.context_del:
                // [削除] 選択時の処理
                // ファイル削除
                if (getActivity().deleteFile(mList.get(info.position).get("filename"))) {
                    Toast.makeText(getActivity(), R.string.msg_del, Toast.LENGTH_SHORT).show();
                }
                // リストからアイテム処理
                mList.remove(info.position);
                // ListView のデータ変更を表示に反映
                mAdapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
        return false;
    }
}
