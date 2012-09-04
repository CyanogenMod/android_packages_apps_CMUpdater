package com.cyanogenmod.updater.listadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cyanogenmod.updater.customTypes.UpdateInfo;
import com.cyanogenmod.updater.misc.Constants;
import com.cyanogenmod.updater.ui.R;

import java.util.List;

public class UpdateListAdapter<T> extends ArrayAdapter<T> {
    private final LayoutInflater _inflater;

    public UpdateListAdapter(Context context, List<T> objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewWrapper wrapper;
        if (row == null) {
            row = _inflater.inflate(R.layout.itemtemplate_updatelist, null);
            wrapper = new ViewWrapper(row);
            row.setTag(wrapper);
        } else {
            wrapper = (ViewWrapper) row.getTag();
        }

        UpdateInfo info = (UpdateInfo) this.getItem(position);
        wrapper.getTextView().setText(info.getName());

        if (info.getBranchCode().equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_NIGHTLY))
            wrapper.getImage().setImageResource(android.R.drawable.ic_menu_manage);
        else
            wrapper.getImage().setImageResource(android.R.drawable.ic_menu_myplaces);
        return row;
    } 
}

//Class that Holds the Ids, so we have not to call findViewById each time which costs a lot of ressources
class ViewWrapper {
    private final View base;
    private TextView label = null;
    private ImageView image = null;

    public ViewWrapper(View base) {
        this.base = base;
    }

    public TextView getTextView() {
        if (label == null) {
            label = (TextView) base.findViewById(R.id.txtDisplay);
        }
        return label;
    }

    public ImageView getImage() {
        if (image == null) {
            image = (ImageView) base.findViewById(R.id.imgNightlyStable);
        }
        return image;
    }
}
