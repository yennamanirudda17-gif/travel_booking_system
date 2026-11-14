import java.sql.*;
import java.util.Scanner;

public class TravelBookingSystem {
    // MySQL Database connection settings
    static final String DB_URL = "jdbc:mysql://localhost:3306/travel_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    static final String DB_USER = "root";
    static final String DB_PASS = "1234"; 
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Initialize Database and Tables automatically
        initializeDatabase();

        int choice = 0;
        do {
            System.out.println("\n========================================");
            System.out.println("     TRAVEL BOOKING SYSTEM (MYSQL)      ");
            System.out.println("========================================");
            System.out.println("1. View Available Flights");
            System.out.println("2. Book a Flight");
            System.out.println("3. View All Bookings");
            System.out.println("4. Exit");
            System.out.print("Enter your choice (1-4): ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        viewFlights();
                        break;
                    case 2:
                        viewFlights();
                        System.out.print("\nEnter Passenger Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Flight ID to Book: ");
                        int flightId = Integer.parseInt(scanner.nextLine());
                        bookFlight(name, flightId);
                        break;
                    case 3:
                        viewBookings();
                        break;
                    case 4:
                        System.out.println("\nExiting system. Safe travels");
                        break;
                    default:
                        System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[!] Error: Please enter a valid numeric input.");
            }
        } while (choice != 4);

        scanner.close();
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // Create Flights Table
            String sqlFlights = "CREATE TABLE IF NOT EXISTS flights (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "flight_no VARCHAR(50) NOT NULL, " +
                    "origin VARCHAR(50) NOT NULL, " +
                    "destination VARCHAR(50) NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "seats INT NOT NULL)";
            stmt.execute(sqlFlights);

            // Create Bookings Table
            String sqlBookings = "CREATE TABLE IF NOT EXISTS bookings (" +
                    "booking_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "passenger_name VARCHAR(100) NOT NULL, " +
                    "flight_id INT, " +
                    "FOREIGN KEY(flight_id) REFERENCES flights(id))";
            stmt.execute(sqlBookings);

            // Seed initial data if table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM flights");
            if (rs.next() && rs.getInt(1) == 0) {
                String seed = "INSERT INTO flights (flight_no, origin, destination, price, seats) VALUES " +
                        "('AI-101', 'Hyderabad', 'Delhi', 120.0, 30), " +
                        "('EK-502', 'Hyderabad', 'Dubai', 350.0, 15), " +
                        "('6E-214', 'Bangalore', 'Goa', 90.0, 20);";
                stmt.execute(seed);
            }
        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
    }

    public static void viewFlights() {
        System.out.println("\n----------------- AVAILABLE FLIGHTS -----------------");
        System.out.printf("%-5s | %-10s | %-12s | %-12s | %-8s | %-5s\n", "ID", "Flight", "Origin", "Destination", "Price", "Seats");
        System.out.println("-----------------------------------------------------");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM flights")) {

            while (rs.next()) {
                System.out.printf("%-5d | %-10s | %-12s | %-12s | $%-7.2f | %-5d\n",
                        rs.getInt("id"),
                        rs.getString("flight_no"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getDouble("price"),
                        rs.getInt("seats"));
            }
        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
        System.out.println("-----------------------------------------------------");
    }

    public static void bookFlight(String passengerName, int flightId) {
        String checkSeatsSQL = "SELECT seats FROM flights WHERE id = ?";
        String insertBookingSQL = "INSERT INTO bookings (passenger_name, flight_id) VALUES (?, ?)";
        String updateSeatsSQL = "UPDATE flights SET seats = seats - 1 WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false); 

            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkSeatsSQL)) {
                pstmtCheck.setInt(1, flightId);
                ResultSet rs = pstmtCheck.executeQuery();

                if (rs.next()) {
                    int seats = rs.getInt("seats");
                    if (seats <= 0) {
                        System.out.println("\n[!] Flight is fully booked.");
                        return;
                    }
                } else {
                    System.out.println("\n[!] Invalid Flight ID.");
                    return;
                }
            }

            try (PreparedStatement pstmtBook = conn.prepareStatement(insertBookingSQL);
                 PreparedStatement pstmtUpdate = conn.prepareStatement(updateSeatsSQL)) {
                
                pstmtBook.setString(1, passengerName);
                pstmtBook.setInt(2, flightId);
                pstmtBook.executeUpdate();

                pstmtUpdate.setInt(1, flightId);
                pstmtUpdate.executeUpdate();

                conn.commit();
                System.out.println("\n[+] Success: Flight booked successfully for " + passengerName + "!");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("\n[-] Transaction failed. Rolled back.");
            }

        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
    }

    public static void viewBookings() {
        System.out.println("\n----------------- PASSENGER BOOKINGS -----------------");
        System.out.printf("%-10s | %-15s | %-10s | %-12s | %-12s\n", "Booking ID", "Passenger", "Flight", "Origin", "Destination");
        System.out.println("------------------------------------------------------");

        String query = "SELECT b.booking_id, b.passenger_name, f.flight_no, f.origin, f.destination " +
                       "FROM bookings b JOIN flights f ON b.flight_id = f.id";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                System.out.printf("%-10d | %-15s | %-10s | %-12s | %-12s\n",
                        rs.getInt("booking_id"),
                        rs.getString("passenger_name"),
                        rs.getString("flight_no"),
                        rs.getString("origin"),
                        rs.getString("destination"));
            }
        } catch (SQLException e) {
            System.out.println("[DB ERROR] " + e.getMessage());
        }
        System.out.println("------------------------------------------------------");
    }
}