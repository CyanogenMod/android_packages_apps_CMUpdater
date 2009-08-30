package cmupdaterapp.ui;

import java.util.List;

import cmupdaterapp.service.UpdateInfo;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UpdateListAdapter<T> extends ArrayAdapter<T>
{
	private final Context _context;
	public UpdateListAdapter(Context context, int textViewResourceId, List<T> objects)
	{
		super(context, textViewResourceId, objects);
		_context = context;
	}
	
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		return getView(position, convertView, parent);
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row=convertView;
		ViewWrapper wrapper=null;
		if (row == null)
		{
			LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row=inflater.inflate(R.layout.updatelist_itemtemplate, null);
			wrapper=new ViewWrapper(row);
			row.setTag(wrapper);
		}
		else
		{
			wrapper=(ViewWrapper)row.getTag();
		}
		
    	UpdateInfo info = (UpdateInfo) this.getItem(position);
    	wrapper.getTextView().setText(info.name);
    	
    	if(info.branchCode.toLowerCase().equals("x"))
    		wrapper.getImage().setImageResource(R.drawable.experimental);
    	else
    		wrapper.getImage().setImageResource(R.drawable.stable);
        return row;
   } 
}

//Class that Holds the Ids, so we have not to call findViewById each time which costs a lot of ressources
class ViewWrapper
{
	View base;
	TextView label = null;
	ImageView image = null;

	ViewWrapper(View base)
	{
		this.base=base;
	}

	TextView getTextView()
	{
		if (label == null)
		{
			label=(TextView)base.findViewById(R.id.txtDisplay);
		}
		return(label);
	}

	ImageView getImage()
	{
		if (image == null)
		{
			image=(ImageView)base.findViewById(R.id.imgExperimentalStable);
		}
		return(image);
	}
}