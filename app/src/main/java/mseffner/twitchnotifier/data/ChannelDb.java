package mseffner.twitchnotifier.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ChannelContract.ChannelEntry;


public class ChannelDb {

    private final Resources resources;
    private final SharedPreferences preferences;
    private ChannelDbHelper dbHelper;


    public ChannelDb(Context context) {
        dbHelper = ChannelDbHelper.getInstance(context);
        resources = context.getResources();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void updateNewChannelData(List<Channel> channelList) {

        // Get the ids already in the database
        int[] existingIds = getAllChannelIds();
        Set<Integer> existingIdSet = new HashSet<>(existingIds.length);
        for (int id : existingIds) {
            existingIdSet.add(id);
        }

        // Get the ids of the new Channel list
        Set<Integer> newIdSet = new HashSet<>(channelList.size());
        for (Channel channel : channelList) {
            newIdSet.add(channel.getId());
        }

        // Delete any channel from the database that isn't in the new list
        // (This means that the channel was unfollowed)
        for (int existingId : existingIds) {
            if (!newIdSet.contains(existingId)) {
                deleteChannel(existingId);
            }
        }

        // Add any channels that aren't in the database
        // (This means that the channel was newly followed)
        for (Channel channel : channelList) {
            if (!existingIdSet.contains(channel.getId())) {
                insertChannel(channel);
            }
        }

    }

    public int[] getAllChannelIds() {

        Cursor cursor = query(new String[]{ChannelEntry._ID}, null, null, null);

        int[] idArray = new int[cursor.getCount()];
        int idColumnIndex = cursor.getColumnIndex(ChannelEntry._ID);

        int i = 0;
        while (cursor.moveToNext()) {
            idArray[i++] = cursor.getInt(idColumnIndex);
        }

        cursor.close();

        return idArray;
    }

    public List<Channel> getAllChannels() {

        String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");
        String vodcastOffline = resources.getString(R.string.pref_vodcast_offline);
        boolean vodcastOnline = !vodcastSetting.equals(vodcastOffline);

        List<Channel> channelList = new ArrayList<>();

        String sortOrder =
            "CASE " + ChannelEntry.COLUMN_STREAM_TYPE +  // Show online streams first
                " WHEN " + ChannelEntry.STREAM_TYPE_LIVE + " THEN 0" +
                // Show vodcasts as online depending on the setting
                (vodcastOnline ? " WHEN " + ChannelEntry.STREAM_TYPE_VODCAST + " THEN 0" : "") +
                " ELSE 1" +
            " END, " +
            "CASE " + ChannelEntry.COLUMN_PINNED + // Show pinned streams first
                " WHEN " + ChannelEntry.IS_PINNED + " THEN 0" +
                " WHEN " + ChannelEntry.IS_NOT_PINNED + " THEN 1 " +
            " END, " +
            ChannelEntry.COLUMN_VIEWERS + " DESC, " +
            ChannelEntry.COLUMN_DISPLAY_NAME + " COLLATE NOCASE";  // Break ties by display_name

        Cursor cursor = query(null, null, null, sortOrder);

        while (cursor.moveToNext()) {

            // Get all the data from the cursor
            int id = cursor.getInt(cursor.getColumnIndex(ChannelEntry._ID));
            String name = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_NAME));
            String displayName = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_DISPLAY_NAME));
            String channelUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_CHANNEL_URL));
            String logoUrl = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_LOGO_URL));
            int pinned = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
            int streamType = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_STREAM_TYPE));
            String status = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_STATUS));
            String game = cursor.getString(cursor.getColumnIndex(ChannelEntry.COLUMN_GAME));
            int viewers = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_VIEWERS));
            long createdAt = cursor.getInt(cursor.getColumnIndex(ChannelEntry.COLUMN_CREATED_AT));

            // Build the Channel object from the data
            Channel channel = new Channel(id, displayName, name, logoUrl, channelUrl, pinned);
            if (streamType != ChannelEntry.STREAM_TYPE_OFFLINE)
                channel.setStream(new Stream(id, game, viewers, status, createdAt, streamType));

            channelList.add(channel);
        }

        cursor.close();

        return channelList;
    }

    public boolean insertChannel(Channel channel) {

        if (channel == null)
            return false;

        ContentValues values = new ContentValues();

        values.put(ChannelEntry._ID, channel.getId());
        values.put(ChannelEntry.COLUMN_NAME, channel.getName());
        values.put(ChannelEntry.COLUMN_DISPLAY_NAME, channel.getDisplayName());
        values.put(ChannelEntry.COLUMN_LOGO_URL, channel.getLogoUrl());
        values.put(ChannelEntry.COLUMN_CHANNEL_URL, channel.getStreamUrl());
        values.put(ChannelEntry.COLUMN_PINNED, channel.getPinned());

        Stream stream = channel.getStream();
        if (stream != null) {
            values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
            values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
            values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
            values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
            values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());
        }

        long numRowsInserted = insert(values);
        return numRowsInserted == 1;
    }

    public long updateStreamData(Stream stream) {

        if (stream == null)
            return 0;

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_GAME, stream.getCurrentGame());
        values.put(ChannelEntry.COLUMN_VIEWERS, stream.getCurrentViewers());
        values.put(ChannelEntry.COLUMN_STATUS, stream.getStatus());
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, stream.getStreamType());
        values.put(ChannelEntry.COLUMN_CREATED_AT, stream.getCreatedAt());

        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(stream.getChannelId())};

        return update(values, selection, selectionArgs);
    }

    public void toggleChannelPin(Channel channel) {

        if (channel == null)
            return;

        ContentValues values = new ContentValues();
        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(channel.getId())};

        String[] projection = {ChannelEntry.COLUMN_PINNED};
        Cursor channelCursor = query(projection, selection, selectionArgs, null);
        channelCursor.moveToFirst();
        int currentPinnedStatus = channelCursor.getInt(channelCursor.getColumnIndex(ChannelEntry.COLUMN_PINNED));
        channelCursor.close();

        if (currentPinnedStatus == ChannelEntry.IS_PINNED) {
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_NOT_PINNED);
        } else {
            values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_PINNED);
        }

        update(values, selection, selectionArgs);
    }

    public void removeAllPins() {

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_PINNED, ChannelEntry.IS_NOT_PINNED);

        String selection = ChannelEntry.COLUMN_PINNED + "=?";
        String[] selectionArgs = {Integer.toString(ChannelEntry.IS_PINNED)};

        update(values, selection, selectionArgs);
    }

    public long deleteAllChannels() {

        return delete(null, null);
    }

    public void resetAllStreamData() {

        ContentValues values = new ContentValues();
        values.put(ChannelEntry.COLUMN_STREAM_TYPE, ChannelEntry.STREAM_TYPE_OFFLINE);
        values.put(ChannelEntry.COLUMN_GAME, "");
        values.put(ChannelEntry.COLUMN_VIEWERS, 0);
        values.put(ChannelEntry.COLUMN_STATUS, "");
        values.put(ChannelEntry.COLUMN_CREATED_AT, 0);

        update(values, null, null);
    }

    private void deleteChannel(int id) {

        String selection = ChannelEntry._ID + "=?";
        String[] selectionArgs = {Integer.toString(id)};
        delete(selection, selectionArgs);
    }

    private Cursor query(String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        return database.query(ChannelEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
    }

    private long insert(ContentValues contentValues) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.insert(ChannelEntry.TABLE_NAME, null, contentValues);
    }

    private long update(ContentValues contentValues, String selection, String[] selectionArgs) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.update(ChannelEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    private long delete(String selection, String[] selectionArgs) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.delete(ChannelEntry.TABLE_NAME, selection, selectionArgs);
    }
}
