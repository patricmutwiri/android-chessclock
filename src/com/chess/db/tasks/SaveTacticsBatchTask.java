package com.chess.db.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.chess.backend.entity.new_api.TacticItem;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.AbstractUpdateTask;
import com.chess.db.DBConstants;
import com.chess.db.DBDataManager;

import java.util.ArrayList;
import java.util.List;


//public class SaveTacticsBatchTask extends AbstractUpdateTask<TacticItemOld, Long> {
public class SaveTacticsBatchTask extends AbstractUpdateTask<TacticItem.TacticsData, Long> {

    private ContentResolver contentResolver;
	private final List<TacticItem.TacticsData> tacticsBatch;
	private static String[] arguments = new String[2];

	public SaveTacticsBatchTask(TaskUpdateInterface<TacticItem.TacticsData> taskFace, List<TacticItem.TacticsData> tacticsBatch,
								ContentResolver resolver) {
        super(taskFace);
		this.tacticsBatch = new ArrayList<TacticItem.TacticsData>();
		this.tacticsBatch.addAll(tacticsBatch);
		this.contentResolver = resolver;
    }

    @Override
    protected Integer doTheTask(Long... ids) {
		Context context = getTaskFace().getMeContext();
		if (context == null) {
			return StaticData.INTERNAL_ERROR;
		}
		String userName = AppData.getUserName(context);
		synchronized (tacticsBatch) {
			for (TacticItem.TacticsData tacticItem : tacticsBatch) {
				tacticItem.setUser(userName);
				arguments[0] = String.valueOf(tacticItem.getId());
				arguments[1] = userName;

				Uri uri = DBConstants.TACTICS_BATCH_CONTENT_URI;
				Cursor cursor = contentResolver.query(uri, DBDataManager.PROJECTION_TACTIC_ITEM_ID_AND_USER,
						DBDataManager.SELECTION_TACTIC_ID_AND_USER, arguments, null);
				if (cursor.moveToFirst()) {
					contentResolver.update(Uri.parse(uri.toString() + DBDataManager.SLASH_ + DBDataManager.getId(cursor)),
							DBDataManager.putTacticItemToValues(tacticItem), null, null);
				} else {
					contentResolver.insert(uri, DBDataManager.putTacticItemToValues(tacticItem));
				}

				cursor.close();
			}
		}

        result = StaticData.RESULT_OK;

        return result;
    }


}
