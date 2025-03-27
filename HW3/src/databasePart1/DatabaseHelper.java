package databasePart1;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.sql.Timestamp;
import application.User;


/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	private Connection connection = null;
	private Statement statement = null; 
	//	PreparedStatement pstmt

	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
//			statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}
	
	public void initializeTestDatabase() throws SQLException {
	    connectToDatabase();
	    statement.execute("DROP ALL OBJECTS");
	    createTables();  // Re-create the necessary tables
	}

	private void createTables() throws SQLException {
		String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "password VARCHAR(255), "
				+ "role VARCHAR(255),"
				+ "oneTimePass VARCHAR(255))";
		statement.execute(userTable);
		
		// Create the invitation codes table
	    String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	            + "code VARCHAR(10) PRIMARY KEY, "
	            + "isUsed BOOLEAN DEFAULT FALSE, "
	            + "admin BOOLEAN DEFAULT FALSE, "
	            + "student BOOLEAN DEFAULT FALSE, "
	            + "instructor BOOLEAN DEFAULT FALSE, "
	            + "staff BOOLEAN DEFAULT FALSE, "
	            + "reviewer BOOLEAN DEFAULT FALSE, "
	            + "generationTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
	    statement.execute(invitationCodesTable);
	}


	// Check if the database is empty
	public boolean isDatabaseEmpty() throws SQLException {
		String query = "SELECT COUNT(*) AS count FROM cse360users";
		ResultSet resultSet = statement.executeQuery(query);
		if (resultSet.next()) {
			return resultSet.getInt("count") == 0;
		}
		return true;
	}

	// Registers a new user in the database.
	public void register(User user) throws SQLException {
		String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole());
			pstmt.executeUpdate();
		}
	}

	// Validates a user's login credentials.
	public boolean login(User user) throws SQLException {
		String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ? AND role = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole());
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}
	
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}
	
	// Retrieves the role of a user from the database using their UserName.
	public String getUserRole(String userName) {
	    String query = "SELECT role FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            return rs.getString("role"); // Return the role if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null; // If no user exists or an error occurs
	}
	
	// Generates a new invitation code for any number of roles and inserts it into the database.
	public String generateInvitationCode(boolean admin, boolean student, boolean instructor, boolean staff, boolean reviewer) {
	    String code = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character code
	    String query = "INSERT INTO InvitationCodes (code, admin, student, instructor, staff, reviewer, generationTime) VALUES (?, ?, ?, ?, ?, ?, ?)";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.setBoolean(2, admin);
	        pstmt.setBoolean(3, student);
	        pstmt.setBoolean(4, instructor);
	        pstmt.setBoolean(5, staff);
	        pstmt.setBoolean(6, reviewer);
	        pstmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return code;
	}
	
	// Validates an invitation code to check if it is unused.
	public boolean validateInvitationCode(String code) {
	    String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE"; 
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	        	Timestamp generationTime = rs.getTimestamp("generationTime");
                long currentTimeMillis = System.currentTimeMillis();
                long generationTimeMillis = generationTime.getTime();

	            // Mark the code as used
	            markInvitationCodeAsUsed(code);
	            
	            // Check if code was created less than an hour ago
	            if ((currentTimeMillis - generationTimeMillis) <= 3600000) {
	            	return true;
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
	
	// Marks the invitation code as used in the database.
	private void markInvitationCodeAsUsed(String code) {
	    String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
	
	// Retrieves the role from an invitation code.
	public String getRoleFromCode(String code) {
		String query = "SELECT admin, student, instructor, staff, reviewer FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        
	        // Checks boolean for each role
	        if (rs.next()) {
	        	boolean admin = rs.getBoolean("admin");
                boolean student = rs.getBoolean("student");
                boolean instructor = rs.getBoolean("instructor");
                boolean staff = rs.getBoolean("staff");
                boolean reviewer = rs.getBoolean("reviewer");
                
                // Add each role to a string to return
                String role = "";
                if (admin) role += "admin, ";
                if (student) role += "student, ";
                if (instructor) role += "instructor, ";
                if (staff) role += "staff, ";
                if (reviewer) role += "reviewer, ";
                
                // Return the role if user exists
                System.out.println(role.substring(0, role.length()-2));
                return role.substring(0, role.length()-2);
	        } else {
	        	System.out.println("Error: No role for code");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null; // If no user exists or an error occurs
	}
	
	public void assignRolesFromCodeToUser(String userName, String code) throws SQLException {
	    // Get the roles from the code
		String rolesFromCode = getRoleFromCode(code);

		//enter roles into table as comma separated string
		String updateRoleQuery = "UPDATE cse360users SET role = ? WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(updateRoleQuery)) {
	        pstmt.setString(1, rolesFromCode);
	        pstmt.setString(2, userName);
	        pstmt.executeUpdate();
	    }catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	public String[] roleToArray(String role) {
		String[] roleArray = role.split(", ");

		return roleArray;
	}

	
	// Generate a random 10-character password and assign it to a userName
	public String createOneTimePass(String userName) {
		String pass = UUID.randomUUID().toString().substring(0, 10);
		String query = "UPDATE cse360users SET oneTimePass = ? WHERE userName = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			
			pstmt.setString(1, pass);
			pstmt.setString(2, userName);
			pstmt.executeUpdate();
			
			// Return the password
			return pass;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// Check if userName and oneTimePassword are a valid pair
	public boolean checkOneTimePass(String userName, String password) throws SQLException {
		String query = "SELECT oneTimePass FROM cse360users WHERE userName = ?"; 
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	    	
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	        	if (rs.getString("oneTimePass") == null) {
	        		return false;
	        	}
	        	if (rs.getString("oneTimePass").equals(password)) {
	        		System.out.println(rs.getString("oneTimePass"));
	        		
	        		// If the password is valid, remove it
	        		removeOneTimePass(userName);
	        		return true;
	        	}
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	// Remove the temporary password
	public void removeOneTimePass(String userName) {
		String query = "UPDATE cse360users SET oneTimePass = ? WHERE userName = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			
			pstmt.setString(1, null);
			pstmt.setString(2, userName);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Update the password associated with the userName
	public void updateUserPassword(String userName, String password) throws SQLException {
		String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			
			pstmt.setString(1, password);
			pstmt.setString(2, userName);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Delete the user (userName) from the database
	public String tryDeleteUser(String userName) throws SQLException {
		// Check if the user exists 
		if (doesUserExist(userName)) {
			
			String query = "SELECT role FROM cse360users WHERE username = ?";
			try(PreparedStatement pstmt = connection.prepareStatement(query)) {
				pstmt.setString(1, userName);
				ResultSet rs = pstmt.executeQuery();
				
				// If user has admin role, do not delete
		        if (rs.next()) {
		        		if (rs.getString("role") != "admin") {
		        			deleteUser(userName);
		        			return userName;
		        		} else {
		        			return "Cannot delete admin";
		        		}
		        }
		        pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return "User does not exist";
	}
	
	// Delete the user (userName) from the database
	public void deleteUser(String userName) throws SQLException {
		String query = "DELETE FROM cse360users WHERE username = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, userName);
			System.out.println("Deleted " + userName);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// List all users and info
	public String listUsers() throws SQLException {
		String query = "SELECT username, role FROM cse360users"; // ADD NAME AND EMAIL WHEN NECESSARY
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			
			String result = "";
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				result = result + "UserName: " + rs.getString("username") + ", Role(s): " + rs.getString("role") + "\n";
			}
			return result;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	// Change user roles
	public void changeRoles(String userName, String roles) throws SQLException {
		
		// If user is last admin, do not accept new roles
		String query1 = "SELECT role FROM cse360users WHERE userName = ?";
		int counter = 2;
		
		try(PreparedStatement pstmt = connection.prepareStatement(query1)) {
			
			pstmt.setString(1, userName);
			
			String oldRoles = "";
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				oldRoles = rs.getString("role");
			}
			
			if (oldRoles.contains("admin") && !(oldRoles.isEmpty())) {
				counter = 0;
				String query2 = "SELECT role FROM cse360users";
				try(PreparedStatement pstmt2 = connection.prepareStatement(query2)) {
					
					ResultSet rs2 = pstmt2.executeQuery();
					while (rs2.next()) {
						if (rs2.getString("role").contains("admin")) {
							counter++;
							System.out.println("yup" + rs2.getString("role"));
						} else {
							System.out.println("nope" + rs2.getString("role"));
						}
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (counter < 2) {
			return;
		}
		String query = "UPDATE cse360users SET role = ? WHERE userName = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			
			pstmt.setString(1, roles);
			pstmt.setString(2, userName);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Closes the database connection and statement.
	public void closeConnection() {
		try{ 
			if(statement!=null) statement.close(); 
		} catch(SQLException se2) { 
			se2.printStackTrace();
		} 
		try { 
			if(connection!=null) connection.close(); 
		} catch(SQLException se){ 
			se.printStackTrace(); 
		} 
	}

}
