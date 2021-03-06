package mseffner.twitchnotifier.networking;


import java.util.ArrayList;
import java.util.List;

/**
 * Defines container classes for use with Gson.
 */
public class Containers {

    /**
     * Used for follows endpoint
     * https://api.twitch.tv/helix/users/follows
     */
    public class Follows {

        public class Data {
            public String from_id;
            public String to_id;
            public String followed_at;
        }

        public class Pagination {
            public String cursor;
        }

        public int total;
        public List<Data> data = new ArrayList<>();
        public Pagination pagination;
    }

    /**
     * Used for users endpoint
     * https://api.twitch.tv/helix/users
     */
    public class Users {

        public class Data {
            public String id;
            public String login;
            public String display_name;
            public String type;
            public String broadcaster_type;
            public String description;
            public String profile_image_url;
            public String offline_image_url;
            public int view_count;
        }

        public List<Data> data = new ArrayList<>();
    }

    /**
     * Used for streams endpoint
     * https://api.twitch.tv/helix/streams
     */
    public class Streams {

        public class Data {
            public String id;
            public String user_id;
            public String game_id;
            public String type;
            public String title;
            public int viewer_count;
            public String started_at;
            public String language;
            public String thumbnail_url;
        }

        public class Pagination {
            public String cursor;
        }

        public List<Data> data = new ArrayList<>();
        public Pagination pagination;
    }

    /**
     * Used for streams endpoint in API v5
     * https://api.twitch.tv/kraken/channels/
     */
    public class StreamsLegacy {

        public class Data {

            public class Channel {
                public String status;
                public String language;
                public int _id;
                public String name;
            }

            public class Preview {
                public String template;
            }

            public String game;
            public int viewers;
            public String created_at;
            public String stream_type;

            public Preview preview;
            public Channel channel;
        }

        public int _total;
        public List<Data> streams = new ArrayList<>();
    }

    /**
     * Used for games endpoint
     * https://api.twitch.tv/helix/games
     */
    public class Games {

        public class Data {
            public String id;
            public String name;
            public String box_art_url;
        }

        public List<Data> data = new ArrayList<>();
    }
}
