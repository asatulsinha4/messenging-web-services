package com.springapplication.messenging.messengingwebservices.dto.test;

import java.util.List;

public class FriendsDTO extends BaseDTO {
    private List<String> friends;

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }
    
}
