package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.genymobile.scrcpy.Ln;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class ApplicationContentResolver extends ContentResolver {
    private final ActivityThread mMainThread;

    public ApplicationContentResolver(Context context, ActivityThread mainThread) {
        super(context);
        mMainThread = Objects.requireNonNull(mainThread);
    }

    @Override
    protected IContentProvider acquireProvider(Context context, String auth) {
        return ServiceManager.getActivityManager().getContentProviderExternal(ContentProvider.getAuthorityWithoutUserId(auth), new Binder());
    }

    @Override
    protected IContentProvider acquireExistingProvider(Context context, String auth) {
        // Nunca é chamado, mas implementado para compatibilidade
        try {
            Method method = ActivityThread.class.getDeclaredMethod("acquireExistingProvider", Context.class, String.class, int.class, boolean.class);
            method.setAccessible(true);
            return (IContentProvider) method.invoke(mMainThread, context, ContentProvider.getAuthorityWithoutUserId(auth), resolveUserIdFromAuthority(auth), true);
        } catch (Exception e) {
            Ln.e("Could not invoke acquireExistingProvider", e);
            return null;
        }
    }

    @Override
    public boolean releaseProvider(IContentProvider provider) {
        // Talvez seja necessário liberar o ContentProviderExternal
        try {
            Method method = ActivityThread.class.getDeclaredMethod("releaseProvider", IContentProvider.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(mMainThread, provider, true);
        } catch (Exception e) {
            Ln.e("Could not invoke releaseProvider", e);
            return false;
        }
    }

    @Override
    protected IContentProvider acquireUnstableProvider(Context context, String auth) {
        // Atualizado para tratar chamadas do ClipboardManager
        return ServiceManager.getActivityManager().getContentProviderExternal(ContentProvider.getAuthorityWithoutUserId(auth), new Binder());
    }

    @Override
    public boolean releaseUnstableProvider(IContentProvider icp) {
        // Talvez seja necessário liberar o ContentProviderExternal
        try {
            Method method = ActivityThread.class.getDeclaredMethod("releaseProvider", IContentProvider.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(mMainThread, icp, false);
        } catch (Exception e) {
            Ln.e("Could not invoke releaseUnstableProvider", e);
            return false;
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void unstableProviderDied(IContentProvider icp) {
        try {
            Method method = ActivityThread.class.getDeclaredMethod("handleUnstableProviderDied", IBinder.class, boolean.class);
            method.setAccessible(true);
            method.invoke(mMainThread, icp.asBinder(), true);
        } catch (Exception e) {
            Ln.e("Could not invoke unstableProviderDied", e);
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Override
    public void appNotRespondingViaProvider(IContentProvider icp) {
        try {
            Method method = ActivityThread.class.getDeclaredMethod("appNotRespondingViaProvider", IBinder.class);
            method.setAccessible(true);
            method.invoke(mMainThread, icp.asBinder());
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
