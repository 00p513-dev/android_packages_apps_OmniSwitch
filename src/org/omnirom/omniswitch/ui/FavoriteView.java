/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.omnirom.omniswitch.PackageManager;
import org.omnirom.omniswitch.SettingsActivity;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.Utils;
import org.omnirom.omniswitch.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FavoriteView extends GridView {
    private static final String TAG = "FavoriteView";
    private static final boolean DEBUG = false;

    protected SwitchConfiguration mConfiguration;
    private FavoriteListAdapter mFavoriteListAdapter;
    private boolean mTransparent;
    protected List<String> mFavoriteList;
    private SwitchManager mRecentsManager;
    protected Typeface mLabelFont;

    public class FavoriteListAdapter extends ArrayAdapter<String> {

        public FavoriteListAdapter(Context context, int resource,
                List<String> values) {
            super(context, R.layout.package_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PackageTextView item = null;
            if (convertView == null) {
                item = getPackageItemTemplate();
            } else {
                item = (PackageTextView) convertView;
            }
            String intent = getItem(position);

            PackageManager.PackageItem packageItem = PackageManager
                    .getInstance(mContext).getPackageItem(intent);
            item.setIntent(packageItem.getIntent());
            if (mConfiguration.mShowLabels) {
                item.setText(packageItem.getTitle());
            } else {
                item.setText("");
            }
            Drawable d = BitmapCache.getInstance(mContext).getPackageIconCached(getResources(), packageItem, mConfiguration);
            d.setBounds(0, 0, mConfiguration.mIconSizePx, mConfiguration.mIconSizePx);
            item.setCompoundDrawables(null, d, null, null);
            return item;
        }
    }

    public FavoriteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConfiguration = SwitchConfiguration.getInstance(mContext);
        mLabelFont = Utils.getAppLabelFont(mContext);
        mFavoriteList = new ArrayList<String>();
        mFavoriteListAdapter = new FavoriteListAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, mFavoriteList);
        setAdapter(mFavoriteListAdapter);
        updateLayout();
        setVerticalScrollBarEnabled(false);

        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                doOnCLickAction(position);
            }
        });

        setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                doOnLongClickAction(position, view);
                return true;
            }
        });
    }

    public void setTransparentMode(boolean value) {
        mTransparent = value;
    }

    public void setRecentsManager(SwitchManager recentsManager) {
        mRecentsManager = recentsManager;
    }

    protected PackageTextView getPackageItemTemplate() {
        PackageTextView item = new PackageTextView(mContext);
        if (mTransparent) {
            item.setTextColor(mContext.getResources().getColor(R.color.text_color_dark));
            item.setShadowLayer(5f, 0, 0, Color.BLACK);
        } else {
            item.setTextColor(mConfiguration.getCurrentTextTint(mConfiguration.getViewBackgroundColor()));
            item.setShadowLayer(mConfiguration.getShadowColorValue(), 0, 0, Color.BLACK);
        }
        item.setTextSize(mConfiguration.mLabelFontSize);
        item.setEllipsize(TextUtils.TruncateAt.END);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(getListItemParams());
        item.setPadding(0, mConfiguration.mIconBorderPx, 0, 0);
        item.setMaxLines(1);
        item.setTypeface(mLabelFont);
        if (mTransparent) {
            item.setBackgroundResource(R.drawable.ripple_dark);
        } else {
            item.setBackgroundResource(mConfiguration.mBgStyle == SwitchConfiguration.BgStyle.SOLID_LIGHT ? R.drawable.ripple_dark
                    : R.drawable.ripple_light);
        }
        return item;
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if (DEBUG) {
            Log.d(TAG, "updatePrefs " + key);
        }
        if (key != null && key.equals(SettingsActivity.PREF_SYSTEM_FONT)) {
            mLabelFont = Utils.getAppLabelFont(mContext);
        }
        if (key != null && key.equals(SettingsActivity.PREF_FAVORITE_APPS)) {
            updateFavoritesList();
        }
        if (key != null && Utils.isPrefKeyForForceUpdate(key)) {
            setAdapter(mFavoriteListAdapter);
        }
        if (key != null && key.equals(PackageManager.PACKAGES_UPDATED_TAG)) {
            mFavoriteListAdapter.notifyDataSetChanged();
        }
        updateLayout();
    }

    public void init() {
        updateFavoritesList();
    }

    private void updateLayout() {
        setColumnWidth(mConfiguration.mMaxWidth + mConfiguration.mIconBorderHorizontalPx);
        int dividerHeight = mConfiguration.calcVerticalDivider(getHeight());
        setVerticalSpacing(dividerHeight);
        requestLayout();
    }

    protected LinearLayout.LayoutParams getListItemParams() {
        return new LinearLayout.LayoutParams(mConfiguration.mMaxWidth,
                mConfiguration.getItemMaxHeight());
    }

    @Override
    public void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int dividerHeight = mConfiguration.calcVerticalDivider(h);
        setVerticalSpacing(dividerHeight);
    }

    private void updateFavoritesList() {
        Utils.updateFavoritesList(mContext, mConfiguration, mFavoriteList);
        if (DEBUG) {
            Log.d(TAG, "updateFavoritesList " + mFavoriteList);
        }
        mFavoriteListAdapter.notifyDataSetChanged();
    }

    protected void doOnCLickAction(int position) {
        String intent = mFavoriteList.get(position);
        if (mRecentsManager != null) {
            mRecentsManager.startIntentFromtString(intent, true);
        } else {
            SwitchManager.startIntentFromtString(mContext, intent);
        }
    }

    protected void doOnLongClickAction(int position, View view) {
        String intent = mFavoriteList.get(position);
        PackageManager.PackageItem packageItem = PackageManager
                .getInstance(mContext).getPackageItem(intent);
        handleLongPressFavorite(packageItem, view);
    }

    private void handleLongPressFavorite(final PackageManager.PackageItem packageItem, View view) {
        ContextMenuUtils.handleLongPressFavorite(mContext, packageItem, view,
                mRecentsManager, mFavoriteList);
    }
}
