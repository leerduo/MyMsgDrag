package me.chenfuduo.mymsgdrag;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * 
 * 没有用的类
 * 
 * @author 
 *
 */
public class MyMsgAdapter extends BaseAdapter {

	private Context context;
	
	private List<Msg> myList;

	public MyMsgAdapter(Context context,List<Msg> msgList) {
		this.context = context;
		this.myList = msgList;
	}

	@Override
	public int getCount() {
		return 30;
	}

	@Override
	public Msg getItem(int position) {
		Msg msg = myList.get(position);
		return msg;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		ViewHolder viewHolder;
		if (convertView == null) {
			view = View.inflate(context, R.layout.item, null);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView) view
					.findViewById(R.id.imageView);
			viewHolder.textView = (TextView) view.findViewById(R.id.textView);
			view.setTag(viewHolder);
		} else {
			view = convertView;
			viewHolder = (ViewHolder) view.getTag();
		}
		
		viewHolder.imageView.setImageResource(myList.get(position).getIvId());
		viewHolder.textView.setText(myList.get(position).getText());
		return view;
	}

	static class ViewHolder {
		ImageView imageView;
		TextView textView;
	}
}