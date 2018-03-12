package mseffner.twitchnotifier.events;


import java.util.List;

import mseffner.twitchnotifier.data.ListEntry;

public class TopListRefreshedEvent {

    public List<ListEntry> list;

    public TopListRefreshedEvent(List<ListEntry> list) {
        this.list = list;
    }
}
