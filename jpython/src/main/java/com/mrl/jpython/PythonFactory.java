package com.mrl.jpython;

import android.content.Context;
import android.util.Log;

import com.srplab.www.starcore.StarCoreFactory;
import com.srplab.www.starcore.StarCoreFactoryPath;
import com.srplab.www.starcore.StarObjectClass;
import com.srplab.www.starcore.StarServiceClass;
import com.srplab.www.starcore.StarSrvGroupClass;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PythonFactory {

    public static final String TAG = "PythonFactory";

    public final static String arch_64 = "arm64-v8a";
    public final static String arch_32 = "armeabi-v7a";
    private StarServiceClass service;
    private StarSrvGroupClass srvGroup;
    private StarObjectClass python;
    private Context context;
    private File appFile;
    private String appLib;

    private static PythonFactory INSTANCE;

    private PythonFactory() {
    }

    public static PythonFactory getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PythonFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PythonFactory();
                    INSTANCE.context = context;
                    INSTANCE.appFile = context.getFilesDir();  /*-- /data/data/packageName/files --*/
                    INSTANCE.appLib = context.getApplicationInfo().nativeLibraryDir;
                    INSTANCE.loadPy();
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void loadPy() {
        //拷贝Python相关环境 (因为.py必须和下面四个库在相同路径下)
        File pythonLibFile = new File(appFile, "python3.4.zip");
        Properties pro = System.getProperties();
//        pro.list(System.out);
        if (!pythonLibFile.exists()) {
            copyFile("python3.4.zip");
            copyFile("binascii.cpython-34m.so");
            //String sys = System.getProperty("ro.product.cpu.abi");
            if (appLib.contains("64")) {
                copyFile("_struct.cpython-34m.so", "_struct.cpython-34m.so");
                copyFile("time.cpython-34m.so", "time.cpython-34m.so");
                copyFile("zlib.cpython-34m.so", "zlib.cpython-34m.so");
            } else {
                copyFile("_struct.cpython86-34m.so", "_struct.cpython-34m.so");
                copyFile("time.cpython86-34m.so", "time.cpython-34m.so");
                copyFile("zlib.cpython86-34m.so", "zlib.cpython-34m.so");
            }
        }

        // 拷贝Python 代码
        copyFile("calljava.py");
        copyFile("test.py");

        try {
            File tmpF = new File(appLib);
            if (!tmpF.isDirectory()) {
                System.err.println(appLib + " is not a directory");
                throw new Exception();
            }
            //加载python库
            System.load(appLib + File.separator + "libpython3.4m.so");
            // 除了将代码直接拷贝，还支持将代码压缩为zip包，通过Install方法解压到指定路径
            InputStream dataSource = context.getAssets().open("py_code.zip");
            StarCoreFactoryPath.Install(dataSource, appFile.getPath(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*----init starcore----*/
        StarCoreFactoryPath.StarCoreCoreLibraryPath = appLib;
        StarCoreFactoryPath.StarCoreShareLibraryPath = appLib;
        StarCoreFactoryPath.StarCoreOperationPath = appFile.getPath();


        /**
         * 重复进入app会报错：
         * W/vsopenapi_module,40191: service [test] has create, it will be deactive for new service[test]
         * W/skeletonproc_module,35024: service[test]has exist,can not create
         * W/Thread.java,841: create service [test] fail
         * W/dalvikvm: threadid=13: thread exiting with uncaught exception (group=0x4167dc80)
         */
        StarCoreFactory starcore = StarCoreFactory.GetFactory();
        service = starcore._InitSimple("test", "123", 0, 0);
        srvGroup = (StarSrvGroupClass) service._Get("_ServiceGroup");
        service._CheckPassword(false);

        /*----run python code----*/
        srvGroup._InitRaw("python34", service);
        python = service._ImportRawContext("python", "", false, "");
        // 设置Python模块加载路径
        python._Call("import", "sys");
        StarObjectClass pythonSys = python._GetObject("sys");
        StarObjectClass pythonPath = (StarObjectClass) pythonSys._Get("path");
        pythonPath._Call("insert", 0, appFile.getPath() + File.separator + "python3.4.zip");
        pythonPath._Call("insert", 0, appLib);
        pythonPath._Call("insert", 0, appFile.getPath());
        //test();
    }

    public synchronized Object call(String file, String method, Object[] param){
        Log.d(TAG, "call "+file+"."+method+"("+param+")");
        service._DoFile("python", appFile.getPath()+"/"+file+".py", "");
        Object oj = python._Call(method, param);
        Log.d(TAG, "result: "+oj.toString());
        return oj;
    }

    private synchronized void test() {
        //调用Python代码
        service._DoFile("python", appFile.getPath() + "/py_code.py", "");
        long time = python._Calllong("get_time");
        Log.d("", "form python time=" + time);

        service._DoFile("python", appFile.getPath() + "/test.py", "");
        int result = python._Callint("add", 5, 2);
        Log.d("", "result=" + result);

        python._Set("JavaClass", Log.class);
        service._DoFile("python", appFile.getPath() + "/calljava.py", "");
    }

    private void copyFile(String name) {
        copyFile(name, name);
    }

    private void copyFile(String nameIn, String nameOut) {
        File outfile = new File(appFile, nameOut);
        BufferedOutputStream outStream = null;
        BufferedInputStream inStream = null;

        try {
            outStream = new BufferedOutputStream(new FileOutputStream(outfile));
            inStream = new BufferedInputStream(context.getAssets().open(nameIn));

            byte[] buffer = new byte[1024 * 10];
            int readLen = 0;
            while ((readLen = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
