package com.chess.backend.tasks;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.backend.statics.StaticData;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Scanner;

public abstract class AbstractUpdateTask<ItemType, Input> extends AsyncTask<Input, Void, Integer> {

	private static final String TAG = "AbstractUpdateTask";
	private TaskUpdateInterface<ItemType> taskFace; // SoftReferences & WeakReferences are not reliable, because they become killed even at the same activity and task become unfinished
	protected ItemType item;
	protected List<ItemType> itemList;
	protected boolean useList;
	protected int result;

	public AbstractUpdateTask(TaskUpdateInterface<ItemType> taskFace) {
		this.taskFace = taskFace;
		useList = taskFace.useList();
		result = StaticData.EMPTY_DATA;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		blockScreenRotation(true);
		try{
			if(getTaskFace().getMeContext() != null)
				getTaskFace().showProgress(true);
		}catch (IllegalStateException ex){
			Log.d(TAG,"onPreExecute " +ex.toString());
		}
	}

	@Override
	protected Integer doInBackground(Input... params) {
		if(isCancelled()) {
			result = StaticData.EMPTY_DATA;
			return result;
		}
		return doTheTask(params);
	}

	protected abstract Integer doTheTask(Input... params);

	protected void blockScreenRotation(boolean block){
		try {

			Context context = getTaskFace().getMeContext();
			if (context instanceof Activity) {
				Activity activity = (Activity) context;
				if(block){
					// Stop the screen orientation changing during an event
					if(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
						activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					}else{
						activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					}
				} else {
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				}
			}
		} catch (IllegalStateException ex) {
			Log.d(TAG, "blockScreenRotation " + ex.toString());
		}
	}

	@Override
	protected void onCancelled(Integer result) {
		super.onCancelled(result);
		blockScreenRotation(false);
		try{
			getTaskFace().errorHandle(StaticData.TASK_CANCELED);
		} catch (IllegalStateException ex) {
			Log.d(TAG, "getTaskFace().errorHandle fails, due to killed state" + ex.toString());
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();

		releaseTaskFace();
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		blockScreenRotation(false);

		if(isCancelled()) {   // no need to check as we catch it
			return;
		}

		try{
			getTaskFace().showProgress(false);

			if (result == StaticData.RESULT_OK) {
				if (useList)
					getTaskFace().updateListData(itemList);
				else
					getTaskFace().updateData(item);
			} else {
				getTaskFace().errorHandle(result);
			}

			releaseTaskFace();
		} catch (IllegalStateException ex) {
			Log.d(TAG, "getTaskFace() at onPostExecute fails, due to killed state" + ex.toString());
		}
	}

	protected void releaseTaskFace() {
		if (taskFace != null) {
			taskFace.releaseContext();
			taskFace = null;
		}
	}

	protected TaskUpdateInterface<ItemType> getTaskFace() throws IllegalStateException{
		if (taskFace == null ) {
			throw new IllegalStateException("TaskFace is already dead");
		} else {
			return taskFace;
		}
	}

	public AbstractUpdateTask<ItemType, Input> executeTask(Input... input){
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB){
			executeOnExecutor(THREAD_POOL_EXECUTOR, input);
		} else {
			execute(input);
		}
		return this;
	}

	protected static String convertStreamToString(java.io.InputStream is) {
		Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}
}
