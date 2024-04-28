package library.server;

import org.bson.types.ObjectId;

public class LibraryMember {
	private ObjectId id; // Use ObjectId type for the id field
	private String username;
	private String password; // Hashed and salted
	private String email;
	private String status;

	// Constructors, getters, and setters

	public LibraryMember() {
	}

	public LibraryMember(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.status = "no status";
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
