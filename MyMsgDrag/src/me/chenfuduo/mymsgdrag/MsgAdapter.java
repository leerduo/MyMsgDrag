package me.chenfuduo.mymsgdrag;

import java.util.List;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MsgAdapter extends ArrayAdapter<Msg> {

	public MsgAdapter(Context context, List<Msg> msgList) {
		super(context, 0, msgList);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		ViewHolder viewHolder;
		if (convertView == null) {
			view = View.inflate(getContext(), R.layout.item, null);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView) view
					.findViewById(R.id.imageView);
			viewHolder.textView = (TextView) view.findViewById(R.id.textView);
			view.setTag(viewHolder);
		} else {
			view = convertView;
			viewHolder = (ViewHolder) view.getTag();
		}

		viewHolder.imageView.setImageResource(getItem(position).getIvId());
		viewHolder.textView.setText(getItem(position).getText());
		return view;
	}

	static class ViewHolder {
		ImageView imageView;
		TextView textView;
	}

}
