package com.genymobile.scrcpy.wrappers;

import com.genymobile.scrcpy.FakeContext;
import com.genymobile.scrcpy.Ln;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ActivityManager {

    private final Object manager;
    private Method getContentProviderExternalMethod;
    private boolean getContentProviderExternalMethodNewVersion = true;
    private Method removeContentProviderExternalMethod;
    private Method startActivityAsUserMethod;
    private Method forceStopPackageMethod;

    public static ActivityManager create() {
        try {
            // Em versões antigas do Android, o ActivityManager não é exposto via AIDL,
            // então usamos ActivityManagerNative.getDefault()
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = activityManagerNativeClass.getDeclaredMethod("getDefault");
            Object am = getDefaultMethod.invoke(null);
            return new ActivityManager(am);
        } catch (ClassNotFoundException e) {
            // Em versões mais recentes, usamos ActivityManager.getService()
            try {
                Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                Method getServiceMethod = activityManagerClass.getDeclaredMethod("getService");
                Object am = getServiceMethod.invoke(null);
                return new ActivityManager(am);
            } catch (Exception ex) {
                throw new AssertionError("Could not get ActivityManager", ex);
            }
        } catch (Exception e) {
            throw new AssertionError("Could not get ActivityManager", e);
        }
    }

    private ActivityManager(Object manager) {
        this.manager = manager;
    }

    public Object getContentProviderExternal(String name, IBinder token) {
        try {
            Method method = getGetContentProviderExternalMethod();
            Object[] args;
            if (getContentProviderExternalMethodNewVersion) {
                // Nova versão
                args = new Object[]{name, FakeContext.ROOT_UID, token, null};
            } else {
                // Versão antiga
                args = new Object[]{name, FakeContext.ROOT_UID, token};
            }
            // ContentProviderHolder providerHolder = getContentProviderExternal(...);
            Object providerHolder = method.invoke(manager, args);
            if (providerHolder == null) {
                return null;
            }
            // Obter o campo 'provider' de providerHolder
            Field providerField = providerHolder.getClass().getDeclaredField("provider");
            providerField.setAccessible(true);
            return providerField.get(providerHolder);
        } catch (Exception e) {
            Ln.e("Could not invoke getContentProviderExternal", e);
            return null;
        }
    }

    private ContentProvider getContentProviderExternalInternal(String name, IBinder token) {
        Object provider = getContentProviderExternal(name, token);
        return new ContentProvider(this, provider, name, token);
    }

    public void removeContentProviderExternal(String name, IBinder token) {
        try {
            Method method = getRemoveContentProviderExternalMethod();
            method.invoke(manager, name, token);
        } catch (Exception e) {
            Ln.e("Could not invoke removeContentProviderExternal", e);
        }
    }

    public ContentProvider createSettingsProvider() {
        return getContentProviderExternalInternal("settings", new Binder());
    }

    private Method getGetContentProviderExternalMethod() throws NoSuchMethodException {
        if (getContentProviderExternalMethod == null) {
            Class<?> iActivityManagerClass = manager.getClass();
            try {
                // Tentar método com assinatura mais recente
                getContentProviderExternalMethod = iActivityManagerClass.getMethod(
                        "getContentProviderExternal",
                        String.class,
                        int.class,
                        IBinder.class,
                        String.class);
                getContentProviderExternalMethodNewVersion = true;
            } catch (NoSuchMethodException e) {
                // Usar método com assinatura antiga
                getContentProviderExternalMethod = iActivityManagerClass.getMethod(
                        "getContentProviderExternal",
                        String.class,
                        int.class,
                        IBinder.class);
                getContentProviderExternalMethodNewVersion = false;
            }
        }
        return getContentProviderExternalMethod;
    }

    private Method getRemoveContentProviderExternalMethod() throws NoSuchMethodException {
        if (removeContentProviderExternalMethod == null) {
            Class<?> iActivityManagerClass = manager.getClass();
            removeContentProviderExternalMethod = iActivityManagerClass.getMethod(
                    "removeContentProviderExternal",
                    String.class,
                    IBinder.class);
        }
        return removeContentProviderExternalMethod;
    }

    private Method getStartActivityAsUserMethod() throws NoSuchMethodException, ClassNotFoundException {
        if (startActivityAsUserMethod == null) {
            Class<?> iApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            Class<?> profilerInfoClass = Class.forName("android.app.ProfilerInfo");
            startActivityAsUserMethod = manager.getClass()
                    .getMethod("startActivityAsUser",
                            iApplicationThreadClass,
                            String.class,
                            Intent.class,
                            String.class,
                            IBinder.class,
                            String.class,
                            int.class,
                            int.class,
                            profilerInfoClass,
                            Bundle.class,
                            int.class);
        }
        return startActivityAsUserMethod;
    }

    @SuppressWarnings("ConstantConditions")
    public int startActivity(Intent intent) {
        try {
            Method method = getStartActivityAsUserMethod();
            return (int) method.invoke(
                    /* this */ manager,
                    /* caller */ null,
                    /* callingPackage */ FakeContext.PACKAGE_NAME,
                    /* intent */ intent,
                    /* resolvedType */ null,
                    /* resultTo */ null,
                    /* resultWho */ null,
                    /* requestCode */ 0,
                    /* startFlags */ 0,
                    /* profilerInfo */ null,
                    /* bOptions */ null,
                    /* userId */ /* UserHandle.USER_CURRENT */ -2);
        } catch (Throwable e) {
            Ln.e("Could not invoke startActivity", e);
            return 0;
        }
    }

    private Method getForceStopPackageMethod() throws NoSuchMethodException {
        if (forceStopPackageMethod == null) {
            forceStopPackageMethod = manager.getClass().getMethod("forceStopPackage", String.class, int.class);
        }
        return forceStopPackageMethod;
    }

    public void forceStopPackage(String packageName) {
        try {
            Method method = getForceStopPackageMethod();
            method.invoke(manager, packageName, /* userId */ /* UserHandle.USER_CURRENT */ -2);
        } catch (Throwable e) {
            Ln.e("Could not invoke forceStopPackage", e);
        }
    }
}
