/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) [2017] [velli20]
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.velli20.materialunixgraph;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;


import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class Graph extends View {

    private static final int TEXT_LABEL_SIZE = 12;

    private static final long ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final long ONE_MINUTE_IN_MILLIS = 1000 * 60;
    private float mScale;

    private Paint mGraphFramePaint = new Paint();
    private Paint mBackgroundPaint = new Paint();

    private TextPaint mVerticalAxisLabelPaint = new TextPaint();
    private TextPaint mHorizontalAxisLabelPaint = new TextPaint();
    private TextPaint mTitlePaint = new TextPaint();
    private TextPaint mSubtitlePaint = new TextPaint();

    private boolean mDrawHorizontalAxisLabels = true;
    private boolean mDrawVerticalAxisLabels = true;
    private boolean mDrawVerticalAxisLabelLines = true;
    private boolean mUse24HourFormat = true;

    private int mWidth;
    private int mHeight;
    private int mMaxVerticalAxisLabelCount = 0;

    private float mMaxVerticalAxisValue;
    private float mMinVerticalAxisValue;

    private ArrayList<Long> mDateAxisTicks = null;
    private String mTitle = "";
    private String mSubtitle = "";
    private String mLegendSubtitle = "";
    private String mLegendSubsubtitle = "";
    private String mDataCopyright = "";


    private long mDateStart;
    private long mDateEnd;

    private String mUnitLabel;

    public Graph(Context context) {
        super(context);
        init(context, null);
    }

    public Graph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Graph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Graph(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs){
        int graphFrameColor = Color.parseColor("#e0e0e0");
        int verticalAxisLabelColor = Color.rgb (239, 239, 239);
        int horizontalAxisLabelsColor = Color.rgb (239, 239, 239);

        int verticalAxisLabelSize = (int) getDpValue(TEXT_LABEL_SIZE);
        int horizontalAxisLabelSize = (int) getDpValue(TEXT_LABEL_SIZE);
        int graphFrameStrokeWidth = (int) getDpValue(1f);

        if(attrs != null && context != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Graph, 0, 0);

            try {
                mDrawHorizontalAxisLabels = attributes.getBoolean(R.styleable.Graph_showHorizontalAxisLabels, true);
                mDrawVerticalAxisLabels = attributes.getBoolean(R.styleable.Graph_showVerticalAxisLabels, true);
                mDrawVerticalAxisLabelLines = attributes.getBoolean(R.styleable.Graph_showVerticalAxisLines, true);
                mUse24HourFormat = attributes.getBoolean(R.styleable.Graph_drawTimeLabelsIn24hourMode, true);

                graphFrameColor = attributes.getColor(R.styleable.Graph_graphFrameColor, graphFrameColor);
                verticalAxisLabelColor = attributes.getColor(R.styleable.Graph_verticalAxisLabelColor, verticalAxisLabelColor);
                horizontalAxisLabelsColor =  attributes.getColor(R.styleable.Graph_horizontalAxisLabelColor, horizontalAxisLabelsColor);

                verticalAxisLabelSize = attributes.getDimensionPixelSize(R.styleable.Graph_verticalAxisLabelTextSize, verticalAxisLabelSize);
                horizontalAxisLabelSize = attributes.getDimensionPixelSize(R.styleable.Graph_horizontalAxisLabelTextSize, horizontalAxisLabelSize);
                graphFrameStrokeWidth = attributes.getDimensionPixelSize(R.styleable.Graph_graphFrameStrokeWidth, graphFrameStrokeWidth);

                mMaxVerticalAxisLabelCount = attributes.getInteger(R.styleable.Graph_maxVerticalAxisCount, 0);
                mMaxVerticalAxisValue = attributes.getFloat(R.styleable.Graph_maxVerticalAxisValue, 0);
                mMinVerticalAxisValue = attributes.getFloat(R.styleable.Graph_minVerticalAxisValue, 0);

                mUnitLabel = attributes.getString(R.styleable.Graph_verticalAxisValueLabel);

            } finally {
                attributes.recycle();
            }
        }

        mScale = getResources().getDisplayMetrics().density;

        /* Paint that is used to draw vertical and horizontal axis lines */
        mGraphFramePaint.setStyle(Paint.Style.STROKE);
        mGraphFramePaint.setAntiAlias(true);
        mGraphFramePaint.setColor(graphFrameColor);
        mGraphFramePaint.setStrokeWidth(graphFrameStrokeWidth);

        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(Color.argb(125, 51, 51, 51));

        /* Text paint for drawing vertical labels */
        mVerticalAxisLabelPaint.setColor(verticalAxisLabelColor);
        mVerticalAxisLabelPaint.setAntiAlias(true);
        mVerticalAxisLabelPaint.setTextSize(verticalAxisLabelSize);

        mTitlePaint.setColor(verticalAxisLabelColor);
        mTitlePaint.setAntiAlias(true);
        mTitlePaint.setTextSize(verticalAxisLabelSize*1.5f);
        mTitlePaint.setFakeBoldText(true);

        mSubtitlePaint.setColor(verticalAxisLabelColor);
        mSubtitlePaint.setAntiAlias(true);
        mSubtitlePaint.setTextSize(verticalAxisLabelSize*1.2f);
        mSubtitlePaint.setFakeBoldText(true);

        /* Text paint for drawing horizontal labels */
        mHorizontalAxisLabelPaint.setColor(horizontalAxisLabelsColor);
        mHorizontalAxisLabelPaint.setAntiAlias(true);
        mHorizontalAxisLabelPaint.setTextSize(horizontalAxisLabelSize);

        if(!isInEditMode()) {
            Typeface roboto = Typeface.createFromAsset(getResources().getAssets(), "Roboto-Regular.ttf");
            if(roboto != null) {
                mHorizontalAxisLabelPaint.setTypeface(roboto);
                mVerticalAxisLabelPaint.setTypeface(roboto);
            }
        }

        setWillNotDraw(false);
    }

    public float getDpValue(float value) {
        return mScale * value;
    }

    public float getVerticalAxisLabelPadding() { return getDpValue(20F); }

    public float getHorizontalAxisLabelPadding() { return mHorizontalAxisLabelPaint.getTextSize() * 1.5f; }

    public float getMaxVerticalAxisValue() { return mMaxVerticalAxisValue; }

    public float getMinVerticalAxisValue() { return mMinVerticalAxisValue; }

    public Paint getGraphFramePaint() { return mGraphFramePaint; }

    public TextPaint getVerticalAxisLabelPaint() { return mVerticalAxisLabelPaint; }

    public TextPaint getHorizontalAxisLabelPaint() { return mHorizontalAxisLabelPaint; }

    public long getGraphStartDate() { return mDateStart; }

    public long getGraphEndDate() { return mDateEnd; }

    public boolean getDrawHorizontalAxisLabels() { return mDrawHorizontalAxisLabels; }

    public boolean getDrawVerticalAxisLabels() { return mDrawVerticalAxisLabels; }

    public boolean getUse24hourFormat() { return mUse24HourFormat; }

    public boolean getDrawVerticalAxisLines() { return mDrawVerticalAxisLabelLines; }

    public ArrayList<Long> getDateAxisTicks() { return mDateAxisTicks; }

    public String getTitle() { return mTitle; }
    public String getSubtitle() { return mSubtitle; }
    public String getLegendSubtitle() { return mLegendSubtitle; }
    public String getDataCopyright() { return mDataCopyright; }
    public String getLegendSubsubtitle() { return mLegendSubsubtitle; }

    public void setMaxVerticalAxisValue(float max) {
        mMaxVerticalAxisValue = max;
        invalidate();
    }

    public void setMinVerticalAxisValue(float min) {
        mMinVerticalAxisValue = min;
        invalidate();
    }

    public void setStartDate(long startDateInMillis) {
        mDateStart = startDateInMillis;
        invalidate();
    }

    public void setEndDate(long endDateInMillis) {
        mDateEnd = endDateInMillis;
        invalidate();
    }

    public void setUnitLabel(String unitLabel) {
        mUnitLabel = unitLabel;
        invalidate();
    }

    public void setDrawHorizontalAxisLabels(boolean enabled) {
        mDrawHorizontalAxisLabels = enabled;
        invalidate();
    }

    public void setDrawVerticalAxisLabels(boolean enabled) {
        mDrawVerticalAxisLabels = enabled;
        invalidate();
    }

    public void setDrawVerticalAxisLabelLines(boolean enabled) {
        mDrawVerticalAxisLabelLines = enabled;
        invalidate();
    }

    public void setUse24HourFormat(boolean use24hourFormat) {
        mUse24HourFormat = use24hourFormat;
        invalidate();
    }

    public void setHorizontalLabelTextColor(int color) {
        mHorizontalAxisLabelPaint.setColor(color);
        invalidate();
    }

    public void setHorizontalLabelTextSize(int size) {
        mHorizontalAxisLabelPaint.setTextSize(getDpValue(size));
        invalidate();
    }

    public void setVerticalLabelTextColor(int color) {
        mVerticalAxisLabelPaint.setColor(color);
        invalidate();
    }

    public void setVerticalLabelTextSize(int size) {
        mVerticalAxisLabelPaint.setTextSize(getDpValue(size));
        invalidate();
    }

    public void setGraphFrameColor(int color) {
        mGraphFramePaint.setColor(color);
        invalidate();
    }

    public void setMaximumVerticalLabelCount(int maxCount) {
        mMaxVerticalAxisLabelCount = maxCount;
        invalidate();
    }

    public void setDateAxisTicks(ArrayList<Long> newList) {
        mDateAxisTicks = new ArrayList<Long>(newList);
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setSubtitle(String subtitle) {
        mSubtitle = subtitle;
    }

    public void setLegendSubtitle(String subtitle) {
        mLegendSubtitle = subtitle;
    }

    public void setDataCopyright(String title) {
        mDataCopyright = title;
    }

    public void setLegendSubsubtitle(String title) { mLegendSubsubtitle = title; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float leftPadding = getPaddingLeft();
        float bottomPadding = getPaddingBottom();
        float topPadding = getPaddingTop();

        float verticalAxisLabelTextSize = mVerticalAxisLabelPaint.getTextSize();
        float graphFrameStrokeWidth = mGraphFramePaint.getStrokeWidth();

        float cyStart = topPadding + verticalAxisLabelTextSize;
        float cyEnd = mHeight - (getHorizontalAxisLabelPadding() + bottomPadding);

        float cxStart = leftPadding;
        float cxEnd = mWidth - (getPaddingRight() + graphFrameStrokeWidth);
        float graphCxStart = cxStart + getVerticalAxisLabelPadding();

        /* Draw the vertical axis labels */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(0, 0, mWidth, mHeight, getDpValue(5F), getDpValue(5F), mBackgroundPaint);
        }
        if(mDrawVerticalAxisLabels || mDrawVerticalAxisLabelLines) {
            drawVerticalAxisLabels(canvas, cyStart, cyEnd, cxStart, cxEnd);
        }

        /* Draw horizontal axis line */
        canvas.drawLine(graphCxStart, cyEnd, cxEnd, cyEnd, mGraphFramePaint);

        if(mDrawHorizontalAxisLabels) {
            /* Draw the horizontal axis labels */
            drawHorizontalAxisLabels(canvas, cyStart, cyEnd, graphCxStart, cxEnd);
        }


        Float subtitleX = (cxEnd-mSubtitlePaint.measureText(mSubtitle)-getDpValue(5));
        canvas.drawText(
                mSubtitle,
                subtitleX,
                getDpValue(29F),
                mSubtitlePaint);
        float titleMeasuredWidth = mTitlePaint.measureText(mTitle);
        Float titleX = (cxEnd-cxStart)/2-titleMeasuredWidth/2;
        Float titleRightEdge = titleX+titleMeasuredWidth;
        if (titleRightEdge>subtitleX) {
            titleX = subtitleX-titleMeasuredWidth-getDpValue(4F);
        }
        float legendSubtitleX = (cxEnd-mSubtitlePaint.measureText(mLegendSubtitle)-getDpValue(11));
        canvas.drawText(
                mTitle,
                titleX,
                getDpValue(29F),
                mTitlePaint);
        canvas.drawText(
                mLegendSubtitle,
                legendSubtitleX,
                cyEnd-getDpValue(30F),
                mSubtitlePaint);
        canvas.drawText(
                mLegendSubsubtitle,
                legendSubtitleX,
                cyEnd-getDpValue(15F),
                mSubtitlePaint);
        canvas.drawText(
                mDataCopyright,
                cxStart+getDpValue(25F),
                cyEnd-getDpValue(13F),
                mSubtitlePaint);
    }

    private void drawVerticalAxisLabels(Canvas canvas, float cyStart, float cyEnd, float cxStart, float cxEnd) {
        float graphFrameStrokeWidth = mGraphFramePaint.getStrokeWidth();
        float labelTextSize = mVerticalAxisLabelPaint.getTextSize() - mVerticalAxisLabelPaint.descent();

        float lineCy = 0;
        float lineCyStart = (cyStart);
        float labelCyStart = cyStart + (labelTextSize /2) - (graphFrameStrokeWidth / 2);
        float labelCxPadding = getVerticalAxisLabelPadding();

        /* Required space for the label including offset */
        float labelSpace = (labelTextSize);
        float labelExtraPad = 0;
        int labelMaxCount = (int) ((cyEnd - cyStart) / labelSpace);

        if(labelMaxCount > mMaxVerticalAxisLabelCount && mMaxVerticalAxisLabelCount > 0) {
            labelSpace = (cyEnd - cyStart) / mMaxVerticalAxisLabelCount;
            labelMaxCount = mMaxVerticalAxisLabelCount;
        } else {
            labelSpace = (labelTextSize * 2);
            labelMaxCount = (int) ((cyEnd - cyStart) / labelSpace);
            labelExtraPad = (((cyEnd - cyStart) % labelSpace) / labelMaxCount);
        }

        long labelsXOffset = (long) getDpValue(10F);
        long labelsYOffset = (long) getDpValue(-5F);
        for (int i = 0; i <= (labelMaxCount); i++) {
            /* Calculate y-axis value to draw */
            float valueToDraw = (lineCy - 0) * (mMinVerticalAxisValue - mMaxVerticalAxisValue) / (cyEnd - cyStart) + mMaxVerticalAxisValue;
            String label = String.valueOf((int) valueToDraw);

            /* If the label to draw is first one, then apply also unit label */
            if (i == 0 && mUnitLabel != null && mDrawVerticalAxisLabelLines) {
                float labelPadding = mVerticalAxisLabelPaint.measureText(mUnitLabel)+getDpValue(17F);
                if (mUnitLabel.startsWith(" ")) {
                    label += mUnitLabel;
                } else {
                    label += " "+mUnitLabel;
                }
                canvas.drawLine(labelPadding + cxStart+labelsXOffset, lineCyStart + lineCy, cxEnd, lineCyStart + lineCy, mGraphFramePaint);
            } else if (mDrawVerticalAxisLabelLines && i != labelMaxCount) {
                /* Skip last line */
                canvas.drawLine(labelCxPadding + cxStart, lineCyStart + lineCy, cxEnd, lineCyStart + lineCy, mGraphFramePaint);
            }
            if (mDrawVerticalAxisLabels) {
                canvas.drawText(label, cxStart+labelsXOffset, labelCyStart + lineCy+labelsYOffset, mVerticalAxisLabelPaint);
            }

            lineCy += (labelSpace + labelExtraPad);

        }
    }



    private void drawHorizontalAxisLabels(Canvas canvas, float cyStart, float cyEnd, float cxStart, float cxEnd) {
        float cy = (mHeight - getPaddingBottom());
        float indicatorLineCyStart = cyEnd;
        float indicatorLineCyEnd = cyEnd + Math.min(getDpValue(4), (getHorizontalAxisLabelPadding()/2));

        long timeInterval = mDateEnd - mDateStart;

        /* Formatter string used to convert date in millis to date time string */
        String timeFormat;

        /* Calculate the tim resolution (days, hours, minutes or milliseconds */
        int dayCount = (int)(timeInterval / ONE_DAY_IN_MILLIS);

        /* Label count to draw*/
        int horizontalLabelCount;

        if(dayCount > 7) {
            /* Draw label every one day in shorten weekdays including date*/
            timeFormat = "EE dd.MM";
            horizontalLabelCount = dayCount;
        } else if(dayCount > 1){
            /* Draw label every one day in shorten weekdays*/
            timeFormat = "EE";
            horizontalLabelCount = dayCount;
        } else{

            int minuteCount = (int)(timeInterval / ONE_MINUTE_IN_MILLIS);

            if(minuteCount > 1) {
                /* Draw label by minutes */
                timeFormat = (!mUse24HourFormat ? "hh:mm a" : "HH:mm");
                horizontalLabelCount = minuteCount;
            } else {
                /* Draw label by milliseconds */
                timeFormat = (!mUse24HourFormat ? "hh:mm:SS a" : "HH:mm:SS");
                horizontalLabelCount = (int)(timeInterval % ONE_MINUTE_IN_MILLIS);
            }
        }

        float labelWidth = mHorizontalAxisLabelPaint.measureText(new SimpleDateFormat(timeFormat, Locale.getDefault()).format(mDateEnd));
        float requiredLabelSpace = (labelWidth * 1.5f);

        /* Available space to draw the labels */
        float horizontalSpace = (cxEnd - cxStart) ;

        /* Remainder of the available space */
        float horizontalLabelExtraPad;

         /* Check how many labels fits on horizontal axis */
        int labelsFitsOnHorizontalAxis = (int)(horizontalSpace / requiredLabelSpace);

        if(horizontalLabelCount > labelsFitsOnHorizontalAxis) {
            /* All time labels wont fit on axis. Lets reduce that count */
            horizontalLabelCount = (int)(horizontalSpace  / requiredLabelSpace);
            horizontalLabelExtraPad = (((horizontalSpace % requiredLabelSpace) / horizontalLabelCount));
        } else {
            /* All time labels fits with given date delta */
            requiredLabelSpace = horizontalSpace / horizontalLabelCount;
            horizontalLabelExtraPad = ((horizontalSpace % requiredLabelSpace) / horizontalLabelCount);
        }
        String timeLabel;
        if (mDateAxisTicks == null) {
            if (horizontalLabelCount > 0) {

                float labelCx = cxStart + (requiredLabelSpace / 2) + horizontalLabelExtraPad;

                for (int i = 0; i < horizontalLabelCount; i++) {

                    if (labelCx < horizontalSpace + cxStart) {
                        long time = (long) ((labelCx - cxStart) * (mDateEnd - mDateStart) / (cxEnd - cxStart) + mDateStart);

                        timeLabel = new SimpleDateFormat(timeFormat, Locale.getDefault()).format(time);
                        canvas.drawText(timeLabel, labelCx - (labelWidth / 2), cy, mHorizontalAxisLabelPaint);
                        canvas.drawLine(labelCx, indicatorLineCyStart, labelCx, indicatorLineCyEnd, mGraphFramePaint);

                        labelCx += (requiredLabelSpace + horizontalLabelExtraPad);
                    }

                }
            }
        } else {
            long labelsXOffset = 20;
            long labelsYOffset = -10;

            for (int i = 0; i < mDateAxisTicks.size(); ++i) {
                long time = mDateAxisTicks.get(i);
                float labelCx = ((time - mDateStart) * (cxEnd - cxStart) / (mDateEnd - mDateStart) + cxStart);
                long timeInSeconds = time/1000L;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:mm");
                    timeLabel = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
                            .withNano(0).plusSeconds(timeInSeconds)
                            .format(outputFormatter);
                    canvas.drawText(timeLabel, labelCx - (labelWidth / 2)+labelsXOffset, cy+labelsYOffset, mHorizontalAxisLabelPaint);
                    canvas.drawLine(labelCx, indicatorLineCyStart, labelCx, indicatorLineCyEnd, mGraphFramePaint);
                }
            }
        }
    }
}
