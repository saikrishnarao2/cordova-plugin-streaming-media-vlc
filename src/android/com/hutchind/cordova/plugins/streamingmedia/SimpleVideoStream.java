package com.hutchind.cordova.plugins.streamingmedia;
		import android.app.Activity;
		import android.content.BroadcastReceiver;
		import android.content.Context;
		import android.content.Intent;
		import android.content.IntentFilter;
		import android.content.res.Configuration;
		import android.graphics.Color;
		import android.net.Uri;
		import android.os.Bundle;
		import android.util.Log;
		import android.view.Gravity;
		import android.view.SurfaceHolder;
		import android.view.SurfaceView;
		import android.view.ViewGroup.LayoutParams;
		import android.view.Window;
		import android.view.WindowManager;
		import android.widget.FrameLayout;
		import android.widget.Toast;

		import org.videolan.libvlc.IVLCVout;
		import org.videolan.libvlc.LibVLC;
		import org.videolan.libvlc.Media;
		import org.videolan.libvlc.MediaPlayer;

		import java.lang.ref.WeakReference;
		import java.util.ArrayList;


public class SimpleVideoStream extends Activity implements IVLCVout.Callback {
	public final static String TAG = "SimpleVideoStream";
	private String mFilePath;
	private SurfaceView mSurface;
	private SurfaceHolder holder;
	private LibVLC libvlc;
	private MediaPlayer mMediaPlayer = null;
	private int mVideoWidth;
	private int mVideoHeight;
	private Boolean mShouldAutoClose = true;
	private SurfaceView surf;
	private Bundle extras;
	private BroadcastReceiver receiver;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

//Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		FrameLayout frameLayout=new FrameLayout(this);
		frameLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		frameLayout.setBackgroundColor(Color.BLACK);
		surf =new SurfaceView(this);
		surf.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		frameLayout.addView(surf);
		//addRule(FrameLayout.CENTER_IN_PARENT, FrameLayout.TRUE);
		//setContentView(R.layout.activity_main);
		setContentView(frameLayout);
		Bundle b = getIntent().getExtras();
		mFilePath = b.getString("mediaUrl");
		mShouldAutoClose = b.getBoolean("shouldAutoClose");
		mShouldAutoClose = mShouldAutoClose == null ? true : mShouldAutoClose;
		// mFilePath = "rtmp://202.53.13.21/live/stream3";//rtmp://rrbalancer.broadcast.tneg.de:1935/pw/ruk/ruk";

