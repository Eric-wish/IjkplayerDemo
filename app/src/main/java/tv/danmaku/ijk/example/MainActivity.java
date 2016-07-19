package tv.danmaku.ijk.example;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import tv.danmaku.ijk.example.widget.CustomCollapsingToolbarLayout;
import tv.danmaku.ijk.example.widget.CustomCoordinatorLayout;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.widget.player.HuPlayer;

public class MainActivity extends AppCompatActivity implements  HuPlayer.OnHuplayerListener,View.OnClickListener
{

    private HuPlayer player;

    private AppBarLayout mAppBar;

    private CoordinatorLayout mCoordinatorLayout;

    private FloatingActionButton fab_play;

    private ImageView cover;

    private CustomCollapsingToolbarLayout mCollapsingToolbarLayout;


    private final int MESSAGE_MOVE=0;
    private final int MESSAGE_PIN=1;

    @SuppressWarnings("HandlerLeak")
    private Handler handler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg)
        {
            AppBarLayout.LayoutParams params=(AppBarLayout.LayoutParams)mCollapsingToolbarLayout.getLayoutParams();
            switch (msg.what)
            {
                case MESSAGE_PIN:
                    AppBarLayout.Behavior behavior = (AppBarLayout.Behavior)((CoordinatorLayout.LayoutParams)mAppBar.getLayoutParams()).getBehavior();
                    behavior.onNestedFling(mCoordinatorLayout, mAppBar, null, 0, -10000, true);        
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
                    mCollapsingToolbarLayout.setLayoutParams(params);
                    break;
                case MESSAGE_MOVE:
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
                    mCollapsingToolbarLayout.setLayoutParams(params);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        //>19   4.4
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
        {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        }
        setContentView(R.layout.activity_main);
        initView();
        player = new HuPlayer(this);
        player.setActionBar(getSupportActionBar());
        player.setOnHuplayerListener(this);

        new Handler().postDelayed(new Runnable(){

                @Override
                public void run()
                {

                    fab_play.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#30469b")));

                }
            }, 1000);

	}

    void initView()
    {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("测试");

        mAppBar = (AppBarLayout) findViewById(R.id.appbar_layout);
        mCollapsingToolbarLayout = (CustomCollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar_layout);
        mCoordinatorLayout = (CustomCoordinatorLayout)findViewById(R.id.coordinator_layout);
        fab_play = (FloatingActionButton)findViewById(R.id.fab_play);
        fab_play.setOnClickListener(this);

        cover = (ImageView)findViewById(R.id.img_cover);
        cover.setBackgroundResource(R.drawable.test_cover);

        String str="    张明明和几个朋友定了一家宾馆第25楼的房间，回来时，宾馆的电梯坏了，服务员安排他们在大厅过夜。"
            + "几个朋友商量了一会儿，大家决定徒步爬楼梯，回房间。为了打发时间，他们轮流讲笑话、故事或唱歌，来缓解疲劳，爬到第18楼时，"
            + "轮到张明明讲故事了，他说，故事不长，却令人悲伤：他把钥匙忘在大厅了。"
            + "\r\n    一天，一位小伙子非常慌张的从银行跑了出来。接着后面跟着一群人，大喊:抓贼啊！警察也闻声赶来。"
            + "就在警察拼命地追赶小偷时，突然一超帅的帅哥骑着紫焰哈雷冲了出来，那位帅哥对小偷说:兄弟，快上车。小偷一脸茫然的问:你谁啊?"
            + "帅哥说:别管那么多了，再不上车就来不及了。小偷也没多想，就上了车。结果，帅哥冷静的把车开到了派出所.....";
        ((TextView) findViewById(R.id.tv_test)).setText(str);
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.fab_play)
        {
            cover.setVisibility(View.GONE);
            //player.setPath("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
            //player.setPath("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear4/prog_index.m3u8");
            player.setPath("http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear1/prog_index.m3u8");
            player.setTitle("测试");
            player.createComplete();
            handler.sendEmptyMessage(MESSAGE_PIN);

        }

    }



    @Override
    public void onStateChange(int status)
    {

        if (status == HuPlayer.STATUS_PLAYING)
        {
            Toast.makeText(this, "播放中", 1000).show();
        }

        if (status == HuPlayer.STATUS_PLAYING)
        {
            handler.sendEmptyMessage(MESSAGE_PIN);
        }
        else if (status == HuPlayer.STATUS_PAUSE || status == HuPlayer.STATUS_COMPLETED)
        {
            handler.sendEmptyMessage(MESSAGE_MOVE);
        }
    }

    @Override
    public void onScreenChange(boolean isFullScreen)
    {
        if (isFullScreen)
        {
            handler.sendEmptyMessage(MESSAGE_PIN);
        }
        else if (!isFullScreen && !player.isPlaying())
        {
            handler.sendEmptyMessage(MESSAGE_MOVE);
        }
    }

    @Override
    public void onControllerChange(boolean isShowing)
    {
        // TODO: Implement this method
    }

    @Override
    public void onBufferingUpdate(int percent)
    {
        //Toast.makeText(this,""+percent,5000).show();
    }


    @Override
    public void onSeekComplete()
    {
        //Toast.makeText(this,"onSeekComplete",1000).show();
    }

    @Override
    public void onPlayerCreate()
    {
        // Toast.makeText(this,"播放器创建完成",1000).show();
    }



    @Override
    public void onPrepared()
    {
        fab_play.setVisibility(View.GONE);

    }

    @Override
    public void onComplete()
    {
        Toast.makeText(getApplicationContext(), "video play completed", Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onError(int what, int extra)
    {

        Toast.makeText(this, "未知错误" + "----" + what + "------" + extra, Toast.LENGTH_SHORT).show();


    }

    @Override
    public void onInfo(int what, int extra)
    {
        switch (what)
        {

            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                //do something when buffering start!
                // Toast.makeText(this,"开始缓冲",1000).show();
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                //do something when buffering end
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                //download speed
                //((TextView) findViewById(R.id.tv_speed)).setText(Formatter.formatFileSize(getApplicationContext(),extra)+"/s");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:

                //do something when video rendering
                // findViewById(R.id.tv_speed).setVisibility(View.GONE);
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
        }
    }








    @Override
    protected void onPause()
    {
        super.onPause();
        if (player != null)
        {
            player.onPause();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (player != null)
        {
            player.onResume();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (player != null)
        {
            player.onDestroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (player != null)
        {
            player.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (player != null && player.onBackPressed())
        {
            return;
        }
        super.onBackPressed();
    }



}
