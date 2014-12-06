package koda;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserDatabase implements Serializable {

	public static final String filename = "users.dat";
	
	private List<User> users;
	
	public UserDatabase() {
		users = new ArrayList<User>();
	}
	
	public void addUser(String username, String password) {
		User user = new User(username, password);
		addUser(user);
	}
	
	public void addUser(User user) {
		System.out.println("Added a new user to database: " + user);
		users.add(user);
	}
	
	public void removeUser(String username) {
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			if (user.getUsername().equals(username)) {
				users.remove(i);
				break;
			}
		}
	}
	
	public int verifyUser(String username, String password) {
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			if (user.getUsername().equalsIgnoreCase(username)) {
				if (user.getPassword().equals(password)) {
					return LoginResponse.LOGIN_SUCCESSFUL;
				} else {
					return LoginResponse.LOGIN_BAD_PASSWORD;
				}
			}
		}
		
		//didn't find the user, so create one. temporary implementation
		addUser(username, password);
		return LoginResponse.LOGIN_NEW_USER;
	}
	
	public int numUsers() {
		return users.size();
	}
	
	public void write() {
		File file = new File(filename);
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(this);
			System.out.println("Server: Database file " + filename + " successfully written.");
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static UserDatabase read() {
		File file = new File(filename);
		if (!file.exists()) {
			System.out.println("Server: Database file " + filename + " not found. Creating a new one.");
			return new UserDatabase();
		}
		
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(file));
			UserDatabase udb = (UserDatabase) ois.readObject();
			System.out.println("Server: Database file " + filename + " successfully read.");
			System.out.println(udb.numUsers() + " user(s) found.");
			ois.close();
			return udb;
		} catch (InvalidClassException e) {
			System.out.println("Unwarranted changes to UserDatabase detected. Creating fresh database.");
			return new UserDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
