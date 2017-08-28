/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.R;

public class FloatingTabsButton extends FloatingActionButton {
    private static final String TOO_MANY_TABS_SYMBOL = "∞";

    private TextPaint textPaint;
    private int tabCount;

    public FloatingTabsButton(Context context) {
        super(context);
        init();
    }

    public FloatingTabsButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingTabsButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        final Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        final int textSize = getResources().getDimensionPixelSize(R.dimen.tabs_button_text_size);

        textPaint = new TextPaint(paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSize);
    }

    public void updateTabsCount(int tabCount) {
        this.tabCount = tabCount;

        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        final FloatingActionButtonBehavior behavior = (FloatingActionButtonBehavior) params.getBehavior();

        final boolean shouldBeVisible = tabCount >= 2;

        if (behavior != null) {
            behavior.setEnabled(shouldBeVisible);
        }

        if (shouldBeVisible) {
            setVisibility(View.VISIBLE);
            invalidate();
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int x = canvas.getWidth() / 2;
        final int y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));

        final String text = tabCount <= 9 ? String.valueOf(tabCount) : TOO_MANY_TABS_SYMBOL;

        canvas.drawText(text, x, y, textPaint);
    }
}
