package com.frostline.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView web;
    private ValueCallback<Uri[]> chooserCallback;
    private Uri cameraUri;
    private PermissionRequest pendingWebPermission;
    private Runnable afterCameraGranted;
    private Runnable afterCameraDenied;

    // Gallery picker / phone camera app result
    private final ActivityResultLauncher<Intent> chooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (chooserCallback == null) return;
                Uri[] result = null;
                if (r.getResultCode() == RESULT_OK) {
                    Intent d = r.getData();
                    if (d != null && d.getData() != null) result = new Uri[]{d.getData()};
                    else if (cameraUri != null) result = new Uri[]{cameraUri};
                }
                chooserCallback.onReceiveValue(result);
                chooserCallback = null;
                cameraUri = null;
            });

    // Camera permission result
    private final ActivityResultLauncher<String> cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                Runnable next = granted ? afterCameraGranted : afterCameraDenied;
                afterCameraGranted = null;
                afterCameraDenied = null;
                if (next != null) next.run();
            });

    private boolean hasCamera() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void withCamera(Runnable granted, Runnable denied) {
        if (hasCamera()) { granted.run(); return; }
        afterCameraGranted = granted;
        afterCameraDenied = denied;
        cameraPermLauncher.launch(Manifest.permission.CAMERA);
    }

    private void cancelChooser() {
        if (chooserCallback != null) chooserCallback.onReceiveValue(null);
        chooserCallback = null;
    }

    private void launchPhoneCamera() {
        try {
            File dir = new File(getCacheDir(), "camera");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File f = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooserLauncher.launch(i);
        } catch (Exception e) {
            cancelChooser();
            Toast.makeText(this, "Could not open the camera", Toast.LENGTH_SHORT).show();
        }
    }

    /** Called from the page (window.FrostApp). */
    public class Bridge {
        @JavascriptInterface
        public String saveImage(String b64, String name) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                    v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Frostline");
                    Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) throw new Exception("insert failed");
                    try (OutputStream o = getContentResolver().openOutputStream(uri)) { o.write(bytes); }
                    return "Saved to your gallery (Pictures/Frostline).";
                } else {
                    shareImage(b64, name, "");
                    return "Choose a gallery or Files app in the share sheet to save the image.";
                }
            } catch (Exception e) {
                return "Could not save the image. Try Share instead.";
            }
        }

        @JavascriptInterface
        public void shareImage(String b64, String name, String text) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "share");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File f = new File(dir, name);
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", f);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("image/png");
                i.putExtra(Intent.EXTRA_STREAM, uri);
                if (text != null && !text.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, text);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(i, "Share")));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Could not share the image", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0d1424"));
        getWindow().setNavigationBarColor(Color.parseColor("#0d1424"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0d1424"));
        setContentView(web);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " FrostlineApp");
        web.addJavascriptInterface(new Bridge(), "FrostApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooserCallback != null) chooserCallback.onReceiveValue(null);
                chooserCallback = cb;
                if (p.isCaptureEnabled()) {
                    withCamera(MainActivity.this::launchPhoneCamera, MainActivity.this::cancelChooser);
                } else {
                    try {
                        chooserLauncher.launch(p.createIntent());
                    } catch (Exception e) {
                        cancelChooser();
                    }
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean wantsCamera = false;
                    for (String r : request.getResources())
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCamera = true;
                    if (!wantsCamera) { request.deny(); return; }
                    pendingWebPermission = request;
                    withCamera(() -> {
                        pendingWebPermission.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                        pendingWebPermission = null;
                    }, () -> {
                        pendingWebPermission.deny();
                        pendingWebPermission = null;
                    });
                });
            }
        });

        if (b == null) web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
        else web.restoreState(b);
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
