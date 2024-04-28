package library.server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

public class LibraryDatabase {

	private MongoClient mongoClient;
	private MongoDatabase database;
	private static final Logger LOGGER = Logger.getLogger(LibraryDatabase.class.getName());
	private static final String STATIC_SALT = "YourConstantStaticSaltHere"; // Replace with your constant static salt
  

	public LibraryDatabase() {
		// Replace this line with your MongoDB connection string
		String uri = "mongodb+srv://lithiumgx:lithiumgx98@cluster0.niazpjl.mongodb.net/library";

		try {
			mongoClient = MongoClients.create(uri);
			database = mongoClient.getDatabase("library");
			LOGGER.info("Connected to the MongoDB database.");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to connect to MongoDB.", e);
		}
	}

	public boolean addItem(CatalogItem newItem) {
		try {
			// Get the collection from the database
			MongoCollection<Document> collection = database.getCollection("library_items");

			// Create a document for the new item
			Document doc = new Document().append("type", newItem.getType()).append("title", newItem.getTitle())
					.append("author", newItem.getAuthor()).append("available", newItem.isAvailable())
					.append("file_path", newItem.getFilePath()); // Include base64-encoded book content

			// Add more fields if necessary
			// .append("borrowerId", newItem.getBorrowerId())
			// .append("due_date", newItem.getDueDate())
			// .append("date_borrowed", newItem.getDateBorrowed())
			// .append("borrower", newItem.getBorrower());

			// Insert the document into the collection
			collection.insertOne(doc);

			// Log success and return true
			LOGGER.info("Item added successfully.");
			return true;
		} catch (Exception e) {
			// Log the error and return false
			LOGGER.log(Level.SEVERE, "Failed to add item: " + e.getMessage(), e);
			return false;
		}
	}

	public boolean borrowItem(String userId, ObjectId itemId) {
		try {
			// Get the collection for library items
			MongoCollection<Document> itemsCollection = database.getCollection("library_items");

			// Find the item by ID
			System.out.println("itemID" + itemId);
			Document query = new Document("_id", itemId);
			Document itemDoc = itemsCollection.find(query).first();
			System.out.println("item" + itemDoc);
			if (itemDoc == null || !itemDoc.getBoolean("available")) {
				// Item not found or not available
				return false;
			}

			
			// Current date as borrow date
	        Date currentDate = new Date();
	        
	        // Update the item's availability, borrowerId, dueDate, and dateBorrowed
	        Document update = new Document("$set", new Document("available", false)
	                .append("borrowerId", new ObjectId(userId))
	                .append("dueDate", calculateDueDate(currentDate)) // Calculate due date
	                .append("dateBorrowed", currentDate.toString()));
	        
	     // Update the item document in the library_items collection
			itemsCollection.updateOne(query, update);

			// Create a new entry in the borrowing history collection
			MongoCollection<Document> borrowingHistoryCollection = database.getCollection("borrowing_history");
			Document historyDoc = new Document()
		            .append("borrow_id", new ObjectId()) // Generate a new borrow_id
		            .append("item_id", itemId)
		            .append("member_id", new ObjectId(userId))
		            .append("borrow_date", currentDate.toString()); // Add the current date as the borrow date

			borrowingHistoryCollection.insertOne(historyDoc);

			return true;
		} catch (Exception e) {
			// Handle exceptions
			LOGGER.log(Level.SEVERE, "Failed to borrow item: " + e.getMessage(), e);
			return false;
		}
		
		
	}
	
	private String calculateDueDate(Date currentDate) {
	    // Calculate the due date, e.g., 14 days from the borrow date
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(currentDate);
	    calendar.add(Calendar.DAY_OF_YEAR, 14); // Add 14 days

	    // Return the due date as a string
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    return sdf.format(calendar.getTime());
	}

