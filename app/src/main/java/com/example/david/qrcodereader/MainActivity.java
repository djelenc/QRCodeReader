package com.example.david.qrcodereader;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.david.desktopsms.protobuf.Messages;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        setTitle("Scan QR code");

        final BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        barcodeDetector.setProcessor(new QRCodeDetector());

        final CameraSource cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        final SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_view);
        cameraView.getHolder().addCallback(new CameraViewCallbacks(this, cameraSource));
    }

    private static class CameraViewCallbacks implements SurfaceHolder.Callback {
        private final CameraSource cameraSource;
        private final Context context;

        public CameraViewCallbacks(Context context, CameraSource cameraSource) {
            this.context = context;
            this.cameraSource = cameraSource;
        }


        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("NO PERMISSION // TODO");
                return;
            }

            try {
                cameraSource.start(surfaceHolder);
                System.out.println("STARTING CAMERA");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (cameraSource != null) {
                System.out.println("STOPPING CAMERA");
                cameraSource.stop();
            }
        }
    }

    private static class QRCodeDetector implements Detector.Processor<Barcode> {

        @Override
        public void release() {

        }

        @Override
        public void receiveDetections(Detector.Detections<Barcode> detections) {
            final SparseArray<Barcode> codes = detections.getDetectedItems();

            for (int i = 0; i < codes.size(); i++) {
                final Barcode barcode = codes.valueAt(i);

                try {
                    final byte[] bytes = barcode.displayValue.getBytes("ISO-8859-1");
                    final Messages.Handshake message = Messages.Handshake.parseFrom(bytes);
                    System.out.printf("HIT [%d]: %s:%d (%s)%n", i, message.getAddressesList(),
                            message.getPort(), message.getPublicKey());
                } catch (UnsupportedEncodingException | InvalidProtocolBufferException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}
