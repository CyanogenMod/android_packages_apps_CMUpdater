package com.cyanogenmod.updater2.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
    private final GestureDetector mDetector;
    private final RecyclerClickListener mListener;

    public RecyclerTouchListener(Context mContext, final RecyclerClickListener mListener) {
        this.mListener = mListener;
        mDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent mEvent) {
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView mRecyclerView, MotionEvent mEvent) {
        View mChild = mRecyclerView.findChildViewUnder(mEvent.getX(), mEvent.getY());
        if (mChild != null && mListener != null && mDetector.onTouchEvent(mEvent)) {
            mListener.onClick(mChild, mRecyclerView.getChildAdapterPosition(mChild));
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView mRecyclerView, MotionEvent mEvent) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean mDisallow) {
    }
}