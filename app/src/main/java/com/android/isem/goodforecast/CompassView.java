package com.android.isem.goodforecast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** CompassView custom view */
public class CompassView extends View {

    private float direction;

    public CompassView(Context context) {
        super(context);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int mHeight = hSpecSize;
        int mWidth = wSpecSize;

        if (hSpecMode == MeasureSpec.EXACTLY) {
            mHeight = hSpecSize;
        } else if (hSpecMode == MeasureSpec.AT_MOST) {
            // Wrap content
            mHeight = Math.min(100, hSpecSize);
        }

        if (wSpecMode == MeasureSpec.EXACTLY) {
            mWidth = wSpecSize;
        } else if (wSpecMode == MeasureSpec.AT_MOST) {
            // Wrap content
            mWidth = Math.min(100, wSpecSize);
        }

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int radius;

        if(width > height){
            radius = height/2;
        }else{
            radius = width/2;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(5);

        // Draw circle
        paint.setColor(getResources().getColor(R.color.grey));
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        //Draw line
        paint.setColor(getResources().getColor(R.color.sunshine_blue));
        canvas.drawLine(
                width / 2,
                height / 2,
                (float) ( width / 2 + (Math.sin(direction * (Math.PI / 180)) * radius) ),
                (float) ( height / 2 - (Math.cos(direction * (Math.PI / 180)) * radius) ),
                paint);

    }

    public void update(float dir){
        direction = dir;
        // To force drawing on page by calling the invalidate() method
        invalidate();
    }
}
