package com.example.refreshlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Benben on 2019/1/13.
 */

public class RefreshListView extends ListView {

    /**
     * 刷新页面的线性布局
     */
    private LinearLayout headerView;
    private View ll_pull_down_refresh;
    private ImageView iv_arrow;
    private ProgressBar pb_status;
    private TextView tv_status;
    private TextView tv_time;

    /**
     * 下拉刷新控件的高
     */
    private int pullDownRefreshHeight;

    /**
     * listView控件在屏幕上的纵坐标
     */
    private int listViewOnScreenY=-1;
    /**
     * 下拉刷新
     */
    public static final int PULL_DOWN_REFRESH=0;
    /**
     * 手松刷新
     */
    public static final int RELEASE_REFRESH=1;
    /**
     * 正在刷新
     */
    public static final int REFRESHING=2;

    private int currentStatus=PULL_DOWN_REFRESH;

    private Animation upAnimation;
    private Animation downAnimation;

    /**
     * 加载更多的视图控件
     */
    private View footerView;
    private int footerViewHeight;
    private boolean isLoadMore=false;

    /**
     * 顶部轮播图
     */
    private View topNewsView;


    public RefreshListView(Context context) {
        this(context,null);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initeHeaderView(context);
        initeAnimation();
        initeFooterView(context);
    }

    private void initeFooterView(Context context) {
        footerView=View.inflate(context,R.layout.refresh_footer,null);
        footerView.measure(0,0);
        footerViewHeight=footerView.getMeasuredHeight();
        footerView.setPadding(0,-footerViewHeight,0,0);

        //listview添加footer
        addFooterView(footerView);



        //监听listview滑到最后一条时
        setOnScrollListener(new MyScrollListener());
    }

    /**
     * 添加顶部轮播图
     * @param topNewsView
     */
    public void addTopNewsView(View topNewsView) {
        //判断一下是否为空
        if (topNewsView!=null){
            this.topNewsView=topNewsView;
            headerView.addView(topNewsView);
        }

    }

