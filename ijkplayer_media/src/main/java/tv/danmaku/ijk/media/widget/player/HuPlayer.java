package tv.danmaku.ijk.media.widget.player;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import tv.danmaku.ijk.media.R;
import tv.danmaku.ijk.media.interfaces.IRenderView;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.widget.IjkVideoView;


/**
 * Created by tcking on 15/10/27.
 */
public class HuPlayer implements IMediaPlayer.OnCompletionListener,IMediaPlayer.OnPreparedListener,IMediaPlayer.OnVideoSizeChangedListener,
IMediaPlayer.OnBufferingUpdateListener,IMediaPlayer.OnErrorListener,IMediaPlayer.OnInfoListener,IMediaPlayer.OnSeekCompleteListener
{

  
    /**
     * 可能会剪裁,保持原视频的大小，显示在中心,当原视频的大小超过view的大小超过部分裁剪处理
     */
    public static final String SCALETYPE_FITPARENT="fitParent";
    /**
     * 可能会剪裁,等比例放大视频，直到填满View为止,超过View的部分作裁剪处理
     */
    public static final String SCALETYPE_FILLPARENT="fillParent";
    /**
     * 将视频的内容完整居中显示，如果视频大于view,则按比例缩视频直到完全显示在view中
     */
    public static final String SCALETYPE_WRAPCONTENT="wrapContent";
    /**
     * 不剪裁,非等比例拉伸画面填满整个View
     */
    public static final String SCALETYPE_FITXY="fitXY";
    /**
     * 不剪裁,非等比例拉伸画面到16:9,并完全显示在View中
     */
    public static final String SCALETYPE_16_9="16:9";
    /**
     * 不剪裁,非等比例拉伸画面到4:3,并完全显示在View中
     */
    public static final String SCALETYPE_4_3="4:3";
    /**
     * handle
     */
    private static final int MESSAGE_SHOW_PROGRESS = 1;//更新progress
    private static final int MESSAGE_FADE_OUT = 2;//超时淡出
    private static final int MESSAGE_SEEK_NEW_POSITION = 3;//跳转
    private static final int MESSAGE_HIDE_CENTER_BOX = 4;//隐藏中间图标
    private static final int MESSAGE_RESTART_PLAY = 5;//重播
    private static final int MESSAGE_SHOW_CONTROLLER_BOX = 6;
    private static final int MESSAGE_HIDE_CONTROLLER_BOX = 7;
    
    
    /**
     * 播放器的不同状态
     */
    public final static int STATUS_ERROR=-1;
    public final static int STATUS_IDLE=0;
    public final static int STATUS_PREPARED=1;
    public final static int STATUS_PLAYING=2;
    public final static int STATUS_PAUSE=3;
    public final static int STATUS_COMPLETED=4;
    /**
     * 播放器当前的状态
     */
    private int mCurrentState=STATUS_IDLE;//当前状态
    
    private final Activity activity;
    private IjkVideoView videoView;//
    private SeekBar seekBar;//
    private final AudioManager audioManager;
    private final int mMaxVolume;
    private boolean playerSupport;//是否支持播放
    
    private BindView v;
    private long pauseTime;//暂停时间
    private boolean isLive = false;//是否为直播
    final private int initHeight;//竖屏高度
    private int defaultTimeout=5000;//默认超时时间
    private int screenWidthPixels;//屏幕宽度像素

    private View liveBox;
    
    private boolean isShowing;//控制器是否显示
    private boolean portrait;//是否是竖屏
    private float brightness=-1;//亮度
    private int volume=-1;//声音
    private long newPosition = -1;
    private long defaultRetryTime=5000;//默认重试时间

    private int currentPosition;//当前播放进度
    private boolean fullScreenOnly;//是否只允许全屏播放
    
    private long duration;//视频时长
    private boolean instantSeeking;//是否立即跳转
    private boolean isDragging;//是否正在拖拽
    
	private boolean mVersionAllow=false;
	
    private OnHuplayerListener listener;
    
    @SuppressWarnings("HandlerLeak")
    private Handler handler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_FADE_OUT://超时隐藏
                    hide(false);
                    break;
                case MESSAGE_HIDE_CENTER_BOX:
                    v.id(R.id.app_video_volume_box).gone();
                    v.id(R.id.app_video_brightness_box).gone();
                    v.id(R.id.app_video_fastForward_box).gone();
                    break;
                case MESSAGE_SEEK_NEW_POSITION:
                    if (!isLive && newPosition >= 0) {
                        videoView.seekTo((int) newPosition);
                        newPosition = -1;
                    }
                    break;
                case MESSAGE_SHOW_PROGRESS://更新进度条
                    setProgress();
                    if (!isDragging && isShowing) {
                        msg = obtainMessage(MESSAGE_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000);
                        updatePausePlay();
                    }
                    break;
                case MESSAGE_RESTART_PLAY://重播
                    videoView.seekTo(0);
                    videoView.start();
                    break;
                case MESSAGE_SHOW_CONTROLLER_BOX://显示控制器布局
                    if(!isFullScreen){
                        showStatusBar(true);
                        showActionBar(true);
                        showTopBox(false);
                        showBottomBox(true); 
                    }else{
                        showStatusBar(false);
                        showActionBar(false);
                        showControllerView(true);
                    }
                    isShowing=true;
                    break;
                 case MESSAGE_HIDE_CONTROLLER_BOX://隐藏控制器
                     showStatusBar(false);
                     showActionBar(false);
                     showControllerView(false);
                     isShowing=false;
                     break;
            }
        }
    };
    
    
    //************
    // 实现接口函数
    //************
    @Override
    public void onPrepared(IMediaPlayer mp)
    {
        setCurrentState(STATUS_PREPARED);
        videoView.start();
        if(listener!=null)
            listener.onPrepared();
    }

    @Override
    public void onCompletion(IMediaPlayer mp)
    {
        setCurrentState(STATUS_COMPLETED);
        if(listener!=null)
            listener.onComplete();
    }
    
    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent)
    {
        if(listener!=null)
            listener.onBufferingUpdate(percent);
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp)
    {
        if(listener!=null)
            listener.onSeekComplete();
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den)
    {
        // TODO: Implement this method
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra)
    {
        setCurrentState(STATUS_ERROR);
        if(listener!=null)
            listener.onError(what,extra);
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra)
    {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START://缓存开始
                showLoading(true);
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END://缓冲结束
                showLoading(false);
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH://
                //显示 下载速度
                //Toaster.show("download rate:" + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START://开始播放
                setCurrentState(STATUS_PLAYING);
                break;
        }
        if(listener!=null)
            listener.onInfo(what,extra);
        return true;
    }


    
    
    private final View.OnClickListener onClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View p) {
            //全屏
            if (p.getId() == R.id.img_huplayer_fullscreen) {
                toggleFullScreen();
            //播放
            } else if (p.getId() == R.id.img_huplayer_play) {
                doOnClickPlay();
                show(defaultTimeout);
            //返回
            }else if (p.getId() == R.id.img_huplayer_back) {
                if (!fullScreenOnly && isFullScreen) {
                    toggleFullScreen();
                } else {
                    activity.finish();
                }
            }
        }
    };
    
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser)
                return;
            v.id(R.id.app_video_status).gone();//移动时隐藏掉状态image
            int newPosition = (int) ((duration * progress*1.0) / 1000);
            String time = generateTime(newPosition);
            if (instantSeeking){
                videoView.seekTo(newPosition);
            }
            v.id(R.id.tv_huplayer_currentTime).text(time);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isDragging = true;
            show(3600000);
            handler.removeMessages(MESSAGE_SHOW_PROGRESS);
            if (instantSeeking){
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!instantSeeking){
                videoView.seekTo((int) ((duration * seekBar.getProgress()*1.0) / 1000));
            }
            show(defaultTimeout);
            handler.removeMessages(MESSAGE_SHOW_PROGRESS);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            isDragging = false;
            handler.sendEmptyMessageDelayed(MESSAGE_SHOW_PROGRESS, 1000);
        }
    };
    
    
    
    
    //***************
    //     构造器
    //***************
    public HuPlayer(final Activity activity) {
		//加载库文件
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            playerSupport=true;
        } catch (Throwable e) {
            Log.e("Huplayer", "加载库文件失败", e);
        }
        this.activity=activity;
        screenWidthPixels = activity.getResources().getDisplayMetrics().widthPixels;
      
		//安卓版本是否高于4.4
		mVersionAllow=Build.VERSION.SDK_INT>Build.VERSION_CODES.KITKAT;
		
        //init view
        initControllerView();
        
        //
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnInfoListener(this);
        videoView.setOnSeekCompleteListener(this);
        videoView.setOnBufferingUpdateListener(this);

        //
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //手势
        final GestureDetector gestureDetector = new GestureDetector(activity, new PlayerGestureListener());

        //
        liveBox.setClickable(false);
        liveBox.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (gestureDetector.onTouchEvent(motionEvent))
                        return true;

                    // 处理手势结束
                    switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_UP:
                            endGesture();
                            break;
                    }

                    return false;
                }
            });


        
       
        if (fullScreenOnly) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        portrait=getScreenOrientation()==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        initHeight=activity.findViewById(R.id.app_video_box).getLayoutParams().height;
       
       showControllerView(false);
        if (!playerSupport) {
            //showStatus(activity.getResources().getString(R.string.not_support));
        }
       
    }
    
    
    //初始化控制器布局
    private void initControllerView(){
        v=new BindView(activity);
        videoView = (IjkVideoView) activity.findViewById(R.id.video_view);
   
        seekBar = (SeekBar) activity.findViewById(R.id.sb_huplayer_seek);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(mSeekListener);
        v.id(R.id.img_huplayer_play).clicked(onClickListener);
        v.id(R.id.img_huplayer_fullscreen).clicked(onClickListener);
        v.id(R.id.img_huplayer_back).clicked(onClickListener);
      
        liveBox = activity.findViewById(R.id.app_video_box);
        
    }
    
    
    //设置当前状态
    private void setCurrentState(int newStatus){
        switch(newStatus){
            case STATUS_ERROR:
                handler.removeMessages(MESSAGE_SHOW_PROGRESS);
                handler.sendEmptyMessage(MESSAGE_HIDE_CONTROLLER_BOX);
                showLoading(false);
                //hideAll();
                if (isLive) {
                    //showStatus(activity.getResources().getString(R.string.small_problem));
                    if (defaultRetryTime>0) {
                        handler.sendEmptyMessageDelayed(MESSAGE_RESTART_PLAY, defaultRetryTime);
                    }
                } else {
                    //showStatus(activity.getResources().getString(R.string.small_problem));
                }
                break;
            case STATUS_PREPARED:
                handler.sendEmptyMessage(MESSAGE_HIDE_CONTROLLER_BOX);
                liveBox.setClickable(true);
                break;
            case STATUS_PLAYING:
                updatePausePlay();
                if(!isFullScreen&&isPlaying()){
                    handler.sendEmptyMessage(MESSAGE_HIDE_CONTROLLER_BOX);
                }
                showCover(false);
                showLoading(false);
                break;
            case STATUS_PAUSE:
                updatePausePlay();
                if(!isFullScreen){
                    handler.sendEmptyMessage(MESSAGE_SHOW_CONTROLLER_BOX);
                }
                break;
            case STATUS_COMPLETED:
                updatePausePlay();
                if(!isLive){
                    handler.removeMessages(MESSAGE_SHOW_PROGRESS);
                    //hideAll();
                    handler.sendEmptyMessage(MESSAGE_HIDE_CONTROLLER_BOX);
                }
                break;
        }
        if(mCurrentState!=newStatus){
            mCurrentState=newStatus;
            if(listener!=null)
                listener.onStateChange(newStatus);
        }
    }
    
    
    //点击播放按钮
    private void doOnClickPlay() {
        if (mCurrentState==STATUS_COMPLETED) {//播放完成
            videoView.seekTo(0);
            videoView.start();
            setCurrentState(STATUS_PLAYING);
        } else if (videoView.isPlaying()) {//正在播放
            videoView.pause();
            setCurrentState(STATUS_PAUSE);
        } else {//暂停
            videoView.start();
            setCurrentState(STATUS_PLAYING);
        }
        //updatePausePlay();
    }

    //********************
    //    播放器初始化设置
    //********************
    private ActionBar mActionBar;
    private String url;//视频url
    private boolean isFullScreen=false;
    /**
     * 设置视频路径
     * @param url
     */
    public HuPlayer setPath(String url) {
        this.url = url;
        if (playerSupport) {
            videoView.setVideoPath(url);
        }
        return this;
    }
    
    /**
     * 设置title
     * @param title
     */
    public HuPlayer setTitle(CharSequence title) {
        v.id(R.id.tv_huplayer_title).text(title);
        return this;
    }
    
    /**
     * 设置播放器封面
     * @param coverId
     */
    public HuPlayer setCover(int coverId){
        v.id(R.id.img_huplayer_cover).image(coverId);
        return this;
    }

    /**
     * try to play when error(only for live video)
     * @param defaultRetryTime millisecond,0 will stop retry,default is 5000 millisecond
     */
    public HuPlayer setDefaultRetryTime(long defaultRetryTime) {
        this.defaultRetryTime = defaultRetryTime;
        return this;
    }
    
    /**
     * 设置是否只能全屏播放
     * @param fullScreenOnly
     */
    public HuPlayer setFullScreenOnly(boolean fullScreenOnly) {
        this.fullScreenOnly = fullScreenOnly;
        tryFullScreen(fullScreenOnly);
        if (fullScreenOnly) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        return this;
    }
    
    /**
     * 设置ActionBar
     * @param bar
     */
    public HuPlayer setActionBar(ActionBar bar){
        this.mActionBar=bar;
        return this;
    }
    
    /**
     * 实现接口
     */
    public HuPlayer setOnHuplayerListener(OnHuplayerListener listener){
        this.listener=listener;
        return this;
    }
    
    /**
     * set is live (can't seek forward)
     * @param isLive
     * @return
     */
    public HuPlayer live(boolean isLive) {
        this.isLive = isLive;
        return this;
    }

    public HuPlayer toggleAspectRatio(){
        if (videoView != null) {
            videoView.toggleAspectRatio();
        }
        return this;
    }
    
    /**
     * <pre>
     *     fitParent:可能会剪裁,保持原视频的大小，显示在中心,当原视频的大小超过view的大小超过部分裁剪处理
     *     fillParent:可能会剪裁,等比例放大视频，直到填满View为止,超过View的部分作裁剪处理
     *     wrapContent:将视频的内容完整居中显示，如果视频大于view,则按比例缩视频直到完全显示在view中
     *     fitXY:不剪裁,非等比例拉伸画面填满整个View
     *     16:9:不剪裁,非等比例拉伸画面到16:9,并完全显示在View中
     *     4:3:不剪裁,非等比例拉伸画面到4:3,并完全显示在View中
     * </pre>
     * @param scaleType
     */
    public HuPlayer setScaleType(String scaleType) {
        if (SCALETYPE_FITPARENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        }else if (SCALETYPE_FILLPARENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
        }else if (SCALETYPE_WRAPCONTENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_WRAP_CONTENT);
        }else if (SCALETYPE_FITXY.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_MATCH_PARENT);
        }else if (SCALETYPE_16_9.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_16_9_FIT_PARENT);
        }else if (SCALETYPE_4_3.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_4_3_FIT_PARENT);
        }
        return this;
    }

    /**
     * 是否显示左上导航图标(一般有actionbar or appToolbar时需要隐藏)
     * @param show
     */
    public HuPlayer setShowNavIcon(boolean show) {
        v.id(R.id.img_huplayer_back).visibility(show ? View.VISIBLE : View.GONE);
        return this;
    }
    
    /**
     * 播放器创建完成,此时显示播放按钮
     */
    public HuPlayer createComplete(){
        showControllerView(false);
        showCover(true);
       // showThumb(true);
        //showLoading(true);
        videoView.setVisibility(View.VISIBLE);
        listener.onPlayerCreate();
        return this;
    }
    
    
    //----------------
    //   
    //----------------
    
    /**
     * 开始播放
     */
    public void start() {
        if(videoView!=null){
            videoView.start();
            setCurrentState(STATUS_PLAYING);
        }
        
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if(videoView!=null&&videoView.canPause()){
            videoView.pause();
            setCurrentState(STATUS_PAUSE);
        }
    }
    
    /**
     * 隐藏
     * @param force 是否被动
     *        isShowing
     */
    public void hide(boolean force) {
        if (force || isShowing) {
            handler.removeMessages(MESSAGE_SHOW_PROGRESS);
            showControllerView(false);
            if(!isFullScreen&&isPlaying()){
                showActionBar(false);
				showStatusBar(false);
            }else{
                
            }
            
            isShowing = false;
            if(listener!=null)
                listener.onControllerChange(false);
        }
    }
    
    /**
     * 显示
     */
    public void show() {
        show(0);
    }
    
    /**
     * 显示
     * @param timeout
     */
    public void show(int timeout) {
        if (!isShowing) {
            if(isFullScreen){
                showTopBox(true);
            }else{
                showActionBar(true);
                showStatusBar(true);
            }
                
            if (!isLive) {
                showBottomBox(true);
            }
            if (!fullScreenOnly) {
                v.id(R.id.img_huplayer_fullscreen).visible();
            }
            isShowing = true;
            if(listener!=null)
                listener.onControllerChange(true);
        }
        updatePausePlay();
        
        handler.sendEmptyMessage(MESSAGE_SHOW_PROGRESS);
        handler.removeMessages(MESSAGE_FADE_OUT);
        if (timeout != 0) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FADE_OUT), timeout);
        }
    }


    //---------
    //   状态
    //---------
  
    //返回按钮响应
    public boolean onBackPressed() {
        if(isFullScreen&&!fullScreenOnly){
            toggleFullScreen();
            return true;
        }
        /*if (!fullScreenOnly && getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE||getScreenOrientation()==ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
         activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
         return true;
         }*/
        return false;
    }
    
    public void onPause() {
        pauseTime=System.currentTimeMillis();
        show(0);//把系统状态栏显示出来
        if (mCurrentState==STATUS_PLAYING) {
            videoView.pause();
            if (!isLive) {
                currentPosition = videoView.getCurrentPosition();
            }
        }
    }

    public void onResume() {
        pauseTime=0;
        if (mCurrentState==STATUS_PLAYING) {
            if (isLive) {
                videoView.seekTo(0);
            } else {
                if (currentPosition>0) {
                    videoView.seekTo(currentPosition);
                }
            }
            videoView.start();
        }
    }

    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        videoView.stopPlayback();
    }
    
    public void onConfigurationChanged(final Configuration newConfig) {
        portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        isFullScreen=!portrait;
        if (videoView != null && !fullScreenOnly) {

            handler.post(new Runnable() {
                    @Override
                    public void run() {

                        //tryFullScreen(!portrait);
                        if (portrait) {
                            v.id(R.id.app_video_box).height(initHeight, false);
                        } else {
                            int heightPixels = activity.getResources().getDisplayMetrics().heightPixels;
                            int widthPixels = activity.getResources().getDisplayMetrics().widthPixels;
                            v.id(R.id.app_video_box).height(Math.min(heightPixels,widthPixels), false);
                        }
                        updateFullScreenButton();

                    }
                });
           
        }
    }

    

    

    


    

    private String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    private int getScreenOrientation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
            || rotation == Surface.ROTATION_180) && height > width ||
            (rotation == Surface.ROTATION_90
            || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    
    /**
     * 手势结束
     */
    private void endGesture() {
        volume = -1;
        brightness = -1f;
        if (newPosition >= 0) {
            handler.removeMessages(MESSAGE_SEEK_NEW_POSITION);
            handler.sendEmptyMessage(MESSAGE_SEEK_NEW_POSITION);
        }
        handler.removeMessages(MESSAGE_HIDE_CENTER_BOX);
        handler.sendEmptyMessageDelayed(MESSAGE_HIDE_CENTER_BOX, 500);

    }
    
    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }
        hide(true);

        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }
        // 显示
        v.id(R.id.app_video_volume_icon).image(i==0?R.drawable.ic_volume_off_white_36dp:R.drawable.ic_volume_up_white_36dp);
        v.id(R.id.app_video_brightness_box).gone();
        v.id(R.id.app_video_volume_box).visible();
        v.id(R.id.app_video_volume_box).visible();
        v.id(R.id.app_video_volume).text(s).visible();
    }

    private void onProgressSlide(float percent) {
        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);


        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition=0;
            delta=-position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            v.id(R.id.app_video_fastForward_box).visible();
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            v.id(R.id.app_video_fastForward).text(text + "s");
            v.id(R.id.app_video_fastForward_target).text(generateTime(newPosition)+"/");
            v.id(R.id.app_video_fastForward_all).text(generateTime(duration));
        }
    }

    /**
     * 滑动改变亮度
     *
     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        if (brightness < 0) {
            brightness = activity.getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f){
                brightness = 0.50f;
            }else if (brightness < 0.01f){
                brightness = 0.01f;
            }
        }
        Log.d(this.getClass().getSimpleName(),"brightness:"+brightness+",percent:"+ percent);
        v.id(R.id.app_video_brightness_box).visible();
        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f){
            lpa.screenBrightness = 1.0f;
        }else if (lpa.screenBrightness < 0.01f){
            lpa.screenBrightness = 0.01f;
        }
        v.id(R.id.app_video_brightness).text(((int) (lpa.screenBrightness * 100))+"%");
        activity.getWindow().setAttributes(lpa);

    }
    
    //********************//
    //      控制器 UI
    //********************//
    //更新进度条
    private long setProgress() {
        if (isDragging){
            return 0;
        }

        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        if (seekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                seekBar.setProgress((int) pos);
            }
            int percent = videoView.getBufferPercentage();//缓冲进度
            seekBar.setSecondaryProgress(percent * 10);
        }

        this.duration = duration;
        v.id(R.id.tv_huplayer_currentTime).text(generateTime(position));
        v.id(R.id.tv_huplayer_endTime).text(generateTime(this.duration));
        return position;
    }
    
	
    //隐藏actionbar
    private void tryFullScreen(boolean fullScreen) {
        if(activity instanceof Activity){
            android.app.ActionBar supportActionBar = (activity).getActionBar();
            if (supportActionBar != null)
            {
                if (fullScreen)
                {
                    supportActionBar.hide();
                } else {
                    supportActionBar.show();
                }
            }
        }else 
        if (activity instanceof AppCompatActivity) {
            ActionBar supportActionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (supportActionBar != null) {
                if (fullScreen) {
                    supportActionBar.hide();
                } else {
                    supportActionBar.show();
                }
            }
        }
        showStatusBar(fullScreen);
    }

    //是否显示状态栏
    private void showStatusBar(boolean isShow){
		
        if (activity != null) {
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            if (!isShow&&(isFullScreen||(!isFullScreen&&mVersionAllow))) {//隐藏
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                activity.getWindow().setAttributes(attrs);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            } else {
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().setAttributes(attrs);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }
        
    }
	
    //设置控制器布局是否隐藏(顶部栏和底部栏)
    private void showControllerView(boolean isShow){
        if(isShow){
            v.id(R.id.rl_huplayer_bottom_box).visible();
            v.id(R.id.ll_huplayer_top_box).visible();
        }else{
            v.id(R.id.rl_huplayer_bottom_box).invisible();
            v.id(R.id.ll_huplayer_top_box).invisible();
        }
    }
    
    //是否显示封面
    private void showCover(boolean isShow){
        if(isShow){
            v.id(R.id.rl_huplayer_root_cover).visible();
        }else{
            v.id(R.id.rl_huplayer_root_cover).invisible();
        }
    }
    
    //是否显示加载等待条
    private void showLoading(boolean isShow){
        v.id(R.id.pb_huplayer_loading).visibility(isShow?View.VISIBLE:View.GONE);
    }
    
    //是否显示顶部导航栏
    private void showTopBox(boolean isShow){
        v.id(R.id.ll_huplayer_top_box).visibility(isShow?View.VISIBLE:View.GONE);
    }
    //是否显示底部栏
    private void showBottomBox(boolean isShow){
        v.id(R.id.rl_huplayer_bottom_box).visibility(isShow?View.VISIBLE:View.GONE);
    }
    
    //是否显示actionbar
    private void showActionBar(boolean isShow){
        if(mActionBar!=null){
            if(!isShow) 
                mActionBar.hide();
            else   
                mActionBar.show();
        }
    }
    
    //更新全屏按钮图标
    private void updateFullScreenButton() {
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE||getScreenOrientation()==ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            v.id(R.id.img_huplayer_fullscreen).image(R.drawable.ic_fullscreen_exit_white_24dp);
        } else {
            v.id(R.id.img_huplayer_fullscreen).image(R.drawable.ic_fullscreen_white_24dp);
        }
    }
    
    //更新暂停播放按钮图标
    private void updatePausePlay() {
        if (videoView.isPlaying()) {
            v.id(R.id.img_huplayer_play).image(R.drawable.bili_player_play_can_pause);
        } else {
            v.id(R.id.img_huplayer_play).image(R.drawable.bili_player_play_can_play);
        }
        
    }
    
    
    
    
    
    //**************//
    //  手势响应类
    //**************//
    public class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;

        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
      
            if(isPlaying()){
                v.id(R.id.img_huplayer_play).image(R.drawable.bili_player_play_can_pause);
                videoView.pause();
                setCurrentState(STATUS_PAUSE);
            }else{
                v.id(R.id.img_huplayer_play).image(R.drawable.bili_player_play_can_play);
                videoView.start();
                setCurrentState(STATUS_PLAYING);
            }
            
            /*
            if(!isFullScreen){
                if(isPlaying){
                    showStatusBar(true);
                    showActionBar(true);
                    showBottomBox(true);
                }else{
                    showStatusBar(false);
                    showActionBar(false);
                    showBottomBox(false);
                }
			}*/
            //videoView.toggleAspectRatio();
            
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            return super.onDown(e);

        }

        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (firstTouch) {
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl=mOldX > screenWidthPixels * 0.5f;
                firstTouch = false;
            }

            if (toSeek) {
                if (!isLive) {
                    onProgressSlide(-deltaX / videoView.getWidth());
                }
            } else {
                float percent = deltaY / videoView.getHeight();
                if (volumeControl) {
                    onVolumeSlide(percent);
                } else {
                    onBrightnessSlide(percent);
                }


            }

            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        //单击
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            if (isShowing) {
                 hide(false);
            } else {
                 show(defaultTimeout);
            }
            return true;
        }

        
    }

    /**
     * is player support this device
     * @return
     */
    public boolean isPlayerSupport() {
        return playerSupport;
    }

    /**
     * 是否正在播放
     * @return
     */
    public boolean isPlaying() {
        return videoView!=null?videoView.isPlaying():false;
    }

    /**
     *
     */
    public void stop(){
        videoView.stopPlayback();
    }

    /**
     * seekTo position
     * @param msec  millisecond
     */
    public HuPlayer seekTo(int msec, boolean showControlPanle){
        videoView.seekTo(msec);
        if (showControlPanle) {
            show(defaultTimeout);
        }
        return this;
    }

    public HuPlayer forward(float percent) {
        if (isLive || percent>1 || percent<-1) {
            return this;
        }
        onProgressSlide(percent);
        showBottomBox(true);
        handler.sendEmptyMessage(MESSAGE_SHOW_PROGRESS);
        endGesture();
        return this;
    }

    public int getCurrentPosition(){
        return videoView.getCurrentPosition();
    }

    /**
     * get video duration
     * @return
     */
    public int getDuration(){
        return videoView.getDuration();
    }

    public HuPlayer playInFullScreen(boolean fullScreen){
        if (fullScreen) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            updateFullScreenButton();
        }
        return this;
    }

    public void toggleFullScreen(){
        isFullScreen=!isFullScreen;
        if(!isFullScreen&&!isPlaying()){
            handler.sendEmptyMessage(MESSAGE_SHOW_CONTROLLER_BOX);
        }else{
            handler.sendEmptyMessage(MESSAGE_HIDE_CONTROLLER_BOX);
        }
        
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        updateFullScreenButton();
        updatePausePlay();
        if(listener!=null)
            listener.onScreenChange(isFullScreen);
    }

   
    

    

    

   
    
    
    
    
    //************//
    //    接口
    //************//
    public interface OnHuplayerListener{
        
        void onStateChange(int status);
        
        void onScreenChange(boolean isFullScreen);
        
        void onControllerChange(boolean isShowing);
        
        void onPlayerCreate();
        
        void onPrepared();
        
        void onError(int what, int extra) ;
        
        void onInfo(int what, int extra);
        
        void onComplete();
        
        void onSeekComplete();
        
        void onBufferingUpdate(int percent);
    }
   
    
    
    
    
    
    
}
