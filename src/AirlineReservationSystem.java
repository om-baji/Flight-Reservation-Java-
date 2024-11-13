import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import org.bson.types.ObjectId;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.util.List;
import java.util.regex.Pattern;

public class AirlineReservationSystem {
    private JFrame mainFrame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> flightsCollection;
    private MongoCollection<Document> passengersCollection;
    private MongoCollection<Document> bookingsCollection;

    public AirlineReservationSystem() {
        setupDatabase();
        createUI();
    }

    private void setupDatabase() {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("airlineDB");

            flightsCollection = database.getCollection("flights");
            passengersCollection = database.getCollection("passengers");
            bookingsCollection = database.getCollection("bookings");

            flightsCollection.createIndex(Indexes.ascending("flightNumber"));
            passengersCollection.createIndex(Indexes.ascending("passportNumber"));
            bookingsCollection.createIndex(Indexes.ascending("flightNumber"));
            bookingsCollection.createIndex(Indexes.ascending("passportNumber"));

            if (flightsCollection.countDocuments() == 0) {
                insertSampleData();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
    }

    private void insertSampleData() {
        List<Document> flights = Arrays.asList(
                new Document().append("flightNumber", "FL001").append("origin", "New York").append("destination", "London").append("departureDate", "2024-12-01 10:00:00").append("totalSeats", 100).append("availableSeats", 100).append("price", 500.0),
                new Document().append("flightNumber", "FL002").append("origin", "London").append("destination", "Paris").append("departureDate", "2024-12-01 14:00:00").append("totalSeats", 150).append("availableSeats", 150).append("price", 200.0)
        );

        flightsCollection.insertMany(flights);
    }

    private void createUI() {
        mainFrame = new JFrame("Airline Reservation System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createMainMenu();
        createSearchFlightsPanel();
        createBookingPanel();
        createViewBookingsPanel();

        mainFrame.add(mainPanel);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private void createMainMenu() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Airline Reservation System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton searchButton = new JButton("Search Flights");
        JButton bookButton = new JButton("Book Flight");
        JButton viewBookingsButton = new JButton("View Bookings");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        menuPanel.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        menuPanel.add(searchButton, gbc);

        gbc.gridy = 2;
        menuPanel.add(bookButton, gbc);

        gbc.gridy = 3;
        menuPanel.add(viewBookingsButton, gbc);

        searchButton.addActionListener(e -> cardLayout.show(mainPanel, "search"));
        bookButton.addActionListener(e -> cardLayout.show(mainPanel, "book"));
        viewBookingsButton.addActionListener(e -> cardLayout.show(mainPanel, "viewBookings"));

        mainPanel.add(menuPanel, "menu");
    }

    private void populateFlightsTable(DefaultTableModel model, String from, String to, String date) {
        model.setRowCount(0);
        try {
            Document query = new Document();
            if (!from.isEmpty()) query.append("origin", Pattern.compile(from, Pattern.CASE_INSENSITIVE));
            if (!to.isEmpty()) query.append("destination", Pattern.compile(to, Pattern.CASE_INSENSITIVE));
            if (!date.isEmpty()) query.append("departureDate", Pattern.compile(date, Pattern.CASE_INSENSITIVE));

            FindIterable<Document> flights = flightsCollection.find(query);
            for (Document flight : flights) {
                model.addRow(new Object[]{
                        flight.getString("flightNumber"),
                        flight.getString("origin"),
                        flight.getString("destination"),
                        flight.getString("departureDate"),
                        flight.getInteger("availableSeats"),
                        flight.getDouble("price")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Error searching flights: " + ex.getMessage());
        }
    }

    private void createSearchFlightsPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        formPanel.add(new JLabel("From:"));
        JTextField fromField = new JTextField();
        formPanel.add(fromField);

        formPanel.add(new JLabel("To:"));
        JTextField toField = new JTextField();
        formPanel.add(toField);

        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        JTextField dateField = new JTextField();
        formPanel.add(dateField);

        JButton searchButton = new JButton("Search");
        JButton backButton = new JButton("Back to Menu");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(searchButton);
        buttonPanel.add(backButton);
        formPanel.add(buttonPanel);

        String[] columns = {"Flight", "From", "To", "Date", "Available Seats", "Price"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable resultsTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(resultsTable);

        searchButton.addActionListener(e -> populateFlightsTable(model, fromField.getText(), toField.getText(), dateField.getText()));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        searchPanel.add(formPanel, BorderLayout.NORTH);
        searchPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(searchPanel, "search");
    }

    private void createBookingPanel() {
        JPanel bookingPanel = new JPanel(new BorderLayout(10, 10));
        bookingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));

        formPanel.add(new JLabel("Flight Number:"));
        JComboBox<String> flightComboBox = new JComboBox<>();
        populateFlightComboBox(flightComboBox);
        formPanel.add(flightComboBox);

        formPanel.add(new JLabel("Passenger Name:"));
        JTextField nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Passport Number:"));
        JTextField passportField = new JTextField();
        formPanel.add(passportField);

        formPanel.add(new JLabel("Contact Number:"));
        JTextField contactField = new JTextField();
        formPanel.add(contactField);

        JButton bookButton = new JButton("Book Flight");
        JButton backButton = new JButton("Back to Menu");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(bookButton);
        buttonPanel.add(backButton);
        formPanel.add(buttonPanel);

        bookButton.addActionListener(e -> {
            String flightNumber = (String) flightComboBox.getSelectedItem();
            String name = nameField.getText().trim();
            String passportNumber = passportField.getText().trim();
            String contact = contactField.getText().trim();

            if (name.isEmpty() || passportNumber.isEmpty() || contact.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Please fill out all fields.");
                return;
            }

            try {
                Document flight = flightsCollection.find(eq("flightNumber", flightNumber)).first();

                if (flight != null && flight.getInteger("availableSeats") > 0) {
                    Document passenger = new Document().append("passportNumber", passportNumber).append("name", name).append("contactNumber", contact);
                    passengersCollection.insertOne(passenger);

                    Document booking = new Document().append("flightNumber", flightNumber).append("passportNumber", passportNumber).append("bookingDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    bookingsCollection.insertOne(booking);

                    flightsCollection.updateOne(eq("flightNumber", flightNumber), inc("availableSeats", -1));

                    JOptionPane.showMessageDialog(mainFrame, "Flight booked successfully!");
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "No available seats for this flight.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(bookingPanel, "Error booking flight: " + ex.getMessage());
            }
        });

        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        bookingPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(bookingPanel, "book");
    }

    private void createViewBookingsPanel() {
        JPanel viewBookingsPanel = new JPanel(new BorderLayout(10, 10));
        viewBookingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.add(new JLabel("Passport Number:"));
        JTextField passportField = new JTextField();
        formPanel.add(passportField);

        JButton searchButton = new JButton("Search Bookings");
        JButton backButton = new JButton("Back to Menu");
        JButton showBoardingPassButton = new JButton("Show Boarding Pass");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(searchButton);
        buttonPanel.add(showBoardingPassButton);
        buttonPanel.add(backButton);
        formPanel.add(buttonPanel);

        String[] columns = {"Booking ID", "Flight Number", "Booking Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable bookingsTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(bookingsTable);

        searchButton.addActionListener(e -> {
            String passportNumber = passportField.getText().trim();
            if (passportNumber.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Please enter a passport number.");
                return;
            }
            populateBookingsTable(model, passportNumber);
        });

        showBoardingPassButton.addActionListener(e -> {
            int selectedRow = bookingsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select a booking to view the boarding pass.");
                return;
            }

            String bookingId = (String) bookingsTable.getValueAt(selectedRow, 0);
            showBoardingPassDialog(bookingId);
        });

        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        viewBookingsPanel.add(formPanel, BorderLayout.NORTH);
        viewBookingsPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(viewBookingsPanel, "viewBookings");
    }

    private void showBoardingPassDialog(String bookingId) {
        try {
            Document booking = bookingsCollection.find(eq("_id", new ObjectId(bookingId))).first();
            if (booking != null) {
                String passportNumber = booking.getString("passportNumber");
                Document passenger = passengersCollection.find(eq("passportNumber", passportNumber)).first();
                Document flight = flightsCollection.find(eq("flightNumber", booking.getString("flightNumber"))).first();

                if (passenger != null && flight != null) {
                    String boardingPassInfo = String.format(
                            "Boarding Pass\n\n" +
                                    "Passenger Name: %s\n" +
                                    "Passport Number: %s\n" +
                                    "Flight Number: %s\n" +
                                    "Origin: %s\n" +
                                    "Destination: %s\n" +
                                    "Departure Date: %s\n\n" +
                                    "Booking Date: %s",
                            passenger.getString("name"),
                            passportNumber,
                            flight.getString("flightNumber"),
                            flight.getString("origin"),
                            flight.getString("destination"),
                            flight.getString("departureDate"),
                            booking.getString("bookingDate")
                    );

                    JTextArea boardingPassText = new JTextArea(boardingPassInfo);
                    boardingPassText.setEditable(false);
                    boardingPassText.setFont(new Font("Monospaced", Font.PLAIN, 14));

                    JScrollPane scrollPane = new JScrollPane(boardingPassText);
                    scrollPane.setPreferredSize(new Dimension(400, 300));

                    JOptionPane.showMessageDialog(mainFrame, scrollPane, "Boarding Pass", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Error: Passenger or flight details not found.");
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Error: Booking details not found.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Error displaying boarding pass: " + ex.getMessage());
        }
    }

    private void populateBookingsTable(DefaultTableModel model, String passportNumber) {
        model.setRowCount(0);
        try {
            FindIterable<Document> bookings = bookingsCollection.find(eq("passportNumber", passportNumber));
            for (Document booking : bookings) {
                model.addRow(new Object[]{
                        booking.getObjectId("_id").toString(),
                        booking.getString("flightNumber"),
                        booking.getString("bookingDate")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Error retrieving bookings: " + ex.getMessage());
        }
    }

    private void populateFlightComboBox(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        FindIterable<Document> flights = flightsCollection.find();
        for (Document flight : flights) {
            comboBox.addItem(flight.getString("flightNumber"));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AirlineReservationSystem::new);
    }
}
