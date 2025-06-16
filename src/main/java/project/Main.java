package project;

import api.ApiServer;
import dao.DatabaseConnection;
import services.OTPHandler;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseConnection.start();
            ApiServer.start();
            OTPHandler.start();
        } catch (SQLException e) {
            System.out.println("Database Connection Error.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("API Server Error.");
            e.printStackTrace();
        }
    }
}