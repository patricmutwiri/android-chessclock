package com.chess.backend;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.chess.R;
import com.chess.backend.interfaces.AbstractUpdateListener;
import com.chess.backend.statics.AppData;
import com.chess.lcc.android.LccHolder;
import com.chess.lcc.android.interfaces.LccConnectionUpdateFace;
import com.chess.live.client.LiveChessClient;
import com.chess.ui.activities.LiveScreenActivity;

public class LiveChessService extends Service {

	private static final String TAG = "LCCLOG-LiveChessService";

	private ServiceBinder serviceBinder = new ServiceBinder();

	// or move holder code to Service itself.
	// but in this case we should have ability to reset holder data when it is necessary, for instance logout
	private LccHolder lccHolder;
	private LccConnectionUpdateFace connectionUpdateFace;

	public void setConnectionUpdateFace(LccConnectionUpdateFace connectionUpdateFace) {
		this.connectionUpdateFace = connectionUpdateFace;
	}


	public class ServiceBinder extends Binder {
		public LiveChessService getService(){
			Log.d(TAG, "SERVICE: getService called");
			return LiveChessService.this;
		}
	}

	public IBinder onBind(Intent intent) {
		Log.d(TAG, "SERVICE: onBind");
		if (lccHolder == null) {
			lccHolder = new LccHolder(getContext(), this, new LccConnectUpdateListener());
			Log.d(TAG, "SERVICE: holder created");
		}
		return serviceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "SERVICE: onUnbind ");
		return super.onUnbind(intent);

		// TODO should be used to release resources
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {   // we should never use this because we are not starting service, refer to {@link http://developer.android.com/guide/components/services.html#Lifecycle}
		Log.d(TAG, "SERVICE: onStartCommand");

		return START_STICKY_COMPATIBILITY;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "SERVICE: onDestroy");
		if (lccHolder != null) {
			lccHolder.logout();
			lccHolder = null;
		}
		stopForeground(true);
	}

	public void checkAndConnect() {
		Log.d(TAG, "AppData.isLiveChess(getContext()) " + AppData.isLiveChess(getContext()));

		Log.d(TAG, "lccHolder.getClient() " + lccHolder.getClient());

 		if (AppData.isLiveChess(getContext()) && !lccHolder.isConnected()
				&& lccHolder.getClient() == null) { // prevent creating several instances when user navigates between activities in "reconnecting" mode
			lccHolder.runConnectTask();
		} else if (lccHolder.isConnected()) {
			onLiveConnected();
		}
	}

	public void onLiveConnected() {
		if (connectionUpdateFace != null) {
			connectionUpdateFace.onConnected();
		}
	}


	public class LccConnectUpdateListener extends AbstractUpdateListener<LiveChessClient> {
		public LccConnectUpdateListener() {
			super(getContext());
		}

		@Override
		public void updateData(LiveChessClient returnedObj) {
			Log.d(TAG, "LiveChessClient initialized " + returnedObj);

			// todo: tune notification
			Notification notification = new Notification(R.drawable.ic_stat_live, // just test. change drawable
					getString(R.string.chess_com_live),
					System.currentTimeMillis());

			Intent intent = new Intent(getContext(), LiveScreenActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);

			notification.setLatestEventInfo(getContext(), getString(R.string.ches_com), getString(R.string.live), pendingIntent);
			notification.flags |= Notification.FLAG_NO_CLEAR;

			startForeground(R.id.live_service_notification, notification);

			onLiveConnected();
		}
	}

	private Context getContext(){
		return this;
	}

	public LccHolder getLccHolder() {
		return lccHolder;
	}
}
