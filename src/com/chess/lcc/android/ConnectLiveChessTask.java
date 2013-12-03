package com.chess.lcc.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import com.chess.backend.RestHelper;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.backend.tasks.AbstractUpdateTask;
import com.chess.live.client.*;
import com.chess.live.client.impl.HttpClientProvider;
import com.chess.statics.AppData;
import com.chess.statics.StaticData;
import com.chess.utilities.LogMe;
import org.eclipse.jetty.client.HttpClient;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * ConnectLiveChessTask class
 *
 * @author alien_roger
 * @created at: 11.06.12 20:35
 */
public class ConnectLiveChessTask extends AbstractUpdateTask<LiveChessClient, Void> {

	private static final String TAG = "LCCLOG-ConnectLiveChessTask";

	//static MemoryUsageMonitor muMonitor = new MemoryUsageMonitor(15);

	public static final String LIVE_HOST_PRODUCTION = "chess.com";
	public static final String LIVE_HOST_TEST = "chess-2.com";

	private static final int MAX_BACKOFF_INTERVAL = 10000;
	private static final long WS_CONNECT_TIMEOUT = 10000L;
	private static final int WS_MAX_MESSAGE_SIZE = 1024 * 1024;

	private boolean useCurrentCredentials;
	private LccHelper lccHelper;

	public  ConnectLiveChessTask(TaskUpdateInterface<LiveChessClient> taskFace, boolean useCurrentCredentials, LccHelper lccHelper) {
		this(taskFace, lccHelper);
		this.useCurrentCredentials = useCurrentCredentials;
	}

	public ConnectLiveChessTask(TaskUpdateInterface<LiveChessClient> taskFace, LccHelper lccHelper) {
		super(taskFace);
		this.lccHelper = lccHelper;
	}

	@Override
	protected Integer doTheTask(Void... params) {
		Context context = getTaskFace().getMeContext();

		try {

			String versionName = null;
			try {
				versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			} catch (PackageManager.NameNotFoundException e) {
				result = StaticData.UNKNOWN_ERROR;
			}
			versionName += ", OS: " + android.os.Build.VERSION.RELEASE + ", " + android.os.Build.MODEL;

			AppData appData = new AppData(context);

			LogMe.forceLog(TAG, "User " + appData.getUsername() + ", " + versionName, context);
			LogMe.dl(TAG, "Start Chess.Com LCC ");
			LogMe.dl(TAG, "Connecting to: " + getConfigBayeuxHost());

			HttpClient httpClient = HttpClientProvider.getHttpClient(HttpClientProvider.DEFAULT_CONFIGURATION, false);

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
			} else {
				httpClient.setConnectorType(HttpClient.CONNECTOR_SOCKET); // Android 2.2
			}

			/*LogMe.dl(TAG, "INITIAL httpClient.getTimeout() = " + httpClient.getTimeout());
			LogMe.dl(TAG, "INITIAL httpClient.getSoTimeout() = " + httpClient.getSoTimeout());
			LogMe.dl(TAG, "INITIAL getIdleTimeout = " + httpClient.getIdleTimeout());
			LogMe.dl(TAG, "INITIAL httpClient.getConnectTimeout() = " + httpClient.getConnectTimeout());*/

			httpClient.setMaxConnectionsPerAddress(2);
			//httpClient.setSoTimeout(11000);
			httpClient.setConnectTimeout(10000); // 75000 is default
			httpClient.setTimeout(10000); // 320000 is default

			List<ConnectionConfiguration> conConfs = new LinkedList<ConnectionConfiguration>();

			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO) { // Android 2.2
				LogMe.dl(TAG, "Support HTTP Live transport");
				conConfs.add(createHttpConnectionConfig(httpClient));

			} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { // prior to 4.1
				LogMe.dl(TAG, "Support WS and HTTP Live transports");
				conConfs.add(createWSConnectionConfig());
				conConfs.add(createHttpConnectionConfig(httpClient));

			} else { // 4.1+
				LogMe.dl(TAG, "Support WSS and HTTP Live transports");
				conConfs.add(createWSSConnectionConfig());
				conConfs.add(createHttpConnectionConfig(httpClient));
			}

