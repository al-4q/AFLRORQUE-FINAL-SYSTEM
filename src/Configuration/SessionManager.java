/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Configuration;

/**
 *
 * @author Renato Masangcay
 */
public class SessionManager {

    private static SessionManager instance;

    private int userId;
    private String username;
    private String role;
    private boolean loggedIn;

    // Private constructor
    private SessionManager() {
        loggedIn = false;
    }

    // Get single instance
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Set session on login
    public void login(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.loggedIn = true;
    }

    // Clear session on logout
    public void logout() {
        userId = 0;
        username = null;
        role = null;
        loggedIn = false;
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}