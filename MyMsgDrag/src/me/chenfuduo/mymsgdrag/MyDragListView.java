package me.chenfuduo.mymsgdrag;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class MyDragListView extends ListView {

	// 原始条目位置
	private int dragSrcPosition;
	// 目标条目位置
	private int dragDestPosition;
	// 在当前数据项中的位置
	private int dragPoint;
	// 拖动的时候，开始向上滚动的边界
	private int upScrollBounce;
	// 拖动的时候，开始向下滚动的边界
	private int downScrollBounce;
	// 窗口控制类
	private WindowManager windowManager;
	// 用于控制拖拽项显示的参数
	private WindowManager.LayoutParams windowParams;
	// 当前视图和屏幕的距离(这里只使用了y轴上的)
	private int dragOffset;

	// 判断滑动的一个距离,scroll的时候会用到
	private int scaledTouchSlop;
	// 被拖拽项的影像，其实就是一个ImageView，在我们这里是"用户头像"
	private ImageView dragImageView;

	public MyDragListView(Context context) {
		this(context, null);
	}

	public MyDragListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyDragListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		scaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

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

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// 如果dragmageView为空，说明拦截事件中已经判定仅仅是点击，不是拖动，返回
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

	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}

	}

}
