package com.hahafather007.kotutil

import android.media.AudioManager
import android.media.MediaPlayer
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Created by chenpengxiang on 2018/6/15
 */
object MusicUtil {
    private val player = MediaPlayer()
    private val listeners = HashMap<RxController, MediaListener>()
    private var status = MusicStatus.STOP
    private var lastController: RxController? = null
    private var lastDispose: Disposable? = null

    /**
     * @param url 传入音乐的url地址，播放音乐
     */
    fun RxController.playMusic(url: String?) {
        lastDispose?.apply {
            lastController?.rxComposite?.remove(this)
        }

        lastController = this

        if (!url.isValid()) return

        status = MusicStatus.PLAY

        if (player.isPlaying) {
            player.stop()
            player.release()
        }

        @Suppress("DEPRECATION")
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)

        //异步进行音乐播放，以免阻塞线程
        Observable.just(url)
                .map {
                    player.reset()
                    player.setDataSource(it)
                    player.setOnCompletionListener { listeners.forEach { it.value.complete(this) } }
                    player.setOnErrorListener { _, what, extra ->
                        listeners.forEach {
                            "MediaPlayer错误信息：$what  其他错误：$extra".logError()

                            it.value.error(this)
                        }
                        false
                    }
                    player.prepare()
                    player
                }
                .asyncSwitch()
                .disposable(this)
                .doOnSubscribe { lastDispose = it }
                .doOnNext {
                    when (status) {
                        MusicStatus.PLAY -> {
                            player.start()
                            listeners.forEach { it.value.playing(this) }
                        }
                        MusicStatus.STOP -> {
                            player.release()
                            listeners.forEach { it.value.stop(this) }
                        }
                        else -> {
                            listeners.forEach { it.value.pause(this) }
                        }
                    }
                }
                .subscribe()
    }

    /**
     * @param listener 传入一个音乐播放状态的监听器
     */
    fun RxController.addMediaListener(listener: MediaListener) {
        listeners[this] = listener
    }

    /**
     * 移除监听器
     */
    fun RxController.removeMediaListener() {
        listeners.remove(this)
    }

    /**
     * 停止播放并释放资源
     */
    fun RxController.stopMusic() {//停止播放
        status = MusicStatus.STOP

        if (player.isPlaying) {
            player.stop()
            player.release()
        }
    }

    /**
     * 暂停播放
     */
    fun RxController.pauseMusic() {//暂停播放
        status = MusicStatus.PAUSE

        if (player.isPlaying) {
            player.pause()
        }
    }

    /**
     * 继续播放
     */
    fun RxController.continueMusic() {
        status = MusicStatus.PLAY

        player.start()
    }

    interface MediaListener {
        /**
         * 播放发送错误
         *
         * @param controller 可根据该值来判断是否属于对应的类
         */
        fun error(controller: RxController) {}

        /**
         * 播放器准备完成
         */
        fun complete(controller: RxController) {}

        /**
         * 正在播放
         */
        fun playing(controller: RxController) {}

        /**
         * 停止播放
         */
        fun stop(controller: RxController) {}

        /**
         * 暂停播放
         */
        fun pause(controller: RxController) {}
    }

    private enum class MusicStatus {
        PLAY,
        PAUSE,
        STOP
    }
}