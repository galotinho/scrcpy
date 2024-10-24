package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;

import com.genymobile.scrcpy.Ln;

import java.lang.reflect.Method;
import java.util.Objects;

public class ApplicationContentResolver extends ContentResolver {
    private final Object mMainThread;

    public ApplicationContentResolver(Context context, Object mainThread) {
        super(context);
        mMainThread = Objects.requireNonNull(mainThread);
    }

    @Override
    protected Object acquireProvider(Context context, String auth) {
        return ServiceManager.getActivityManager().getContentProviderExternal(auth, new Binder());
    }

    @Override
    protected Object acquireExistingProvider(Context context, String auth) {
        // Implemented for compatibility, though it may not be called
        try {
            Class<?> activityThreadClass = mMainThread.getClass();
            Method method = activityThreadClass.getDeclaredMethod("acquireExistingProvider", Context.class, String.class, int.class, boolean.class);
            method.setAccessible(true);
            return method.invoke(mMainThread, context, auth, resolveUserIdFromAuthority(auth), true);
        } catch (Exception e) {
            Ln.e("Could not invoke acquireExistingProvider", e);
            return null;
        }
    }

    @Override
    public boolean releaseProvider(Object provider) {
        // May need to release the ContentProviderExternal
        try {
            Class<?> activityThreadClass = mMainThread.getClass();
            Method method = activityThreadClass.getDeclaredMethod("releaseProvider", Class.forName("android.content.IContentProvider"), boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(mMainThread, provider, true);
        } catch (Exception e) {
            Ln.e("Could not invoke releaseProvider", e);
            return false;
        }
    }

    @Override
    protected Object acquireUnstableProvider(Context context, String auth) {
        // Updated to handle calls from ClipboardManager
        return ServiceManager.getActivityManager().getContentProviderExternal(auth, new Binder());
    }

    @Override
    public boolean releaseUnstableProvider(Object icp) {
        // May need to release the ContentProviderExternal
        try {
            Class<?> activityThreadClass = mMainThread.getClass();
            Method method = activityThreadClass.getDeclaredMethod("releaseProvider", Class.forName("android.content.IContentProvider"), boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(mMainThread, icp, false);
        } catch (Exception e) {
            Ln.e("Could not invoke releaseUnstableProvider", e);
            return false;
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void unstableProviderDied(Object icp) {
        try {
            Class<?> activityThreadClass = mMainThread.getClass();
            Method method = activityThreadClass.getDeclaredMethod("handleUnstableProviderDied", IBinder.class, boolean.class);
            method.setAccessible(true);
            Method asBinderMethod = icp.getClass().getMethod("asBinder");
            IBinder binder = (IBinder) asBinderMethod.invoke(icp);
            method.invoke(mMainThread, binder, true);
        } catch (Exception e) {
            Ln.e("Could not invoke unstableProviderDied", e);
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Override
    public void appNotRespondingViaProvider(Object icp) {
        try {
            Class<?> activityThreadClass = mMainThread.getClass();
            Method method = activityThreadClass.getDeclaredMethod("appNotRespondingViaProvider", IBinder.class);
            method.setAccessible(true);
            Method asBinderMethod = icp.getClass().getMethod("asBinder");
            IBinder binder = (IBinder) asBinderMethod.invoke(icp);
            method.invoke(mMainThread, binder);
        } catch (Exception e) {
            Ln.e("Could not invoke appNotRespondingViaProvider", e);
        }
    }

    protected int resolveUserIdFromAuthority(String auth) {
        try {
            Method method = ContentProvider.class.getDeclaredMethod("getUserIdFromAuthority", String.class, int.class);
            method.setAccessible(true);
            return (int) method.invoke(null, auth, getUserId());
        } catch (Exception e) {
            Ln.e("Could not invoke resolveUserIdFromAuthority", e);
            return getUserId();
        }
    }
}
