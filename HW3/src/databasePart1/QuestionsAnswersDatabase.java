package databasePart1;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import application.Question;
import application.Answer;
import application.Comment;

public class QuestionsAnswersDatabase {
	
	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/QuestionAnswerDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	private Connection connection = null;
	private Statement statement = null;
		
	public void connectToQuestionAnswerDatabase() throws SQLException {
		System.out.println("tester\n");
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
//			statement.execute("DROP ALL OBJECTS");

			// Create the necessary tables if they don't exist
			createQuestionTable();  
			createAnswerTable();
			createCommentTable();
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}
	
	
	// Create the questions table
	private void createQuestionTable() throws SQLException {
		String questionTable = "CREATE TABLE IF NOT EXISTS questions ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "title VARCHAR(255), "
				+ "body TEXT, "
				+ "deleted BOOLEAN DEFAULT FALSE, "
				+ "resolved BOOLEAN DEFAULT FALSE)";
		statement.execute(questionTable);
	}
	
	// Create the answers table
	private void createAnswerTable() throws SQLException {
		String answerTable = "CREATE TABLE IF NOT EXISTS answers ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "qID INT,"
				+ "userName VARCHAR(255), "
				+ "content TEXT, "
				+ "resolves BOOLEAN DEFAULT FALSE ,"
				+ "FOREIGN KEY (qID) REFERENCES questions(id) ON DELETE CASCADE)";
		statement.execute(answerTable);
	}
	
	private void createCommentTable() throws SQLException {
		String commentTable = "CREATE TABLE IF NOT EXISTS comments ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "qID INT,"
				+ "userName VARCHAR(255), "
				+ "content TEXT, "
				+ "FOREIGN KEY (qID) REFERENCES questions(id) ON DELETE CASCADE)";
		statement.execute(commentTable);
	}
	
