/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

import androidx.slice.Slice;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceBuilderUtilsTest {

    private final String KEY = "KEY";
    private final String TITLE = "title";
    private final String SUMMARY = "summary";
    private final String SCREEN_TITLE = "screen title";
    private final String FRAGMENT_NAME = "fragment name";
    private final int ICON = 1234; // I declare a thumb war
    private final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private final Class PREF_CONTROLLER = FakeToggleController.class;
    private final Class PREF_CONTROLLER2 = FakeContextOnlyPreferenceController.class;

    private final String INTENT_PATH = SettingsSlicesContract.PATH_SETTING_INTENT + "/" + KEY;
    private final String ACTION_PATH = SettingsSlicesContract.PATH_SETTING_ACTION + "/" + KEY;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private ArgumentCaptor<Pair<Integer, Object>> mLoggingArgumentCatpor;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mLoggingArgumentCatpor = ArgumentCaptor.forClass(Pair.class);
    }

    @Test
    public void testBuildSlice_returnsMatchingSlice() {
        Slice slice = SliceBuilderUtils.buildSlice(mContext, getDummyData());

        assertThat(slice).isNotNull(); // TODO improve test for Slice content
    }

    @Test
    public void testSliderSlice_returnsSeekBarSlice() {
        final SliceData data = getDummyData(FakeSliderController.class);
        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        assertThat(slice).isNotNull();
    }

    @Test
    public void testUriBuilder_oemAuthority_intentPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(INTENT_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(INTENT_PATH, false);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_oemAuthority_actionPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, false);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_platformAuthority_intentPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, true);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testUriBuilder_platformAuthority_actionPath_returnsValidSliceUri() {
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(ACTION_PATH)
                .build();

        final Uri actualUri = SliceBuilderUtils.getUri(ACTION_PATH, true);

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void testGetPreferenceController_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext, getDummyData());

        assertThat(controller).isInstanceOf(FakeToggleController.class);
    }

    @Test
    public void testGetPreferenceController_contextOnly_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext, getDummyData(PREF_CONTROLLER2));

        assertThat(controller).isInstanceOf(FakeContextOnlyPreferenceController.class);
    }

    @Test
    public void testDynamicSummary_returnsSliceSummary() {
        final SliceData data = getDummyData();
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(data.getSummary());
    }

    @Test
    public void testDynamicSummary_returnsFragmentSummary() {
        final SliceData data = getDummyData((String) null);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void testDynamicSummary_returnsSliceEmptyString() {
        final SliceData data = getDummyData((String) null);
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void testDynamicSummary_placeHolderString_returnsEmptyString() {
        final SliceData data = getDummyData(mContext.getString(R.string.summary_placeholder));
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void testDynamicSummary_sliceDataAndFragmentPlaceholder_returnsSliceEmptyString() {
        final String summaryPlaceholder = mContext.getString(R.string.summary_placeholder);
        final SliceData data = getDummyData(summaryPlaceholder);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        doReturn(summaryPlaceholder).when(controller).getSummary();

        CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo("");
    }

    @Test
    public void summaryText_bothDynamicAndStaticSummary_dynamicSummaryReturned() {
        SliceData data = getDummyData("bad_summary");
        FakePreferenceController controller = spy(new FakePreferenceController(mContext, KEY));
        String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getPathData_splitsIntentUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isFalse();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test
    public void getPathData_splitsActionUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isTrue();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPathData_noKey_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();

        SliceBuilderUtils.getPathData(uri);
    }

    @Test
    public void getPathData_extraArg_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isTrue();
        assertThat(pathPair.second).isEqualTo(KEY + "/" + KEY);
    }

    @Test
    public void testUnsupportedSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class.getName());
        Settings.System.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_UNSUPPORTED);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNotNull();
    }

    @Test
    public void testDisabledForUserSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class.getName());
        Settings.System.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_FOR_USER);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNotNull();
    }

    @Test
    public void testDisabledDependententSettingSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class.getName());
        Settings.System.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());
        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        assertThat(slice).isNotNull();
    }

    @Test
    public void testUnavailableUnknownSlice_validTitleSummary() {
        final SliceData data = getDummyData(FakeUnavailablePreferenceController.class.getName());
        Settings.System.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.UNAVAILABLE_UNKNOWN);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext), eq(MetricsEvent.ACTION_SETTINGS_SLICE_REQUESTED),
                        mLoggingArgumentCatpor.capture());

        final Pair<Integer, Object> capturedLoggingPair = mLoggingArgumentCatpor.getValue();

        assertThat(capturedLoggingPair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(capturedLoggingPair.second)
                .isEqualTo(data.getKey());
        assertThat(slice).isNotNull();
    }

    private SliceData getDummyData() {
        return getDummyData(PREF_CONTROLLER, SUMMARY);
    }

    private SliceData getDummyData(String summary) {
        return getDummyData(PREF_CONTROLLER, summary);
    }

    private SliceData getDummyData(Class prefController) {
        return getDummyData(prefController, SUMMARY);
    }

    private SliceData getDummyData(Class prefController, String summary) {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(summary)
                .setScreenTitle(SCREEN_TITLE)
                .setIcon(ICON)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(prefController.getName())
                .build();
    }
}
