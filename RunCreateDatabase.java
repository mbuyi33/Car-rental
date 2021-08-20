public class RunCreateDatabase
{
    public static void main(String[] args)
    {
        CreateDatabase runCreateDatabase = new CreateDatabase();
        runCreateDatabase.connectToDatabase();
        runCreateDatabase.createTables();
        runCreateDatabase.loadDataToTables();
    }
}
