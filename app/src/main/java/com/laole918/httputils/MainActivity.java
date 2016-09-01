package com.laole918.httputils;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.laole918.utils.HttpUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                testHttpUtils();
            }
        }).start();
    }

    private void testHttpUtils() {
        String baiduUrl = "http://www.baidu.com";
        String response = HttpUtils.request(baiduUrl).get();
        Log.d("HttpUtils", baiduUrl);
        Log.d("HttpUtils", String.valueOf(response));

        String apikey = "7c835ed01bd98ae64d1ff44d7a09b8e3";
        String httpUrl = "http://apis.baidu.com/heweather/weather/free";
        response = HttpUtils.request(httpUrl)
                .param("city", "beijing")
                .property("apikey", apikey).get();
        Log.d("HttpUtils", httpUrl);
        Log.d("HttpUtils", String.valueOf(response));

        File sdcard = Environment.getExternalStorageDirectory();
        final String meizi1 = "http://i.meizitu.net/2014/09/23mt01.jpg";
        File file1 = new File(sdcard, "meizi1.jpg");
        boolean r = HttpUtils.request(meizi1)
                .download()
                .target(file1)
                .downloadInOneUnit();
        Log.d("HttpUtils", meizi1);
        Log.d("HttpUtils", String.valueOf(r));

        String meizi2 = "http://i.meizitu.net/2014/09/23mt02.jpg";
        File file2 = new File(sdcard, "meizi2.jpg");
        r = HttpUtils.request(meizi1)
                .download()
                .target(file2)
                .breakpoint(true)
                .downloadInOneUnit();
        Log.d("HttpUtils", meizi2);
        Log.d("HttpUtils", String.valueOf(r));

        final String meizi3 = "http://i.meizitu.net/2013/07/2011103023431314867.jpg";
        final File file3 = new File(sdcard, "meizi3.jpg");
        HttpUtils.DownloadUnit[] dus = HttpUtils.request(meizi3)
                .download()
                .target(file3)
                .breakpoint(true)
                .units(3);
        if(dus != null) {
            for (final HttpUtils.DownloadUnit du : dus) {
                Log.d("HttpUtils", String.valueOf(du.id));
                Log.d("HttpUtils", String.valueOf(du.startBytes));
                Log.d("HttpUtils", String.valueOf(du.endBytes));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean r = HttpUtils.request(meizi1)
                                .download()
                                .target(file3)
                                .breakpoint(true)
                                .downloadUnit(du);
                        Log.d("HttpUtils", meizi3);
                        Log.d("HttpUtils", String.valueOf(r));
                    }
                }).start();
            }
        }
    }
}
