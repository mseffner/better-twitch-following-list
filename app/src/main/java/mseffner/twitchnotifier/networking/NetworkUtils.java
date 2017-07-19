package mseffner.twitchnotifier.networking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


import javax.net.ssl.HttpsURLConnection;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.LiveStream;


public final class NetworkUtils {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    // Note that this is a test client_id and is not used for release versions
    private static final String CLIENT_ID = "6mmva5zc6ubb4j8zswa0fg6dv3y4xw";

    private static final int QUERY_TYPE_CHANNEL = 0;
    private static final int QUERY_TYPE_STREAM = 1;
    private static final int QUERY_TYPE_USER_FOLLOWS = 2;
    private static final int QUERY_TYPE_STREAM_MULTIPLE = 3;

    private static final String TWITCH_API_BASE_URL = "https://api.twitch.tv/kraken/";

    private static final String PATH_CHANNELS = "channels";
    private static final String PATH_STREAMS = "streams";
    private static final String PATH_USERS = "users";
    private static final String PATH_FOLLOWS_CHANNELS = "follows/channels";

    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_CHANNEL = "channel";

    private static final String LIMIT_MAX = "100";


    private NetworkUtils() {}

    public static List<Channel> getUserFollowChannels(String userName) {

        URL followsQueryUrl = buildUrl(userName, QUERY_TYPE_USER_FOLLOWS);
        String followsJsonResponse = makeTwitchQuery(followsQueryUrl);

        List<Channel> channels = new ArrayList<>();
        Map<String, Channel> channelMap = new HashMap<>();

        try {

            JSONArray followsJsonArray = new JSONObject(followsJsonResponse).getJSONArray("follows");

            // Iterate over the array of followed channels
            for (int i = 0; i < followsJsonArray.length(); i++) {

                // Get the JSONObject String for each channel
                String channelJsonString = followsJsonArray.getJSONObject(i)
                        .getJSONObject("channel").toString();

                // Build the Channel object
                Channel channel = getChannelFromJson(channelJsonString);

                if (channel != null) {

                    // Get the logo image
                    String logoUrlString = channel.getLogoUrl();
                    // If a channel has not set a custom logo, logoUrlString will be "null"
                    if (logoUrlString.equals("null")) {
                        logoUrlString = "https://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png";
                    }
                    URL logoUrl = buildChannelLogoQueryURL(logoUrlString);
                    Bitmap logoBmp = getLogoBitmap(logoUrl);
                    channel.setLogoBmp(logoBmp);

                    // Get the stream data
                    URL streamQueryUrl = buildUrl(channel.getName(), QUERY_TYPE_STREAM);
                    String streamJsonResponse = makeTwitchQuery(streamQueryUrl);
                    LiveStream stream = getLiveStreamFromJson(streamJsonResponse);

                    channel.setStream(stream);

                    // Put the channel in the output list
                    channels.add(channel);
                }
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return channels;

    }

    public static List<Channel> getChannels(String[] channelNames) {

        List<Channel> channels = new ArrayList<>();

        for (String channelName : channelNames) {

            // Get the channel data
            URL channelQueryUrl = buildUrl(channelName, QUERY_TYPE_CHANNEL);
            String channelJsonResponse = makeTwitchQuery(channelQueryUrl);
            Channel channel = getChannelFromJson(channelJsonResponse);

            if (channel != null) {

                // Get the logo image
                URL logoUrl = buildChannelLogoQueryURL(channel.getLogoUrl());
                Bitmap logoBmp = getLogoBitmap(logoUrl);
                channel.setLogoBmp(logoBmp);

                // Get the stream data
                URL streamQueryUrl = buildUrl(channelName, QUERY_TYPE_STREAM);
                String streamJsonResponse = makeTwitchQuery(streamQueryUrl);
                LiveStream stream = getLiveStreamFromJson(streamJsonResponse);

                channel.setStream(stream);

                // Put the channel in the output list
                channels.add(channel);
            }
        }

        return channels;
    }

    private static String makeTwitchQuery(URL url) {

        String response;

        HttpsURLConnection urlConnection = openHttpConnection(url);
        if (urlConnection == null)
            return null;

        InputStream inputStream = getInputStreamFromConnection(urlConnection);
        if (inputStream == null) {
            closeConnections(urlConnection, null);
            return null;
        }

        response = readStringFromInputStream(inputStream);

        closeConnections(urlConnection, inputStream);

        return response;
    }

    private static Bitmap getLogoBitmap(URL url) {

        HttpsURLConnection connection = openHttpConnection(url);
        if (connection == null)
            return null;

        InputStream inputStream = getInputStreamFromConnection(connection);
        if (inputStream == null) {
            closeConnections(connection, null);
            return null;
        }

        Bitmap bmp = getBitmapFromInputStream(inputStream);

        closeConnections(connection, inputStream);

        return bmp;
    }

    private static HttpsURLConnection openHttpConnection(URL url) {

        HttpsURLConnection urlConnection = null;

        try {

            urlConnection = setupHttpsURLConnection(url);
            urlConnection.connect();

            // Check the response code, log and return null if it's bad
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(LOG_TAG,  "Error response code: " + responseCode);
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return urlConnection;
    }

    private static InputStream getInputStreamFromConnection(HttpsURLConnection connection) {
        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            closeConnections(connection, null);
            Log.e(LOG_TAG, e.toString());
        }
        return inputStream;
    }

    private static HttpsURLConnection setupHttpsURLConnection(URL url) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10_000);
        urlConnection.setConnectTimeout(15_000);
        return urlConnection;
    }

