package mseffner.twitchnotifier.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.SystemClock;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.DarkModeChangedEvent;
import mseffner.twitchnotifier.events.ListModeChangedEvent;
import mseffner.twitchnotifier.networking.Updates;

/**
 * SettingsManager is a class with static methods allowing all of the preferences
 * in SharedPreferences to be accessed easily from anywhere in the app without
 * requiring a context. It stores Context and Resources objects that must be
 * supplied at startup via SettingsManager.initialize().
 */
public class SettingsManager {

    // Public constants for rerun settings
    public static final int RERUN_ONLINE = 0;
    public static final int RERUN_ONLINE_TAG = 1;
    public static final int RERUN_OFFLINE = 2;

    // Public constants for sort order settings
    public static final int SORT_BY_VIEWER_COUNT = 0;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_GAME = 2;
    public static final int SORT_BY_UPTIME = 3;

    // Public constants for list mode setting
    public static final int LIST_MODE_FULL = 0;
    public static final int LIST_MODE_COMPACT = 1;
    public static final int LIST_MODE_MINIMAL = 2;

    // Public constant for invalid username id
    public static final long INVALID_USERNAME_ID = -1;

    public static final long RATE_LIMIT_MILLISECONDS = 60 * 1000;

    private static SharedPreferences sharedPreferences;
    private static Resources resources;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> onSharedPreferenceChanged(key);

    private static String usernameKey;
    private static String usernameIdKey;
    private static String rerunKey;
    private static String darkmodeKey;
    private static String lastUpdatedKey;
    private static String sortByKey;
    private static String sortAscDescKey;
    private static String followsNeedUpdateKey;
    private static String listModeKey;
    private static String counterKey;
    private static String pinsAtTopKey;
    private static String favoritesAtTopKey;

    private SettingsManager() {}

    /**
     * Initializes the SettingsManager singleton.
     *
     * @param sharedPreferences a SharedPreferences object
     * @param resources         a Resources object for resolving Strings
     */
    public static void initialize(SharedPreferences sharedPreferences, Resources resources) {
        SettingsManager.sharedPreferences = sharedPreferences;
        SettingsManager.resources = resources;
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        SettingsManager.usernameKey = resources.getString(R.string.pref_username_key);
        SettingsManager.usernameIdKey = resources.getString(R.string.pref_username_id_key);
        SettingsManager.rerunKey = resources.getString(R.string.pref_vodcast_key);
        SettingsManager.darkmodeKey = resources.getString(R.string.pref_dark_mode);
        SettingsManager.lastUpdatedKey = resources.getString(R.string.last_updated);
        SettingsManager.sortByKey = resources.getString(R.string.pref_order_by_key);
        SettingsManager.sortAscDescKey = resources.getString(R.string.pref_order_ascending_key);
        SettingsManager.followsNeedUpdateKey = resources.getString(R.string.need_follows_update_key);
        SettingsManager.listModeKey = resources.getString(R.string.pref_list_mode_key);
        SettingsManager.counterKey = resources.getString(R.string.pref_counter_key);
        SettingsManager.pinsAtTopKey = resources.getString(R.string.pref_pins_at_top_key);
        SettingsManager.favoritesAtTopKey = resources.getString(R.string.pref_favorites_at_top_key);
    }

