经常看到如下的效果，ListView拖拽排序的效果，这部分就是实现这样的效果。
![ListView拖拽排序](http://1.infotravel.sinaapp.com/pic/34.gif)
<!--more-->
ListView列表拖拽排序可以参考Android源代码下的Music播放列表，他是可以拖拽的，源码在[packages/apps/Music下的`TouchInterceptor.java`下]。
首先是搭建框架,此处的ListView列表类似于QQ消息列表，当然数据只是模拟，为了简单起见，没有把ListView的条目的所有的属性全部写上。首先是消息的实体类Msg.java:
```java
package me.chenfuduo.mymsgdrag;

public class Msg {

	private int ivId;

	private String text;

	public Msg() {

	}

	public Msg(int ivId, String text) {
		this.ivId = ivId;
		this.text = text;
	}

	public int getIvId() {
		return ivId;
	}

	
	public String getText() {
		return text;
	}

	

}
```
然后是数据列表的每个Item的布局item.xml：
```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    >
    
    <ImageView
        android:layout_width="50dp"
        android:layout_height="50dp"
        android:layout_alignParentLeft="true"
        android:layout_marginLeft="10dp"
        android:layout_centerInParent="true"
        android:id="@+id/imageView"
        />
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:id="@+id/textView"
        android:layout_toRightOf="@id/imageView"
        android:layout_centerInParent="true"
        android:layout_marginLeft="10dp"
        />
    
    

</RelativeLayout>
```
现在可以新建一个MsgAdapter适配器类，让其继承自ArrayAdapter，并实现其构造方法和重写`getView()`方法。
```java
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
```
主界面MainActivity设置适配器：
```java
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
```
这里提到的`MyDragListView `就是下面我们要着重介绍的自定义的ListView，先不管。
OK，现在运行，数据全部展示在ListView上了，下面开始新建一个类`MyDragListView`，并让其继承自ListView，提供三个构造方法。
```java
public MyDragListView(Context context) {
		this(context, null);
	}

	public MyDragListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyDragListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
```
接下来，需要重写`onInterceptTouchEvent()`拦截事件的方法，为了能在子控件响应触摸事件的情况下此ListView也能监听到触摸事件，需要重写此方法，做一些初始化的工作，在这里捕获`ACTION_DOWN`事件，在`ACTION_DOWN`事件中，做一些拖动的准备工作。

> * 获取点击数据项，初始化一些变量(pointToPosition)
> * 判断是否是拖动还是仅仅是点击
> * 如果是拖动，建立拖动影像（）


以上都是后面拖动的基础。
那首先定义我们需要的一些变量：
```java
	// 原始条目位置
	private int dragSrcPosition;
	// 目标条目位置
	private int dragDestPosition;
	//在当前数据项中的位置
	private int dragPoint;
	//拖动的时候，开始向上滚动的边界
	private int upScrollBounce;
	//拖动的时候，开始向下滚动的边界
	private int downScrollBounce;
	//窗口控制类
	private WindowManager windowManager;
	//用于控制拖拽项显示的参数
	private WindowManager.LayoutParams windowParams;
	//当前视图和屏幕的距离(这里只使用了y轴上的)
	private int dragOffset;

	// 判断滑动的一个距离,scroll的时候会用到
	private int scaledTouchSlop;
	// 被拖拽项的影像，其实就是一个ImageView，在我们这里是"用户头像"
	private ImageView dragImageView;
```

我们在构造器中获取滑动的距离：
```java
public MyDragListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		scaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}
```
注意获取这个`系统所能识别出的被认为是滑动的最小距离`的方式。

`getScaledTouchSlop()`是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件。

接下来，在`onInterceptTouchEvent()`捕获的`ACTION_DOWN`事件中，做处理。
还是按照上面的来，第一部需要得到选中的数据项的位置，这里使用`pointToPosition(x,y)`即可。如果想要测试这个api，也很简单，下面是实例代码：
```java
 mListView.setOnTouchListener(new OnTouchListener() {  
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                int item=mListView.pointToPosition((int) event.getX(), (int) event.getY());  
                System.out.println("---> 现在点击了ListView中第"+(item+1)+"个Item");  
                return true;  
            }  
        });  
```
ok,在我们这里是这样：
```java
if (ev.getAction() == MotionEvent.ACTION_DOWN) {

			// 触点所在的条目的位置

			int x = (int) ev.getX();
			int y = (int) ev.getY();
			dragSrcPosition = dragDestPosition = pointToPosition(x, y);

			//如果是无效位置(超出边界，分割线等位置)，返回
	       		 if(dragDestPosition==AdapterView.INVALID_POSITION){
	            		return super.onInterceptTouchEvent(ev);
	       		 }

}
```
现在我们要获取ListView的单个Item，因为获取了这个单个的Item，才能获取Item里面的"头像"(姑且这么叫),ok,代码如下：
```java
			//这里如果不减去,会报空指针异常
			ViewGroup itemView = (ViewGroup) getChildAt(dragSrcPosition
					- getFirstVisiblePosition());
```
在这里我当时遇到一个`NPE`的问题，就是当ListView滚动到下面的时候，我选择下面的Item，报错了，归根到底，还是没有理解好`getChildAt（i）`这个方法。这里，我参考了下面的资料去理解的。
[ListView中getChildAt(index)的使用注意事项](http://ahua186186.iteye.com/blog/1830180)
[通过getChildAt方法取得AdapterView中第n个Item](http://blog.csdn.net/banking17173/article/details/8253310)
[stackover:ListView getChildAt returning null for visible children](http://stackoverflow.com/questions/6766625/listview-getchildat-returning-null-for-visible-children)
说到底，`getChildAt（i）`是获取可见视图的。
接下来，就可以获取"用户头像"了：
```java
			// 图标
			View dragger = itemView.findViewById(R.id.imageView);
```
下面需要判断手指的触点是不是在logo（"用户头像"）范围内：
```java
		if (dragger != null && x < dragger.getRight() + 10) {

				upScrollBounce = Math.min(y - scaledTouchSlop, getHeight() / 3);
				downScrollBounce = Math.max(y + scaledTouchSlop,
						getHeight() * 2 / 3);
}
```
这个很好理解。
接着便可以获取选中条目的图片了。
```java
				itemView.setDrawingCacheEnabled(true);
				Bitmap bitmap = itemView.getDrawingCache();
				startDrag(bitmap, y);
				itemView.setDrawingCacheEnabled(false);
```	
这里，又学到一招，将View转化为Bitmap,相关的api:

> * setDrawingCacheEnabled(boolean)注意最后需要设置其为false
> * getDrawingCache()

那么现在就可以拖动了。
```java
startDrag(bitmap, y);
```
最后，我们返回false,让事件可以传递到子控件。
整体的代码入下：
```java
@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		if (ev.getAction() == MotionEvent.ACTION_DOWN) {

			// 触点所在的条目的位置

			int x = (int) ev.getX();
			int y = (int) ev.getY();
			dragSrcPosition = dragDestPosition = pointToPosition(x, y);

			// 如果是无效位置(超出边界，分割线等位置)，返回
			if (dragDestPosition == AdapterView.INVALID_POSITION) {
				return super.onInterceptTouchEvent(ev);
			}

			
			//这里如果不减去
			ViewGroup itemView = (ViewGroup) getChildAt(dragSrcPosition
					- getFirstVisiblePosition());

			// 手指在条目中的相对y坐标

			dragPoint = y - itemView.getTop();
			
			/*Log.e("Test", "dragPoint:" + dragPoint + "\n" + "y:"+ y
					+ "\n" + "itemView.getTop():" + itemView.getTop());*/

			dragOffset = (int) (ev.getRawY() - y);

			/*Log.e("Test", "dragOffset:" + dragPoint + "\n" + "y:"+ y
					+ "\n" + "ev.getRawY():" + ev.getRawY());*/
			
			// 图标
			View dragger = itemView.findViewById(R.id.imageView);
			// 判断触点是否在logo的区域

			if (dragger != null && x < dragger.getRight() + 10) {

				upScrollBounce = Math.min(y - scaledTouchSlop, getHeight() / 3);
				downScrollBounce = Math.max(y + scaledTouchSlop,
						getHeight() * 2 / 3);

				// 获取选中条目的图片

				itemView.setDrawingCacheEnabled(true);
				Bitmap bitmap = itemView.getDrawingCache();
				itemView.setDrawingCacheEnabled(false);
				startDrag(bitmap, y);

			}

			// 可以传递到子控件
			return false;

		}

		return super.onInterceptTouchEvent(ev);
	}
```
在上面有获取坐标的getRawY()等等，如果不清楚，可以看下这个文章。
[android MotionEvent中getX()和getRawX()的区别](http://trylovecatch.iteye.com/blog/1096694)
接下来是拖拽的方法`startDrag(bitmap, y);`:
```java
private void startDrag(Bitmap bitmap, int y) {

		// 释放影像，在准备影像的时候，防止影像没释放，每次都执行一下
		stopDrag();

		windowManager = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);

		// 窗体参数配置

		windowParams = new WindowManager.LayoutParams();

		windowParams.gravity = Gravity.TOP;

		windowParams.x = 0;

		// 图片在屏幕上的绝对坐标

		windowParams.y = y - dragPoint + dragOffset;
		
		/*Log.e("Test", "windowParams.y:" + windowParams.y + "\n" + "y:"+ y
				+ "\n" + "dragPoint:" + dragPoint + 
				"\n" + "dragOffset" + dragOffset);*/

		// 添加显示窗体

		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		// 下面这些参数能够帮助准确定位到选中项点击位置，照抄即可
		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		windowParams.format = PixelFormat.TRANSLUCENT;
		windowParams.windowAnimations = 0;

		// 把影像ImagView添加到当前视图中
		ImageView imageView = new ImageView(getContext());
		imageView.setImageBitmap(bitmap);
		windowManager = (WindowManager) getContext().getSystemService("window");
		windowManager.addView(imageView, windowParams);
		// 把影像ImageView引用到变量drawImageView，用于后续操作(拖动，释放等等)

		dragImageView = imageView;
	}
```
如果做过自定义Toast，对上面的代码不会陌生。
不做解释，接着重写`boolean onTouchEvent(MotionEvent ev)`:
```java
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// 如果dragImageView为空，说明拦截事件中已经判定仅仅是点击，不是拖动，返回
		// 如果点击的是无效位置，返回，需要重新判断
		if (dragImageView != null && dragDestPosition != INVALID_POSITION) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
				int upY = (int) ev.getY();
				// 释放拖动影像
				stopDrag();
				// 放下后，判断位置，实现相应的位置删除和插入
				onDrop(upY);
				break;
			case MotionEvent.ACTION_MOVE:
				int moveY = (int) ev.getY();
				// 拖动影像
				onDrag(moveY);
				break;
			default:
				break;
			}
			return true;
		}
		// 这个返回值能够实现selected的选中效果，如果返回true则无选中效果
		return super.onTouchEvent(ev);
	}
```
首先得判断是点击还是拖动，直接通过`dragImageView`即可判断，如果dragImageView为空，说明拦截事件中已经判定仅仅是点击，不是拖动，返回。接着分析拖动的方法`onDrag(moveY);`:
拖动的时候，当前拖动的条目的透明度让其有所变化，然后是位置在不断更新，其次需要判断位置是否合法，最后是滚动。
```java
private void onDrag(int y) {
		if (dragImageView != null) {
			windowParams.alpha = 0.8f;
			windowParams.y = y - dragPoint + dragOffset;
			windowManager.updateViewLayout(dragImageView, windowParams);
		}
		// 为了避免滑动到分割线的时候，返回-1的问题
		int tempPosition = pointToPosition(0, y);
		if (tempPosition != INVALID_POSITION) {
			dragDestPosition = tempPosition;
		}

		// 滚动
		int scrollHeight = 0;
		if (y < upScrollBounce) {
			scrollHeight = 8;// 定义向上滚动8个像素，如果可以向上滚动的话
		} else if (y > downScrollBounce) {
			scrollHeight = -8;// 定义向下滚动8个像素，，如果可以向上滚动的话
		}

		if (scrollHeight != 0) {
			// 真正滚动的方法setSelectionFromTop()
			setSelectionFromTop(dragDestPosition,
					getChildAt(dragDestPosition - getFirstVisiblePosition())
							.getTop() + scrollHeight);
		}
	}
```
这里的`ViewManager.updateViewLayout(View arg0, LayoutParams arg1)`会使得view所引用的实例使用params重新绘制自己。
接下来介绍下ListView的`setSelectionFromTop(...)`和`setSelection(...)`方法。
看一下setSelectionFromTop()的具体实现，代码如下：
```java
/** 
 * Sets the selected item and positions the selection y pixels from the top edge 
 * of the ListView. (If in touch mode, the item will not be selected but it will 
 * still be positioned appropriately.) 
 * 
 * @param position Index (starting at 0) of the data item to be selected. 
 * @param y The distance from the top edge of the ListView (plus padding) that the 
 *        item will be positioned. 
 */  
public void setSelectionFromTop(int position, int y) {  
    if (mAdapter == null) {  
        return;  
    }  
  
    if (!isInTouchMode()) {  
        position = lookForSelectablePosition(position, true);  
        if (position >= 0) {  
            setNextSelectedPositionInt(position);  
        }  
    } else {  
        mResurrectToPosition = position;  
    }  
  
    if (position >= 0) {  
        mLayoutMode = LAYOUT_SPECIFIC;  
        mSpecificTop = mListPadding.top + y;  
  
        if (mNeedSync) {  
            mSyncPosition = position;  
            mSyncRowId = mAdapter.getItemId(position);  
        }  
  
        requestLayout();  
    }  
}  
```
从上面的代码可以得知，`setSelectionFromTop()`的作用是设置ListView选中的位置，同时在Y轴设置一个偏移量（padding值）。
ListView还有一个方法叫`setSelection()`，传入一个index整型数值，就可以让ListView定位到指定Item的位置。
这两个方法有什么区别呢？看一下setSelection()的具体实现，代码如下：
```java
/** 
 * Sets the currently selected item. If in touch mode, the item will not be selected 
 * but it will still be positioned appropriately. If the specified selection position 
 * is less than 0, then the item at position 0 will be selected. 
 * 
 * @param position Index (starting at 0) of the data item to be selected. 
 */  
@Override  
public void setSelection(int position) {  
    setSelectionFromTop(position, 0);  
}  
```
原来，`setSelection()`内部就是调用了`setSelectionFromTop()`，只不过是Y轴的偏移量是0而已。
Ok，当手指抬起来的时候，需要停止拖动：
```java
	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}

	}
```
最后得将Item放到正确的位置：
```java
private void onDrop(int y) {
		// 获取放下位置在数据集合中position
		// 定义临时位置变量为了避免滑动到分割线的时候，返回-1的问题，如果为-1，则不修改dragPosition的值，急需执行，达到跳过无效位置的效果
		int tempPosition = pointToPosition(0, y);
		if (tempPosition != INVALID_POSITION) {
			dragDestPosition = tempPosition;
		}

		// 超出边界处理
		if (y < getChildAt(1).getTop()) {
			// 超出上边界，设为最小值位置0
			dragDestPosition = 0;
		} else if (y > getChildAt(getChildCount() - 1).getTop()) {
			// 超出下边界，设为最大值位置，注意哦，如果大于可视界面中最大的View的底部则是越下界，所以判断中用getChildCount()方法
			// 但是最后一项在数据集合中的position是getAdapter().getCount()-1，这点要区分清除
			dragDestPosition = getAdapter().getCount() - 1;
		}

		// 数据更新
		if (dragDestPosition >= 0 && dragDestPosition < getAdapter().getCount()) {
			MsgAdapter adapter = (MsgAdapter) getAdapter();
			Msg dragItem = adapter.getItem(dragSrcPosition);

			// 删除原位置数据项
			adapter.remove(dragItem);
			// 在新位置插入拖动项
			adapter.insert(dragItem, dragDestPosition);
		}
	}
```




