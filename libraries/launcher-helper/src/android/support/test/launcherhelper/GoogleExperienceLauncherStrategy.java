/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.test.launcherhelper;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;
import android.util.Log;
import android.widget.TextView;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implementation of {@link ILauncherStrategy} to support Google experience launcher
 */
public class GoogleExperienceLauncherStrategy implements ILauncherStrategy {

    private static final String LOG_TAG = GoogleExperienceLauncherStrategy.class.getSimpleName();
    private static final String LAUNCHER_PKG = "com.google.android.googlequicksearchbox";
    private static final BySelector APPS_CONTAINER = By.res(LAUNCHER_PKG, "all_apps_container");
    private static final BySelector WIDGETS_CONTAINER = By.res(LAUNCHER_PKG, "widgets_list_view");
    private static final BySelector WORKSPACE = By.res(LAUNCHER_PKG, "workspace");
    private static final BySelector HOTSEAT = By.res(LAUNCHER_PKG, "hotseat");
    private UiDevice mDevice;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUiDevice(UiDevice uiDevice) {
        mDevice = uiDevice;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() throws UiObjectNotFoundException {
        // if we see hotseat, assume at home screen already
        if (!mDevice.hasObject(HOTSEAT)) {
            mDevice.pressHome();
            // ensure launcher is shown
            if (!mDevice.wait(Until.hasObject(By.res(LAUNCHER_PKG, "hotseat")), 5000)) {
                // HACK: dump hierarchy to logcat
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    mDevice.dumpWindowHierarchy(baos);
                    Log.d(LOG_TAG, baos.toString());
                    baos.flush();
                    baos.close();
                } catch (IOException ioe) {
                    Log.e(LOG_TAG, "error dumping XML to logcat", ioe);
                }
                Assert.fail("Failed to open launcher");
            }
            mDevice.waitForIdle();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiObject2 openAllApps(boolean reset) throws UiObjectNotFoundException {
        // if we see all apps container, skip the opening step
        if (!mDevice.hasObject(APPS_CONTAINER)) {
            open();
            // taps on the "apps" button at the bottom of the screen
            mDevice.findObject(By.desc("Apps")).click();
            // wait until hotseat disappears, so that we know that we are no longer on home screen
            mDevice.wait(Until.gone(HOTSEAT), 2000);
            mDevice.waitForIdle();
        }
        UiObject2 allAppsContainer = mDevice.wait(Until.findObject(APPS_CONTAINER), 2000);
        Assert.assertNotNull("openAllApps: did not find all apps container", allAppsContainer);
        if (reset) {
            CommonLauncherHelper.getInstance(mDevice).scrollBackToBeginning(
                    allAppsContainer, Direction.reverse(getAllAppsScrollDirection()));
        }
        return allAppsContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getAllAppsScrollDirection() {
        return Direction.DOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiObject2 openAllWidgets(boolean reset) throws UiObjectNotFoundException {
        if (!mDevice.hasObject(WIDGETS_CONTAINER)) {
            open();
            // trigger the wallpapers/widgets/settings view
            mDevice.pressMenu();
            mDevice.waitForIdle();
            mDevice.findObject(By.res(LAUNCHER_PKG, "widget_button")).click();
        }
        UiObject2 allWidgetsContainer = mDevice.wait(Until.findObject(WIDGETS_CONTAINER), 2000);
        Assert.assertNotNull("openAllWidgets: did not find all widgets container",
                allWidgetsContainer);
        if (reset) {
            CommonLauncherHelper.getInstance(mDevice).scrollBackToBeginning(
                    allWidgetsContainer, Direction.reverse(getAllWidgetsScrollDirection()));
        }
        return allWidgetsContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getAllWidgetsScrollDirection() {
        return Direction.DOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean launch(String appName, String packageName) throws UiObjectNotFoundException {
        BySelector app = By.res(LAUNCHER_PKG, "icon").clazz(TextView.class).desc(appName);
        return CommonLauncherHelper.getInstance(mDevice).launchApp(this, app, packageName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSupportedLauncherPackage() {
        return LAUNCHER_PKG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BySelector getAllAppsSelector() {
        return APPS_CONTAINER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BySelector getAllWidgetsSelector() {
        return WIDGETS_CONTAINER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BySelector getWorkspaceSelector() {
        return WORKSPACE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getWorkspaceScrollDirection() {
        return Direction.RIGHT;
    }
}
