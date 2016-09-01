# HttpUtils
单文件Http工具类，支持断点续传，多线程断点续传
## 1普通请求
``` java
        String baiduUrl = "http://www.baidu.com";
        String response = HttpUtils.request(baiduUrl).get();
        Log.d("HttpUtils", baiduUrl);
        Log.d("HttpUtils", String.valueOf(response));
```
## 2带参数
[API Store中免费接口](http://apistore.baidu.com.cn/, "http://apistore.baidu.com.cn/")
``` java
        String apikey = "7c835ed01bd98ae64d1ff44d7a09b8e3";
        String httpUrl = "http://apis.baidu.com/heweather/weather/free";
        String response = HttpUtils.request(httpUrl)
                .param("city", "beijing")
                .property("apikey", apikey)
                .get();
        Log.d("HttpUtils", httpUrl);
        Log.d("HttpUtils", String.valueOf(response));
```
## 3普通下载
``` java
        File sdcard = Environment.getExternalStorageDirectory();
        final String meizi1 = "http://i.meizitu.net/2014/09/23mt01.jpg";
        File file1 = new File(sdcard, "meizi1.jpg");
        boolean r = HttpUtils.request(meizi1)
                .download()
                .target(file1)
                .downloadInOneUnit();
        Log.d("HttpUtils", meizi1);
        Log.d("HttpUtils", String.valueOf(r));
```
### 4断点续传
``` java
        String meizi2 = "http://i.meizitu.net/2014/09/23mt02.jpg";
        File file2 = new File(sdcard, "meizi2.jpg");
        boolean r = HttpUtils.request(meizi1)
                .download()
                .target(file2)
                .breakpoint(true)
                .downloadInOneUnit();
        Log.d("HttpUtils", meizi2);
        Log.d("HttpUtils", String.valueOf(r));
```
## 5多线程断点续传
``` java
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
```