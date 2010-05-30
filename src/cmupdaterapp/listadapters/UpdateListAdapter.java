package cmupdaterapp.listadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cmupdaterapp.customTypes.UpdateInfo;
import cmupdaterapp.misc.Constants;
import cmupdaterapp.ui.R;

import java.util.List;

public class UpdateListAdapter<T> extends ArrayAdapter<T>
{
	private final LayoutInflater _inflater;

	public UpdateListAdapter(Context context, int textViewResourceId, List<T> objects)
	{
		super(context, textViewResourceId, objects);
		_inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		return getView(position, convertView, parent);
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		ViewWrapper wrapper;
		if (row == null)
		{
			row = _inflater.inflate(R.layout.itemtemplate_updatelist, null);
			wrapper = new ViewWrapper(row);
			row.setTag(wrapper);
		}
		else
		{
			wrapper=(ViewWrapper)row.getTag();
		}

    	UpdateInfo info = (UpdateInfo) this.getItem(position);
    	if (info.getType().equalsIgnoreCase(Constants.UPDATE_INFO_TYPE_THEME))
    		wrapper.getTextView().setText(info.getName() + " " + info.getVersion());
    	else
    		wrapper.getTextView().setText(info.getName());
    	
    	if(info.getBranchCode().equalsIgnoreCase(Constants.UPDATE_INFO_BRANCH_EXPERIMENTAL))
    		wrapper.getImage().setImageResource(R.drawable.experimental);
    	else
    		wrapper.getImage().setImageResource(R.drawable.stable);
        return row;
   } 
}

//Class that Holds the Ids, so we have not to call findViewById each time which costs a lot of ressources
class ViewWrapper
{
	private View base;
	private TextView label = null;
	private ImageView image = null;

	public ViewWrapper(View base)
	{
		this.base = base;
	}

	public TextView getTextView()
	{
		if (label == null)
		{
			label = (TextView)base.findViewById(R.id.txtDisplay);
		}
		return label;
	}

	public ImageView getImage()
	{
		if (image == null)
		{
			image = (ImageView)base.findViewById(R.id.imgExperimentalStable);
		}
		return image;
	}
}