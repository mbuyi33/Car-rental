import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server
{
    ServerSocket listener;
    Socket client;

    ObjectInputStream in;
    ObjectOutputStream out;

    Connection con;
    PreparedStatement prepstmt;
    ResultSet rs;

    String msg = "";

    public Server()
    {
        System.out.println("running server...");

        connectToDatabase();
        startServer();
        listen();
        createStreams();
        processClient();
    }

    public void startServer()
    {
        try
        {
            listener = new ServerSocket(4000,1);
        }
        catch (IOException ie)
        {
            System.out.println(ie.getMessage());
        }
    }

    public void listen()
    {
        try
        {
            client = listener.accept();
        }
        catch (IOException ie)
        {
            System.out.println(ie.getMessage());
        }
    }

    public void createStreams()
    {
        try
        {
            out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            in = new ObjectInputStream(client.getInputStream());
        }
        catch (IOException ie)
        {
            System.out.println(ie.getMessage());
        }
    }

    public void connectToDatabase()
    {
        try
        {
            con = DriverManager.getConnection("jdbc:ucanaccess://database.accdb");
        }
        catch (SQLException se)
        {
            System.out.println(se.getMessage());
        }
    }

    public void processClient()
    {
        try
        {
            do
            {
                msg = (String)in.readObject();

                if(msg.equalsIgnoreCase("getCustomers"))
                {
                    ArrayList<Customer> customers = new ArrayList<>();

                    prepstmt = con.prepareStatement("SELECT * FROM Customer");
                    rs = prepstmt.executeQuery();

                    while (rs.next())
                    {
                        customers.add(new Customer(rs.getInt("custNumber"), rs.getString("firstName"), rs.getString("surName"), rs.getString("idNum"), rs.getString("phoneNum"), rs.getBoolean("canRent")));
                    }

                    out.writeObject(customers);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("getVehicles"))
                {
                    ArrayList<Vehicle> vehicles = new ArrayList<>();

                    prepstmt = con.prepareStatement("SELECT * FROM Vehicle");
                    rs = prepstmt.executeQuery();

                    while (rs.next())
                    {
                        int cat = 1;

                        if(rs.getString("category").equalsIgnoreCase("SUV"))
                        {
                            cat = 2;
                        }

                        vehicles.add(new Vehicle(rs.getInt("vehNumber"), rs.getString("make"), cat, rs.getDouble("rentalPrice"), rs.getBoolean("availableForRent")));
                    }

                    out.writeObject(vehicles);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("getRentals"))
                {
                    ArrayList<Rental> rentals = new ArrayList<>();

                    prepstmt = con.prepareStatement("SELECT * FROM Rental");
                    rs = prepstmt.executeQuery();

                    while (rs.next())
                    {
                        rentals.add(new Rental(rs.getInt("rentalNumber"), rs.getDate("rentDate"), rs.getDate("returnedDate"), rs.getDouble("pricePerDay"), rs.getInt("custNumber"), rs.getInt("vehNumber")));
                    }

                    out.writeObject(rentals);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("addCustomer"))
                {
                    Customer customer = (Customer) in.readObject();

                    prepstmt = con.prepareStatement("INSERT INTO Customer (firstName, surName, idNum, phoneNum, canRent) VALUES (?,?,?,?,?)");

                    prepstmt.setString(1,customer.getName());
                    prepstmt.setString(2,customer.getSurname());
                    prepstmt.setString(3,customer.getIdNum());
                    prepstmt.setString(4,customer.getPhoneNum());
                    prepstmt.setBoolean(5,customer.canRent());

                    int res = prepstmt.executeUpdate();

                    out.writeBoolean(res == 1);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("addVehicle"))
                {
                    Vehicle vehicle = (Vehicle) in.readObject();

                    prepstmt = con.prepareStatement("INSERT INTO Vehicle (make, category, rentalPrice, availableForRent) VALUES (?,?,?,?)");

                    prepstmt.setString(1,vehicle.getMake());
                    prepstmt.setString(2,vehicle.getCategory());
                    prepstmt.setDouble(3,vehicle.getRentalPrice());
                    prepstmt.setBoolean(4,vehicle.isAvailableForRent());

                    int res = prepstmt.executeUpdate();

                    out.writeBoolean(res == 1);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("idNumberExist"))
                {
                    String idNumber = (String)in.readObject();

                    prepstmt = con.prepareStatement("SELECT * FROM Customer WHERE idNum = ?");
                    prepstmt.setString(1, idNumber);
                    rs = prepstmt.executeQuery();

                    out.writeBoolean(rs.next());
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("phoneNumberExist"))
                {
                    String phoneNumber = (String)in.readObject();

                    prepstmt = con.prepareStatement("SELECT * FROM Customer WHERE phoneNum = ?");
                    prepstmt.setString(1, phoneNumber);
                    rs = prepstmt.executeQuery();

                    out.writeBoolean(rs.next());
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("makeExist"))
                {
                    String make = (String)in.readObject();

                    prepstmt = con.prepareStatement("SELECT * FROM Vehicle WHERE make = ?");
                    prepstmt.setString(1, make);
                    rs = prepstmt.executeQuery();

                    out.writeBoolean(rs.next());
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("rent"))
                {
                    Rental rental = (Rental)in.readObject();

                    prepstmt = con.prepareStatement("INSERT INTO Rental (rentDate, returnedDate, pricePerDay, totalPrice, custNumber, vehNumber) VALUES (?,?,?,?,?,?)");

                    prepstmt.setDate(1, rental.getDateRented());
                    prepstmt.setDate(2, rental.getDateReturned());
                    prepstmt.setDouble(3, rental.getPricePerDay());
                    prepstmt.setDouble(4, rental.getTotalRental());
                    prepstmt.setInt(5, rental.getCustNumber());
                    prepstmt.setInt(6, rental.getVehNumber());

                    int res = prepstmt.executeUpdate();

                    if(res != 1)
                    {
                        out.writeBoolean(false);
                        out.flush();
                    }

                    prepstmt = con.prepareStatement("UPDATE Customer SET canRent = false WHERE custNumber = ?");
                    prepstmt.setInt(1, rental.getCustNumber());
                    prepstmt.executeUpdate();

                    prepstmt = con.prepareStatement("UPDATE Vehicle SET availableForRent = false WHERE vehNumber = ?");
                    prepstmt.setInt(1, rental.getVehNumber());
                    res = prepstmt.executeUpdate();

                    out.writeBoolean(res == 1);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("return"))
                {
                    Rental rental = (Rental)in.readObject();

                    prepstmt = con.prepareStatement("UPDATE Rental SET returnedDate = ?, totalPrice = ? WHERE rentalNumber = ?");

                    prepstmt.setDate(1, rental.getDateReturned());
                    prepstmt.setDouble(2, rental.getTotalRental());
                    prepstmt.setInt(3, rental.getRentalNumber());

                    int res = prepstmt.executeUpdate();

                    if(res != 1)
                    {
                        out.writeBoolean(false);
                        out.flush();
                    }

                    prepstmt = con.prepareStatement("UPDATE Customer SET canRent = true WHERE custNumber = ?");
                    prepstmt.setInt(1, rental.getCustNumber());
                    prepstmt.executeUpdate();

                    prepstmt = con.prepareStatement("UPDATE Vehicle SET availableForRent = true WHERE vehNumber = ?");
                    prepstmt.setInt(1, rental.getVehNumber());
                    res = prepstmt.executeUpdate();

                    out.writeBoolean(res == 1);
                    out.flush();
                }
                else if(msg.equalsIgnoreCase("getRentalDates"))
                {
                    ArrayList<String> rentalDates = new ArrayList<>();

                    prepstmt = con.prepareStatement("SELECT rentDate FROM Rental Group By rentDate ORDER BY rentDate");
                    rs = prepstmt.executeQuery();

                    while (rs.next())
                    {
                        rentalDates.add(new SimpleDateFormat("yyyy/MM/dd").format(new Date(rs.getDate("rentDate").getTime())));
                    }

                    out.writeObject(rentalDates);
                    out.flush();
                }

            }while (!msg.equalsIgnoreCase("terminate"));
        }
        catch (SQLException | IOException | ClassNotFoundException e)
        {
            System.out.println(e);
        }
    }

    public static void main(String[] args)
    {
        new Server();
    }
}
