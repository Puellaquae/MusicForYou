# Music For You, just ENJOY IT!

## Some Ideas

### 根据文件夹自动建立的播放列表

比起手工建，更适合我的使用场景。

### 音乐转发

当你恰好想听 A 手机上的歌，而又因种种原因不能在 A 手机上放（譬如没有 3.5 毫米耳机接口），这时就可以用音乐转发功能，使用 B 手机听 A 手机上的歌（当然不能是单纯的发文件）。

预想方案：使用 NFC 配对，通过蓝牙或网络建立连接，通过 `MediaRecorder` 获取设备音频或者直接发送歌曲音频数据，由对方程序接受播放（B 手机可以选择同步播放或不播放（如果可能的实现的话（好像是可以的，我发现录屏时即使不开声音也能录进声音）））。此时可以由 B 手机接管 A 手机的音量控制，如果恰好的也是用此软件播放的，可以接管全部播放控制（类似于 remote develop 的体验）。

### 心跳式渐进睡眠

常常会发生睡眠计时结束但还没睡着的情况，这时候如果亮屏去继续播放，就要被亮瞎。所以如果在睡眠计时结束后，音量被调节（不用眼镜和亮屏就可以完成），就会重新开始一次睡眠计时，但是计时的时间会一定程度的减少。

### ここ好き

还没想明白

### 播放队列

有时候，是想顺序先听了某几首歌再随机播放其他的。可以加入播放队列，队列内内容顺序播放，队列外再随机或别的（感觉这个哪个别人已经有做了）。
