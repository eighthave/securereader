package info.guardianproject.securereaderinterface;
		
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.Settings.UiLanguage;
import info.guardianproject.securereader.SocialReader.SocialReaderLockListener;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.widgets.CustomFontButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontEditText;
import info.guardianproject.securereaderinterface.widgets.CustomFontRadioButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontTextView;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReporter;

import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.tinymission.rss.Feed;

public class App extends MultiDexApplication implements OnSharedPreferenceChangeListener, SocialReaderLockListener
{
	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;
	
	public static final boolean UI_ENABLE_POPULAR_ITEMS = false;
			
	public static final boolean UI_ENABLE_COMMENTS = false;
	public static final boolean UI_ENABLE_TAGS = true;
	public static final boolean UI_ENABLE_POST_LOGIN = false;
	public static final boolean UI_ENABLE_REPORTER = false;
	public static final boolean UI_ENABLE_CHAT = false;
	public static final boolean UI_ENABLE_LANGUAGE_CHOICE = true;
	
	public static final String EXIT_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.exit.action";
	public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.setuilanguage.action";
	public static final String WIPE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.wipe.action";
	public static final String LOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.lock.action";
	public static final String UNLOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.unlock.action";

	public static final String FRAGMENT_TAG_RECEIVE_SHARE = "FragmentReceiveShare";
	public static final String FRAGMENT_TAG_SEND_BT_SHARE = "FragmentSendBTShare";

	private static App m_singleton;

	public static Context m_context;
	public static SettingsUI m_settings;

	public SocialReader socialReader;
	public SocialReporter socialReporter;
	
	private String mCurrentLanguage;
	private FeedFilterType mCurrentFeedFilterType = FeedFilterType.ALL_FEEDS;
	private Feed mCurrentFeed = null;

	private boolean mIsWiping = false;
	
	@Override
	public void onCreate()
	{
		m_singleton = this;
		m_context = this;
		m_settings = new SettingsUI(m_context);
		applyUiLanguage(false);
		super.onCreate();

		socialReader = SocialReader.getInstance(this.getApplicationContext());
		socialReader.setLockListener(this);
		socialReporter = new SocialReporter(socialReader);
		//applyPassphraseTimeout();
		
		m_settings.registerChangeListener(this);
		
		mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
		UICallbacks.getInstance().addListener(new UICallbackListener()
		{
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				Feed feed = null;
				if (type == FeedFilterType.SINGLE_FEED)
				{
					feed = getFeedById(feedId);
				}
				mCurrentFeedFilterType = type;
				mCurrentFeed = feed;
			}
		});
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	public static Context getContext()
	{
		return m_context;
	}

	public static App getInstance()
	{
		return m_singleton;
	}

	public static SettingsUI getSettings()
	{
		return m_settings;
	}

	private Bitmap mTransitionBitmap;

	private LockScreenActivity mLockScreen;

	public Bitmap getTransitionBitmap()
	{
		return mTransitionBitmap;
	}

