package com.egai.wukong.tools;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;


/**

 * Created by wanglingsheng on 2018/5/29.

 * 文件下载工具类（单例模式）

 */



public class DownloadUtil {




    private static DownloadUtil downloadUtil;

    private final OkHttpClient okHttpClient;



    public static DownloadUtil get() {

        if (downloadUtil == null) {

            downloadUtil = new DownloadUtil();

        }

        return downloadUtil;

    }



    public DownloadUtil() {
//        HttpLoggingInterceptor loggingInterceptor=new HttpLoggingInterceptor(new HttpLog());
//        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpClient =new  OkHttpClient.Builder()

                .build();

    }


    public void checkNewVersion(String url,final onCheckListener listener)
    {
        url+="?check=1";
        Log.d("DownLoadUtil","url"+url);
        final Request request=new Request.Builder()
                .url(url)
                .build();
        OkHttpClient client=new OkHttpClient();
        try{
            Response response=client.newCall(request).execute();
        }catch (IOException e) {

            e.printStackTrace();

        }
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onCheckFail(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code()!=200)
                {
                    listener.onCheckFail("未找到页面");
                    return;
                }
                String newVersion;
                long newVersionCode=0;

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String fileName=json.getString("fileName");
                    String versionName=json.getString("versionName");
                    newVersionCode=json.getInt("versionCode");
                    listener.onCheckSuccess(fileName,versionName,newVersionCode);
                    Log.d("DownLoadUtils","newVersion:"+versionName);
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onCheckFail("解析失败");
                }



            }
        });




    }






    /**

     * @param url          下载连接

     * @param destFileDir  下载的文件储存目录



     * @param listener     下载监听

     */



    public void download(final String url, final String destFileDir, final OnDownloadListener listener) {



        final Request request = new Request.Builder()

                .url(url)
                .build();
        OkHttpClient client = new OkHttpClient();
        try {

            Response response = client.newCall(request).execute();

        } catch (IOException e) {

            e.printStackTrace();

        }



        //异步请求

        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override

            public void onFailure(Call call, IOException e) {

                // 下载失败监听回调

                listener.onDownloadFailed(e);

            }



            @Override

            public void onResponse(Call call, Response response) throws IOException {





                String fileName=response.header("fileName");

                if(response.code()!=200 || fileName.isEmpty())
                {
                    listener.onDownloadFailed(new Exception("下载失败"));
                    return;

                }





                InputStream is = null;

                byte[] buf = new byte[2048];

                int len = 0;

                FileOutputStream fos = null;



                //储存下载文件的目录

                File dir = new File(destFileDir);

                if (!dir.exists()) {

                    dir.mkdirs();

                }

                File file = new File(dir, fileName);



                try {



                    is = response.body().byteStream();

                    long total = response.body().contentLength();

                    fos = new FileOutputStream(file);

                    long sum = 0;

                    while ((len = is.read(buf)) != -1) {

                        fos.write(buf, 0, len);

                        sum += len;

                        int progress = (int) (sum * 1.0f / total * 100);

                        //下载中更新进度条

                        listener.onDownloading(progress);

                    }

                    fos.flush();

                    //下载完成

                    listener.onDownloadSuccess(file);

                } catch (Exception e) {

                    listener.onDownloadFailed(e);

                }finally {



                    try {

                        if (is != null) {

                            is.close();

                        }

                        if (fos != null) {

                            fos.close();

                        }

                    } catch (IOException e) {



                    }



                }





            }

        });

    }

    public  interface onCheckListener{

        void onCheckSuccess(String fileName,String newVersion,long versionCode);
        void onCheckFail(String message);
    }





    public interface OnDownloadListener{



        /**

         * 下载成功之后的文件

         */

        void onDownloadSuccess(File file);



        /**

         * 下载进度

         */

        void onDownloading(int progress);



        /**

         * 下载异常信息

         */



        void onDownloadFailed(Exception e);

    }

}







