import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.ArrayList;

public class CreateDatabase
{
    private Connection con;
    private PreparedStatement prepstmt;

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

    public void createTables()
    {
        try
        {
            prepstmt = con.prepareStatement("CREATE TABLE Customer (custNumber AUTOINCREMENT NOT NULL PRIMARY KEY, firstName VARCHAR(30) NOT NULL, surName VARCHAR(30) NOT NULL, idNum VARCHAR(13) NOT NULL, phoneNum VARCHAR(10) NOT NULL, canRent Boolean NOT NULL)");
            prepstmt.executeUpdate();

            prepstmt = con.prepareStatement("CREATE TABLE Vehicle (vehNumber AUTOINCREMENT NOT NULL PRIMARY KEY, make VARCHAR(30) NOT NULL, category VARCHAR(10) NOT NULL, rentalPrice CURRENCY NOT NULL, availableForRent Boolean NOT NULL)");
            prepstmt.executeUpdate();

            prepstmt = con.prepareStatement("CREATE TABLE Rental (rentalNumber AUTOINCREMENT NOT NULL PRIMARY KEY, rentDate DATE NULL, returnedDate DATE NULL, pricePerDay CURRENCY NOT NULL, totalPrice CURRENCY NULL, custNumber INT NOT NULL, vehNumber INT NOT NULL)");
            prepstmt.executeUpdate();

            prepstmt = con.prepareStatement("ALTER TABLE Rental ADD FOREIGN KEY (custNumber) REFERENCES Customer (custNumber)");
            prepstmt.executeUpdate();

            prepstmt = con.prepareStatement("ALTER TABLE Rental ADD FOREIGN KEY (vehNumber) REFERENCES Vehicle (vehNumber)");
            prepstmt.executeUpdate();
        }
        catch(SQLException sqle)
        {
            System.out.println(sqle);
        }


    }

    public void loadDataToTables()
    {
        ArrayList<Customer> customers = new ArrayList<>();

        try
        {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("Customers.ser"));

            while (true)
            {
                customers.add((Customer) objectInputStream.readObject());
            }
        }
        catch (EOFException e)
        {

        }
        catch (IOException | ClassNotFoundException e)
        {
            System.out.println(e);
        }

        ArrayList<Vehicle> vehicles = new ArrayList<>();

        try
        {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("Vehicles.ser"));

            while (true)
            {
                vehicles.add((Vehicle) objectInputStream.readObject());
            }
        }
        catch (EOFException e)
        {

        }
        catch (IOException | ClassNotFoundException e)
        {
            System.out.println(e);
        }

        try
        {
            for (Customer customer : customers)
            {
                prepstmt = con.prepareStatement("INSERT INTO Customer(firstName, surName, idNUm, phoneNum, CanRent) VALUES (?,?,?,?,?)");
                prepstmt.setString(1, customer.getName());
                prepstmt.setString(2, customer.getSurname());
                prepstmt.setString(3, customer.getIdNum());
                prepstmt.setString(4, customer.getPhoneNum());
                prepstmt.setBoolean(5, customer.canRent());
                prepstmt.executeUpdate();
            }

            System.out.println(customers.size() + " customers added." );

            for (Vehicle vehicle : vehicles)
            {
                prepstmt = con.prepareStatement("INSERT INTO Vehicle(make, category, rentalPrice, availableForRent) VALUES (?,?,?,?)");
                prepstmt.setString(1, vehicle.getMake());
                prepstmt.setString(2, vehicle.getCategory());
                prepstmt.setDouble(3, vehicle.getRentalPrice());
                prepstmt.setBoolean(4, vehicle.isAvailableForRent());
                prepstmt.executeUpdate();
            }

            System.out.println(vehicles.size() + " vehicles added." );
        }
        catch (SQLException e)
        {
            System.out.println(e);
        }
    }
}
