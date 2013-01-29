package com.chess.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import com.chess.R;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.SendLiveMessageTask;
import com.chess.lcc.android.interfaces.LccChatMessageListener;
import com.chess.model.MessageItem;
import com.chess.ui.adapters.MessagesAdapter;
import com.chess.utilities.AppUtils;

import java.util.List;

public class ChatLiveActivity extends LiveBaseActivity implements LccChatMessageListener {

	private EditText sendEdt;
	private ListView chatListView;
	private MessagesAdapter messagesAdapter;

	private MessageUpdateListener messageUpdateListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_screen);

		sendEdt = (EditText) findViewById(R.id.sendEdt);
		chatListView = (ListView) findViewById(R.id.chatLV);
		findViewById(R.id.sendBtn).setOnClickListener(this);

		messageUpdateListener = new MessageUpdateListener();
	}

	@Override
	protected void onResume() {
		super.onResume();

		showKeyBoard(sendEdt);

		if (getLccHolder() != null) {
		    getLccHolder().setLccChatMessageListener(this);
		    updateList();
		}
	}

	@Override
	protected void onLiveServiceConnected() {
		super.onLiveServiceConnected();

		messagesAdapter = new MessagesAdapter(ChatLiveActivity.this, getLccHolder().getMessagesList());
		chatListView.setAdapter(messagesAdapter);
		showActionRefresh = true;

		showKeyBoard(sendEdt);
		getLccHolder().setLccChatMessageListener(this);
		updateList();
	}

	private void updateList() {
		List<MessageItem> chatItems = getLccHolder().getMessagesList();
		messagesAdapter.setItemsList(chatItems);
		chatListView.post(new AppUtils.ListSelector((chatItems.size() - 1), chatListView));
	}

	@Override
	public void onMessageReceived(){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateList();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				updateList();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.sendBtn) {
			Long gameId = getLccHolder().getCurrentGameId();
			new SendLiveMessageTask(messageUpdateListener, getTextFromField(sendEdt), getLccHolder()).execute(gameId);
			updateList();

			sendEdt.setText(StaticData.SYMBOL_EMPTY);
		}
	}

	private class MessageUpdateListener extends ActionBarUpdateListener<String> {

		public MessageUpdateListener() {
			super(getInstance());
		}

		@Override
		public void updateData(String returnedObj) {
			messagesAdapter.setItemsList(getLccHolder().getMessagesList());
		}
	}

}
