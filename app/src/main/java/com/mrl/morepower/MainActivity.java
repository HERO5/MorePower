package com.mrl.morepower;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mrl.jpython.PythonFactory;
import com.mrl.morepower.master.boot.TcpMaster;
import com.mrl.morepower.master.manager.JobManager;
import com.mrl.morepower.worker.boot.TcpWorker;
import com.mrl.netty.common.utils.FileUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static String CACHE_DIR;
    public static String FILE_DIR;
    public static String SD_DIR;

    private ScrollView scrollMaster;
    private ScrollView scrollWorker;
    private TextView msgMaster;
    private TextView msgWorker;
    private EditText ip;
    private EditText port;
    private Button master;
    private Button worker;
    private Button stop;
    private final Handler handlerMaster = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            msgMaster.append(msg.obj+"\n");
            scrollMaster.scrollTo(0,msgMaster.getBottom());
        }
    };
    private final Handler handlerWorker = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            msgWorker.append(msg.obj+"\n");
            scrollWorker.scrollTo(0,msgWorker.getBottom());
        }
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static PythonFactory factory;
    private final List<TcpWorker> tcpClients = new ArrayList<>();

    //通过一个函数来申请权限
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        init();
    }

    private void init() {
        factory = PythonFactory.getInstance(MainActivity.this);
        CACHE_DIR = this.getCacheDir().toString();
        FILE_DIR = this.getFilesDir().toString();
        SD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
        scrollMaster = findViewById(R.id.scroll_master);
        scrollWorker = findViewById(R.id.scroll_worker);
        msgMaster = findViewById(R.id.msg_master);
        msgWorker = findViewById(R.id.msg_worker);
        ip = findViewById(R.id.ip);
        port = findViewById(R.id.port);
        master = findViewById(R.id.master);
        worker = findViewById(R.id.worker);
        stop = findViewById(R.id.stop);
        master.setOnClickListener(mListener);
        worker.setOnClickListener(mListener);
        stop.setOnClickListener(mListener);
    }

    View.OnClickListener mListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            String i = ip.getText().toString().trim();
            String p = port.getText().toString().trim();
            switch (view.getId()) {
                case R.id.master:
                    if(!regxPort(p)) break;
                    JobManager.initTask(10);
//                    Intent intent1 = new Intent(MainActivity.this, ServerService.class);
//                    startService(intent1);
                    TcpMaster.getInstance().init(handlerMaster, Integer.valueOf(p));
                    msgMaster.append("......Master is  ready......\n");
                    Log.d("on master click", "......Master is  ready......");
                    break;
                case R.id.worker:
                    Log.d("on worker clicked", "......Worker is  init......");
                    if(!regxIp(i)) break;
                    if(!regxPort(p)) break;
                    TcpWorker tcpClient = new TcpWorker(handlerWorker, i, Integer.valueOf(p));
                    tcpClients.add(tcpClient);
                    tcpClient.connect();

                    // 定义DexClassLoader
                    // 第一个参数：是dex压缩文件的路径
                    // 第二个参数：是dex解压缩后存放的目录
                    // 第三个参数：是C/C++依赖的本地库文件目录,可以为null
                    // 第四个参数：是上一级的类加载器
//                    DexClassLoader classLoader = new DexClassLoader(SD_DIR + "/sum1.dex",
//                            SD_DIR + "/", null,
//                            getClassLoader());
//                    try {
//                        Class<?> cls = classLoader.loadClass("com.even.test.Sum");
//                        //Constructor<?> istructor = iclass.getConstructor(Context.class);
//                        Map<String, Double> data = new HashMap();
//                        data.put("f1", 10.0D);
//                        data.put("f2", 20.0D);
//                        data.put("f3", 30.0D);
//                        //利用反射原理去调用
//                        Method method = cls.getMethod("calculate", new Class[]{Map.class});
//                        String res = method.invoke(cls.newInstance(), data).toString();
//                        System.out.println(res);
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                    msgMaster.append("......client is  ready......\n");
                    Log.d("on worker clicked", "......Worker is  ready......");
                    break;
                case R.id.stop:
                    for(TcpWorker worker : tcpClients){
                        worker.shutDown();
                    }
//                    Intent intent2 = new Intent(MainActivity.this, ServerService.class);
//                    stopService(intent2);
                    TcpMaster.getInstance().shutDown();
                    String files = reportFiles();
                    msgMaster.setText(files);
                    msgWorker.setText(null);
                    Log.d("on stop clicked", "......stop all......");
                    break;
            }
        }
    };

    public String reportFiles(){
        StringBuilder sb = new StringBuilder();
        ArrayList<String> listFileName = new ArrayList<String>();
        sb.append(FILE_DIR+":\n");
        FileUtil.getAllFileName(FILE_DIR, listFileName);
        Collections.sort(listFileName);
        for(String name : listFileName){
            sb.append(name+"\n");
            if(name.endsWith(".py")){
                FileUtil.delFile(name);
            }
        }
        return sb.toString();
    }

    private boolean regxPort(String port){
        Pattern pattern = Pattern.compile("([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{4}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])");
        Matcher matcher = pattern.matcher(port);
        if(matcher.matches()){
            return true;
        }else {
            Toast.makeText(MainActivity.this, "非法端口", Toast.LENGTH_LONG).show();
            return false;
        }
    }
    private boolean regxIp(String ip){
        Pattern pattern = Pattern.compile("(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])");
        Matcher matcher = pattern.matcher(ip);
        if(matcher.matches()){
            return true;
        }else {
            Toast.makeText(MainActivity.this, "非法IP", Toast.LENGTH_LONG).show();
            return false;
        }
    }

}
