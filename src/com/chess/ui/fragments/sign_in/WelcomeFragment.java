package com.chess.ui.fragments.sign_in;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.*;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.RegisterItem;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.FlurryData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.utilities.AppUtils;
import com.flurry.android.FlurryAgent;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.slidingmenu.lib.SlidingMenu;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 25.02.13
 * Time: 19:54
 */
public class WelcomeFragment extends ProfileSetupsFragment implements YouTubePlayer.OnInitializedListener,
		YouTubePlayer.OnFullscreenListener {

	private static final int PAGE_CNT = 5;
	private static final int SIGN_UP_PAGE = 4;

	private static final long ANIMATION_DELAY = 2000;
	private static final long REPEAT_TIMEOUT = 6000;
	private static final int DURATION = 450;
	public static final String YOUTUBE_DEMO_LINK = "AgTQJUhK2MY";
	private static final String YOUTUBE_FRAGMENT_TAG = "youtube fragment";

	private Interpolator accelerator = new AccelerateInterpolator();
	private Interpolator decelerator = new DecelerateInterpolator();

	private ObjectAnimator flipFirstHalf;

	private RadioGroup homePageRadioGroup;
	private LayoutInflater inflater;
	private ViewPager viewPager;
	private YouTubePlayerSupportFragment youTubePlayerFragment;
	private View youTubeFrameContainer;
	private boolean youtubeFragmentGoFullScreen;
	private View bottomButtonsLay;

	// SignUp Part

	protected Pattern emailPattern = Pattern.compile("[a-zA-Z0-9\\._%\\+\\-]+@[a-zA-Z0-9\\.\\-]+\\.[a-zA-Z]{2,4}");
	protected Pattern gMailPattern = Pattern.compile("[a-zA-Z0-9\\._%\\+\\-]+@[g]");   // TODO use for autoComplete

	private EditText userNameEdt;
	private EditText emailEdt;
	private EditText passwordEdt;
	private EditText passwordRetypeEdt;

	private String userName;
	private String email;
	private String password;
	private RegisterUpdateListener registerUpdateListener;
	private YouTubePlayer youTubePlayer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_welcome_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getActivityFace().setTouchModeToSlidingMenu(SlidingMenu.TOUCHMODE_NONE);

		inflater = LayoutInflater.from(getActivity());

		viewPager = (ViewPager) view.findViewById(R.id.viewPager);
		WelcomePagerAdapter mainPageAdapter = new WelcomePagerAdapter();
		viewPager.setAdapter(mainPageAdapter);
		viewPager.setOnPageChangeListener(pageChangeListener);

		homePageRadioGroup = (RadioGroup) view.findViewById(R.id.pagerIndicatorGroup);
		for (int i = 0; i < PAGE_CNT; ++i) {
			inflater.inflate(R.layout.new_page_indicator_view, homePageRadioGroup, true);
		}

		view.findViewById(R.id.signUpBtn).setOnClickListener(this);
		view.findViewById(R.id.signInBtn).setOnClickListener(this);

		((RadioButton) homePageRadioGroup.getChildAt(0)).setChecked(true);

		bottomButtonsLay = view.findViewById(R.id.bottomButtonsLay);

		{// SignUp part
			registerUpdateListener = new RegisterUpdateListener();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		handler.postDelayed(startAnimation, ANIMATION_DELAY);
	}

	@Override
	public void onPause() {
		super.onPause();

		handler.removeCallbacks(startAnimation);

		if (!youtubeFragmentGoFullScreen) {
			releaseYouTubeFragment();
		}
	}

	private final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {

		@Override
		public void onPageSelected(int position) {
			if (position == 4) {
				bottomButtonsLay.setVisibility(View.GONE);
				homePageRadioGroup.setVisibility(View.GONE);
			} else {
				hideKeyBoard();
				bottomButtonsLay.setVisibility(View.VISIBLE);
				homePageRadioGroup.setVisibility(View.VISIBLE);
				((RadioButton) homePageRadioGroup.getChildAt(position)).setChecked(true);
			}

			if (position == 0) { // add youTubeView to control visibility to first welcome frame
				initYoutubeFragment();
			} else {
				releaseYouTubeFragment();
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int position) {
		}
	};

	public void showNextPage() {
		int currentItem = viewPager.getCurrentItem();
		if (currentItem <= PAGE_CNT) {
			viewPager.setCurrentItem(currentItem + 1, true);
		}
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.signInBtn) {
			getActivityFace().openFragment(new SignInFragment());
		} else if (view.getId() == R.id.signUpBtn) {
			getActivityFace().openFragment(new SignUpFragment());
		} else if (view.getId() == R.id.playBtn) {
			initYoutubeFragment();
			youTubeFrameContainer.setVisibility(View.VISIBLE);
		} else if (view.getId() == R.id.whatChessComTxt) {
			showNextPage();
		} else if (view.getId() == R.id.completeSignUpBtn) {
			if (!checkRegisterInfo()) {
				return;
			}

			if (!AppUtils.isNetworkAvailable(getActivity())) {
				popupItem.setPositiveBtnId(R.string.wireless_settings);
				showPopupDialog(R.string.warning, R.string.no_network, BasePopupsFragment.NETWORK_CHECK_TAG);
				return;
			}

			submitRegisterInfo();
		}
	}

	private void initYoutubeFragment() {
		youTubePlayerFragment = new YouTubePlayerSupportFragment();
		getFragmentManager().beginTransaction()
				.replace(R.id.youTubeFrameContainer, youTubePlayerFragment, YOUTUBE_FRAGMENT_TAG)
				.commit();
		youTubePlayerFragment.initialize(AppConstants.YOUTUBE_DEVELOPER_KEY, this);
	}

	private void releaseYouTubeFragment() {
		if (youTubeFrameContainer != null)
			youTubeFrameContainer.setVisibility(View.GONE);
		if (youTubePlayerFragment != null) {
			getFragmentManager().beginTransaction()
					.detach(youTubePlayerFragment)
					.commit();
			youTubePlayerFragment = null;
		}
	}

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
		if (!wasRestored) {
			youTubePlayer.cueVideo(YOUTUBE_DEMO_LINK);
		}
		this.youTubePlayer = youTubePlayer;
		youTubePlayer.setOnFullscreenListener(this);
	}

	private static final int RECOVERY_DIALOG_REQUEST = 1;

	@Override
	public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
		if (errorReason.isUserRecoverableError()) {
			errorReason.getErrorDialog(getActivity(), RECOVERY_DIALOG_REQUEST).show();
		} else {
			String errorMessage = String.format("error_player", errorReason.toString());
			Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onFullscreen(boolean youtubeFragmentGoFullScreen) {
		this.youtubeFragmentGoFullScreen = youtubeFragmentGoFullScreen;
	}

	/**
	 *
	 * @return true player was previously initiated and fullscreen was turned off
	 */
	public boolean hideYoutubeFullScreen() {
		if (youTubePlayer != null) {
			youTubePlayer.setFullscreen(false);
			return true;
		} else {
			return false;
		}
	}

	public boolean swipeBackFromSignUp() {
		if (viewPager.getCurrentItem() == SIGN_UP_PAGE) {
			viewPager.setCurrentItem(SIGN_UP_PAGE - 1, true);
			return true;
		} else {
			return false;
		}
	}

	private class WelcomePagerAdapter extends PagerAdapter {

		private RelativeLayout firstView;
		private RelativeLayout secondView;
		private RelativeLayout thirdView;
		private RelativeLayout fourthView;
		private RelativeLayout signUpView;
		private boolean initiatedFirst;
		private boolean initiatedSecond;
		private boolean initiatedThird;
		private boolean initiatedFour;

		@Override
		public int getCount() {
			return PAGE_CNT;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View view = null;
			switch (position) {
				case 0:
					if (firstView == null) {
						firstView = (RelativeLayout) inflater.inflate(R.layout.new_welcome_one_frame, container, false);
					}
					view = firstView;

					// add youTubeView to control visibility
					youTubeFrameContainer = firstView.findViewById(R.id.youTubeFrameContainer);
					youTubeFrameContainer.setVisibility(View.GONE);

					int orientation = getResources().getConfiguration().orientation; // auto-init for fullscreen
					if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
						Fragment fragmentByTag = getFragmentManager().findFragmentByTag(YOUTUBE_FRAGMENT_TAG);
						youTubePlayerFragment = (YouTubePlayerSupportFragment) fragmentByTag;
						getFragmentManager().beginTransaction()
								.replace(R.id.youTubeFrameContainer, youTubePlayerFragment, YOUTUBE_FRAGMENT_TAG)
								.commit();
						youTubePlayerFragment.initialize(AppConstants.YOUTUBE_DEVELOPER_KEY, WelcomeFragment.this);
					}

					if (!initiatedFirst) {
						{// add ImageView back
							ImageView imageView = new ImageView(getContext());
							imageView.setAdjustViewBounds(true);
							imageView.setScaleType(ImageView.ScaleType.FIT_XY);
							imageView.setImageResource(R.drawable.img_welcome_back);
							imageView.setId(R.id.firstBackImg);

							int screenWidth = getResources().getDisplayMetrics().widthPixels;
							int imageHeight = screenWidth / 2;

							RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(screenWidth, imageHeight);
							params.addRule(RelativeLayout.CENTER_IN_PARENT);
							firstView.addView(imageView, 0, params);
						}

						view.findViewById(R.id.playBtn).setOnClickListener(WelcomeFragment.this);

						View whatChessComTxt = view.findViewById(R.id.whatChessComTxt);
						whatChessComTxt.setOnClickListener(WelcomeFragment.this);

						flipFirstHalf = ObjectAnimator.ofFloat(whatChessComTxt, "rotationX", 0f, 90f);
						flipFirstHalf.setDuration(DURATION);
						flipFirstHalf.setInterpolator(accelerator);

						final ObjectAnimator flipSecondHalf = ObjectAnimator.ofFloat(whatChessComTxt, "rotationX", -90f, 0f);
						flipSecondHalf.setDuration(DURATION);
						flipSecondHalf.setInterpolator(decelerator);

						flipFirstHalf.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator anim) {
								flipSecondHalf.start();
							}
						});
						initiatedFirst = true;
					}
					break;
				case 1:
					if (secondView == null) {
						secondView = (RelativeLayout) inflater.inflate(R.layout.new_welcome_two_frame, container, false);
					}
					view = secondView;

					if (!initiatedSecond) {
						ImageView imageView = new ImageView(getContext());
						imageView.setAdjustViewBounds(true);
						imageView.setScaleType(ImageView.ScaleType.FIT_XY);
						imageView.setImageResource(R.drawable.img_welcome_two_back);

						int screenWidth = getResources().getDisplayMetrics().widthPixels;
						int imageHeight = screenWidth / 2;

						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(screenWidth, imageHeight);
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						secondView.addView(imageView, 0, params);

						initiatedSecond = true;
					}
					break;
				case 2:
					if (thirdView == null) {
						thirdView = (RelativeLayout) inflater.inflate(R.layout.new_welcome_three_frame, container, false);
					}
					view = thirdView;

					if (!initiatedThird) {
						ImageView imageView = new ImageView(getContext());
						imageView.setAdjustViewBounds(true);
						imageView.setScaleType(ImageView.ScaleType.FIT_XY);
						imageView.setImageResource(R.drawable.img_welcome_three_back);

						int screenWidth = getResources().getDisplayMetrics().widthPixels;
						int imageHeight = screenWidth / 2;

						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(screenWidth, imageHeight);
						params.addRule(RelativeLayout.CENTER_IN_PARENT);

						thirdView.addView(imageView, 0, params);

						initiatedThird = true;
					}
					break;
				case 3:
					if (fourthView == null) {
						fourthView = (RelativeLayout) inflater.inflate(R.layout.new_welcome_four_frame, container, false);
					}
					view = fourthView;

					if (!initiatedFour) {
						ImageView imageView = new ImageView(getContext());
						imageView.setAdjustViewBounds(true);
						imageView.setScaleType(ImageView.ScaleType.FIT_XY);
						imageView.setImageResource(R.drawable.img_welcome_four_back);

						int screenWidth = getResources().getDisplayMetrics().widthPixels;
						int imageHeight = screenWidth / 2;

						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(screenWidth, imageHeight);
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						fourthView.addView(imageView, 0, params);

						initiatedFour = true;
					}
					break;
				case 4:
					if (signUpView == null) {
						signUpView = (RelativeLayout) inflater.inflate(R.layout.new_welcome_sign_up_frame, container, false);
					}
					view = signUpView;

					userNameEdt = (EditText) view.findViewById(R.id.usernameEdt);
					emailEdt = (EditText) view.findViewById(R.id.emailEdt);
					passwordEdt = (EditText) view.findViewById(R.id.passwordEdt);
					passwordRetypeEdt = (EditText) view.findViewById(R.id.passwordRetypeEdt);
					view.findViewById(R.id.completeSignUpBtn).setOnClickListener(WelcomeFragment.this);

					userNameEdt.addTextChangedListener(new FieldChangeWatcher(userNameEdt));
					emailEdt.addTextChangedListener(new FieldChangeWatcher(emailEdt));
					passwordEdt.addTextChangedListener(new FieldChangeWatcher(passwordEdt));
					passwordRetypeEdt.addTextChangedListener(new FieldChangeWatcher(passwordRetypeEdt));

					setLoginFields(userNameEdt, passwordEdt);
					break;

				default:
					break;
			}

			container.addView(view);

			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object view) {
			container.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}

	private Runnable startAnimation = new Runnable() {
		@Override
		public void run() {
			if (flipFirstHalf != null) { // can be null if we return from savedInstance to non first page
				flipFirstHalf.start();
			}
			handler.postDelayed(this, REPEAT_TIMEOUT);
		}
	};

	private boolean checkRegisterInfo() {
		userName = getTextFromField(userNameEdt);
		email = getTextFromField(emailEdt);
		password = getTextFromField(passwordEdt);

		if (userName.length() < 3) {
			userNameEdt.setError(getString(R.string.too_short));
			userNameEdt.requestFocus();
			return false;
		}

		if (!emailPattern.matcher(getTextFromField(emailEdt)).matches()) {
			emailEdt.setError(getString(R.string.invalidEmail));
			emailEdt.requestFocus();
			return true;
		}

		if (email.equals(StaticData.SYMBOL_EMPTY)) {
			emailEdt.setError(getString(R.string.can_not_be_empty));
			emailEdt.requestFocus();
			return false;
		}

		if (password.length() < 6) {
			passwordEdt.setError(getString(R.string.too_short));
			passwordEdt.requestFocus();
			return false;
		}

		if (!password.equals(passwordRetypeEdt.getText().toString())) {
			passwordRetypeEdt.setError(getString(R.string.pass_dont_match));
			passwordRetypeEdt.requestFocus();
			return false;
		}


		return true;
	}

	private void submitRegisterInfo() {
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_USERS);
		loadItem.setRequestMethod(RestHelper.POST);
		loadItem.addRequestParams(RestHelper.P_USERNAME, userName);
		loadItem.addRequestParams(RestHelper.P_PASSWORD, password);
		loadItem.addRequestParams(RestHelper.P_EMAIL, email);
		loadItem.addRequestParams(RestHelper.P_APP_TYPE, RestHelper.V_ANDROID);

		new RequestJsonTask<RegisterItem>(registerUpdateListener).executeTask(loadItem);
	}

	private class RegisterUpdateListener extends CommonLogicFragment.ChessUpdateListener<RegisterItem> {

		public RegisterUpdateListener() {
			super(RegisterItem.class);
		}

		@Override
		public void showProgress(boolean show) {
			if (show) {
				showPopupHardProgressDialog(R.string.processing_);
			} else {
				if (isPaused)
					return;

				dismissProgressDialog();
			}
		}

		@Override
		public void updateData(RegisterItem returnedObj) {
			FlurryAgent.logEvent(FlurryData.NEW_ACCOUNT_CREATED);
			showToast(R.string.congratulations);

			preferencesEditor.putString(AppConstants.USERNAME, userNameEdt.getText().toString().toLowerCase());
			preferencesEditor.putInt(AppConstants.USER_PREMIUM_STATUS, RestHelper.V_BASIC_MEMBER);
			processLogin(returnedObj.getData());
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {  // TODO restore
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == BasePopupsFragment.NETWORK_REQUEST) {
				submitRegisterInfo();
			}
		}
	}


	private class FieldChangeWatcher implements TextWatcher {
		private EditText editText;

		public FieldChangeWatcher(EditText editText) {
			this.editText = editText;
		}

		@Override
		public void onTextChanged(CharSequence str, int start, int before, int count) {
			if (str.length() > 1) {
				editText.setError(null);
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	}
}