    /**
     * Removes the references to SharedPreferences and Resources.
     * This should be called in MainActivity.onDestroy to prevent memory leaks.
     */
    public static void destroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        sharedPreferences = null;
        resources = null;
    }

    /**
     * @return the user's username, may be empty String
     */
    public static String getUsername() {
        return sharedPreferences.getString(usernameKey, "");
    }

    /**
     * @return  the user's id or INVALID_USERNAME_ID
     */
    public static long getUsernameId() {
        long id = sharedPreferences.getLong(usernameIdKey, INVALID_USERNAME_ID);
        /* If we get the default value, then the key is not in the sharedPreferences,
        which means that the user set their username in an older version of the app,
        so we need to get their user id */
        if (id == INVALID_USERNAME_ID)
            Updates.updateUserId();
        return id;
    }

    /**
     * @return whether a valid username is set
     */
    public static boolean validUsername() {
        return !getUsername().equals("") && getUsernameId() != INVALID_USERNAME_ID;
    }

    /**
     * Writes a new username id into persistent storage.
     *
     * @param newId the new username id
     */
    public static void setUsernameId(long newId) {
        sharedPreferences.edit().putLong(usernameIdKey, newId).apply();
    }

    /**
     * Returns the current rerun setting. Compare result to public constants
     * RERUN_ONLINE, RERUN_ONLINE_TAG, and RERUN_OFFLINE to determine what the
     * setting is.
     *
     * @return the rerun setting
     */
    public static int getRerunSetting() {
        String setting = sharedPreferences.getString(rerunKey, "");

        String online = resources.getString(R.string.pref_vodcast_online);
        String onlineTag = resources.getString(R.string.pref_vodcast_online_tag);

        if (setting.equals(online))
            return RERUN_ONLINE;
        else if (setting.equals(onlineTag))
            return RERUN_ONLINE_TAG;
        else
            return RERUN_OFFLINE;
    }

    /**
     * Returns the current order by setting. Compare result to public constants
     * SORT_BY_VIEWER_COUNT, SORT_BY_NAME, SORT_BY_GAME, and SORT_BY_UPTIME to
     * determine what the setting is.
     *
     * @return the sort by setting
     */
    public static int getSortBySetting() {
        String setting = sharedPreferences.getString(sortByKey, "");

        String name = resources.getString(R.string.pref_order_name);
        String game = resources.getString(R.string.pref_order_game);
        String uptime = resources.getString(R.string.pref_order_uptime);

        if (setting.equals(name))
            return SORT_BY_NAME;
        else if (setting.equals(game))
            return SORT_BY_GAME;
        else if (setting.equals(uptime))
            return SORT_BY_UPTIME;
        else
            return SORT_BY_VIEWER_COUNT;
    }

    /**
     * @return true if ascending, false if descending
     */
    public static boolean getSortAscendingSetting() {
        String setting = sharedPreferences.getString(sortAscDescKey, "");
        String asc = resources.getString(R.string.pref_order_ascending);
        return setting.equals(asc);
    }

    /**
     * @return true if dark mode is on, else false
     */
    public static boolean getDarkModeSetting() {
        return sharedPreferences.getBoolean(darkmodeKey, false);
    }

    /**
     * @return true if counter is on, else false
     */
    public static boolean getCounterSetting() {
        return sharedPreferences.getBoolean(counterKey, false);
    }

    /**
     * @return true if pins at top of list is on, else false
     */
    public static boolean getPinsAtTopSetting() {
        return sharedPreferences.getBoolean(pinsAtTopKey, true);
    }

    /**
     * @return true if favorite games at top of list is on, else false
     */
    public static boolean getFavoritesAtTopSetting() {
        return sharedPreferences.getBoolean(favoritesAtTopKey, false);
    }

    /**
     * @return true if compact is on, else false
     */
    public static int getListModeSetting() {
        String setting = sharedPreferences.getString(listModeKey, "");

        String compact = resources.getString(R.string.pref_list_mode_compact);
        String minimal = resources.getString(R.string.pref_list_mode_minimal);

        if (setting.equals(compact))
            return LIST_MODE_COMPACT;
        else if (setting.equals(minimal))
            return LIST_MODE_MINIMAL;
        else
            return LIST_MODE_FULL;
    }

    /**
     * Sets the last updated time.
     */
    public static void setLastUpdated() {
        sharedPreferences.edit().putLong(lastUpdatedKey, SystemClock.elapsedRealtime()).apply();
    }

    /**
     * @return whether or not the follows data needs to be updated
     */
    public static boolean getFollowsNeedUpdate() {
        return sharedPreferences.getBoolean(followsNeedUpdateKey, false);
    }

    /**
     * Sets whether or not the follows data needs to be updated.
     */
    public static void setFollowsNeedUpdate(boolean needsUpdate) {
        sharedPreferences.edit().putBoolean(followsNeedUpdateKey, needsUpdate).apply();
    }

    /**
     * @return whether or not the rate limit has reset
     */
    public static boolean rateLimitReset() {
        long lastTime = sharedPreferences.getLong(lastUpdatedKey, 0L);
        /* Previous versions of the app incorrectly used System.nanoTime to track
        when it was last updated. nanoTime can return negative values, so we need to
        take the abs of this difference to ensure that the rate limit is not permanently
        stuck when the user updates the app from older versions. */
        return Math.abs(SystemClock.elapsedRealtime() - lastTime) > RATE_LIMIT_MILLISECONDS;
    }

    /**
     * Called by preferenceChangeListener when a preference has changed.
     * Notifies all OnSettingsChangedListeners.
     */
    private static void onSharedPreferenceChanged(String key) {
        if (key.equals(usernameKey)) {
            setFollowsNeedUpdate(true);
            Updates.updateUserId();
            ThreadManager.post(Database::deleteAllFollows);
        } else if (key.equals(darkmodeKey))
            EventBus.getDefault().post(new DarkModeChangedEvent(getDarkModeSetting()));
        else if (key.equals(listModeKey))
            EventBus.getDefault().post(new ListModeChangedEvent());
    }
}
