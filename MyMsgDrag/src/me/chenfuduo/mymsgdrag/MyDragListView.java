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

	// ԭʼ��Ŀλ��
	private int dragSrcPosition;
	// Ŀ����Ŀλ��
	private int dragDestPosition;
	// �ڵ�ǰ�������е�λ��
	private int dragPoint;
	// �϶���ʱ�򣬿�ʼ���Ϲ����ı߽�
	private int upScrollBounce;
	// �϶���ʱ�򣬿�ʼ���¹����ı߽�
	private int downScrollBounce;
	// ���ڿ�����
	private WindowManager windowManager;
	// ���ڿ�����ק����ʾ�Ĳ���
	private WindowManager.LayoutParams windowParams;
	// ��ǰ��ͼ����Ļ�ľ���(����ֻʹ����y���ϵ�)
	private int dragOffset;

	// �жϻ�����һ������,scroll��ʱ����õ�
	private int scaledTouchSlop;
	// ����ק���Ӱ����ʵ����һ��ImageView��������������"�û�ͷ��"
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

			// �������ڵ���Ŀ��λ��

			int x = (int) ev.getX();
			int y = (int) ev.getY();
			dragSrcPosition = dragDestPosition = pointToPosition(x, y);

			// �������Чλ��(�����߽磬�ָ��ߵ�λ��)������
			if (dragDestPosition == AdapterView.INVALID_POSITION) {
				return super.onInterceptTouchEvent(ev);
			}

			
			//�����������ȥ
			ViewGroup itemView = (ViewGroup) getChildAt(dragSrcPosition
					- getFirstVisiblePosition());

			// ��ָ����Ŀ�е����y����

			dragPoint = y - itemView.getTop();
			
			/*Log.e("Test", "dragPoint:" + dragPoint + "\n" + "y:"+ y
					+ "\n" + "itemView.getTop():" + itemView.getTop());*/

			dragOffset = (int) (ev.getRawY() - y);

			/*Log.e("Test", "dragOffset:" + dragPoint + "\n" + "y:"+ y
					+ "\n" + "ev.getRawY():" + ev.getRawY());*/
			
			// ͼ��
			View dragger = itemView.findViewById(R.id.imageView);
			// �жϴ����Ƿ���logo������

			if (dragger != null && x < dragger.getRight() + 10) {

				upScrollBounce = Math.min(y - scaledTouchSlop, getHeight() / 3);
				downScrollBounce = Math.max(y + scaledTouchSlop,
						getHeight() * 2 / 3);

				// ��ȡѡ����Ŀ��ͼƬ

				itemView.setDrawingCacheEnabled(true);
				Bitmap bitmap = itemView.getDrawingCache();
				itemView.setDrawingCacheEnabled(false);
				startDrag(bitmap, y);

			}

			// ���Դ��ݵ��ӿؼ�
			return false;

		}

		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// ���dragmageViewΪ�գ�˵�������¼����Ѿ��ж������ǵ���������϶�������
		// ������������Чλ�ã����أ���Ҫ�����ж�
		if (dragImageView != null && dragDestPosition != INVALID_POSITION) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
				int upY = (int) ev.getY();
				// �ͷ��϶�Ӱ��
				stopDrag();
				// ���º��ж�λ�ã�ʵ����Ӧ��λ��ɾ���Ͳ���
				onDrop(upY);
				break;
			case MotionEvent.ACTION_MOVE:
				int moveY = (int) ev.getY();
				// �϶�Ӱ��
				onDrag(moveY);
				break;
			default:
				break;
			}
			return true;
		}
		// �������ֵ�ܹ�ʵ��selected��ѡ��Ч�����������true����ѡ��Ч��
		return super.onTouchEvent(ev);
	}

	private void onDrag(int y) {
		if (dragImageView != null) {
			windowParams.alpha = 0.8f;
			windowParams.y = y - dragPoint + dragOffset;
			windowManager.updateViewLayout(dragImageView, windowParams);
		}
		// Ϊ�˱��⻬�����ָ��ߵ�ʱ�򣬷���-1������
		int tempPosition = pointToPosition(0, y);
		if (tempPosition != INVALID_POSITION) {
			dragDestPosition = tempPosition;
		}

		// ����
		int scrollHeight = 0;
		if (y < upScrollBounce) {
			scrollHeight = 8;// �������Ϲ���8�����أ�����������Ϲ����Ļ�
		} else if (y > downScrollBounce) {
			scrollHeight = -8;// �������¹���8�����أ�������������Ϲ����Ļ�
		}

		if (scrollHeight != 0) {
			// ���������ķ���setSelectionFromTop()
			setSelectionFromTop(dragDestPosition,
					getChildAt(dragDestPosition - getFirstVisiblePosition())
							.getTop() + scrollHeight);
		}
	}

	private void onDrop(int y) {
		// ��ȡ����λ�������ݼ�����position
		// ������ʱλ�ñ���Ϊ�˱��⻬�����ָ��ߵ�ʱ�򣬷���-1�����⣬���Ϊ-1�����޸�dragPosition��ֵ������ִ�У��ﵽ������Чλ�õ�Ч��
		int tempPosition = pointToPosition(0, y);
		if (tempPosition != INVALID_POSITION) {
			dragDestPosition = tempPosition;
		}

		// �����߽紦��
		if (y < getChildAt(1).getTop()) {
			// �����ϱ߽磬��Ϊ��Сֵλ��0
			dragDestPosition = 0;
		} else if (y > getChildAt(getChildCount() - 1).getTop()) {
			// �����±߽磬��Ϊ���ֵλ�ã�ע��Ŷ��������ڿ��ӽ���������View�ĵײ�����Խ�½磬�����ж�����getChildCount()����
			// �������һ�������ݼ����е�position��getAdapter().getCount()-1�����Ҫ�������
			dragDestPosition = getAdapter().getCount() - 1;
		}

		// ���ݸ���
		if (dragDestPosition >= 0 && dragDestPosition < getAdapter().getCount()) {
			MsgAdapter adapter = (MsgAdapter) getAdapter();
			Msg dragItem = adapter.getItem(dragSrcPosition);

			// ɾ��ԭλ��������
			adapter.remove(dragItem);
			// ����λ�ò����϶���
			adapter.insert(dragItem, dragDestPosition);
		}
	}

	private void startDrag(Bitmap bitmap, int y) {

		// �ͷ�Ӱ����׼��Ӱ���ʱ�򣬷�ֹӰ��û�ͷţ�ÿ�ζ�ִ��һ��
		stopDrag();

		windowManager = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);

		// �����������

		windowParams = new WindowManager.LayoutParams();

		windowParams.gravity = Gravity.TOP;

		windowParams.x = 0;

		// ͼƬ����Ļ�ϵľ�������

		windowParams.y = y - dragPoint + dragOffset;
		
		/*Log.e("Test", "windowParams.y:" + windowParams.y + "\n" + "y:"+ y
				+ "\n" + "dragPoint:" + dragPoint + 
				"\n" + "dragOffset" + dragOffset);*/

		// �����ʾ����

		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		// ������Щ�����ܹ�����׼ȷ��λ��ѡ������λ�ã��ճ�����
		windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		windowParams.format = PixelFormat.TRANSLUCENT;
		windowParams.windowAnimations = 0;

		// ��Ӱ��ImagView��ӵ���ǰ��ͼ��
		ImageView imageView = new ImageView(getContext());
		imageView.setImageBitmap(bitmap);
		windowManager = (WindowManager) getContext().getSystemService("window");
		windowManager.addView(imageView, windowParams);
		// ��Ӱ��ImageView���õ�����drawImageView�����ں�������(�϶����ͷŵȵ�)

		dragImageView = imageView;
	}

	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}

	}

}