    class MyScrollListener implements OnScrollListener{

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            //当静止或者是惯性滚动的时候
            if (scrollState==OnScrollListener.SCROLL_STATE_IDLE||scrollState==OnScrollListener.SCROLL_STATE_FLING){
                //并且是最后一条可见的
                if (getLastVisiblePosition()>=getCount()-1){
                    //1.显示加载更多布局
                    footerView.setPadding(8,8,8,8);
                    // 2.状态改变
                    isLoadMore=true;
                    // 3.回调接口
                    if (mOnRefreshListener!=null){
                        mOnRefreshListener.onLoadMore();
                    }
                }
            }


        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }
    }

    private void initeAnimation() {
        upAnimation=new RotateAnimation(0,-180,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        upAnimation.setDuration(500);
        upAnimation.setFillAfter(true);

        downAnimation=new RotateAnimation(-180,-360,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        downAnimation.setDuration(500);
        downAnimation.setFillAfter(true);
    }

    private void initeHeaderView(Context context) {
        headerView= (LinearLayout) View.inflate(context, R.layout.refresh_header,null);
        //下拉刷新控件的初始化,must be headerView，否则会空指针
        ll_pull_down_refresh= headerView.findViewById(R.id.ll_pull_down_refresh);
        iv_arrow= (ImageView) headerView.findViewById(R.id.iv_arrow);
        pb_status= (ProgressBar) headerView.findViewById(R.id.pb_status);
        tv_status= (TextView) headerView.findViewById(R.id.tv_status);
        tv_time= (TextView) headerView.findViewById(R.id.tv_time);
        //测量
        ll_pull_down_refresh.measure(0,0);
        pullDownRefreshHeight = ll_pull_down_refresh.getMeasuredHeight();

        //默认隐藏下拉刷新控件
        // view.setpadding(0,-控件高，0,0)完全隐藏
        // view.setpadding(0,0，0,0)完全显示
        ll_pull_down_refresh.setPadding(0,-pullDownRefreshHeight,0,0);
        //添加listview的头

        addHeaderView(headerView);
    }

    private float startY=-1;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                //1.记录起始坐标
                startY=ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (startY==-1){
                    startY=ev.getY();
                }
                //判断顶部轮播图是否完全显示，只哟完全显示才有下拉刷新
                boolean isDisplayTopNews=isDisplayTopNews();
                if (!isDisplayTopNews){
                    //加载更多的情形break出去没有机会执行下面的代码了
                    break;
                }

                if (currentStatus==REFRESHING){
                    break;//正在刷新状态不变不让它一直刷新
                }

                //2.来到新的坐标
                float endY=ev.getY();
                float distanceY=endY-startY;
                if (distanceY>0){//下拉
                    //int paddingTop=-控件高+distanceY；
                    int paddingTop= (int) (-pullDownRefreshHeight+distanceY);

                    if (paddingTop<0&&currentStatus!=PULL_DOWN_REFRESH){
                        //下拉刷新状态
                        currentStatus=PULL_DOWN_REFRESH;
                        //更新状态
                        refreshViewState();
                    }else if (paddingTop>0&&currentStatus!=RELEASE_REFRESH){
                        //手松刷新状态
                        currentStatus=RELEASE_REFRESH;
                        //更新状态
                        refreshViewState();
                    }

                    //view。setpadding（0，paddingTop，0,0）动态显示下拉刷新控件
                    ll_pull_down_refresh.setPadding(0,paddingTop,0,0);
                }
                break;
            case MotionEvent.ACTION_UP:
                startY=-1;
                if (currentStatus==PULL_DOWN_REFRESH){
                    //完全隐藏
                    ll_pull_down_refresh.setPadding(0,-pullDownRefreshHeight,0,0);
                }else if (currentStatus==RELEASE_REFRESH){

                    //设置状态为正在刷新
                    currentStatus=REFRESHING;
                    refreshViewState();

                    // 完全显示
                    ll_pull_down_refresh.setPadding(0,0,0,0);
                    //回调接口
                    if (mOnRefreshListener!=null){
                        mOnRefreshListener.onPullDownRefresh();
                    }
                }
                break;

        }
        return super.onTouchEvent(ev);
    }

    /**
     * 判断是否完全显示轮播图
     * @return
     */
    private boolean isDisplayTopNews() {

        if (topNewsView!=null){
            //1.得到ListView在屏幕上的坐标location[0]=x,location[1]=y,
            int[] location=new int[2];
            if (listViewOnScreenY==-1){
                getLocationOnScreen(location);
                listViewOnScreenY=location[1];
            }

            //2.得到顶部轮播图在屏幕上的坐标
            topNewsView.getLocationOnScreen(location);
            int topNewsViewOnScreenY=location[1];
            if (listViewOnScreenY<=topNewsViewOnScreenY){
                return true;
            }else{
                return false;
            }
        }else {
            return true;
        }
    }

    private void refreshViewState() {

        switch (currentStatus){
            case PULL_DOWN_REFRESH:
                //下拉刷新状态
                iv_arrow.startAnimation(downAnimation);
                tv_status.setText("下拉刷新");
                break;
            case RELEASE_REFRESH://手松刷新状态
                iv_arrow.startAnimation(upAnimation);
                tv_status.setText("手松刷新");
                break;
            case REFRESHING://正在刷新状态
                tv_status.setText("正在刷新");
                pb_status.setVisibility(VISIBLE);
                iv_arrow.clearAnimation();
                iv_arrow.setVisibility(GONE);
                break;
        }
    }
    //更新数据成功与否-更新时间与否

    /**
     * 当联网成功和失败的时候回调该方法
     * 用户刷新状态的还原
     * @param success
     */
    public void onRefreshFinish(boolean success) {
        if (isLoadMore){
            //加载更多
            isLoadMore=false;
            //隐藏加载更多布局
            footerView.setPadding(0,-footerViewHeight,0,0);
        }else {
            //下拉刷新
            tv_status.setText("下拉刷新");
            currentStatus=PULL_DOWN_REFRESH;
            iv_arrow.clearAnimation();
            pb_status.setVisibility(GONE);
            iv_arrow.setVisibility(VISIBLE);
            //隐藏下拉刷新控件
            ll_pull_down_refresh.setPadding(0,-pullDownRefreshHeight,0,0);
            if (success){
                //设置最新更新时间
                tv_time.setText("上次更新时间"+getSystemTime());
            }
        }

    }
    private String getSystemTime(){
        SimpleDateFormat format=new SimpleDateFormat("MM-dd HH:mm");
        return format.format(new Date());
    }

    /**
     * 刷新监听接口
     */
    public interface OnRefreshListener{

        /**
         * 当下拉刷新时回调这个方法
         */
        public void onPullDownRefresh();
        public void onLoadMore();

    }

    private OnRefreshListener mOnRefreshListener;

    /**
     * 设置监听刷新，由外界设置
     */
    public void setOnRefreshListener(OnRefreshListener l){
        this.mOnRefreshListener=l;//在up时调用
    }
}
