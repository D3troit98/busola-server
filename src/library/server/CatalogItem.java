package library.server;

import java.util.List;

import org.bson.types.ObjectId;

public class CatalogItem {
	private String title;
	private String author;
	private String type;
	private boolean available;
	private ObjectId borrowerId;
	private String borrower;
	private String dueDate;
	private String dateBorrowed;
	private ObjectId itemId;
	private String filePath;
	private List<ObjectId> holdList; // List of user IDs waiting for the item
	private int rating; // Rating attribute

	// Constructors
	public CatalogItem(ObjectId itemId, String title, String author, String type, boolean available,
			ObjectId borrowerId, String dueDate, String dateBorrowed, String borrower,
			String filePath, List<ObjectId> holdList, int rating) {
		this.title = title;
		this.author = author;
		this.type = type;
		this.available = available;
		this.borrowerId = borrowerId;
		this.dueDate = dueDate;
		this.dateBorrowed = dateBorrowed;
		this.borrower = borrower;
		this.itemId = itemId;
		this.filePath = filePath;
		this.holdList = holdList;
		this.rating = rating;
	}

	// Getters and setters for all attributes...

	public List<ObjectId> getHoldList() {
		return holdList;
	}

	public void setHoldList(List<ObjectId> holdList) {
		this.holdList = holdList;
	}

	public float getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	// Getters and setters
	public String getDueDate() {
		return dueDate;
	}

	public ObjectId getItemId() {
		return itemId;
	}

	public void setItemId(ObjectId itemId) {
		this.itemId = itemId;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getAuthor() {
		return author;
	}

	public String getType() {
		return type;
	}

	public boolean isAvailable() {
		return available;
	}

	/**
	 * Retrieves the book content associated with this CatalogItem.
	 * 
	 * @return the book content as a byte array.
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Sets the book content for this CatalogItem.
	 * 
	 * @param bookContent the book content as a byte array.
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public ObjectId getBorrowerId() {
		return borrowerId;
	}

	public void setBorrowerId(ObjectId borrowerId) {
		this.borrowerId = borrowerId;
	}

	public void setDateBorrowed(String dateBorrowed) {
		this.dateBorrowed = dateBorrowed;
	}

	public String getDateBorrowed() {
		return dateBorrowed;
	}

	public void setBorrower(String borrower) {
		this.borrower = borrower;
	}

	public String getBorrower() {
		return borrower;
	}
}