	// Count the number of questions
	public int numQuestions() throws SQLException {
		String q = "SELECT COUNT(*) AS count FROM questions";
		ResultSet rs = statement.executeQuery(q);
		if (rs.next()) {
			return rs.getInt("count");
		}
		return 0;
	}
	
	
	// Adds a question to the database
	public void addQuestion(Question question) throws SQLException {
		String q = "INSERT INTO questions (userName, title, body) VALUES (?, ?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setString(1, question.getUserName());
			pstmt.setString(2, question.getTitle());
			pstmt.setString(3, question.getBody());
			pstmt.executeUpdate();
		}
	}
	
	
	// Return the title associated with a question id
	public String getTitleFromQuestionID(int id) {
		String q = "SELECT title FROM questions WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("title");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "Error";
	}
	
	
	// Return an array containing the user name, title, and body of a question associated with an id
	public String[] getQuestionInfo(int id) {
		String q = "SELECT * FROM questions WHERE id = ?";
		String out[] = new String[3];
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				out[0] = rs.getString("userName");
				out[1] = rs.getString("title");
				out[2] = rs.getString("body");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	// Return true if the question associated with an id was created by a specified user
	public boolean doesUserOwnQuestion(int id, String userName) {
		String q = "SELECT userName FROM questions WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				String user = rs.getString("userName");
				if (user.equals(userName)) {
					return true;
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	// 'Delete' a question by setting the deleted boolean to true
	// Does not actually delete the question so it is still held in the database
	public void deleteQuestion(int id) {
		String q = "UPDATE questions SET deleted = TRUE WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	// Return true if a question has been deleted
	public boolean isDeleted(int id) throws SQLException {
		String q = "SELECT deleted FROM questions WHERE id = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, id);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getBoolean("deleted");
	        }
	    }
	    return false;
	}
	
	
	// Set a question to resolved
	public void resolveQuestion(int id) {
		String q = "UPDATE questions SET resolved = TRUE WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	// Set a question to unresolved
	public void unResolveQuestion(int id) {
		String q = "UPDATE questions SET resolved = FALSE WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	// Return true if a question has been resolved
	public boolean isResolved(int id){
		String q = "SELECT resolved FROM questions WHERE id = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, id);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getBoolean("resolved");
	        }
	        
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	    return false;
	}
	
	
	// Update the body of a question to any new string
	public void updateQuestion(int id, String updatedBody) {	
		String q = "UPDATE questions SET body = ? WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setString(1, updatedBody);
			pstmt.setInt(2, id);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	// Return the number of answers for any question
	public int numAnswers(int qID) throws SQLException {
		String q = "SELECT COUNT(*) AS count FROM answers WHERE qID = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, qID);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("count");
			}
		}
		return 0;
	}
	
	
	// Adds an answer to a question
	public void addAnswer(Answer answer) throws SQLException{
		String q = "INSERT INTO answers (qID, userName, content) VALUES (?, ?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, answer.getQID());
			pstmt.setString(2, answer.getUserName());
			pstmt.setString(3, answer.getContent());
			pstmt.executeUpdate();
		}
	}
	
	
	// Return an array list of arrays of strings containing the user name and content associated with answers
	public List<String[]> getAnswers(int qID) {
		String q = "SELECT userName, content FROM answers WHERE qID = ?";
		List<String[]> answers = new ArrayList<>();
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, qID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				answers.add(new String[] {rs.getString("userName"), rs.getString("content")});
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return answers;
	}
	
	
	//Return the answer ID associated with a question ID given the number of response
	public int getAnswerID(int qID, int answerCount){
		String q = "SELECT id FROM answers WHERE qID = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, qID);
	        ResultSet rs = pstmt.executeQuery();
	        int counter = 0;
	        while (rs.next()) {
	        	if (answerCount == counter) {
	        		return rs.getInt("id");
	        	}
	        	counter++;
	        }
	        
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	    return 0;
	}
	
	
	// Sets the answer resolves to true
	public void answerResolves(int id, int answerID) {
		clearResolves(id, answerID);
		String q = "UPDATE answers SET resolves = TRUE WHERE id = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, answerID);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public boolean doesAnswerResolve(int answerID) {
		String q = "SELECT resolves FROM answers WHERE id = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, answerID);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	        	return rs.getBoolean("resolves");
	        }
	        
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	    return false;
	}
	
	
	public void clearResolves(int qID, int answerID) {
		String q = "UPDATE answers SET resolves = FALSE WHERE qID = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, qID);
	        pstmt.executeUpdate();
	        
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	}
	
	//comment implementation in database
	
	public int numComments(int qID) throws SQLException {
		String q = "SELECT COUNT(*) AS count FROM comments WHERE qID = ?";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, qID);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("count");
			}
		}
		return 0;
	}
	
	
	// Adds an comment to a question
	public void addComments(Comment comment) throws SQLException{
		String q = "INSERT INTO comments (qID, userName, content) VALUES (?, ?, ?)";
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, comment.getQID());
			pstmt.setString(2, comment.getUserName());
			pstmt.setString(3, comment.getContent());
			pstmt.executeUpdate();
		}
	}
	
	
	// Return an array list of arrays of strings containing the user name and content associated with comments
	public List<String[]> getComments(int qID) {
		String q = "SELECT userName, content FROM comments WHERE qID = ?";
		List<String[]> comments = new ArrayList<>();
		
		try (PreparedStatement pstmt = connection.prepareStatement(q)) {
			pstmt.setInt(1, qID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				comments.add(new String[] {rs.getString("userName"), rs.getString("content")});
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return comments;
	}
	
	
	//Return the comment ID associated with a question ID given the number of response
	public int getCommentID(int qID, int commentCount){
		String q = "SELECT id FROM comments WHERE qID = ?";
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(q)) {
	        pstmt.setInt(1, qID);
	        ResultSet rs = pstmt.executeQuery();
	        int counter = 0;
	        while (rs.next()) {
	        	if (commentCount == counter) {
	        		return rs.getInt("id");
	        	}
	        	counter++;
	        }
	        
	    } catch (SQLException e) {
	    	e.printStackTrace();
	    }
	    return 0;
	}
	
	
}