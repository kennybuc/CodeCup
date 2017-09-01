package com.app.flexivendsymbol.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.app.flexivendsymbol.R;
import com.er.ERusbsdk.UsbController;
import com.er.ERusbsdk.PrintImage;

public class UsbConnectActivity extends AppCompatActivity {
    private Button btn_conn = null;
    private EditText txtSpace = null;
    private Button btnSendTxt = null;
    private Button btnPrintTestPage = null;
    private Button btnDisconnect = null;
    View llay;


    UsbController usbCtrl = null;
    UsbDevice dev = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_connect);
        llay = (View) findViewById(R.id.llay);
        btn_conn = (Button) findViewById(R.id.btn_conn);
        txtSpace = (EditText) findViewById(R.id.txtContent);
        btnSendTxt = (Button) findViewById(R.id.btnSendTxt);
        btnPrintTestPage = (Button) findViewById(R.id.btnPrintTestPage);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);


        btn_conn.setOnClickListener(new ClickEvent());
        btnSendTxt.setOnClickListener(new ClickEvent());
        btnPrintTestPage.setOnClickListener(new ClickEvent());
        btnDisconnect.setOnClickListener(new ClickEvent());

        btnSendTxt.setEnabled(false);
        btnPrintTestPage.setEnabled(false);
        btnDisconnect.setEnabled(false);
        llay.setVisibility(View.GONE);

        usbCtrl = new UsbController(this, mHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        usbCtrl.close();
    }

    //check access permission?
    public boolean CheckUsbPermission() {
        if (dev != null) {
            if (usbCtrl.isHasPermission(dev)) {
                return true;
            }
        }
        btnSendTxt.setEnabled(false);
        btnPrintTestPage.setEnabled(false);
        btnDisconnect.setEnabled(false);
        btn_conn.setEnabled(true);
        llay.setVisibility(View.GONE);
        Toast.makeText(getApplicationContext(), getString(R.string.msg_AccessFail),
                Toast.LENGTH_SHORT).show();
        return false;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbController.USB_CONNECTED:
                    Toast.makeText(getApplicationContext(), getString(R.string.msg_AccessSuccess),
                            Toast.LENGTH_SHORT).show();
                    btnSendTxt.setEnabled(true);
                    btnPrintTestPage.setEnabled(true);
                    btnDisconnect.setEnabled(true);
                    btn_conn.setEnabled(false);
                    llay.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    };

    //button click event
    class ClickEvent implements View.OnClickListener {
        public void onClick(View v) {
            byte printStatus;
            if (v == btn_conn) {
                usbCtrl.close();
                dev = usbCtrl.getUsbDev();

                if (dev != null) {
                    if (!(usbCtrl.isHasPermission(dev))) {
                        usbCtrl.getPermission(dev);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_AccessSuccess),
                                Toast.LENGTH_SHORT).show();
                        btnSendTxt.setEnabled(true);
                        btnPrintTestPage.setEnabled(true);
                        btnDisconnect.setEnabled(true);
                        btn_conn.setEnabled(false);
                        llay.setVisibility(View.VISIBLE);
                    }
                }
            } else if (v == btnSendTxt) {
                printStatus = usbCtrl.revByte(dev);
                if (printStatus == 0x38) {
                    Toast.makeText(getApplicationContext(), getString(R.string.paper_stat),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String txt_msg = txtSpace.getText().toString();
                if (txt_msg == null || "".equals(txt_msg)) {
                    Toast.makeText(UsbConnectActivity.this, getString(R.string.content_null), Toast.LENGTH_SHORT).show();
                    txtSpace.requestFocus();
                    return;
                }
                if (CheckUsbPermission() == true) {
                    byte[] cmd_resume = new byte[4];
                    cmd_resume[0] = 0x1B;
                    cmd_resume[1] = 0x40; // reset command
                    usbCtrl.sendByte(cmd_resume, dev);
                    usbCtrl.sendMsg(txt_msg, "GBK", dev);
                }
            } else if (v == btnPrintTestPage) {
                String msg = "";
                String lang = getString(R.string.strLang);
                byte[] cmd = new byte[3];
                byte[] cmd_resume = new byte[4];
                cmd_resume[0] = 0x1B;
                cmd_resume[1] = 0x40;     // reset command
                usbCtrl.sendByte(cmd_resume, dev);

                printStatus = usbCtrl.revByte(dev);
                if (printStatus == 0x38) {
                    Toast.makeText(getApplicationContext(), getString(R.string.paper_stat),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                printImage();
                if ((lang.compareTo("en")) == 0) {
                    cmd[0] = 0x1b;
                    cmd[1] = 0x45;
                    cmd[2] = 0x01; // set bold font commands
                    usbCtrl.sendByte(cmd, dev);
                    usbCtrl.sendMsg("Thanks for choosing our printer!\n", "GBK", dev);
                    cmd[2] = 0x00; //  cancel bold font commands
                    usbCtrl.sendByte(cmd, dev);
                    msg = "You have sucessfully got your\ndevice connected with printer.\n"
                            + "Please feel free to contact us \nif you need any help.\n\n";
                    usbCtrl.sendMsg(msg, "GBK", dev);
                } else if ((lang.compareTo("ch")) == 0) {
                    cmd[0] = 0x1b;
                    cmd[1] = 0x45;
                    cmd[2] = 0x01;
                    usbCtrl.sendByte(cmd, dev);
                    usbCtrl.sendMsg("��л��ѡ�����ǵĴ�ӡ��!\n", "GBK", dev);
                    cmd[2] = 0x00;//  cancel bold font commands
                    usbCtrl.sendByte(cmd, dev);
                    msg = "�����豸�ʹ�ӡ���Ѿ��ɹ��Խ�,\n"
                            + "�������κ����ʣ���ӭ��ѯ!\n\n";
                    usbCtrl.sendMsg(msg, "GBK", dev);
                }
            } else if (v == btnDisconnect) {
                usbCtrl.close();
                btnSendTxt.setEnabled(false);
                btnPrintTestPage.setEnabled(false);
                btnDisconnect.setEnabled(false);
                btn_conn.setEnabled(true);
                llay.setVisibility(View.GONE);
            }
        }
    }

    //print image
    private void printImage() {
        int i = 0, p = 0, j = 0, index = 0;
        byte[] buf = new byte[56];
        byte[] bitsData = null;
        PrintImage image = new PrintImage();
        image.initCanvas(384);
        image.initPaint();
        //get the path of image to be printed
        String path = Environment.getExternalStorageDirectory() + "/icon2.jpg";
        image.drawImage(0, 0, path);
        bitsData = image.printDraw();

        for (i = 0; i < image.getLength(); i++) {
            p = 0;
            buf[p++] = 0x1D;
            buf[p++] = 0x76;
            buf[p++] = 0x30;
            buf[p++] = 0x00;
            buf[p++] = (byte) (image.getWidth() / 8);
            buf[p++] = 0x00;
            buf[p++] = 0x01;
            buf[p++] = 0x00;
            for (j = 0; j < (image.getWidth() / 8); j++)
                buf[p++] = bitsData[index++];
            usbCtrl.sendByte(buf, dev);
        }
    }
}
