package test;

import static org.junit.Assert.*;

import org.mockito.Mock ;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import library.server.CatalogItem;
import library.server.ChatMessage;
import library.server.LibraryDatabase;
import library.server.LibraryMember;
import library.server.Server;

import java.io.*;

public class ServerTest {
    private Server server;

    @Mock
    private LibraryDatabase database;

    @SuppressWarnings("deprecation")
	@Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        server = new Server();
        server.database = database;
    }

    @Test
    public void testRegisterUser_Success() throws IOException {
        // Prepare mock data
        String requestData = "{\"username\":\"testuser\",\"password\":\"testpass\",\"email\":\"test@example.com\"}";
        String httpRequest = "POST /register HTTP/1.1\r\n" +
                             "Host: localhost\r\n" +
                             "Content-Type: application/json\r\n" +
                             "Content-Length: " + requestData.length() + "\r\n\r\n" +
                             requestData;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter out = new PrintWriter(outputStream, true);

        // Mock the database behavior
        when(database.registerUser("testuser", "testpass", "test@example.com")).thenReturn(true);

        // Call the method to test
        server.handleRegisterUser(in, out);

        // Verify the expected response
        String response = new String(outputStream.toByteArray());
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("\"status\":\"success\""));

        // Verify the database interactions
        verify(database).registerUser("testuser", "testpass", "test@example.com");
    }
   

    
    
    @Test
    public void testRegisterUser_Failure() throws IOException {
        // Prepare mock data with missing email
        String requestData = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        // Create an HTTP request with necessary headers and the mock data
        String httpRequest = "POST /register HTTP/1.1\r\n" +
                             "Host: localhost\r\n" +
                             "Content-Type: application/json\r\n" +
                             "Content-Length: " + requestData.length() + "\r\n\r\n" +
                             requestData;

        // Create input and output streams
        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter out = new PrintWriter(outputStream, true);

        // Call the method to test
        server.handleRegisterUser(in, out);

        // Verify the expected response
        String response = new String(outputStream.toByteArray());
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"));
        assertTrue(response.contains("\"status\":\"error\""));
        assertTrue(response.contains("Empty fields"));
    }
    
   
    @Test
    public void testUserLogin_Success() throws IOException {
        // Prepare mock data for a successful login
        String requestData = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        String httpRequest = "POST /login HTTP/1.1\r\n" +
                             "Host: localhost\r\n" +
                             "Content-Type: application/json\r\n" +
                             "Content-Length: " + requestData.length() + "\r\n\r\n" +
                             requestData;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter out = new PrintWriter(outputStream, true);

        // Mock the database behavior for user authentication
        LibraryMember testUser = new LibraryMember();
        testUser.setUsername("testuser");
        testUser.setPassword("hashed_password");
        testUser.setId(new ObjectId());
        when(database.authenticateUser("testuser", "testpass")).thenReturn(testUser);

        // Call the method to test
        server.handleLogin(in, out);

        // Verify the expected response
        String response = new String(outputStream.toByteArray());
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("\"status\":\"success\""));
        assertTrue(response.contains("\"username\":\"testuser\""));

        // Verify the database interactions
        verify(database).authenticateUser("testuser", "testpass");
    }

    @Test
    public void testSendChatMessage_Success() throws IOException {
        // Prepare mock data for sending a chat message
        String requestData = "{\"userId\":\"testuserid\",\"message\":\"Hello World!\"}";
        String httpRequest = "POST /send-chat-message HTTP/1.1\r\n" +
                             "Host: localhost\r\n" +
                             "Content-Type: application/json\r\n" +
                             "Content-Length: " + requestData.length() + "\r\n\r\n" +
                             requestData;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter out = new PrintWriter(outputStream, true);

        // Mock the database behavior for adding a chat message
        when(database.addChatMessage(any(ChatMessage.class))).thenReturn(true);

        // Call the method to test
        server.handleSendChatMessage(in, out);

        // Verify the expected response
        String response = new String(outputStream.toByteArray());
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("\"status\":\"success\""));

        // Verify the database interactions
        verify(database).addChatMessage(any(ChatMessage.class));
    }

    @Test
    public void testAddCatalogItem_Success() throws IOException {
        // Prepare mock data for adding a catalog item
        String requestData = "{\"title\":\"Sample Book\",\"author\":\"Author Name\",\"type\":\"Book\",\"available\":true,\"file_path\":\"/path/to/file.pdf\"}";
        String httpRequest = "POST /add-catalog-item HTTP/1.1\r\n" +
                             "Host: localhost\r\n" +
                             "Content-Type: application/json\r\n" +
                             "Content-Length: " + requestData.length() + "\r\n\r\n" +
                             requestData;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter out = new PrintWriter(outputStream, true);

        // Mock the database behavior for adding a catalog item
        when(database.addItem(any(CatalogItem.class))).thenReturn(true);

        // Call the method to test
        server.handleAddCatalogItem(in, out);

        // Verify the expected response
        String response = new String(outputStream.toByteArray());
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("\"status\":\"success\""));

        // Verify the database interactions
        verify(database).addItem(any(CatalogItem.class));
    }
}