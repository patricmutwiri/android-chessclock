package com.chess.db.tasks;

import android.content.ContentResolver;
import com.chess.R;
import com.chess.backend.entity.api.CommonFeedCategoryItem;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.backend.tasks.AbstractUpdateTask;
import com.chess.db.DbDataManager;
import com.chess.statics.StaticData;

import java.util.ArrayList;
import java.util.List;


public class SaveVideoCategoriesTask extends AbstractUpdateTask<CommonFeedCategoryItem.Data, Long> {

	private ContentResolver contentResolver;
	protected static String[] arguments = new String[1];

	public SaveVideoCategoriesTask(TaskUpdateInterface<CommonFeedCategoryItem.Data> taskFace, List<CommonFeedCategoryItem.Data> currentItems,
								   ContentResolver resolver) {
		super(taskFace, new ArrayList<CommonFeedCategoryItem.Data>());
		this.itemList.addAll(currentItems);

		this.contentResolver = resolver;
	}

	@Override
	protected Integer doTheTask(Long... ids) {

		CommonFeedCategoryItem.Data curriculumCategory = new CommonFeedCategoryItem.Data();

		curriculumCategory.setId(StaticData.CURRICULUM_VIDEOS_CATEGORY_ID);
		curriculumCategory.setName(getTaskFace().getMeContext().getString(R.string.curriculum));
		curriculumCategory.setDisplay_order(0);

		itemList.add(0, curriculumCategory);

		for (CommonFeedCategoryItem.Data currentItem : itemList) {
			DbDataManager.saveVideoCategory(contentResolver, currentItem);
		}

		return StaticData.RESULT_OK;
	}
}
