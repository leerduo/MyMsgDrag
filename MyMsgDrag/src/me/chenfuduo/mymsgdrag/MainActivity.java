package me.chenfuduo.mymsgdrag;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	private MyDragListView list;

	private List<Msg> msgList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = (MyDragListView) findViewById(R.id.list);
		msgList = new ArrayList<Msg>();
		initData();
		list.setAdapter(new MsgAdapter(this, msgList));
	}

	private void initData() {
		for (int i = 0; i < 30; i++) {
			msgList.add(new Msg(R.drawable.ic_launcher, "new item" + i));
		}

	}

}
