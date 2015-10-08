package com.expandlistviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;

/**
 * Created by Jaydipsinh Zala on 8/10/15.
 */
public class DragNDropListView extends ExpandableListView {

    private static final String TAG = "DragNDropListView";
    private boolean mDragMode;
    private boolean limitHorizontalDrag = true;
    private int[] mStartPosition = new int[2];
    private int[] mEndPosition = new int[2];
    private int mDragPointOffset; // Used to adjust drag view location
    private int mStartFlatPosition;
    private int prevY = -1;
    private int backgroundColor = 0xe0103010; // different color to identify
    private int defaultBackgroundColor;
    private float screenHeight;
    private float dragRatio;
    private ImageView mDragView;
    private DragNDropAdapter adapter;
    private DragNDropListeners listeners;
    private int dragOffset = 50;
    private boolean dragOnLongPress;
    private boolean pressedItem;
    private Handler handler = new Handler();

    public DragNDropListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            screenHeight = display.getHeight();
        } else {
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenHeight = size.y;
        }
    }

    public void setSelectedBackgroud(int color) {
        backgroundColor = color;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchHandler(event);
    }

    private boolean touchHandler(final MotionEvent event) {
        final int action = event.getAction();
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (prevY < 0) {
            prevY = y;
        }
        Log.d(TAG, "Motion event " + event.getAction());
        int flatPosition = pointToPosition(x, y);
        dragRatio = getHeight() / screenHeight;
        long packagedPosition = getExpandableListPosition(flatPosition);

        if (action == MotionEvent.ACTION_DOWN
                && getPackedPositionType(packagedPosition) == 1) {
            if (dragOnLongPress) {
                if (pressedItem) {
                    mDragMode = true;
                    pressedItem = false;
                } else {
                    pressedItem = true;
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            // y coordinate is changing for no reason ??
                            event.setLocation(x, y);
                            touchHandler(event);
                        }
                    };
                    handler.postDelayed(r, 200);
                    return true;
                }
            } else if (x < dragOffset) {
                mDragMode = true;
            }
        }

        if (!mDragMode) {
            /** when user action on other areas */
            if ((pressedItem && Math.abs(prevY - y) > 30)
                    || event.getAction() != MotionEvent.ACTION_MOVE) {
                pressedItem = false;
                handler.removeCallbacksAndMessages(null);
            }
            return super.onTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartFlatPosition = flatPosition;
                mStartPosition[0] = getPackedPositionGroup(packagedPosition);
                mStartPosition[1] = getPackedPositionChild(packagedPosition);
                if (packagedPosition != PACKED_POSITION_VALUE_NULL) {

                    int mItemPosition = flatPosition - getFirstVisiblePosition();
                    mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                    mDragPointOffset -= ((int) event.getRawY()) - y;
                    startDrag(mItemPosition, y);
                    if (listeners != null) {
                        listeners.onPick(mStartPosition);
                    }
                    drag(x, y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int speed = (int) ((y - prevY) * dragRatio);
                if (getLastVisiblePosition() < getCount() && speed > 0) {
                    smoothScrollBy(speed, 1);
                }
                if (getFirstVisiblePosition() > 0 && speed < 0) {
                    smoothScrollBy(speed, 1);
                }
                drag(x, y);// replace 0 with x if desired
                if (listeners != null) {
                    listeners.onDrag(x, y);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:

                mDragMode = false;
                if (getPackedPositionType(packagedPosition) == 0) {
                    mEndPosition[0] = getPackedPositionGroup(packagedPosition);
                    mEndPosition[1] = 0;
                } else {
                    mEndPosition[0] = getPackedPositionGroup(packagedPosition);
                    mEndPosition[1] = getPackedPositionChild(packagedPosition);
                }

                stopDrag(mStartFlatPosition);
                if (packagedPosition != PACKED_POSITION_VALUE_NULL) {
                    if (adapter != null) {
                        adapter.onDrop(mStartPosition, mEndPosition);
                    }
                    if (listeners != null) {
                        listeners.onDrop(mStartPosition, mEndPosition);
                    }
                }
                break;
        }
        prevY = y;
        return true;
    }

    // move the drag view
    private void drag(int x, int y) {
        if (mDragView != null) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView
                    .getLayoutParams();
            if (!limitHorizontalDrag) {// no need to move if horizontal drag is
                // limited
                layoutParams.x = x;
            }
            if (dragOnLongPress) {
                // to show that item is detached from the list
                layoutParams.y = y - mDragPointOffset - 20;
            } else {
                layoutParams.y = y - mDragPointOffset;
            }

            WindowManager mWindowManager = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(mDragView, layoutParams);
        }
    }

    // enable the drag view for dragging
    private void startDrag(int itemIndex, int y) {
        // stopDrag(itemIndex);

        View item = getChildAt(itemIndex);
        if (item == null)
            return;
        hideItem(item, mStartPosition);

        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Utils.getViewBitmap(item);
        item.setBackgroundColor(defaultBackgroundColor);
        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);

        WindowManager mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {
        // TODO Auto-generated method stub
        super.setAdapter(adapter);
        this.adapter = (DragNDropAdapter) adapter;
    }

    private void hideItem(View itemView, int[] position) {
        if (adapter != null) {
            adapter.onPick(position);
        }
        itemView.setVisibility(View.INVISIBLE); // make the item invisible as we
        // have picked it
        itemView.setDrawingCacheEnabled(true);
        defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
        itemView.setBackgroundColor(backgroundColor);
        ImageView iv = (ImageView) itemView
                .findViewById(R.id.move_icon_customizer_item);
        if (iv != null)
            iv.setVisibility(View.INVISIBLE);
    }

    public void showItem(View itemView) {
        if (itemView != null) {
            itemView.setVisibility(View.VISIBLE);
            itemView.setBackgroundColor(defaultBackgroundColor);
            ImageView iv = (ImageView) itemView
                    .findViewById(R.id.move_icon_customizer_item);
            if (iv != null)
                iv.setVisibility(View.VISIBLE);
        }

    }

    /**
     * destroy the drag view
     *
     * @param itemIndex Index of the item
     */

    private void stopDrag(int itemIndex) {
        int firstPosition = getFirstVisiblePosition() - getHeaderViewsCount();
        int wantedChild = itemIndex - firstPosition;
        if (mDragView != null) {
            if (wantedChild < 0 || wantedChild >= getChildCount()) {
                // no need to do anything
            } else {
                showItem(getChildAt(wantedChild));
            }
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager) getContext().getSystemService(
                    Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
    }

    /**
     * @return the limitHorizontalDrag
     */
    public boolean isLimitHorizontalDrag() {
        return limitHorizontalDrag;
    }

    /**
     * @param limitHorizontalDrag the limitHorizontalDrag to set
     */
    public void setLimitHorizontalDrag(boolean limitHorizontalDrag) {
        this.limitHorizontalDrag = limitHorizontalDrag;
    }

    /**
     * @return the listeners
     */
    public DragNDropListeners getListeners() {
        return listeners;
    }

    /**
     * @param listeners the listeners to set
     */
    public void setListeners(DragNDropListeners listeners) {
        this.listeners = listeners;
    }

    /**
     * @return the dragOffset
     */
    public int getDragOffset() {
        return dragOffset;
    }

    /**
     * @param dragOffset the dragOffset to set
     */
    public void setDragOffset(int dragOffset) {
        this.dragOffset = dragOffset;
    }

    public boolean isDragOnLongPress() {
        return dragOnLongPress;
    }

    /**
     * set this to drag an item by long press
     *
     * @param flag
     */
    public void setDragOnLongPress(boolean flag) {
        dragOnLongPress = flag;
        if (flag) {

        }
    }
}
