package mseffner.twitchnotifier.data;


public class Channel {

    private long id;
    private String displayName;
    private String logoUrl;
    private String streamUrl;
    private int pinned;

    private Stream stream;

    public Channel(long id, String displayName, String logoUrl, String streamUrl, int pinned) {
        this.id = id;
        this.displayName = displayName;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.stream = null;
        this.pinned = pinned;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public Stream getStream() {
        return stream;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public long getId() {
        return id;
    }

    public int getPinned() {
        return pinned;
    }

    public void togglePinned() {
        if (pinned == ChannelContract.ChannelEntry.IS_PINNED) {
            pinned = ChannelContract.ChannelEntry.IS_NOT_PINNED;
        } else {
            pinned = ChannelContract.ChannelEntry.IS_PINNED;
        }
    }

    @Override
    public String toString() {
        return "\nChannel: " +
                "\tid: " + id +
                "\tdisplayName: " + displayName +
                "\tlogoUrl: " + logoUrl +
                "\tstreamUrl: " + streamUrl +
                "\n\t" + stream;
    }
}