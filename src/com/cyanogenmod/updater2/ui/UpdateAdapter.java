package com.cyanogenmod.updater2.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cyanogenmod.updater2.ListActivity;
import com.cyanogenmod.updater2.R;
import com.cyanogenmod.updater2.misc.UpdateInfo;

import java.util.List;

public class UpdateAdapter extends RecyclerView.Adapter<UpdateHolder> {
    private List<UpdateInfo> mUpdates;
    private ListActivity mCallingActivity;

    public UpdateAdapter(List<UpdateInfo> mUpdates, ListActivity mCallingActivity) {
        this.mUpdates = mUpdates;
        this.mCallingActivity = mCallingActivity;
    }

    @Override
    public UpdateHolder onCreateViewHolder(ViewGroup mParent, int mType) {
        View mItem = LayoutInflater.from(mParent.getContext())
                .inflate(R.layout.item_update, mParent, false);

        return new UpdateHolder(mItem);
    }

    @Override
    public void onBindViewHolder(UpdateHolder mHolder, int mPosition) {
        UpdateInfo mUpdate = mUpdates.get(mPosition);
        mHolder.init(mUpdate, mCallingActivity);
    }

    @Override
    public int getItemCount() {
        return mUpdates.size();
    }
}
