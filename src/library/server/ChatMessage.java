package library.server;

import java.time.LocalDateTime;

public class ChatMessage {
    private String username;
    private String message;
    private LocalDateTime dateSent;

    // Constructor
    public ChatMessage(String username, String message, LocalDateTime dateSent) {
        this.username = username;
        this.message = message;
        this.dateSent = dateSent;
    }

    // Getter and Setter methods
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateSent() {
        return dateSent;
    }

    public void setDateSent(LocalDateTime dateSent) {
        this.dateSent = dateSent;
    }
}