	public List<BorrowingHistory> getBorrowingHistoryForMember(String memberId) {
	    List<BorrowingHistory> history = new ArrayList<>();
	    System.out.println("Getting member history for memberId: " + memberId);

	    // Convert the memberId from string to ObjectId
	    ObjectId objectIdMemberId;
	    try {
	        objectIdMemberId = new ObjectId(memberId);
	    } catch (IllegalArgumentException e) {
	        System.out.println("Invalid member ID format: " + memberId);
	        return history; // Return empty history list if the memberId is not a valid ObjectId
	    }

	    // Get the borrowing history collection
	    MongoCollection<Document> borrowingHistoryCollection = database.getCollection("borrowing_history");

	    // Query the collection for the given member ID (as an ObjectId)
	    Document query = new Document("member_id", objectIdMemberId);
	    List<Document> results = borrowingHistoryCollection.find(query).into(new ArrayList<>());

	    System.out.println("Results: " + results);
	    for (Document doc : results) {
	        ObjectId borrowId = doc.getObjectId("borrow_id");
	        ObjectId itemId = doc.getObjectId("item_id");
	        ObjectId documentMemberId = doc.getObjectId("member_id");

	        // Retrieve the corresponding library item using the item_id
	        MongoCollection<Document> itemsCollection = database.getCollection("library_items");
	        Document itemQuery = new Document("_id", itemId);
	        Document itemDocument = itemsCollection.find(itemQuery).first();

	        // Extract item details (title, author, type) if found
	        String title = itemDocument != null ? itemDocument.getString("title") : null;
	        String author = itemDocument != null ? itemDocument.getString("author") : null;
	        String type = itemDocument != null ? itemDocument.getString("type") : null;

	        // Retrieve other fields from the borrowing history document
	        String borrower = doc.getString("borrower");
	        String dueDate = doc.getString("due_date");
	        String dateReturned = doc.getString("dateReturned");
	        String dateBorrowed = doc.getString("borrow_date");

	        // Add the BorrowingHistory object to the history list
	        history.add(new BorrowingHistory(borrowId, itemId, documentMemberId, title, author, type, borrower, dueDate,
	                dateBorrowed, dateReturned));
	    }

	    return history;
	}
	public boolean registerUser(String username, String password, String email) {
		// Generate a unique member ID
		String memberId = generateUniqueMemberId();

		// Hash the password with the constant static salt
		String hashedPassword = hashPassword(password);

		// Create a document representing the user
		Document userDoc = new Document().append("member_id", memberId).append("username", username)
				.append("password", hashedPassword).append("email", email);

		// Get the collection for library members
		MongoCollection<Document> membersCollection = database.getCollection("library_members");

		// Insert the user document
		membersCollection.insertOne(userDoc);

		// Check if the user was added successfully
		boolean registrationSuccessful = membersCollection.find(new Document("username", username)).first() != null;
		if (registrationSuccessful) {
			LOGGER.info("User registered successfully: " + username);
			return true;
		} else {
			LOGGER.warning("User registration failed: " + username);
			return false;
		}
	}

	private String generateUniqueMemberId() {
		// Generate a UUID (Universally Unique Identifier) as a unique member ID
		return UUID.randomUUID().toString();
	}

