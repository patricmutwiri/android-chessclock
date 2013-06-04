package com.chess.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.image_load.ProgressImageView;
import com.chess.backend.statics.StaticData;
import com.chess.db.DBConstants;
import com.chess.model.BaseGameItem;

public class DailyFinishedGamesCursorRightAdapter extends ItemsCursorAdapter {

	protected static final String CHESS_960 = " (960)";
	private final int imageSize;
	private final String drawStr;
	private final String lossStr;
	private final String winStr;


	public DailyFinishedGamesCursorRightAdapter(Context context, Cursor cursor) {
		super(context, cursor);
		imageSize = (int) (resources.getDimension(R.dimen.list_item_image_size_big) / resources.getDisplayMetrics().density);
		lossStr = context.getString(R.string.loss);
		winStr = context.getString(R.string.won);
		drawStr = context.getString(R.string.draw);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = inflater.inflate(R.layout.new_daily_games_item, parent, false);
		ViewHolder holder = new ViewHolder();
		holder.playerImg = (ProgressImageView) view.findViewById(R.id.playerImg);
		holder.playerTxt = (TextView) view.findViewById(R.id.playerNameTxt);
		holder.gameInfoTxt = (TextView) view.findViewById(R.id.timeLeftTxt);

		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) convertView.getTag();

		String gameType = StaticData.SYMBOL_EMPTY;
		if (getInt(cursor, DBConstants.V_GAME_TYPE) == BaseGameItem.CHESS_960) {
			gameType = CHESS_960;
		}

		holder.playerTxt.setText(getString(cursor, DBConstants.V_OPPONENT_NAME) + gameType);

		String result = context.getString(R.string.loss);
		if (getInt(cursor, DBConstants.V_GAME_RESULT) == BaseGameItem.GAME_WON) {
			result = context.getString(R.string.won);
		} else if (getInt(cursor, DBConstants.V_GAME_RESULT) == BaseGameItem.GAME_DRAW) {
			result = context.getString(R.string.draw);
		}
		holder.gameInfoTxt.setText(result);

		//		String avatarUrl = getString(cursor, DBConstants.OP)
		String avatarUrl = "https://s3.amazonaws.com/chess-7/images_users/avatars/erik_small.1.png";
		imageLoader.download(avatarUrl, holder.playerImg, imageSize);
	}

	protected class ViewHolder {
		public ProgressImageView playerImg;
		public TextView playerTxt;
		public TextView gameInfoTxt;
	}
}