		Log.d(TAG, "Playing: " + mFilePath);
		mSurface = (SurfaceView) surf;//findViewById(R.id.surface);
		holder = mSurface.getHolder();
		extras = getIntent().getExtras();
		if (this.receiver == null) {
			this.receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.v("VLC BroadcastReceiver",intent.toString());
					String action = intent.getAction();
					Log.v("VLC BroadcastReceiver",action);
					if (action.equals("finish_activity")) {
						Log.v("VLC BroadcastReceiver","got intent");
						finish();
						// DO WHATEVER YOU WANT.
					}
				}
			};
			registerReceiver(this.receiver,  new IntentFilter("finish_activity"));
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setSize(mVideoWidth, mVideoHeight);
	}

	@Override
	protected void onResume() {
		super.onResume();
		createPlayer(mFilePath);
	}

	@Override
	protected void onPause() {
		super.onPause();
		releasePlayer();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releasePlayer();
	}


	/**
	 * Used to set size for SurfaceView
	 *
	 * @param width
	 * @param height
	 */
	private void setSize(int width, int height) {
		mVideoWidth = width;
		mVideoHeight = height;
		if (mVideoWidth * mVideoHeight <= 1)
			return;

		if (holder == null || mSurface == null)
			return;

		int w = getWindow().getDecorView().getWidth();
		int h = getWindow().getDecorView().getHeight();
		int w1 = getWindow().getDecorView().getWidth();
		int h1 = getWindow().getDecorView().getHeight();
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (w > h && isPortrait || w < h && !isPortrait) {
			int i = w;
			w = h;
			h = i;
		}

		float videoAR = (float) mVideoWidth / (float) mVideoHeight;
		float screenAR = (float) w / (float) h;

		if (screenAR < videoAR)
			h = (int) (w / videoAR);
		else
			w = (int) (h * videoAR);

		holder.setFixedSize(w1,h1);//mVideoWidth, mVideoHeight);
		LayoutParams lp = mSurface.getLayoutParams();
		lp.width = w1;
		lp.height = h1;
		mSurface.setLayoutParams(lp);
		mSurface.invalidate();
	}

	/**
	 * Creates MediaPlayer and plays video
	 *
	 * @param media
	 */
	private void createPlayer(String media) {
		releasePlayer();
		try {
			if (media.length() > 0) {

              /*  Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();*/
				Log.d(TAG,media);
			}

			// Create LibVLC
			// TODO: make this more robust, and sync with audio demo
			ArrayList<String> options = new ArrayList<String>();
			//options.add("--subsdec-encoding <encoding>");
			options.add("--aout=opensles");
			options.add("--audio-time-stretch"); // time stretching
			options.add("-vvv"); // verbosity
			libvlc = new LibVLC(this, options);
			holder.setKeepScreenOn(true);

			// Creating media player
			mMediaPlayer = new MediaPlayer(libvlc);
			mMediaPlayer.setEventListener(mPlayerListener);

			// Seting up video output
			final IVLCVout vout = mMediaPlayer.getVLCVout();
			vout.setVideoView(mSurface);
			//vout.setSubtitlesView(mSurfaceSubtitles);
			vout.addCallback(this);
			vout.attachViews();

			Media m = new Media(libvlc, Uri.parse(media));
			mMediaPlayer.setMedia(m);
			mMediaPlayer.play();
		} catch (Exception e) {
			Toast.makeText(this, "Error: Couldn't play!", Toast
					.LENGTH_LONG).show();
		}
	}

	private void releasePlayer() {
		if (libvlc == null)
			return;
		mMediaPlayer.stop();
		final IVLCVout vout = mMediaPlayer.getVLCVout();
		vout.removeCallback(this);
		vout.detachViews();
		holder = null;
		libvlc.release();
		libvlc = null;

		mVideoWidth = 0;
		mVideoHeight = 0;
		if (this.receiver != null) {
			try {
				unregisterReceiver(this.receiver);
				this.receiver = null;
			} catch (Exception e) {
				Log.e(TAG, "Error unregistering media receiver of VLC: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Registering callbacks
	 */
	private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

	@Override
	public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
		if (width * height == 0)
			return;

		// store video size
		mVideoWidth = width;
		mVideoHeight = height;
		setSize(mVideoWidth, mVideoHeight);
	}

	@Override
	public void onSurfacesCreated(IVLCVout vout) {

	}

	@Override
	public void onSurfacesDestroyed(IVLCVout vout) {

	}

	@Override
	public void onHardwareAccelerationError(IVLCVout vlcVout) {
		Log.e(TAG, "Error with hardware acceleration");
		this.releasePlayer();
		Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
	}

	private static class MyPlayerListener implements MediaPlayer.EventListener {
		private WeakReference<SimpleVideoStream> mOwner;

		public MyPlayerListener(SimpleVideoStream owner) {
			mOwner = new WeakReference<SimpleVideoStream>(owner);
		}

		@Override
		public void onEvent(MediaPlayer.Event event) {
			SimpleVideoStream player = mOwner.get();

			switch (event.type) {
				case MediaPlayer.Event.EndReached:
					Log.d(TAG, "MediaPlayerEndReached");
					player.releasePlayer();
					break;
				case MediaPlayer.Event.Playing:
				case MediaPlayer.Event.Paused:
				case MediaPlayer.Event.Stopped:
				default:
					break;
			}
		}
	}
}