			item = LiveChessClientFacade.createClient(getAuthUrl(), conConfs);

			item.setClientInfo("Android", versionName, "No-Key");

			item.setSupportedClientFeature(ClientFeature.AnnounceService, true);
			item.setSupportedClientFeature(ClientFeature.AdminService, true); // UPDATELCC todo: check

			item.setSupportedClientFeature(ClientFeature.GenericGameSupport, true);
			item.setSupportedClientFeature(ClientFeature.GenericChatSupport, true);
			item.setSupportedClientFeature(ClientFeature.GameObserve, true);

			//PublicChatsBasic
			//PublicChatsFull
			//PrivateChats
			//MultiGames
			//GameObserve
			//MultiGameObserve
			//Tournaments
			//ExamineBoards
			//PingService

			item.setMaxBackoffInterval(MAX_BACKOFF_INTERVAL);

			httpClient.start();
		} catch (IOException e) {
//            e.printStackTrace();
			result = StaticData.NO_NETWORK;
			LogMe.dl(TAG, e.toString());
		} catch (Exception e) {
			result = StaticData.UNKNOWN_ERROR;
			throw new LiveChessClientException("Unable to initialize HttpClient", e);
		}

		//lccHelper.updateNetworkType(null); // todo: probably reset networkTypeName somewhere else?
		//lccHelper.setConnectionFailure(false);
		lccHelper.setLiveChessClient(item);
		lccHelper.performConnect(useCurrentCredentials);
		return StaticData.RESULT_OK;
	}

	private SimpleHttpConnectionConfiguration createHttpConnectionConfig(HttpClient httpClient) {
		return new SimpleHttpConnectionConfiguration(httpClient, ClientTransport.HTTP, getHttpConnectionUrl());
	}

	private SimpleWebSocketConnectionConfiguration createWSConnectionConfig() {
		return new SimpleWebSocketConnectionConfiguration(ClientTransport.WS, getWSConnectionUrl(), WS_CONNECT_TIMEOUT,
				WS_MAX_MESSAGE_SIZE, false);
	}

	private SimpleWebSocketConnectionConfiguration createWSSConnectionConfig() {
		return new SimpleWebSocketConnectionConfiguration(ClientTransport.WSS, getWSSConnectionUrl(), WS_CONNECT_TIMEOUT,
				WS_MAX_MESSAGE_SIZE, false);
	}

	/*private SimpleHttpConnectionConfiguration createHttpsConnectionConfig(HttpClient httpClient) {
		return new SimpleHttpConnectionConfiguration(httpClient, ClientTransport.HTTPS, HTTPS_URL);
	}*/

	private String getLiveHost() {
		return RestHelper.HOST.equals(RestHelper.HOST_PRODUCTION) ? LIVE_HOST_PRODUCTION : LIVE_HOST_TEST;
	}

	private String getAuthUrl() {
		return "http://www." + getLiveHost() + "/api/v2/login" + "?username=%s&password=%s";
	}

	private String getConfigBayeuxHost() {
		return "live." + getLiveHost();
	}
	private String getWSConnectionUrl() {
		return "ws://" + getConfigBayeuxHost() + ":80/cometd";
	}

	private String getWSSConnectionUrl() {
		return "wss://" + /*"zsldfngsdlf.com"*/ getConfigBayeuxHost() + ":443/cometd";
	}

	private String getHttpConnectionUrl() {
		return "http://" + getConfigBayeuxHost() + ":80/cometd";
	}

	/*private String getHttpsConnectionUrl() {
		return "https://" + getConfigBayeuxHost() + ":443/cometd";
	}*/

}