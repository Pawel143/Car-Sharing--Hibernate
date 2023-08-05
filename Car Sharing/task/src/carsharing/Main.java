package carsharing;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {


    private static int cho;

    private static String formatPath(String[] args) {
        // DeleteDbFiles.execute("dir", "db", true);
        StringBuilder db = new StringBuilder("src/carsharing/db/");
        boolean found = !Files.exists(Path.of(db + ".mv.db"));

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-databaseFileName")) {
                if (i + 1 < args.length) {
                    db.append(args[i + 1]);
                    found = true;
                }
            }
        }
        if (!found) {
            db.append("anyPath");
        }
        return db.toString();
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        String dbPath = formatPath(args);

        File file = new File(dbPath + ".mv.db");

        boolean fileCreated = true;
        try {
            fileCreated = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Class.forName("org.h2.Driver");
        Connection con = DriverManager.getConnection("jdbc:h2:./" + dbPath);
        con.setAutoCommit(true);
        Statement statement = con.createStatement();


        statement.execute("CREATE TABLE IF NOT EXISTS COMPANY (ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR UNIQUE NOT NULL)");
        statement.execute("CREATE TABLE IF NOT EXISTS CAR (ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR UNIQUE NOT NULL, COMPANY_ID INT NOT NULL REFERENCES COMPANY(ID))");
        statement.execute("CREATE TABLE IF NOT EXISTS CUSTOMER (ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR UNIQUE NOT NULL, RENTED_CAR_ID INT NULL, FOREIGN KEY (RENTED_CAR_ID) REFERENCES CAR(ID))");

        statement.close();
        menuImp(con, start());


    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private static void carsList(Connection con, int COMPANY_ID) throws SQLException, ClassNotFoundException {
        Scanner sc = new Scanner(System.in);
        Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);


        PreparedStatement result = con.prepareStatement("SELECT NAME FROM COMPANY WHERE ID = ?");
        result.setInt(1, COMPANY_ID);
        result.setInt(1, COMPANY_ID);

        ResultSet res = result.executeQuery();
        if (COMPANY_ID == 0) {
            menuImp(con, 1);
        }

        if (res.next()) {
            String n = res.getString("NAME");
            System.out.println(n + " company");
            System.out.println("""
                    1. Car list
                    2. Create a car
                    0. Back""");

            int in = sc.nextInt();
            switch (in) {
                case 1: {
                    Statement statement1 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    ResultSet resultSet = statement1.executeQuery("SELECT ID, NAME FROM CAR  WHERE COMPANY_ID = " + COMPANY_ID + " ORDER BY ID");

                    int id = 0;

                    if (resultSet.next()) {
                        System.out.println("\nCar List:");
                        resultSet.beforeFirst();
                        while (resultSet.next()) {
                            id++;
                            String name = resultSet.getString("NAME");
                            System.out.println((id) + ". " + name);

                        }
                        System.out.println("\n");
                        resultSet.close();
                        carsList(con, COMPANY_ID);


                    } else {
                        System.out.println("The car list is empty!");
                        carsList(con, COMPANY_ID);
                    }


                    break;
                }
                case 2: {
                    System.out.println("Enter the car name:");
                    sc.nextLine();
                    String carName = sc.nextLine();

                    if (isNumeric(carName)) {
                        System.out.println("The car list is empty!");
                        carsList(con, COMPANY_ID);
                    }

                    con.setAutoCommit(true);
                    String insertQuery = "INSERT INTO CAR (NAME, COMPANY_ID) VALUES (?, ?)";
                    PreparedStatement preparedStatement = con.prepareStatement(insertQuery);
                    preparedStatement.setString(1, carName);
                    preparedStatement.setInt(2, COMPANY_ID);
                    preparedStatement.executeUpdate();

                    preparedStatement.close();


                    System.out.println("The car was added!");

                    carsList(con, COMPANY_ID);
                    break;
                }
                case 0: {
                    menuImp(con, 1);
                }
            }
        } else {
            System.out.println("No company found.");
        }

        statement.close();

    }


    private static void printCompanyList(Connection con) throws SQLException, ClassNotFoundException {
        Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery("SELECT ID, NAME FROM COMPANY ORDER BY ID");
        Scanner sc = new Scanner(System.in);

        if (resultSet.next()) {
            System.out.println("Choose a company:");
            resultSet.beforeFirst();
            while (resultSet.next()) {
                int id = resultSet.getInt("ID");
                String name = resultSet.getString("NAME");
                System.out.println(id + ". " + name);
            }
            System.out.println("0. Back");
            int choice = sc.nextInt();
            if (choice > 0) {
                carsList(con, choice);
            } else {
                menuImp(con, 1);
            }
        } else {
            System.out.println("The company list is empty");
            menuImp(con, 1);
        }

        statement.close();

    }

    private static void rentedCarList(Connection con, int CUSTOMER_ID) throws SQLException, ClassNotFoundException {


        Statement statement1 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        ResultSet cusName = statement1.executeQuery("SELECT NAME FROM CUSTOMER WHERE ID = " + cho);
        String re;
        if (cusName.next()) {
            re = cusName.getString("NAME");
        } else {
            re = "wrong";
            System.out.println("You didn't rent a car! ");
        }


        Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet resikm = statement.executeQuery("SELECT RENTED_CAR_ID FROM CUSTOMER  WHERE NAME = '" + re + "'");

        int rentedCarId;

        if (resikm.next()) {
            if (!resikm.wasNull()) {
                rentedCarId = resikm.getInt("RENTED_CAR_ID");
            } else {
                System.out.println("You didn't rent a car! \n");
                printCustomerList(con, CUSTOMER_ID, 2);
                return;
            }
        } else {
            System.out.println("You didn't rent a car! \n");
            printCustomerList(con, CUSTOMER_ID, 2);
            return;
        }


        ResultSet resultSet = statement1.executeQuery("SELECT NAME FROM CAR WHERE ID = '" + rentedCarId + "'");
        if (!resultSet.next() || resultSet.wasNull()) {
            System.out.println("You didn't rent a car! \n");
            printCustomerList(con, CUSTOMER_ID, 2);
            return;

        }


        String carName = resultSet.getString("NAME");
        ResultSet statemen = statement1.executeQuery("SELECT NAME FROM COMPANY WHERE ID = (SELECT COMPANY_ID FROM CAR WHERE NAME = '" + carName + "')");
        String companyName;
        if (statemen.next()) {
            companyName = statemen.getString("NAME");

        } else {

            System.out.println("You didn't rent a car! \n");
            printCustomerList(con, CUSTOMER_ID, 2);
            return;
        }

        System.out.println("Your rented car:");


        System.out.println(carName);

        System.out.println("Company:");


        System.out.println(companyName + "\n");


        resultSet.close();
        statemen.close();
        statement1.close();
        printCustomerList(con, CUSTOMER_ID, 2);


        System.out.println("You didn't rent a car! \n");
        printCustomerList(con, CUSTOMER_ID, 2);
    }


    private static void printCustomerList(Connection con, int CUSTOMER_ID, int option) throws SQLException, ClassNotFoundException {
        Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ResultSet resultSet = statement.executeQuery("SELECT ID, NAME FROM CUSTOMER ORDER BY ID");
        Scanner sc = new Scanner(System.in);
        int choice = 0;
        if (resultSet.next()) {
            switch (option) {
                case 1: {

                    System.out.println("Customer list:");
                    resultSet.beforeFirst();
                    while (resultSet.next()) {
                        int id = resultSet.getInt("ID");
                        String name = resultSet.getString("NAME");
                        System.out.println(id + ". " + name);
                    }
                    System.out.println("0. Back");
                    System.out.println("choooo");
                    //customer ID
                    cho = sc.nextInt();
                }
                case 2: {
                    System.out.println("""
                            1. Rent a car
                            2. Return a rented car
                            3. My rented car
                            0. Back""");


                    int op = sc.nextInt();
                    ArrayList<Integer> objects = new ArrayList<>();
                    switch (op) {
                        case 1: {
                            PreparedStatement sadada = con.prepareStatement("SELECT RENTED_CAR_ID FROM CUSTOMER WHERE ID = ?");
                            sadada.setInt(1, cho);
                            ResultSet a = statement.executeQuery("SELECT RENTED_CAR_ID FROM CUSTOMER WHERE ID = " + cho);

                            System.out.println();

                            System.out.println();

                            if (a.next()) {
                                if (a.getString("RENTED_CAR_ID") == null || a.getString("RENTED_CAR_ID").equals("0")) {
                                    // System.out.println("Wartość jest NULL");
                                    ResultSet resu = statement.executeQuery("SELECT ID, NAME FROM COMPANY ORDER BY ID");

                                    System.out.println("Company:");
                                    while (resu.next()) {
                                        int id = resu.getInt("ID");
                                        String name = resu.getString("NAME");
                                        System.out.println(id + ". " + name);
                                    }
                                    System.out.println("0. Back");

                                    choice = sc.nextInt();


                                    if (choice > 0) {
                                        Statement statement1 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                        Statement statement2 = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                        ResultSet RENTED_CAR_ID = statement2.executeQuery("SELECT NAME FROM CAR WHERE ID IN (SELECT RENTED_CAR_ID FROM CUSTOMER WHERE RENTED_CAR_ID > 0)");
                                        ResultSet tSet = statement1.executeQuery("SELECT ID, NAME FROM CAR  WHERE COMPANY_ID = " + choice + " ORDER BY ID");

                                        ArrayList<String> objects1 = new ArrayList<>();
                                        while (RENTED_CAR_ID.next()) {
                                            objects1.add(RENTED_CAR_ID.getString("NAME"));
                                        }

                                        int id = 0;
                                        ArrayList<String> strings = new ArrayList<>();
                                //        tSet.beforeFirst();
                                        if (tSet.next()) {

                                            System.out.println("\nChoose a car:");
                                            tSet.beforeFirst();
                                            String name;
                                            while (tSet.next()) {
                                                name = tSet.getString("NAME");
                                                if (!(objects1.contains(name))) {
                                                    id++;
                                                    strings.add(name);
                                                    System.out.println((id) + ". " + strings.get(id-1));
                                                }
                                            }
                                            System.out.println("0. Back");
                                            int rented = sc.nextInt();
                                            if(rented == 0){
                                                printCustomerList(con, CUSTOMER_ID, 2);
                                                break;
                                            }

                                            System.out.println("\nYou rented '" + strings.get(rented-1) + "'");


                                            ResultSet aa = statement1.executeQuery("SELECT ID FROM CAR WHERE NAME = '" + strings.get(rented - 1) + "'");

                                            final PreparedStatement st = con.prepareStatement("UPDATE CUSTOMER SET RENTED_CAR_ID = ? WHERE ID = ?");


                                            if (aa.next()) {

                                                int carId = aa.getInt(1);
                                                st.setInt(1, carId);
                                                st.setInt(2, cho);


                                                st.executeUpdate();


                                            }


                                            resultSet.close();

                                            printCustomerList(con, choice, 2);
                                        } else {
                                            System.out.println("The car list is empty!");

                                        }

                                    } else {
                                        menuImp(con, 1);
                                    }


                                    statement.close();
                                    resultSet.close();


                                    printCompanyList(con);
                                } else {
                                    System.out.println("You've already rented a car!");
                                    printCustomerList(con, CUSTOMER_ID, 2);
                                }
                                break;
                            }
                        }
                        case 2: {
                            //There now  UPDATE CUSTOMER SET RENTED_CAR_ID = ? WHERE ID = ?
                            ResultSet ress = statement.executeQuery("SELECT RENTED_CAR_ID FROM CUSTOMER WHERE ID = " + cho);
                            String str = "";
                            if (ress.next()) {
                                str = ress.getString("RENTED_CAR_ID");
                            } else {
                                str = "0";
                            }

                            if (!(str == (null)) && !(str.equals("0"))) {
                                final PreparedStatement sta = con.prepareStatement("UPDATE CUSTOMER SET RENTED_CAR_ID = ? WHERE ID = ?");
                                sta.setString(1, null);
                                sta.setInt(2, cho);
                                sta.executeUpdate();
                                System.out.println("You've returned a rented car!\n");
                            } else if (str == null) {
                                System.out.println("You didn't rent a car!\n");
                            }


                            break;
                        }
                        case 3: {
                            rentedCarList(con, CUSTOMER_ID);
                            break;
                        }
                        case 0: {
                            menuImp(con, start());
                        }
                    }


                }

                statement.close();
                resultSet.close();


            }
        } else {
            System.out.println("The customer list is empty!\n");

            menuImp(con, start());
        }
    }


    public static int start() {
        Scanner sc = new Scanner(System.in);
        System.out.println("1. Log in as a manager");
        System.out.println("2. Log in as a customer");
        System.out.println("3. Create a customer");
        System.out.println("0. Exit");
        return sc.nextInt();
    }

    private static void menuImp(Connection con, int a) throws ClassNotFoundException, SQLException {

        Scanner sc = new Scanner(System.in);

        switch (a) {
            case 1: {
                System.out.println("""
                        1. Company list
                        2. Create a company
                        0. Back""");
                switch (sc.nextInt()) {
                    case 1: {
                        printCompanyList(con);
                        break;

                    }
                    case 2: {

                        System.out.println("Enter the company name:");
                        sc.nextLine();
                        String comName = sc.nextLine();


                        //con.setAutoCommit(true);
//                        Statement statement = con.createStatement();
                        String insertQuery = "INSERT INTO COMPANY (NAME) VALUES (?)";

                        PreparedStatement preparedStatement = con.prepareStatement(insertQuery);
                        preparedStatement.setString(1, comName); // Ustawienie wartości dla parametru '?' w zapytaniu
                        preparedStatement.executeUpdate();
                        System.out.println("The company was created!");
                        con.setAutoCommit(true);
                        menuImp(con, 1);
                        preparedStatement.close();
                        break;
                    }


                    case 0: {
                        menuImp(con, start());
                    }

                }
                break;
            }
            case 2: {
                printCustomerList(con, a, 1);
                break;
            }
            case 3: {

                System.out.println("Enter the customer name:");

                String cusName = sc.nextLine();

                String insertQuery = "INSERT INTO CUSTOMER (NAME) VALUES (?)";

                PreparedStatement preparedStatement = con.prepareStatement(insertQuery);
                preparedStatement.setString(1, cusName); // Ustawienie wartości dla parametru '?' w zapytaniu
                preparedStatement.executeUpdate();
                System.out.println("The customer was created!");
                con.setAutoCommit(true);
                menuImp(con, start());
                preparedStatement.close();
                break;
            }
            case 0:

        }

    }
}