    private static void closeConnections(HttpsURLConnection urlConnection, InputStream inputStream) {

        if (urlConnection != null)
            urlConnection.disconnect();

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private static URL buildUrl(String query, int queryType) {

        Uri uri = Uri.EMPTY;
        if (queryType == QUERY_TYPE_CHANNEL)
            uri = buildChannelQueryUri(query);
        else if (queryType == QUERY_TYPE_STREAM)
            uri = buildStreamQueryUri(query);
        else if (queryType == QUERY_TYPE_USER_FOLLOWS)
            uri = buildUserFollowsQueryUri(query);
        else if (queryType == QUERY_TYPE_STREAM_MULTIPLE)
            uri = buildMultiStreamQueryUri(query);

        // Convert Uri into URL
        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return url;
    }

    private static URL buildChannelLogoQueryURL(String logoUrl) {
        URL url = null;
        try {
            url = new URL(logoUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return url;
    }

    private static Uri buildStreamQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static Uri buildChannelQueryUri(String channelName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_CHANNELS)
                .appendPath(channelName)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .build();
    }

    private static Uri buildUserFollowsQueryUri(String userName) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_USERS)
                .appendPath(userName)
                .appendEncodedPath(PATH_FOLLOWS_CHANNELS)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .appendQueryParameter(PARAM_LIMIT, LIMIT_MAX)
                .build();
    }

    private static Uri buildMultiStreamQueryUri(String commaSeparatedChannelNames) {
        return Uri.parse(TWITCH_API_BASE_URL).buildUpon()
                .appendPath(PATH_STREAMS)
                .appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID)
                .appendQueryParameter(PARAM_LIMIT, LIMIT_MAX)
                .appendQueryParameter(PARAM_CHANNEL, commaSeparatedChannelNames)
                .build();
    }

    private static String readStringFromInputStream(InputStream inputStream) {

        Scanner scanner = new Scanner(inputStream);
        // Using \A as the delimiter causes the Scanner to read in the InputStream in one chunk
        scanner.useDelimiter("\\A");

        String result = "";
        if (scanner.hasNext()) {
            result = scanner.next();
        }
        scanner.close();

        return result;
    }

    private static Channel getChannelFromJson(String jsonResponse) {

        try {

            JSONObject channelData = new JSONObject(jsonResponse);

            String displayName = channelData.getString("display_name");
            String name = channelData.getString("name");
            String logoUrl = channelData.getString("logo");
            String streamUrl = channelData.getString("url");

            return new Channel(displayName, name, logoUrl, streamUrl);

        }catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return null;
    }

    private static LiveStream getLiveStreamFromJson(String jsonResponse) {

        if (jsonResponse == null)
            return null;

        try {

            JSONObject resultJson = new JSONObject(jsonResponse);
            if (resultJson.isNull("stream"))
                return null;

            JSONObject streamData = resultJson.getJSONObject("stream");
            JSONObject channelData = streamData.getJSONObject("channel");

            String game = streamData.getString("game");
            int viewers = streamData.getInt("viewers");
            String status = channelData.getString("status");
            String streamType = streamData.getString("stream_type");
            String startTime = streamData.getString("created_at");

            return new LiveStream(game, viewers, status, startTime, streamType);

        }catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return null;
    }

    private static Bitmap getBitmapFromInputStream(InputStream inputStream) {
        return BitmapFactory.decodeStream(inputStream);
    }

}
