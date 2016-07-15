# IjkplayerDemo
视频播放器，基于ijkplayer的简单封装

***********************
### 使用

# 配置AndroidManifest
```javascript
<activity
    android:name=".MainActivity"
    android:configChanges="keyboardHidden|orientation|screenSize"
    android:screenOrientation="user"
    android:label="@string/app_name" >
```

# MainActivity
```javascript
player = new HuPlayer(this);
player.setOnHuplayerListener(this);
player.setOrientationLock(true);
//player.setCover();
player.setPath("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
player.setTitle("测试");
player.createComplete(); 
```
```javascript
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
```