	public void putTransitionBitmap(Bitmap bitmap)
	{
		mTransitionBitmap = bitmap;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(Settings.KEY_UI_LANGUAGE))
		{
			applyUiLanguage(true);
		}
		else if (key.equals(Settings.KEY_PASSPHRASE_TIMEOUT))
		{
			applyPassphraseTimeout();
		}
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		applyUiLanguage(false);
	}

	@SuppressLint("NewApi")
	private void applyUiLanguage(boolean sendNotifications)
	{
		// Update language!
		//
		String language = m_settings.uiLanguageCode();
		if (language.equals(mCurrentLanguage))
			return;
		mCurrentLanguage = language;
		setCurrentLanguageInConfig(m_context);

		// Notify activities (if any)
		if (sendNotifications)
			LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
	}

	public void setCurrentLanguageInConfig(Context context)
	{
		Configuration config = new Configuration();
		String language = m_settings.uiLanguageCode();
		Locale loc = new Locale(language);
		if (Build.VERSION.SDK_INT >= 17)
			config.setLocale(loc);
		else
			config.locale = loc;
		Locale.setDefault(loc);
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
	}

	private void applyPassphraseTimeout()
	{
		socialReader.setCacheWordTimeout(m_settings.passphraseTimeout());
	}

	public void wipe(Context context, int wipeMethod)
	{
		mIsWiping = true;
		socialReader.doWipe(wipeMethod);
		m_settings.resetSettings();
		mLastResumed = null;

		// Notify activities (if any)
		LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.WIPE_BROADCAST_ACTION));

		mIsWiping = false;
		if (wipeMethod == SocialReader.DATA_WIPE)
		{
			Intent intent = new Intent(m_context, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}
	
	public static View createView(String name, Context context, AttributeSet attrs)
	{
		int id = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "id", -1);
		if (Build.VERSION.SDK_INT < 11)
		{
			// Older devices don't support setting the "android:alertDialogTheme" in styles.xml
			int idParent = Resources.getSystem().getIdentifier("parentPanel", "id", "android");
			if (id == idParent)
				context.setTheme(R.style.ModalDialogTheme);
		}
		
		if (name.equals("TextView") || name.endsWith("DialogTitle"))
		{
			return new CustomFontTextView(context, attrs);
		}
		else if (name.equals("Button"))
		{
			View view = null;
			if (id == android.R.id.button1) // Positive button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonPositiveTheme), attrs);
			else if (id == android.R.id.button2) // Negative button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonNegativeTheme), attrs);
			else
				view = new CustomFontButton(context, attrs);		
			return view;
		}
		else if (name.equals("RadioButton"))
		{
			return new CustomFontRadioButton(context, attrs);
		}
		else if (name.equals("EditText"))
		{
			return new CustomFontEditText(context, attrs);
		}
		return null;
	}

	private int mnResumed = 0;
	private Activity mLastResumed;
	private boolean mIsLocked = true;
	
	public void onActivityPause(Activity activity)
	{
		mnResumed--;
		if (mnResumed == 0)
			socialReader.onPause();
		if (mLastResumed == activity)
			mLastResumed = null;
	}

	public void onActivityResume(Activity activity)
	{
		mLastResumed = activity;
		mnResumed++;
		if (mnResumed == 1)
			socialReader.onResume();
		showLockScreenIfLocked();
	}
	
	public boolean isActivityLocked()
	{
		return mIsLocked;
	}
	
	private void showLockScreenIfLocked()
	{
		if (mIsLocked && mLastResumed != null && mLockScreen == null && !mIsWiping)
		{
			Intent intent = new Intent(App.this, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra("originalIntent", mLastResumed.getIntent());
			mLastResumed.startActivity(intent);
			mLastResumed.overridePendingTransition(0, 0);
			mLastResumed = null;
		}
	}
	
	@Override
	public void onLocked()
	{
		mIsLocked = true;
		showLockScreenIfLocked();
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(LOCKED_BROADCAST_ACTION));
	}

	@Override
	public void onUnlocked()
	{
		mIsLocked = false;
		if (mLockScreen != null)
			mLockScreen.onUnlocked();
		mLockScreen = null;
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(UNLOCKED_BROADCAST_ACTION));
	}

	public void onLockScreenResumed(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = lockScreenActivity;
	}

	public void onLockScreenPaused(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = null;
	}
	
	private Feed getFeedById(long idFeed)
	{
		ArrayList<Feed> items = socialReader.getSubscribedFeedsList();
		for (Feed feed : items)
		{
			if (feed.getDatabaseId() == idFeed)
				return feed;
		}
		return null;
	}
	
	public FeedFilterType getCurrentFeedFilterType()
	{
		return mCurrentFeedFilterType;
	}
	
	public Feed getCurrentFeed()
	{
		return mCurrentFeed;
	}
	
	public long getCurrentFeedId()
	{
		if (getCurrentFeed() != null)
			return getCurrentFeed().getDatabaseId();
		return 0;
	}

	/**
	 * Update the current feed property. Why is this needed? Because if the feed was just
	 * updated from the network a new Feed object will have been created and we want to
	 * pick up changes to the network pull date (and possibly other changes) here.
	 * @param feed
	 */
	public void updateCurrentFeed(Feed feed)
	{
		if (mCurrentFeed != null && mCurrentFeed.getDatabaseId() == feed.getDatabaseId())
			mCurrentFeed = feed;
	}
}
