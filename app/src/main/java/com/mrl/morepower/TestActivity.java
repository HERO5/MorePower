package com.mrl.morepower;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mrl.morepower.business.OnReceiveListener;
import com.mrl.morepower.business.OnServerConnectListener;
import com.mrl.morepower.master.boot.TestServer;
import com.mrl.morepower.master.handler.Test;
import com.mrl.morepower.service.ServerService;
import com.mrl.morepower.worker.boot.TestClient;

import java.net.InetSocketAddress;

public class TestActivity  extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    private EditText etTitle;
    private EditText etContent;
    private TextView tvRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        etTitle = (EditText) findViewById(R.id.etTitle);
        etContent = (EditText) findViewById(R.id.etContent);
        tvRes = (TextView) findViewById(R.id.tvRes);
        Intent intent = new Intent(this, ServerService.class);
        startService(intent);

    }

    public void connect(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TestClient.getInstance()
                        .connect(new InetSocketAddress("127.0.0.1", TestServer.PORT_NUMBER), new OnServerConnectListener() {
                            @Override
                            public void onConnectSuccess() {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(TestActivity.this, "connect success!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }

                            @Override
                            public void onConnectFailed() {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(TestActivity.this, "connect failed!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        });
            }
        }).start();

    }

    public void send(View view) {
        Test.ProtoTest protoTest = Test.ProtoTest
                .newBuilder()
                .setId(1)
                .setTitle(etTitle.getText().toString())
                .setContent(etContent.getText().toString())
                .build();
        TestClient.getInstance().send(protoTest, new OnReceiveListener() {
            @Override
            public void handleReceive(final Object msg) {
                if (msg instanceof Test.ProtoTest) {
                    Log.d(TAG, "handleReceive: " + ((Test.ProtoTest) msg).getContent());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            tvRes.setText("");
                            Test.ProtoTest test = (Test.ProtoTest) msg;
                            tvRes.setText(test.getId() + "\n" +
                                    test.getTitle() + "\n" +
                                    test.getContent());
                        }
                    });

                }
            }
        });
    }
}
