package com.example.audioplayerm3u8

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


@ExperimentalCoroutinesApi
class VideoPlayerActivity : AppCompatActivity(), Player.Listener {
    private lateinit var simpleExoplayer: SimpleExoPlayer
    private var playbackPosition: Long = 0
    private lateinit var videoUrl: String
    private lateinit var titleText: AppCompatTextView
    private lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectivityObserver = NetworkConnectionObserver(applicationContext)
        setContentView(R.layout.activity_video_player)
        this.titleText = findViewById(R.id.header_tv)

        // We collect the flow and based on the status of the network connection
        // and we show the appropriate toast to the user.
        connectivityObserver.observe().onEach {
            when (it) {
                ConnectivityObserver.Status.Available -> Toast.makeText(
                    this,
                    "Network Available",
                    Toast.LENGTH_LONG
                ).show()
                ConnectivityObserver.Status.Losing -> Toast.makeText(
                    this,
                    "Weak Network",
                    Toast.LENGTH_LONG
                ).show()
                ConnectivityObserver.Status.Unavailable -> Toast.makeText(
                    this,
                    "Network Unavailable",
                    Toast.LENGTH_LONG
                ).show()
                ConnectivityObserver.Status.Lost -> Toast.makeText(
                    this,
                    "Network Lost",
                    Toast.LENGTH_LONG
                ).show()
                //else -> Toast.makeText(this, "Network Status Unknown", Toast.LENGTH_LONG).show()
            }
        }.launchIn(lifecycleScope)

        val bundle = intent.extras
        videoUrl = bundle!!.getString("videoUrl").toString()
        fullScreen()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    // Building instance of the ExoPlayer.
    private fun initializePlayer() {
        simpleExoplayer = SimpleExoPlayer.Builder(this).build()
        preparePlayer(videoUrl)
    }

    private fun preparePlayer(videoUrl: String) {
        val uri = Uri.parse(videoUrl)
        val mediaSource = buildMediaSource(uri)
        titleText.text = uri.lastPathSegment
        simpleExoplayer.setMediaSource(mediaSource, false)
        simpleExoplayer.playWhenReady = true
        simpleExoplayer.addListener(this)
        val playerViewFullscreen = findViewById<PlayerView>(R.id.playerViewFullscreen)
        playerViewFullscreen.player = simpleExoplayer
        simpleExoplayer.seekTo(playbackPosition)
        simpleExoplayer.prepare()
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val lastPathSegment = uri.lastPathSegment
        return if (lastPathSegment?.contains("mp4") == true) {
            ProgressiveMediaSource.Factory(DefaultDataSourceFactory(this, "channel-video-player"))
                .createMediaSource(MediaItem.fromUri(uri))
        } else if (lastPathSegment?.contains("m3u8") == true) {
            HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                .createMediaSource(MediaItem.fromUri(uri))
        } else if (lastPathSegment?.contains("ts") == true) {
            val defaultExtractorsFactory = DefaultExtractorsFactory()
            defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
            return ProgressiveMediaSource.Factory(
                DefaultDataSourceFactory(this, "channel-video-player"),
                defaultExtractorsFactory
            ).createMediaSource(MediaItem.fromUri(uri))
        } else {
            val dashChunkSourceFactory = DefaultDashChunkSource.Factory(
                DefaultHttpDataSource.Factory()
            )
            val manifestDataSourceFactory = DefaultHttpDataSource.Factory()
            DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
    }

    private fun releasePlayer() {
        playbackPosition = simpleExoplayer.currentPosition
        simpleExoplayer.release()
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        // handle error add more logs related to error occurance.
        Log.e("VideoPlayerActivity", "error: $error")
        Toast.makeText(this,"Please check your internet connection!", Toast.LENGTH_LONG).show()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        if (playbackState == Player.STATE_BUFFERING) {
            progressBar.visibility = View.VISIBLE
        } else if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED)
            progressBar.visibility = View.INVISIBLE
    }

    private fun fullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}