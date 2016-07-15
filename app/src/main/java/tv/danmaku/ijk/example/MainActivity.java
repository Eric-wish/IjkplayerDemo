package tv.danmaku.ijk.example;

import android.app.*;
import android.os.*;
import android.view.View.OnClickListener;
import android.view.View;
import android.content.Intent;
import tv.danmaku.ijk.media.activity.PlayerActivity;
import tv.danmaku.ijk.media.activity.HuPlayerActivity;
import tv.danmaku.ijk.media.widget.player.HuPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import android.widget.Toast;
import android.widget.TextView;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.CollapsingToolbarLayout;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity implements HuPlayer.OnHuplayerListener,View.OnClickListener
{

    
    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent)
    {
        Toast.makeText(this,""+percent,5000).show();
    }


    @Override
    public void onSeekComplete(IMediaPlayer mp)
    {
        Toast.makeText(this,"onSeekComplete",1000).show();
    }



    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.start){
            //HuPlayerActivity.configPlayer(this).setTitle("测试").setScaleType(HuPlayer.SCALETYPE_FITPARENT).setFullScreenOnly(true).play("/sdcard/test.mp4");
            
           // player.start();
        }
        
    }
    

    @Override
    public void onPlayerCreate()
    {
        Toast.makeText(this,"播放器创建完成",1000).show();
    }
    
    

    @Override
    public void onPrepared(IMediaPlayer mp)
    {
        Toast.makeText(this,"准备完成",1000).show();
    }

    

    @Override
    public void onChange(boolean isShowing)
    {
        // TODO: Implement this method
    }

    @Override
    public void onComplete(IMediaPlayer mp)
    {
        Toast.makeText(getApplicationContext(), "video play completed",Toast.LENGTH_SHORT).show();
        
    }
    

    @Override
    public void onError(IMediaPlayer mp,int what, int extra)
    {
       
        Toast.makeText(this,"未知错误"+"----"+what+"------"+extra,Toast.LENGTH_SHORT).show();
      
        mp.reset();

    }

    @Override
    public void onInfo(int what, int extra)
    {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                //do something when buffering start!
                Toast.makeText(this,"开始缓冲",1000).show();
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

   /*void initView(){
       Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
       setSupportActionBar(mToolbar);
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);
     //  getSupportActionBar().setTitle("");
       
    
       CollapsingToolbarLayout mCollapsingToolbarLayout= (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar_layout);
       
       //mCollapsingToolbarLayout.setTitle("");
   }*/


    private HuPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /*WindowManager.LayoutParams attrs = this.getWindow().getAttributes();
        
           attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            this.getWindow().setAttributes(attrs);
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        */
        
        //this.getWindow().setStatusBarColor(Color.BLACK);
        
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
         
           
        
        setContentView(R.layout.main);
        //initView();
        player = new HuPlayer(this);
        player.setOnHuplayerListener(this);
		//findViewById(R.id.start).setOnClickListener(this);
        new Handler().postDelayed(new Runnable(){

                @Override
                public void run()
                {
                    player.setOrientationLock(true);
                    //player.setCover();
                    
                    player.setPath("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
                    player.setTitle("测试");
                    player.createComplete();
                }
            }, 1000);

	}
    
    
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.onDestroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (player != null) {
            player.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
    
    
}
