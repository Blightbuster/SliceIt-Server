package com.BlightStudios.SliceItServer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import org.jongo.Jongo;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class DatabaseManager {
	private static String ip;
	private static int port;
	private static String username;
	private static String password;
	private static String authdatabase;
	private static String database;
	private static String collection;

	public static org.jongo.MongoCollection PlayerData;

	public static void main(String args[]) {
		// Get all properties
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("db.config");

			// Load the properties file
			prop.load(input);

			// Get the property values
			ip = prop.getProperty("ip");
			port = Integer.parseInt(prop.getProperty("port"));
			username = prop.getProperty("username");
			password = prop.getProperty("password");
			authdatabase = prop.getProperty("authdatabase");
			database = prop.getProperty("database");
			collection = prop.getProperty("collection");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Establish connection to the database
		MongoCredential credential = MongoCredential.createCredential(username, authdatabase, password.toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port), Arrays.asList(credential));
		System.out.println("Connected to the database successfully");

		// Accessing the database
		DB db = mongoClient.getDB(database);
		Jongo jongo = new Jongo((DB) db);
		PlayerData = jongo.getCollection(collection);
	}

	public static User getUserByToken(String token) {
		if (!isUserRegisteredByToken(token))
			return null;
		return PlayerData.findOne("{Token: '" + token + "'}").as(User.class);
	}

	public static User getUserByName(String name) {
		if (!isUserRegisteredByName(name))
			return null;
		return PlayerData.findOne("{Name: '" + name + "'}").as(User.class);
	}

	public static boolean isUserRegisteredByToken(String token) {
		long count = PlayerData.count("{Token: '" + token + "'}");
		return count > 0;
	}

	public static boolean isUserRegisteredByName(String name) {
		long count = PlayerData.count("{Name: '" + name + "'}");
		return count > 0;
	}

	public static boolean isCredentialValid(String name, String password) {
		if (!isUserRegisteredByName(name))
			return false;
		User userObject = getUserByName(name);
		if (userObject.PasswordHash.equals(WorkerRunnable.SHA256(password)))
			return true;
		return false;
	}

	public static void createUser(User user) {
		PlayerData.insert(user);
	}

	public static void updateUser(User user) {
		PlayerData.update("{Name: '" + user.Name + "'}").with(user);
	}
}