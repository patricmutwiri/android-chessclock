package com.chess.ui.fragments.welcome;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.chess.R;
import com.chess.statics.WelcomeHolder;
import com.chess.ui.engine.configs.CompGameConfig;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.articles.ArticleCategoriesFragmentTablet;
import com.chess.ui.interfaces.FragmentTabsFace;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 19.05.13
 * Time: 17:01
 */
public class WelcomeTabsFragment extends CommonLogicFragment implements FragmentTabsFace {

	public static final int WELCOME_FRAGMENT = 0;
	public static final int SIGN_IN_FRAGMENT = 1;
	public static final int SIGN_UP_FRAGMENT = 2;
	public static final int GAME_FRAGMENT = 3;

	private View leftTabBtn;
	private View rightTabBtn;
	private CompGameConfig config;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		config = new CompGameConfig.Builder().build();

		changeInternalFragment(GameWelcomeCompFragment.createInstance(this, config));
		if (WelcomeHolder.getInstance().isFullscreen()) {
			changeInternalFragment(WELCOME_FRAGMENT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_welcome_tabs_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		enableSlideMenus(false);

		leftTabBtn = view.findViewById(R.id.leftTabBtn);
		leftTabBtn.setOnClickListener(this);

		view.findViewById(R.id.centerTabBtn).setOnClickListener(this);

		rightTabBtn = view.findViewById(R.id.rightTabBtn);
		rightTabBtn.setOnClickListener(this);
		showActionBar(false);
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);

		if (view.getId() == R.id.leftTabBtn) {
			openSignInFragment();
		} else if (view.getId() == R.id.centerTabBtn) {
			changeInternalFragment(WELCOME_FRAGMENT);
		} else if (view.getId() == R.id.rightTabBtn) {
			openSingUpFragment();
		}
	}

	@Override
	public void changeInternalFragment(int code) {
		if (code == WELCOME_FRAGMENT) {
			openInternalFragment(WelcomeTourFragment.createInstance(this));
		} else if (code == SIGN_IN_FRAGMENT) {
			openSignInFragment();
		} else if (code == SIGN_UP_FRAGMENT) {
			openSingUpFragment();
		} else if (code == GAME_FRAGMENT) {
			config.setMode(getAppData().getCompGameMode());
			config.setStrength(getAppData().getCompLevel());
			changeInternalFragment(GameWelcomeCompFragment.createInstance(this, config));
		}
	}

	public void openSignInFragment() {
		CommonLogicFragment fragment = (CommonLogicFragment) findFragmentByTag(SignInFragment.class.getSimpleName());
		if (fragment == null) {
			fragment = new SignInFragment();
		}
		getActivityFace().openFragment(fragment);
	}

	private void openSingUpFragment() {
		CommonLogicFragment fragment = (CommonLogicFragment) findFragmentByTag(SignUpFragment.class.getSimpleName());
		if (fragment == null) {
			fragment = new SignUpFragment();
		}
		getActivityFace().openFragment(fragment);
	}

	@Override
	public void onPageSelected(int page) {
		if (page == WelcomeTourFragment.SIGN_UP_PAGE) {
			leftTabBtn.setVisibility(View.GONE);
			rightTabBtn.setVisibility(View.GONE);
		} else {
			leftTabBtn.setVisibility(View.VISIBLE);
			rightTabBtn.setVisibility(View.VISIBLE);
		}
	}

	private void changeInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.tab_content_frame, fragment, fragment.getClass().getSimpleName());
		transaction.commit();
	}

	private void openInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.tab_content_frame, fragment, fragment.getClass().getSimpleName());
		transaction.addToBackStack(fragment.getClass().getSimpleName());
		transaction.commit();
	}

	@Override
	public boolean showPreviousFragment() {
		if (getActivity() == null) {
			return false;
		}
		int entryCount = getChildFragmentManager().getBackStackEntryCount();
		if (entryCount > 0) {
			return getChildFragmentManager().popBackStackImmediate();
		} else {
			return super.showPreviousFragment();
		}
//		if (getChildFragmentManager().getBackStackEntryCount() > 0) {
////			FragmentManager.BackStackEntry entry = getChildFragmentManager().getBackStackEntryAt(0);
////			if (entry!= null && entry.getName().equals(WelcomeTourFragment.class.getSimpleName()) && openWelcomeFragment){
////				getChildFragmentManager().popBackStackImmediate();
////				openWelcomeFragment = false;
////				return true;
////			} else {
////				return false;
////			}
//		} else {
//			return false;
//		}
	}

}
