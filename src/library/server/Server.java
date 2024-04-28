package library.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;


import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Server {
	private static final int PORT = 8080;
	public LibraryDatabase database;
	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private ServerSocket serverSocket;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public void run() {
		try {
			// Initialize logger
			Handler consoleHandler = new ConsoleHandler();
			consoleHandler.setLevel(Level.ALL);

			database = new LibraryDatabase(); // Initialize the database
			serverSocket = new ServerSocket(PORT);
			LOGGER.info("Server started on port " + PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				LOGGER.info("New connection: " + clientSocket);

				// Handle the request in a separate thread
				new Thread(() -> handleRequest(clientSocket)).start();
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error starting server", e);
		} finally {
			// Ensure database connection is closed when the server exits
			if (database != null) {
				database.close();
			}
			// Close the server socket
			if (serverSocket != null) {
				try {
					serverSocket.close();
					LOGGER.info("Server socket closed.");
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Error closing server socket", e);
				}
			}
		}
	}

	private void handleRequest(Socket clientSocket) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

			// Read the request line
			String requestLine = in.readLine();
			LOGGER.info("Request: " + requestLine);


			// Handle different request types based on the request line
			if (requestLine != null && requestLine.startsWith("POST /register")) {
				handleRegisterUser(in, out);

			} else if (requestLine != null && requestLine.startsWith("POST /login")) {
				// Handle login request
				handleLogin(in, out);
			} else if (requestLine != null && requestLine.startsWith("GET /library-members")) {
				handleGetAllMembers(out);
			}
			// Add the new route
			else if (requestLine != null && requestLine.startsWith("POST /toggle-member-status")) {
				handleToggleMemberStatus(in, out);
			} else if (requestLine != null && requestLine.startsWith("POST /admin-login")) {
				// Handle login request
				handleLoginAdminLogin(in, out);

			} else if (requestLine != null && requestLine.startsWith("GET /catalog")) {
				// Handle catalog retrieval
				handleCatalog(out);
			} else if (requestLine != null && requestLine.startsWith("POST /add-catalog-item")) {
				handleAddCatalogItem(in, out);
			} else if (requestLine != null && requestLine.startsWith("POST /borrow-item")) {
				// Handle borrowed request
				handleBorrowItem(in, out);
			} 
			else if(requestLine != null && requestLine.startsWith("POST /reset-password")) {
				handleResetPassword(in,out);
			}
			else if (requestLine != null && requestLine.startsWith("GET /chats")) {
				handleGetChats(in, out);
			}
			else if (requestLine != null && requestLine.startsWith("POST /send-chat-message")) {
                handleSendChatMessage(in, out);
            } 
			else if (requestLine != null && requestLine.startsWith("POST /update-rating")) {
				handleUpdateRating(in, out);
			} 
			

			else if (requestLine != null && requestLine.startsWith("GET /borrowed-history/")) {
				System.out.println("we got there");
				handleGetBorrowedHistory(in, out, requestLine);
			}
			else if (requestLine != null && requestLine.startsWith("GET /borrowed/")) {
				handleGetBorrowedItems(in, out,requestLine);
			} else if (requestLine != null && requestLine.startsWith("GET /user/")) {
				handleUser(out, requestLine);
			}
			else if(requestLine != null && requestLine.startsWith("POST /add-to-hold-list")) {
				handleAddToHoldList(in, out);
			}
			else if(requestLine != null && requestLine.startsWith("POST /return-item")) {
				handleReturnItem(in, out);
			}
			else {
				// Handle invalid request
				out.println("HTTP/1.1 400 Bad Request");
				out.println();
				out.println("400 Bad Request");
				JSONObject errorResponse = new JSONObject();
				
				errorResponse.put("error", "Route not found");
				LOGGER.log(Level.SEVERE, "Route not found", requestLine);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error handling request", e);
		}
	}

	private void handleAddToHoldList(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Parse the JSON payload
			JSONObject requestJson = new JSONObject(body);
			// Retrieve the user ID and item ID from the request JSON
			String userId = requestJson.getString("userId");
			String itemIdStr = requestJson.getString("itemId");

			// Convert the item ID from string to ObjectId
			ObjectId itemId = new ObjectId(itemIdStr);

			// Add the user to the hold list of the specified item
			boolean success = database.addUserToHoldList(userId, itemId);

			// Prepare the JSON response
			JSONObject jsonResponse = new JSONObject();
			if (success) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "User added to hold list successfully.");
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Failed to add user to hold list.");
			}

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());

		} catch (Exception e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Error handling add-to-hold-list request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println(errorResponse.toString());
		}
	}

	
	 public void handleSendChatMessage(BufferedReader in, PrintWriter out) {
	        try {
	            // Read and validate headers
	            String header;
	            while ((header = in.readLine()) != null && !header.isEmpty()) {
	                // Skip headers
	            }

	            // Read JSON payload from the client
	            String body = in.readLine();
	            JSONObject requestJson = new JSONObject(body);
	            String userId = requestJson.getString("userId");
	            String message = requestJson.getString("message");

	            // Encrypt the message using the secret key
	            String encryptedMessage = MessageEncryption.encryptMessage(message);

	            // Create a ChatMessage object and add it to the database
	            ChatMessage chatMessage = new ChatMessage(userId, encryptedMessage, LocalDateTime.now());
	            database.addChatMessage(chatMessage);

	            // Prepare the JSON response
	            JSONObject jsonResponse = new JSONObject();
	            jsonResponse.put("status", "success");
	            jsonResponse.put("message", "Chat message sent successfully");

	            // Send the HTTP response
	            out.println("HTTP/1.1 200 OK");
	            out.println("Content-Type: application/json");
	            out.println("Content-Length: " + jsonResponse.toString().length());
	            out.println();
	            out.println(jsonResponse.toString());
	        } catch (Exception e) {
	            LOGGER.log(Level.SEVERE, "Error handling send chat message request", e);
	            out.println("HTTP/1.1 500 Internal Server Error");
	            out.println("Content-Type: application/json");
	            out.println();
	            JSONObject errorResponse = new JSONObject();
	            errorResponse.put("status", "error");
	            errorResponse.put("message", "Internal server error");
	            out.println(errorResponse.toString());
	        }
	    }
	private void handleGetChats(BufferedReader in, PrintWriter out) {
		try {
			// Skip and validate headers
			String header;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				// Skip headers
			}

			// Fetch all chat messages from the database
			List<ChatMessage> chatMessages = database.getAllChats(); // Assuming you have a method to fetch all chats

			// Create a JSON array to hold chat messages
			JSONArray chatsArray = new JSONArray();

			// Format each chat message as a JSON object
			for (ChatMessage chat : chatMessages) {
				JSONObject chatJson = new JSONObject();
				chatJson.put("username", chat.getUsername());
				chatJson.put("message", chat.getMessage());
				chatJson.put("dateSent", chat.getDateSent());

				// Add the chat JSON object to the array
				chatsArray.put(chatJson);
			}

			// Create the JSON response
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("chats", chatsArray);

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			// Handle exceptions
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println(errorResponse.toString());
		}
	}

	private void handleGetBorrowedHistory(BufferedReader in, PrintWriter out, String requestLine) {
		try {
			System.out.println("the borrowed line request line: " + requestLine);
			String[] requestParts = requestLine.split(" ");
			if (requestParts.length < 2) {
				throw new IllegalArgumentException("Invalid request format");
			}

			String userId = requestParts[1].replace("/borrowed-history/", "");
			System.out.println("the user id gotten from requests: " + userId);
			System.out.println("fetching history");
			// Fetch the borrowing history from the database using the user ID
			List<BorrowingHistory> borrowingHistory = database.getBorrowingHistoryForMember(userId);

			// Create a JSON array to hold the borrowing history
			JSONArray historyArray = new JSONArray();

			// Convert each BorrowedItem to a JSON object and add to the array
			for (BorrowingHistory item : borrowingHistory) {
				JSONObject itemJson = new JSONObject();
				itemJson.put("title", item.getTitle());
				itemJson.put("author", item.getAuthor());
				itemJson.put("type", item.getType());
				itemJson.put("borrower", item.getBorrower());
				itemJson.put("dueDate", item.getDueDate());
				itemJson.put("dateBorrowed", item.getDateBorrowed());
				itemJson.put("dateReturned", item.getReturnedDate());
				historyArray.put(itemJson);
			}

			// Create a JSON object to hold the response
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("borrowedHistory", historyArray);

			// Print the JSON response for debugging purposes
			System.out.println("JSON response: " + jsonResponse.toString());

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			// Handle exceptions
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println();
			out.println("Internal Server Error");
			LOGGER.log(Level.SEVERE, "Error handling borrowed history request", e);
		}
	}


	private void handleUpdateRating(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Parse the JSON payload
			JSONObject requestJson = new JSONObject(body);
			// Retrieve the user ID, item ID, and new rating from the request JSON
			String userId = requestJson.getString("userId");
			String itemIdStr = requestJson.getString("itemId");
			int newRating = requestJson.getInt("rating");

			// Convert the item ID from string to ObjectId
			ObjectId itemId = new ObjectId(itemIdStr);

			// Update the rating of the specified item in the database
			boolean success = database.updateItemRating(userId, itemId, newRating);

			// Prepare the JSON response
			JSONObject jsonResponse = new JSONObject();
			if (success) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Rating updated successfully.");
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Failed to update rating.");
			}

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Error handling update rating request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println(errorResponse.toString());
		}
	}
	public void handleRegisterUser(BufferedReader in, PrintWriter out) {
	    try {
	        // Read and validate headers and content length
	        String header;
	        int contentLength = 0;
	        while ((header = in.readLine()) != null && !header.isEmpty()) {
	            if (header.startsWith("Content-Length: ")) {
	                contentLength = Integer.parseInt(header.substring(16));
	            }
	        }

	        // Read JSON payload based on content length
	        char[] bodyBuffer = new char[contentLength];
	        in.read(bodyBuffer, 0, contentLength);
	        String body = new String(bodyBuffer);

	        // Parse JSON payload
	        JSONObject userData = new JSONObject(body);

	        // Extract user data from the JSON object
	        String username = userData.optString("username", null);
	        String password = userData.optString("password", null);
	        String email = userData.optString("email", null);

	        // Check for empty fields
	        if (username == null || username.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
	            out.println("HTTP/1.1 400 Bad Request");
	            out.println("Content-Type: application/json");
	            out.println("Content-Length: 45");
	            out.println();
	            out.println("{\"status\":\"error\",\"message\":\"Empty fields\"}");
	            LOGGER.warning("User registration failed: One or more fields are empty.");
	            return;
	        }

	        // Attempt to register the user
	        boolean registrationSuccessful = database.registerUser(username, password, email);

	        // Send the response based on the result of the registration
	        if (registrationSuccessful) {
	            out.println("HTTP/1.1 200 OK");
	            out.println("Content-Type: application/json");
	            out.println("Content-Length: 44");
	            out.println();
	            out.println("{\"status\":\"success\",\"message\":\"Registration successful\"}");
	            LOGGER.info("User registered successfully: " + username);
	        } else {
	            out.println("HTTP/1.1 500 Internal Server Error");
	            out.println("Content-Type: application/json");
	            out.println("Content-Length: 44");
	            out.println();
	            out.println("{\"status\":\"error\",\"message\":\"Registration failed\"}");
	            LOGGER.warning("User registration failed: " + username);
	        }
	    } catch (Exception e) {
	        // Log error and send an error response
	        LOGGER.log(Level.SEVERE, "Error handling registration", e);
	        out.println("HTTP/1.1 500 Internal Server Error");
	        out.println("Content-Type: application/json");
	        out.println("Content-Length: 43");
	        out.println();
	        out.println("{\"status\":\"error\",\"message\":\"Server error\"}");
	    }
	}

	public synchronized LibraryDatabase getDatabase() {
		return database;
	}

	private void handleResetPassword(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Parse the JSON payload
			JSONObject resetData = new JSONObject(body);
			String email = resetData.getString("email");
			String username = resetData.getString("username");
			String newPassword = resetData.getString("newPassword");

			// Look for the user with the provided email and username
			LibraryMember user = database.getUserByEmailAndUsername(email, username);

			if (user != null) {
				// User found, hash the new password
				String hashedPassword = database.hashPassword(newPassword);

				// Update the user's password in the database
				boolean success = database.updateUserPassword(user.getId(), hashedPassword);

				// Construct the JSON response
				JSONObject jsonResponse = new JSONObject();
				if (success) {
					jsonResponse.put("status", "success");
					jsonResponse.put("message", "Password reset successful.");
				} else {
					jsonResponse.put("status", "error");
					jsonResponse.put("message", "Failed to reset password.");
				}

				// Send the HTTP response
				out.println("HTTP/1.1 200 OK");
				out.println("Content-Type: application/json");
				out.println("Content-Length: " + jsonResponse.toString().length());
				out.println();
				out.println(jsonResponse.toString());
			} else {
				// User not found
				JSONObject errorResponse = new JSONObject();
				errorResponse.put("status", "error");
				errorResponse.put("message", "User not found.");

				out.println("HTTP/1.1 404 Not Found");
				out.println("Content-Type: application/json");
				out.println("Content-Length: " + errorResponse.toString().length());
				out.println();
				out.println(errorResponse.toString());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error handling reset password request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println(errorResponse.toString());
		}
	}

	private void handleReturnItem(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Print the body of the request
			System.out.println("Request Body: " + body);

			// Parse JSON payload
			JSONObject requestJson = new JSONObject(body);
			// Retrieve the user ID and item ID from the request body
			String userId = requestJson.optString("userId", null);
			String itemIdStr = requestJson.optString("itemId", null);

			// If the itemIdStr is not null, convert it to ObjectId
			ObjectId itemId = null;
			if (itemIdStr != null) {
				itemId = new ObjectId(itemIdStr);
			}


			// Update the database to return the item
			boolean success = database.returnItem(itemId);

			// Prepare the JSON response
			JSONObject jsonResponse = new JSONObject();
			if (success) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Item returned successfully");
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Failed to return the item");
			}

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());

		} catch (Exception e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Error handling return item request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("error", "Internal server error");
			out.println(errorResponse.toString());
		}
	}
	private void handleGetBorrowedItems(BufferedReader in, PrintWriter out, String requestLine) {
		try {

			System.out.println("the user borrowed line request line: " + requestLine);
			String[] requestParts = requestLine.split(" ");
			if (requestParts.length < 2) {
				throw new IllegalArgumentException("Invalid request format");
			}

			String userId = requestParts[1].replace("/borrowed/", "");
			System.out.println("the user id gotten from requests: " + userId);
			System.out.println("fetching user history");
			// Retrieve borrowed items from the database
			List<CatalogItem> borrowedItems = database.getBorrowedItemsForMember(userId);

			// Create a JSON object to hold the response
			JSONObject jsonResponse = new JSONObject();
			JSONArray borrowedArray = new JSONArray();

			// Convert each BorrowedItem to a JSON object and add it to the array
			for (CatalogItem item : borrowedItems) {
				JSONObject itemJson = new JSONObject();
				itemJson.put("title", item.getTitle());
				itemJson.put("author", item.getAuthor());
				itemJson.put("type", item.getType());
				itemJson.put("borrower", item.getBorrower());
				itemJson.put("due_date", item.getDueDate());
				itemJson.put("date_borrowed", item.getDateBorrowed());
				itemJson.put("borrowerId", item.getBorrowerId());
				itemJson.put("_id", item.getItemId());
				itemJson.put("file_path", item.getFilePath());
				itemJson.put("holdList", item.getHoldList() != null ? item.getHoldList().size() : 0);
				itemJson.put("rating",item.getRating());
				borrowedArray.put(itemJson);

			}

			// Add the array to the response JSON object
			jsonResponse.put("borrowed", borrowedArray);

			// Print the JSON response for debugging purposes
			System.out.println("JSON borrow response: " + jsonResponse.toString());


			// Send the JSON response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());

		} catch (Exception e) {
			// Handle any exceptions
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("error", "Failed to retrieve borrowed items");
			out.println(errorResponse.toString());
		}
	}

	private void handleToggleMemberStatus(BufferedReader in, PrintWriter out) {
		try {
			// Read the JSON payload from the request
			String line;
			StringBuilder bodyBuilder = new StringBuilder();
			while ((line = in.readLine()) != null && !line.isEmpty()) {
				bodyBuilder.append(line);
			}
			JSONObject requestJson = new JSONObject(bodyBuilder.toString());

			// Extract the member ID from the request JSON
			String memberId = requestJson.getString("memberId");

			// Get the member from the database
			LibraryMember member = database.getUserById(memberId);
			if (member == null) {
				// Member not found
				out.println("HTTP/1.1 404 Not Found");
				out.println("Content-Type: application/json");
				out.println();
				out.println("{\"status\":\"error\", \"message\":\"Member not found\"}");
				return;
			}

			// Update the member's status in the database
			boolean success = database.updateMemberStatus(memberId);

			if (success) {
				out.println("HTTP/1.1 200 OK");
				out.println("Content-Type: application/json");
				out.println();
				out.println("{\"status\":\"success\", \"message\":\"Member status updated\"}");
			} else {
				out.println("HTTP/1.1 500 Internal Server Error");
				out.println("Content-Type: application/json");
				out.println();
				out.println("{\"status\":\"error\", \"message\":\"Failed to update member status\"}");
			}
		} catch (Exception e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Error handling toggle-member-status request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			out.println("{\"status\":\"error\", \"message\":\"Internal server error\"}");
		}
	}

	private void handleLoginAdminLogin(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Parse JSON payload
			JSONObject loginData = new JSONObject(body);

			// Retrieve the username and password from the login data
			String username = loginData.getString("username");
			String password = loginData.getString("password");

			// Authenticate the user
			LibraryMember user = database.authenticateUser(username, password);

			// Initialize the JSON response object
			JSONObject jsonResponse = new JSONObject();

			// Check if the user is authenticated and is an admin
			if (user != null && user.getUsername().equals("admin")) {
				// User is authenticated and is an admin
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Login successful");
				jsonResponse.put("username", user.getUsername());
				jsonResponse.put("userId", user.getId().toString());
			} else {
				// User is either not authenticated or not an admin
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Invalid username or password, or user is not an admin");
			}

			// Send the HTTP response with the JSON response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			// Log the error and send a 500 Internal Server Error response
			LOGGER.log(Level.SEVERE, "Error handling admin login request", e);
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + errorResponse.toString().length());
			out.println();
			out.println(errorResponse.toString());
		}
	}

	public void handleLogin(BufferedReader in, PrintWriter out) {
		try {

			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Parse JSON payload
			JSONObject loginData = new JSONObject(body);
			// Get the username and password from the request data
			String username = loginData.getString("username");
			String password = loginData.getString("password");

			// Authenticate the user
			LibraryMember user = database.authenticateUser(username, password);

			// Construct the JSON response
			JSONObject jsonResponse = new JSONObject();
			if (user != null) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Login successful");
				jsonResponse.put("username", user.getUsername());
				jsonResponse.put("userId", user.getId().toString()); // Include the user object ID
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Invalid username or password");
			}

			// Send the response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error handling login request", e);
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + errorResponse.toString().length());
			out.println();
			out.println(errorResponse.toString());
		}
	}

	private void handleCatalog(PrintWriter out) {
		try {
			// Retrieve catalog items from the database
			List<CatalogItem> catalogItems = database.getCatalogItems();

			// Convert catalog items to JSON array
			JSONArray catalogJsonArray = new JSONArray();
			for (CatalogItem item : catalogItems) {
				System.out.println("title" + item.getTitle());
				JSONObject itemJson = new JSONObject();
				itemJson.put("title", item.getTitle());
				itemJson.put("author", item.getAuthor());
				itemJson.put("type", item.getType());
				itemJson.put("available", item.isAvailable());
				itemJson.put("_id", item.getItemId().toString());

				//							
				itemJson.put("borrower", item.getBorrower());
				itemJson.put("due_date", item.getDueDate());
				itemJson.put("date_borrowed", item.getDateBorrowed());
				itemJson.put("borrowerId", item.getBorrowerId());
				itemJson.put("file_path", item.getFilePath());
				itemJson.put("holdList", item.getHoldList() != null ? item.getHoldList().size() : 0);
				itemJson.put("rating",item.getRating());
				// Add more fields as needed
				System.out.println("item id" + item.getItemId().toString());
				catalogJsonArray.put(itemJson);
			}

			// Create a JSON response
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("catalog", catalogJsonArray);

			// Send the JSON response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error handling catalog request", e);
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + errorResponse.toString().length());
			out.println();
			out.println(errorResponse.toString());
		}
	}

	private void handleBorrowItem(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Print the body of the request
			System.out.println("Request Body: " + body);

			// Parse JSON payload
			JSONObject requestJson = new JSONObject(body);
			// Retrieve the user ID and item ID from the request body
			String userId = requestJson.optString("userId", null);
			String itemIdStr = requestJson.optString("itemId", null);

			// If the itemIdStr is not null, convert it to ObjectId
			ObjectId itemId = null;
			if (itemIdStr != null) {
				itemId = new ObjectId(itemIdStr);
			}

			// Borrow the item using the user ID and item ID
			boolean success = database.borrowItem(userId, itemId);

			// Send response based on the success of the borrowing operation
			JSONObject jsonResponse = new JSONObject();
			if (success) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Item borrowed successfully.");
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Failed to borrow item.");
			}

			// Send the HTTP response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error handling borrow item request", e);
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println();
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("error", "Internal server error");
			out.println(errorResponse.toString());
		}
	}

	private void handleUser(PrintWriter out, String requestLine) {
		try {
			// Extract the user ID from the request line
			// The request line is expected to be something like "GET /user/{userId}
			// HTTP/1.1"
			System.out.println("the request line: " + requestLine);
			String[] requestParts = requestLine.split(" ");
			if (requestParts.length < 2) {
				throw new IllegalArgumentException("Invalid request format");
			}

			String userId = requestParts[1].replace("/user/", "");
			System.out.println("the user id gotten from requests: " + userId);

			// Retrieve user information from the database using the user ID
			LibraryMember user = database.getUserById(userId);

			// Create a JSON response
			JSONObject jsonResponse = new JSONObject();
			if (user != null) {
				// If the user exists, return user information in JSON format
				jsonResponse.put("username", user.getUsername());
				jsonResponse.put("email", user.getEmail());
				System.out.println(user.getUsername());
				// Add other user information as needed
			} else {
				// If the user does not exist, return an error message
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "User not found");
			}

			// Send the JSON response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error handling user request", e);
			// Send a 500 Internal Server Error response in case of an exception
			JSONObject errorResponse = new JSONObject();
			errorResponse.put("status", "error");
			errorResponse.put("message", "Internal server error");
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + errorResponse.toString().length());
			out.println();
			out.println(errorResponse.toString());
		}
	}

	public void handleAddCatalogItem(BufferedReader in, PrintWriter out) {
		try {
			// Read and validate headers and body
			String header;
			int contentLength = 0;
			while ((header = in.readLine()) != null && !header.isEmpty()) {
				if (header.startsWith("Content-Length: ")) {
					contentLength = Integer.parseInt(header.substring(16));
				}
			}

			// Read the JSON payload based on content length
			char[] bodyBuffer = new char[contentLength];
			in.read(bodyBuffer, 0, contentLength);
			String body = new String(bodyBuffer);

			// Debugging: Print the JSON payload
			System.out.println("JSON Payload: " + body);

			// Parse the JSON payload
			JSONObject requestData = new JSONObject(body);

			// Parse values from the requestData, using opt methods to handle missing keys
			String title = requestData.optString("title", null);
			String author = requestData.optString("author", null);
			String type = requestData.optString("type", null);
			boolean available = requestData.optBoolean("available", false);
			String filePath = requestData.optString("file_path", "/resources/temp.pdf");

			// Validate required fields
			if (title == null || title.isEmpty() || author == null || author.isEmpty() || type == null
					|| type.isEmpty()) {
				// Respond with an error and do not proceed with adding the catalog item
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Missing required fields (title, author, or type).");

				out.println("HTTP/1.1 400 Bad Request");
				out.println("Content-Type: application/json");
				out.println("Content-Length: " + jsonResponse.toString().length());
				out.println();
				out.println(jsonResponse.toString());
				return; // Exit the method
			}

			// Create a new CatalogItem instance with the parsed data
			// Generate a new itemId
			ObjectId newItemId = new ObjectId();

			// Create a new CatalogItem instance with default values for holdList and rating
			CatalogItem newItem = new CatalogItem(
					newItemId, // itemId (generated)
					title, 
					author, 
					type, 
					available, 
					null, // borrowerId (initially null)
					null, // dueDate (initially null)
					null, // dateBorrowed (initially null)
					null, // borrower (initially null)
					filePath, // filePath (from request data or default)
					new ArrayList<>(), // holdList (initially an empty list)
					0 // rating (initially 0.0)
					);

			// Add the new catalog item to the database
			boolean success = database.addItem(newItem);

			// Construct the JSON response
			JSONObject jsonResponse = new JSONObject();
			if (success) {
				jsonResponse.put("status", "success");
				jsonResponse.put("message", "Catalog item added successfully.");
			} else {
				jsonResponse.put("status", "error");
				jsonResponse.put("message", "Failed to add catalog item.");
			}

			// Send the response
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());
		} catch (IOException | JSONException e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Error handling add catalog item request", e);
			// Respond with a server error
			out.println("HTTP/1.1 500 Internal Server Error");
			out.println();
			out.println("Internal Server Error");
		}
	}

	private void handleGetAllMembers(PrintWriter out) {
		try {
			// Query the database to retrieve all members
			List<LibraryMember> members = database.getAllMembers(); // Assuming you have a method in your database class

			// Create a JSON array to hold the members' data
			JSONArray membersArray = new JSONArray();
			for (LibraryMember member : members) {
				// Create a JSON object for each member
				JSONObject memberJson = new JSONObject();
				memberJson.put("memberId", member.getId());
				memberJson.put("username", member.getUsername());
				memberJson.put("email", member.getEmail());
				memberJson.put("status", member.getStatus());

				// Add the member JSON object to the array
				membersArray.put(memberJson);
			}

			// Create the final JSON response
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("members", membersArray);

			// Send the response to the client
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: application/json");
			out.println("Content-Length: " + jsonResponse.toString().length());
			out.println();
			out.println(jsonResponse.toString());

		} catch (Exception e) {
			// Handle any exceptions
			LOGGER.log(Level.SEVERE, "Error handling request to get all members", e);
			sendErrorResponse(out, "Internal server error", 500);
		}
	}

	private void sendErrorResponse(PrintWriter out, String message, int statusCode) {
		// Create an error response JSON object
		JSONObject errorResponse = new JSONObject();
		errorResponse.put("status", "error");
		errorResponse.put("message", message);

		// Send the error response
		out.println("HTTP/1.1 " + statusCode + " " + getStatusMessage(statusCode));
		out.println("Content-Type: application/json");
		out.println("Content-Length: " + errorResponse.toString().length());
		out.println();
		out.println(errorResponse.toString());
	}

	private String getStatusMessage(int statusCode) {
		switch (statusCode) {
		case 400:
			return "Bad Request";
		case 500:
			return "Internal Server Error";
		default:
			return "OK";
		}
	}

}
