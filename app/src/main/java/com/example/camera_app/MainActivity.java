package com.example.camera_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;

    private final int REQUEST_CAMERA_PERMISSION = 1;
    private TextureView textureView;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private ImageView galleryPreview;
    private Button cameraRotate,captureButton;
    private String currentCameraId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.texture_view);
        captureButton = findViewById(R.id.captureBtn);
        cameraRotate = findViewById(R.id.cameraRotate);
        galleryPreview = findViewById(R.id.galleryPreview);



        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                cameraId = cameraIds[0]; // Use the first available camera

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Open the camera
                    openCamera();
                } else {
                    // Request camera permission
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        cameraRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                // Use the first available camera as the initial camera
                cameraId = cameraIds[0];
                currentCameraId = cameraId;

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Open the camera
                    openCamera();
                } else {
                    // Request camera permission
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        galleryPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCustomGallery();
            }
        });

    }
    private void openCustomGallery() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName("com.example.gallery_application", "com.example.gallery_application.MainActivity")); // Replace with your gallery app's package name and main activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Handle the case where the gallery app is not installed or accessible
            // You can open the Play Store to prompt the user to install the app
            Toast.makeText(this, "Gallery app not installed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    // Camera opened, you can start camera operations here
                    createCameraPreviewSession(camera);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    // Handle camera disconnect
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    // Handle camera errors
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession(CameraDevice cameraDevice) {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(textureView.getWidth(),textureView.getHeight());

            Surface surface = new Surface(texture);

            // Create a capture request builder
            CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            // Create a capture session
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            // The camera is already closed
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                // Set the preview request
                                session.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // Handle configuration failure
                        }
                    }, null);
            captureButton.setOnClickListener(v -> {
                try {
                    CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureBuilder.addTarget(imageReader.getSurface());

                    // Capture the image
                    cameraCaptureSession.capture(captureBuilder.build(), null, null);
                }catch (CameraAccessException e){
                    e.printStackTrace();
                }


            });
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // Handle permission request response
    private void switchCamera() {
        closeCamera();
        if (currentCameraId == null || cameraId == null) {
            // Ensure camera IDs are initialized before comparison
            Log.e("MyApp","currentcameraId"+currentCameraId);
            Log.e("MyApp","cameraId"+cameraId);
            return;
        }
        // Switch to the other camera
        if (currentCameraId.equals(cameraId)) {
            currentCameraId = getFrontCameraId();
        } else {
            currentCameraId = cameraId;
        }

        openCamera();
    }

    private String getFrontCameraId() {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraId; // If no front camera found, return the default camera
    }

    private void closeCamera() {
        // Close and release the camera resources
        // ...
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                // Camera permission denied, handle accordingly
                Toast.makeText(this, "Camera permission is denied Please accept the permission to use camera ", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
//private void openCamera() {
//    try {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
//            return;
//        }
//
//        if (cameraManager != null) {
//            String[] cameraIds = cameraManager.getCameraIdList();
//            boolean cameraFound = false;
//
//            for (String id : cameraIds) {
//                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
//                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//
//                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
//                    cameraId = id; // Set the rear camera ID (or the camera you want to use)
//                    cameraFound = true;
//                    break;
//                }
//            }
//
//            if (!cameraFound) {
//                // Rear camera not found or unavailable
//                // Handle this scenario (show an error message or take appropriate action)
//                return;
//            }
//
//            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
//                @Override
//                public void onOpened(@NonNull CameraDevice camera) {
//                    createCameraPreviewSession(camera);
//                }
//
//                @Override
//                public void onDisconnected(@NonNull CameraDevice camera) {
//
//                }
//
//                @Override
//                public void onError(@NonNull CameraDevice camera, int error) {
//
//                }
//                // Rest of the camera open logic remains unchanged...
//                // ...
//            }, null);
//        }
//    } catch (CameraAccessException e) {
//        e.printStackTrace();
//        // Handle the CameraAccessException (e.g., show error message to the user)
//    }
//}