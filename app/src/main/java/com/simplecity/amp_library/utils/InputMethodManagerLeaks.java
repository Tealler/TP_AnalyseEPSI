package com.simplecity.amp_library.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * Copied from: https://gist.github.com/pyricau/4df64341cc978a7de414
 */
public class InputMethodManagerLeaks {

    static class ReferenceCleaner
            implements MessageQueue.IdleHandler, View.OnAttachStateChangeListener,
            ViewTreeObserver.OnGlobalFocusChangeListener {

        private final InputMethodManager inputMethodManager;
        private final Field mHField;
        private final Field mServedViewField;
        private final Method finishInputLockedMethod;

        ReferenceCleaner(InputMethodManager inputMethodManager, Field mHField, Field mServedViewField,
                Method finishInputLockedMethod) {
            this.inputMethodManager = inputMethodManager;
            this.mHField = mHField;
            this.mServedViewField = mServedViewField;
            this.finishInputLockedMethod = finishInputLockedMethod;
        }

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            if (newFocus == null) {
                return;
            }
            if (oldFocus != null) {
                oldFocus.removeOnAttachStateChangeListener(this);
            }
            Looper.myQueue().removeIdleHandler(this);
            newFocus.addOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View v) {
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            v.removeOnAttachStateChangeListener(this);
            Looper.myQueue().removeIdleHandler(this);
            Looper.myQueue().addIdleHandler(this);
        }

        @Override
        public boolean queueIdle() {
            clearInputMethodManagerLeak();
            return false;
        }

        private void clearInputMethodManagerLeak() {
            try {
                final Object lock = mHField.get(inputMethodManager);
                if (lock == null) return;

                synchronizedInputMethodManagerLock(lock);
            } catch (Exception unexpected) {
                Log.e("IMMLeaks", "Unexpected reflection exception", unexpected);
            }
        }

            private final Object synchronizationLock = new Object();

            private void synchronizedInputMethodManagerLock() throws IllegalAccessException, InvocationTargetException {
                synchronized (synchronizationLock) {
                    View servedView = (View) mServedViewField.get(inputMethodManager);
                    if (servedView != null) {
                        handleServedView(servedView);
                    }
                }
            }

        private void handleServedView(View servedView) throws IllegalAccessException, InvocationTargetException {
            boolean servedViewAttached = servedView.getWindowVisibility() != View.GONE;

            if (servedViewAttached) {
                handleAttachedServedView(servedView);
            } else {
                handleDetachedServedView(servedView);
            }
        }

        private void handleAttachedServedView(View servedView) {
            servedView.removeOnAttachStateChangeListener(this);
            servedView.addOnAttachStateChangeListener(this);
        }

        private void handleDetachedServedView(View servedView) throws IllegalAccessException, InvocationTargetException {
            Activity activity = extractActivity(servedView.getContext());
            if (activity == null || activity.getWindow() == null) {
                finishInputLockedMethod.invoke(inputMethodManager);
            } else {
                handleDetachedActivity(activity);
            }
        }

        private void handleDetachedActivity(Activity activity) throws IllegalAccessException, InvocationTargetException {
            View decorView = activity.getWindow().peekDecorView();
            boolean windowAttached = decorView.getWindowVisibility() != View.GONE;
            if (!windowAttached) {
                finishInputLockedMethod.invoke(inputMethodManager);
            } else {
                decorView.requestFocusFromTouch();
            }
        }


        private Activity extractActivity(Context context) {
            while (true) {
                if (context instanceof Application) {
                    return null;
                } else if (context instanceof Activity) {
                    return (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    Context baseContext = ((ContextWrapper) context).getBaseContext();
                    // Prevent Stack Overflow.
                    if (baseContext == context) {
                        return null;
                    }
                    context = baseContext;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Fix for https://code.google.com/p/android/issues/detail?id=171190 .
     * <p>
     * When a view that has focus gets detached, we wait for the main thread to be idle and then
     * check if the InputMethodManager is leaking a view. If yes, we tell it that the decor view got
     * focus, which is what happens if you press home and come back from recent apps. This replaces
     * the reference to the detached view with a reference to the decor view.
     * <p>
     * Should be called from {@link Activity#onCreate(android.os.Bundle)} )}.
     */
    @SuppressLint("PrivateApi")
    public static void fixFocusedViewLeak(Application application) {

        // Still not fixed until android 23
        if (SDK_INT < KITKAT || SDK_INT > Build.VERSION_CODES.N_MR1) {
            return;
        }

        final InputMethodManager inputMethodManager =
                (InputMethodManager) application.getSystemService(INPUT_METHOD_SERVICE);

        final Field mServedViewField;
        final Field mHField;
        final Method finishInputLockedMethod;
        final Method focusInMethod;
        try {
            mServedViewField = InputMethodManager.class.getDeclaredField("mServedView");
            mHField = InputMethodManager.class.getDeclaredField("mServedView");
            finishInputLockedMethod = InputMethodManager.class.getDeclaredMethod("finishInputLocked");
            focusInMethod = InputMethodManager.class.getDeclaredMethod("focusIn", View.class);
        } catch (NoSuchMethodException | NoSuchFieldException unexpected) {
            Log.e("IMMLeaks", "Unexpected reflection exception", unexpected);
            return;
        }

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                ReferenceCleaner cleaner =
                        new ReferenceCleaner(inputMethodManager, mHField, mServedViewField,
                                finishInputLockedMethod);
                View rootView = activity.getWindow().getDecorView().getRootView();
                ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
                viewTreeObserver.addOnGlobalFocusChangeListener(cleaner);
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }
}