	public String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(STATIC_SALT.getBytes());
			byte[] hash = digest.digest(password.getBytes());
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.log(Level.SEVERE, "Error hashing password", e);
			return null;
		}
	}
	
	public LibraryMember getUserByEmailAndUsername(String email, String username) {
	    // Get the collection for library members
	    MongoCollection<Document> membersCollection = database.getCollection("library_members");

	    // Create a query to find a user with the given email and username
	    Document query = new Document("email", email).append("username", username);

	    // Execute the query and retrieve the user document
	    Document userDoc = membersCollection.find(query).first();

	    if (userDoc != null) {
	        // Create a LibraryMember object from the user document
	        LibraryMember user = new LibraryMember();
	        user.setId(userDoc.getObjectId("_id"));
	        user.setUsername(userDoc.getString("username"));
	        user.setEmail(userDoc.getString("email"));
	        // Return the LibraryMember object
	        return user;
	    } else {
	        // User not found
	        return null;
	    }
	}

	public boolean updateUserPassword(ObjectId userId, String hashedPassword) {
	    // Get the collection for library members
	    MongoCollection<Document> membersCollection = database.getCollection("library_members");

	    // Create a query to find the user by ID
	    Document query = new Document("_id", userId);

	    // Create an update document to set the new password
	    Document update = new Document("$set", new Document("password", hashedPassword));

	    // Perform the update in the collection
	    long matchedCount = membersCollection.updateOne(query, update).getMatchedCount();

	    // Return true if the update was successful (matchedCount is 1)
	    return matchedCount == 1;
	}

	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
			LOGGER.info("MongoDB client closed.");
		}
	}

	public LibraryMember authenticateUser(String username, String password) {
		// Get the library members collection
		MongoCollection<Document> membersCollection = database.getCollection("library_members");

		// Query the database for the given username
		Document query = new Document("username", username);
		Document userDoc = membersCollection.find(query).first();

		if (userDoc == null) {
			// User not found
			return null;
		}

		// Retrieve the stored hashed password
		String storedHashedPassword = userDoc.getString("password");

		// Hash the provided password with the same static salt
		String hashedPassword = hashPassword(password);

		// Compare the provided hashed password with the stored hashed password
		if (storedHashedPassword.equals(hashedPassword)) {
			// If the password matches, return the user as a LibraryMember object
			LibraryMember user = new LibraryMember();
			user.setUsername(userDoc.getString("username"));
			user.setEmail(userDoc.getString("email"));

			// Retrieve the _id as an ObjectId from the userDoc
			ObjectId id = userDoc.getObjectId("_id");
			user.setId(id);

			return user;
		} else {
			// If the password does not match, return null
			return null;
		}
	}

	public List<CatalogItem> getCatalogItems() {
	    List<CatalogItem> catalogItems = new ArrayList<>();

	    // Get the library items collection
	    MongoCollection<Document> itemsCollection = database.getCollection("library_items");

	    // Current date
	    LocalDate currentDate = LocalDate.now();
	    DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

	    // Iterate through the documents in the collection
	    for (Document doc : itemsCollection.find()) {
	        try {
	            // Retrieve the object ID
	            ObjectId itemId = doc.getObjectId("_id");

	            // Null check for itemId
	            if (itemId == null) {
	                System.err.println("Warning: Missing _id in document: " + doc.toJson());
	                continue;
	            }

	            // Retrieve other fields and handle potential null values
	            String title = doc.getString("title");
	            String author = doc.getString("author");
	            String type = doc.getString("type");
	            Boolean available = doc.getBoolean("available");
	            ObjectId borrowerId = doc.getObjectId("borrowerId");
	            String dueDate = doc.getString("dueDate");
	            String dateBorrowed = doc.getString("dateBorrowed");
	            String borrower = doc.getString("borrower");
	            String filePath = doc.getString("filePath");
	            List<ObjectId> holdList = doc.getList("holdList", ObjectId.class);
	            int rating = doc.getInteger("rating",0);

	            // Check and update availability
	            if (!available && dueDate != null) {
	                LocalDate dueDateParsed = LocalDate.parse(dueDate, dateFormatter);
	                if (currentDate.isAfter(dueDateParsed)) {
	                    // Check if there are users waiting for the item
	                    if (holdList != null && !holdList.isEmpty()) {
	                        // Get the next user in the hold list
	                        ObjectId nextUser = holdList.remove(0);
	                        
	                        // Update the document with new borrowerId, dateBorrowed, and dueDate
	                        Document update = new Document("$set", new Document()
	                            .append("borrowerId", nextUser)
	                            .append("dateBorrowed", currentDate.toString())
	                            .append("dueDate", currentDate.plusWeeks(2).toString())
	                            .append("available", false)
	                            .append("holdList", holdList));
	                        
	                        // Update the document in the database
	                        itemsCollection.updateOne(new Document("_id", itemId), update);
	                        
	                        // Update local CatalogItem instance
	                        borrowerId = nextUser;
	                        dateBorrowed = currentDate.toString();
	                        dueDate = currentDate.plusWeeks(2).toString();
	                        available = false;
	                    } else {
	                        // Update the 'available' field in the document
	                        Document update = new Document("$set", new Document("available", true));
	                        itemsCollection.updateOne(new Document("_id", itemId), update);

	                        // Update local CatalogItem instance
	                        available = true;
	                    }
	                }
	            }

	            // Create a CatalogItem instance from the document
	            CatalogItem item = new CatalogItem(itemId, title, author, type, available, borrowerId, dueDate,
	                                               dateBorrowed, borrower, filePath, holdList, rating);

	            // Add the CatalogItem to the list
	            catalogItems.add(item);
	        } catch (Exception e) {
	            // Log any exception that occurs while processing the document
	            System.err.println("Error processing document: " + doc.toJson());
	            e.printStackTrace();
	        }
	    }

	    return catalogItems;
	}
	
	public boolean returnItem(ObjectId itemId) {
	    try {
	        // Get the collection for library items
	        MongoCollection<Document> itemsCollection = database.getCollection("library_items");

	        // Create a query to find the item by its ID
	        Document query = new Document("_id", itemId);

	        // Create an update document to set 'available' to true and 'borrowerId' to null
	        Document update = new Document("$set", new Document("available", true)
	            .append("borrowerId", null));

	        // Perform the update in the collection
	        long matchedCount = itemsCollection.updateOne(query, update).getMatchedCount();

	        // If matchedCount is 1, the update was successful
	        return matchedCount == 1;
	    } catch (Exception e) {
	        // Handle exceptions
	        LOGGER.log(Level.SEVERE, "Failed to return item: " + e.getMessage(), e);
	        return false;
	    }
	}

	public List<CatalogItem> getBorrowedItemsForMember(String userId) {
	    List<CatalogItem> borrowedItems = new ArrayList<>();

	    // Convert the userId from string to ObjectId
	    ObjectId objectIdMemberId;
	    try {
	        objectIdMemberId = new ObjectId(userId);
	    } catch (IllegalArgumentException e) {
	        System.out.println("Invalid member ID format: " + userId);
	        return borrowedItems; // Return an empty list if the memberId is not a valid ObjectId
	    }

	    // Get the library items collection
	    MongoCollection<Document> libraryItemsCollection = database.getCollection("library_items");

	    // Create a query to find borrowed items for the given userId
	    Document query = new Document()
	        .append("available", false) // Filter for unavailable items
	        .append("borrowerId", objectIdMemberId); // Filter for items borrowed by the user

	    // Execute the query and iterate through the results
	    try (MongoCursor<Document> cursor = libraryItemsCollection.find(query).iterator()) {
	        while (cursor.hasNext()) {
	            Document doc = cursor.next();
	            System.out.println("the doc: "+ doc);
	            // Parse the document to create a CatalogItem instance
	            // Adjust the fields to match the structure of your CatalogItem class and document
	            ObjectId id = doc.getObjectId("_id");
	            String title = doc.getString("title");
	            String author = doc.getString("author");
	            String type = doc.getString("type");
	            boolean available = doc.getBoolean("available", true);
	            ObjectId borrowerId = doc.getObjectId("borrowerId");
	            String dueDate = doc.getString("dueDate");
	            String dateBorrowed = doc.getString("dateBorrowed");
	            String borrower = doc.getString("borrower");
	            String filePath = doc.getString("file_path");
	            List<ObjectId> holdList = doc.getList("holdList", ObjectId.class);
	            int rating = doc.getInteger("rating",0);

	            // Check if the item is overdue and users are waiting for it
	            if (dueDate != null && holdList != null && !holdList.isEmpty()) {
	                LocalDate dueDateParsed = LocalDate.parse(dueDate);
	                LocalDate currentDate = LocalDate.now();

	                if (currentDate.isAfter(dueDateParsed)) {
	                    // The item is overdue and there are users in the hold list

	                    // Get the next user in the hold list
	                    ObjectId nextUser = holdList.remove(0);

	                    // Update the document with new borrowerId, dateBorrowed, and dueDate
	                    Document update = new Document("$set", new Document()
	                        .append("borrowerId", nextUser)
	                        .append("dateBorrowed", currentDate.toString())
	                        .append("dueDate", currentDate.plusWeeks(2).toString())
	                        .append("available", false)
	                        .append("holdList", holdList));
	                    
	                    // Update the document in the database
	                    libraryItemsCollection.updateOne(new Document("_id", id), update);

	                    // Update local CatalogItem instance
	                    borrowerId = nextUser;
	                    dateBorrowed = currentDate.toString();
	                    dueDate = currentDate.plusWeeks(2).toString();
	                    available = false;
	                }
	            }

	            // Create a CatalogItem instance from the document
	            CatalogItem catalogItem = new CatalogItem(id, title, author, type, available, borrowerId, dueDate, dateBorrowed, borrower, filePath, holdList, rating);
	            
	            // Add the CatalogItem to the borrowedItems list
	            borrowedItems.add(catalogItem);
	        }
	    } catch (Exception e) {
	        System.err.println("Error retrieving borrowed items for member: " + e.getMessage());
	    }

	    return borrowedItems;
	}
	
	@SuppressWarnings("unchecked")
	public boolean addUserToHoldList(String userId, ObjectId itemId) {
	    try {
	        // Get the collection for library items
	        MongoCollection<Document> itemsCollection = database.getCollection("library_items");

	        // Create a query to find the item by its ID
	        Document query = new Document("_id", itemId);

	        // Retrieve the document for the item
	        Document itemDoc = itemsCollection.find(query).first();
	        if (itemDoc == null) {
	            // Item not found
	            return false;
	        }

	        // Retrieve the hold list from the item document
	        List<ObjectId> holdList;
	        Object holdListObject = itemDoc.get("holdList");
	        
	        // Check if holdListObject is a List and contains ObjectId elements
	        if (holdListObject instanceof List) {
	            holdList = (List<ObjectId>) holdListObject;
	        } else {
	            holdList = new ArrayList<>();
	        }

	        // Convert the userId from string to ObjectId
	        ObjectId objectIdUserId = new ObjectId(userId);

	        if (!holdList.contains(objectIdUserId)) {
	            // Add the user ID to the hold list if it's not already present
	            holdList.add(objectIdUserId);

	            // Create an update document with the updated hold list
	            Document update = new Document("$set", new Document("holdList", holdList));

	            // Update the document in the collection
	            itemsCollection.updateOne(query, update);

	            // Return true to indicate success
	            return true;
	        } else {
	            // User is already on the hold list, return false
	            return false;
	        }
	    } catch (Exception e) {
	        // Handle exceptions
	        LOGGER.log(Level.SEVERE, "Failed to add user to hold list: " + e.getMessage(), e);
	        return false;}
	    }
	
	public boolean updateItemRating(String userId, ObjectId itemId, int newRating) {
	    try {
	        // Get the collection for library items
	        MongoCollection<Document> itemsCollection = database.getCollection("library_items");

	        // Create a query to find the item by its ID
	        Document query = new Document("_id", itemId);

	        // Create an update document to set the new rating
	        Document update = new Document("$set", new Document("rating", newRating));

	        // Perform the update in the collection
	        long matchedCount = itemsCollection.updateOne(query, update).getMatchedCount();

	        // Return true if the update was successful (matchedCount is 1)
	        return matchedCount == 1;
	    } catch (Exception e) {
	        // Handle exceptions
	        LOGGER.log(Level.SEVERE, "Failed to update item rating: " + e.getMessage(), e);
	        return false;
	    }
	}

	 // Method to get all chat messages
	// Method to get all chat messages
	public List<ChatMessage> getAllChats() {
	    List<ChatMessage> chatMessages = new ArrayList<>();

	    try {
	        // Get the chat collection from the database
	        MongoCollection<Document> chatCollection = database.getCollection("chats");

	        // Iterate through the chat documents
	        try (MongoCursor<Document> cursor = chatCollection.find().iterator()) {
	            while (cursor.hasNext()) {
	                Document doc = cursor.next();

	                // Retrieve the username, encrypted message, and date sent from the document
	                String userId = doc.getString("username");
	                ObjectId objectId = new ObjectId(userId);
	                String encryptedMessage = doc.getString("encryptedMessage");
	                LocalDateTime dateSent = doc.getDate("dateSent").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

	                // Decrypt the message using the static secret key
	                String decryptedMessage = MessageEncryption.decryptMessage(encryptedMessage);
	             // Query the library_members collection to fetch the user document
	                MongoCollection<Document> membersCollection = database.getCollection("library_members");
	                Document userDoc = membersCollection.find(new Document("_id", objectId)).first();

	                // Check if the user document is found
	                if (userDoc != null) {
	                    // Extract the username from the user document
	                    String username = userDoc.getString("username");

	                    // Create a ChatMessage object and add it to the list
	                    ChatMessage chatMessage = new ChatMessage(username, decryptedMessage, dateSent);
	                    chatMessages.add(chatMessage);
	                }
	            }
	        }
	    } catch (Exception e) {
	        // Log exceptions and handle errors
	        LOGGER.log(Level.SEVERE, "Failed to get all chats: " + e.getMessage(), e);
	    }

	    return chatMessages;
	}
	
	
	public List<CatalogItem> getBorrowedItems() {
	    List<CatalogItem> borrowedItems = new ArrayList<>();

	    try {
	        // Get the library items collection
	        MongoCollection<Document> itemsCollection = database.getCollection("library_items");

	        // Query the collection for items where available is false (indicating the item is borrowed)
	        Document query = new Document("available", false);
	        List<Document> results = itemsCollection.find(query).into(new ArrayList<>());

	        // Iterate through the results and create CatalogItem instances
	        for (Document doc : results) {
	            // Retrieve item details from the document
	            ObjectId itemId = doc.getObjectId("_id");
	            String title = doc.getString("title");
	            String author = doc.getString("author");
	            String type = doc.getString("type");
	            ObjectId borrowerId = doc.getObjectId("borrowerId");
	            String borrower = doc.getString("borrower");
	            String dueDate = doc.getString("due_date");
	            String dateBorrowed = doc.getString("date_borrowed");
	            boolean available = doc.getBoolean("available");
	            String filePath = doc.getString("filePath");
	            List<ObjectId> holdList = doc.getList("holdList", ObjectId.class);
	            int rating = doc.getInteger("rating",0);

	            // Create a CatalogItem instance
	            CatalogItem borrowedItem = new CatalogItem(itemId, title, author, type, available, borrowerId, dueDate,
	                    dateBorrowed, borrower, filePath, holdList, rating);

	            // Add the CatalogItem to the list
	            borrowedItems.add(borrowedItem);
	        }
	    } catch (Exception e) {
	        // Handle any exceptions
	        e.printStackTrace();
	    }

	    // Return the list of borrowed items
	    return borrowedItems;
	}

	/**
	 * Retrieves a user from the database based on the user ID.
	 *
	 * @param userId the ID of the user to retrieve.
	 * @return a LibraryMember object representing the user, or null if the user
	 *         does not exist.
	 */
	public LibraryMember getUserById(String userId) {
		try {
			// Replace this with your database connection and querying code.
			// For example, using MongoDB:
			MongoCollection<Document> usersCollection = database.getCollection("library_members");

			// Convert the userId string to an ObjectId
			ObjectId objectId = new ObjectId(userId);

			System.out.println("objectID: " + objectId);
			Document query = new Document("_id", objectId);
			Document userDocument = usersCollection.find(query).first();
			System.out.println("user document: " + userDocument);
			if (userDocument != null) {
				// Create a LibraryMember object from the user document
				String username = userDocument.getString("username");
				String email = userDocument.getString("email");
				// Retrieve other fields as needed

				// Create a LibraryMember object
				LibraryMember user = new LibraryMember();
				user.setId(objectId);
				user.setUsername(username);
				user.setEmail(email);
				// Set other fields as needed

				return user;
			} else {
				// User not found
				return null;
			}
		} catch (Exception e) {
			// Handle exceptions (e.g., database connection errors)
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean addChatMessage(ChatMessage chatMessage) {
	    try {
	        // Get the chat collection from the database
	        MongoCollection<Document> chatCollection = database.getCollection("chats");

	        // Create a document for the chat message
	        Document chatDoc = new Document()
	            .append("username", chatMessage.getUsername())
	            .append("encryptedMessage", chatMessage.getMessage())
	            .append("dateSent", Date.from(chatMessage.getDateSent().atZone(ZoneId.systemDefault()).toInstant()));

	        // Insert the document into the collection
	        chatCollection.insertOne(chatDoc);

	        // Return true to indicate the chat message was added successfully
	        return true;
	    } catch (Exception e) {
	        // Log the error and return false
	        LOGGER.log(Level.SEVERE, "Failed to add chat message: " + e.getMessage(), e);
	        return false;
	    }
	}

	/**
	 * Updates the status of a library member based on the member ID. If the current
	 * status is "suspended", it will set the status to "active". Otherwise, it will
	 * set the status to "suspended".
	 *
	 * @param memberId The ID of the member to update.
	 * @return true if the status was updated successfully, false otherwise.
	 */
	public boolean updateMemberStatus(String memberId) {
		// Get the collection for library members
		MongoCollection<Document> membersCollection = database.getCollection("library_members");

		// Query the member by ID
		Document query = new Document("_id", new ObjectId(memberId));
		Document memberDoc = membersCollection.find(query).first();

		if (memberDoc == null) {
			// Member not found
			return false;
		}

		// Get the current status of the member
		String currentStatus = memberDoc.getString("status");

		// Determine the new status based on the current status
		String newStatus;
		if ("suspended".equalsIgnoreCase(currentStatus)) {
			newStatus = "active";
		} else {
			newStatus = "suspended";
		}

		// Update the member's status
		Document update = new Document("$set", new Document("status", newStatus));
		membersCollection.updateOne(query, update);

		// Verify the update was successful
		Document updatedMemberDoc = membersCollection.find(query).first();
		return updatedMemberDoc != null && newStatus.equals(updatedMemberDoc.getString("status"));
	}

	public List<LibraryMember> getAllMembers() {
		List<LibraryMember> members = new ArrayList<>();
		try {
			// Get the collection for library members
			MongoCollection<Document> membersCollection = database.getCollection("library_members");

			// Find all documents in the collection
			try (MongoCursor<Document> cursor = membersCollection.find().iterator()) {
				while (cursor.hasNext()) {
					Document doc = cursor.next();

					// Extract member data from the document
					// Extract the member ID as an ObjectId
					ObjectId memberId = doc.getObjectId("_id");
					String username = doc.getString("username");
					String email = doc.getString("email");
					String status = doc.getString("status"); // Additional fields can be extracted

					// Create a LibraryMember object from the document data
					LibraryMember member = new LibraryMember();
					member.setId(memberId);
					member.setUsername(username);
					member.setEmail(email);
					member.setStatus(status);

					// Add the member to the list
					members.add(member);
				}
			}
		} catch (Exception e) {
			// Handle any exceptions
			e.printStackTrace();
		}

		// Return the list of members
		return members;
	}

}