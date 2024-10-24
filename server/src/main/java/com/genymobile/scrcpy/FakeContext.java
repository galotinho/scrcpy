package com.genymobile.scrcpy;

import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Process;
import android.app.ActivityThread;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.IContentProvider;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import com.genymobile.scrcpy.Workarounds;

// Se Ln está no pacote util, corrija a importação
// import com.genymobile.scrcpy.util.Ln;

import com.genymobile.scrcpy.Ln;

public final class FakeContext extends ContextWrapper {

    public static final String PACKAGE_NAME = "com.android.shell";
    public static final int ROOT_UID = 0; // Like android.os.Process.ROOT_UID, but before API 29
    private final ApplicationContentResolver mContentResolver;
    private static boolean isAudioManagerPatched = false;


    private static final FakeContext INSTANCE = new FakeContext();

    public static FakeContext get() {
        return INSTANCE;
    }

    private FakeContext() {
        super(Workarounds.getSystemContext());
        mContentResolver = new ApplicationContentResolver(this, (ActivityThread) Workarounds.ACTIVITY_THREAD);
    }


    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public String getOpPackageName() {
        return PACKAGE_NAME;
    }

    @TargetApi(Build.VERSION_CODES.S)
    @Override
    public AttributionSource getAttributionSource() {
        AttributionSource.Builder builder = new AttributionSource.Builder(Process.SHELL_UID);
        builder.setPackageName(PACKAGE_NAME);
        return builder.build();
    }

    // @Override to be added on SDK upgrade for Android 14
    @SuppressWarnings("unused")
    public int getDeviceId() {
        return 0;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public ContentResolver getContentResolver() {
        try {
            return mContentResolver;
        } catch (Exception e) {
            Ln.e("getContentResolver Exception", e);
        }
        return super.getContentResolver();
    }

    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        if (Context.AUDIO_SERVICE.equals(name)) {
            if (!isAudioManagerPatched) {
                patchAudioManagerContext(service);
                isAudioManagerPatched = true;
            }
        }
        return service;
    }


    @SuppressLint("SoonBlockedPrivateApi")
    private void patchAudioManagerContext(Object service) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Method setContextMethod = AudioManager.class.getDeclaredMethod("setContext", Context.class);
                setContextMethod.setAccessible(true);
                setContextMethod.invoke(service, this);
            } else {
                Field mContextField = AudioManager.class.getDeclaredField("mContext");
                mContextField.setAccessible(true);
                mContextField.set(service, this);
            }
        } catch (Exception e) {
            Ln.e("patchAudioManagerContext Exception", e);
        }
    }


}
