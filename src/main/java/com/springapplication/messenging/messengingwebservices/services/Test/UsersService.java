package com.springapplication.messenging.messengingwebservices.services.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.springapplication.messenging.messengingwebservices.Exceptions.test.CustomException;
import com.springapplication.messenging.messengingwebservices.db.Test.db;
import com.springapplication.messenging.messengingwebservices.dto.test.CreateDTO;
import com.springapplication.messenging.messengingwebservices.dto.test.FriendRequestDTO;
import com.springapplication.messenging.messengingwebservices.dto.test.FriendsDTO;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * This service is used for creating a user
 * sending friend request and retrieving 
 * friend list
 * 
 * @author Atul Sinha
 * 
 */
@Slf4j
@Service
public class UsersService {

    // Logger log = LoggerFactory.getLogger(UsersService.class);

    /**
     * This creates a user.
     * 
     * @param username
     * @return
     */
    public CreateDTO createUser(String username) {
        CreateDTO body = new CreateDTO();
        log.info("Request received for creating user: " + username);
        try {
            if (db.AllUsers.contains(username)) {
                body.setStatusCode(400);
                body.setReason("already exists");
                body.setStatus("failure");
                log.warn("user " + username + " already exists");
            } else {
                db.AllUsers.add(username);
                body.setStatusCode(201);
                body.setUsername(username);
                body.setStatus("success");
                log.info("created user: " + username + " successfully");
            }
        } catch (Exception exception) {
            log.error("Exception occured while creating user: " + username + " :: " + exception.getMessage());
        }
        return body;
    }

    /**
     * 
     * @param username
     * @return list of all friends of the username
     */
    public FriendsDTO getFriendList(String username) {
        log.info("Received request to fetch friend list for user: " + username);
        FriendsDTO body = new FriendsDTO();
        try {
            if (!db.AllUsers.contains(username)) {
                body.setStatus("failure");
                body.setReason("no user found ");
                body.setStatusCode(400);
                log.warn("user: " + username + " does not exist!");
            } else {
                try {
                    List<String> friendList = new ArrayList<>(db.Friends.get(username));
                    friendList.get(0);
                    body.setFriends(friendList);
                    body.setStatusCode(200);
                    log.info("Successfully retrieved friend list for user: " + username);
                } catch (Exception exception) {
                    body.setStatusCode(404);
                    body.setStatus("failure");
                    body.setReason("no Friends found");
                    log.info("Empty friend list found for user: " + username);
                }
            }
        } catch (Exception exception) {
            log.error("Error occurred while searching for friend list for user: " + username + " :: "
                    + exception.getMessage());
        }
        return body;

    }

    /**
     * This function sends friend request from user1 to user2.
     * If request was sent from user2 to user1 earlier, then
     * friend request will be accepted
     * 
     * @param user1
     * @param user2
     * @return status
     */
    public FriendRequestDTO requestResponse(String user1, String user2) {
        log.info("request received to send friend request from " + user1 + " to " + user2);
        FriendRequestDTO body = new FriendRequestDTO();
        try {
            if (db.AllUsers.contains(user1) && db.AllUsers.contains(user2)) {
                db.FriendRequest.putIfAbsent(user1, new HashSet<>());
                db.FriendRequest.putIfAbsent(user2, new HashSet<>());
                db.Friends.putIfAbsent(user1, new HashSet<>());
                if(db.FriendRequest.get(user1).contains(user2)) throw new CustomException("request already sent");
                if(db.Friends.get(user1).contains(user2)) throw new CustomException(user1+" and "+user2+" are already friends");
                if(db.FriendRequest.get(user2).contains(user1)){
                    log.info("friend request found from " + user2 + " to " + user1);
                    db.FriendRequest.get(user2).remove(user1);
                    db.Friends.putIfAbsent(user1, new HashSet<>());
                    db.Friends.putIfAbsent(user2, new HashSet<>());
                    db.Friends.get(user1).add(user2);
                    db.Friends.get(user2).add(user1);
                    log.info("request accepted :: " + user1 + " and " + user2 + " have become friends");
                }else{
                    db.FriendRequest.get(user1).add(user2);
                    log.info(user1 + " sent request successfully to " + user2);
                }
                body.setStatusCode(202);
                body.setStatus("success");
            } else {
                String users = "";
                if(!db.AllUsers.contains(user1)) users = users+ user1 + " ";
                if(!db.AllUsers.contains(user2)) users = users + user2 + " ";
                throw new CustomException("users: "+users+"do not exist");
            }
        } catch (Exception exception) {
            body.setStatus("failure");
            body.setReason("Exception : " + exception.getMessage());
            body.setStatusCode(400);
            log.error("error occurred while sending friend request from " + user1 + " to " + user2 + " :: "
                    + exception.getMessage());
        }

        return body;
    }

}
