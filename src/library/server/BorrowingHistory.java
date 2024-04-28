package library.server;

import org.bson.types.ObjectId;

public class BorrowingHistory {
	private ObjectId borrowId;
	private ObjectId itemId;
	private ObjectId memberId;

	// New attributes for borrowed history
	private String title;
	private String author;
	private String type;
	private String borrower;
	private String dueDate;
	private String dateBorrowed;
	private String dateReturned;

	// Constructors, getters, and setters
	public BorrowingHistory() {
	}

	// Adjusted constructor to include new attributes
	public BorrowingHistory(ObjectId borrowId, ObjectId itemId, ObjectId memberId, String title, String author,
			String type, String borrower, String dueDate, String dateBorrowed, String dateReturned) {
		this.borrowId = borrowId;
		this.itemId = itemId;
		this.memberId = memberId;

		this.title = title;
		this.author = author;
		this.type = type;
		this.borrower = borrower;
		this.dueDate = dueDate;
		this.dateBorrowed = dateBorrowed;
	}

	public ObjectId getBorrowId() {
		return borrowId;
	}

	public void setBorrowId(ObjectId borrowId) {
		this.borrowId = borrowId;
	}

	public void setReturnedDate(String returnedDate) {
		this.dateReturned = returnedDate;
	}

	public String getReturnedDate() {
		return dateReturned;
	}

	public ObjectId getItemId() {
		return itemId;
	}

	public void setItemId(ObjectId itemId) {
		this.itemId = itemId;
	}

	public ObjectId getMemberId() {
		return memberId;
	}

	public void setMemberId(ObjectId memberId) {
		this.memberId = memberId;
	}

	// Getter and setter methods for new attributes

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBorrower() {
		return borrower;
	}

	public void setBorrower(String borrower) {
		this.borrower = borrower;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getDateBorrowed() {
		return dateBorrowed;
	}

	public void setDateBorrowed(String dateBorrowed) {
		this.dateBorrowed = dateBorrowed;
	}
}
