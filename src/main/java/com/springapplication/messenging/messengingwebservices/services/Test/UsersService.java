package com.springapplication.messenging.messengingwebservices.services.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.springapplication.messenging.messengingwebservices.dto.test.CreateDTO;
import com.springapplication.messenging.messengingwebservices.dto.test.FriendRequestDTO;
import com.springapplication.messenging.messengingwebservices.Exceptions.CustomException;
import com.springapplication.messenging.messengingwebservices.db.TestDb;
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
            if (TestDb.AllUsers.contains(username)) {
                body.setStatusCode(400);
                body.setReason("already exists");
                body.setStatus("failure");
                log.warn("user " + username + " already exists");
            } else {
                TestDb.AllUsers.add(username);
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
            if (!TestDb.AllUsers.contains(username)) {
                body.setStatus("failure");
                body.setReason("no user found ");
                body.setStatusCode(400);
                log.warn("user: " + username + " does not exist!");
            } else {
                try {
                    List<String> friendList = new ArrayList<>(TestDb.Friends.get(username));
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
            if (TestDb.AllUsers.contains(user1) && TestDb.AllUsers.contains(user2)) {
                TestDb.FriendRequest.putIfAbsent(user1, new HashSet<>());
                TestDb.FriendRequest.putIfAbsent(user2, new HashSet<>());
                TestDb.Friends.putIfAbsent(user1, new HashSet<>());
                if(TestDb.FriendRequest.get(user1).contains(user2)) throw new CustomException("request already sent");
                if(TestDb.Friends.get(user1).contains(user2)) throw new CustomException(user1+" and "+user2+" are already friends");
                if(TestDb.FriendRequest.get(user2).contains(user1)){
                    log.info("friend request found from " + user2 + " to " + user1);
                    TestDb.FriendRequest.get(user2).remove(user1);
                    TestDb.Friends.putIfAbsent(user1, new HashSet<>());
                    TestDb.Friends.putIfAbsent(user2, new HashSet<>());
                    TestDb.Friends.get(user1).add(user2);
                    TestDb.Friends.get(user2).add(user1);
                    log.info("request accepted :: " + user1 + " and " + user2 + " have become friends");
                }else{
                    TestDb.FriendRequest.get(user1).add(user2);
                    log.info(user1 + " sent request successfully to " + user2);
                }
                body.setStatusCode(202);
                body.setStatus("success");
            } else {
                String users = "";
                if(!TestDb.AllUsers.contains(user1)) users = users+ user1 + " ";
                if(!TestDb.AllUsers.contains(user2)) users = users + user2 + " ";
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
