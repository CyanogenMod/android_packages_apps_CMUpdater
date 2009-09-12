package cmupdaterapp.ui;

import java.util.List;

import cmupdaterapp.customTypes.ThemeList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ThemeListAdapter<T> extends ArrayAdapter<T>
{
	private final Context _context;
	
	public ThemeListAdapter(Context context, int textViewResourceId, List<T> objects)
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
		ThemeListViewWrapper wrapper=null;
		if (row == null)
		{
			LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row=inflater.inflate(R.layout.themelist_itemtemplate, null);
			wrapper=new ThemeListViewWrapper(row);
			row.setTag(wrapper);
		}
		else
		{
			wrapper=(ThemeListViewWrapper)row.getTag();
		}
		
    	ThemeList info = (ThemeList) this.getItem(position);
    	wrapper.getThemeNameView().setText(info.name);
    	wrapper.getThemeUriView().setText(info.url.toString());
    	wrapper.getImage().setImageResource(R.drawable.icon);
        return row;
   } 
}

//Class that Holds the Ids, so we have not to call findViewById each time which costs a lot of ressources
class ThemeListViewWrapper
{
	private View base;
	private TextView ThemeListName = null;
	private TextView ThemeListUri = null;
	private ImageView image = null;

	public ThemeListViewWrapper(View base)
	{
		this.base=base;
	}

	public TextView getThemeNameView()
	{
		if (ThemeListName == null)
		{
			ThemeListName=(TextView)base.findViewById(R.id.txtThemeName);
		}
		return(ThemeListName);
	}
	
	public TextView getThemeUriView()
	{
		if (ThemeListUri == null)
		{
			ThemeListUri=(TextView)base.findViewById(R.id.txtThemeUri);
		}
		return(ThemeListUri);
	}

	public ImageView getImage()
	{
		if (image == null)
		{
			image=(ImageView)base.findViewById(R.id.imgExperimentalStable);
		}
		return(image);
	}
}