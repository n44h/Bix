package IvoryDatabase;

import java.io.*;

import java.util.ArrayList;

import IvoryDatabase.IvoryDBExceptions.*;

public class IvoryDatabase implements AutoCloseable, Serializable, Cloneable{
    /**
     * Each Attribute object in attributes[] acts like a Column in the Ivory Database.
     * Each Attribute object holds the column values for an Attribute in an Object Array.
     * Hence, an Ivory Database may have several Attributes associated to it and these
     * Attribute.java objects are kept track of using an Array of Attributes in the 
     * IvoryDatabase.java class: 'Attribute[] attributes'
     */
    private ArrayList<Column> COLUMNS; // ArrayList of all Column objects of this Ivory Database.
    private int no_of_columns; // keeps track of the total number of columns in the Database.
    private int no_of_rows; // keeps track of the total number of rows in the Database.

    private transient File FILE_LOCATION = null; // the file where the Ivory Database is stored and saved to.

    private static final String file_extension = ".ivry"; // the file extension of the IvoryDB file.
    private static final String file_separator = File.separator; // the file separator of the System.


    /** 
     * rows 
     *        total number of rows in the database. "Rows" are also called "Entries".
     * 
     * columns
     *        total number of columns in the database. "Columns" are also called 
     *        "Attributes" and they are implemented using Attribute objects.
     *              
     *        This is done because Java does not natively support multidimensional 
     *        arrays of different Types. Hence the Attribute.java object is 
     *        utilized to realize 'columns' in the Ivory Database and the
     *        IvoryDatabase.java class contains the class variable attributes 
     *        which is an Array of Attribute.java objects. 
     *           
     *        Hence, we are able to create a 2 Dimensional data structure that is
     *        capable of storing data of different data types in each column.
     */


    /*** 
        *** Constructors and their helper methods ***
    ***/

    /** 
     * creating a new Ivory Database object.
     */
    public IvoryDatabase() {
        this.no_of_rows = 0;
        this.no_of_columns = 0;
        COLUMNS = new ArrayList<>();

        // creating default Attribute "ID" that every Ivory Database must contain.
        this.ADD_COLUMN("ID");
    } // constructor IvoryDatabase()


    /**
     * creating a new Ivory Database file at {@code file_path} 
     * from another Ivory Database object. 
     * 
     * @param ivoryDBObject
     *        IvoryDatabase object from which attributes are being copied.
     */
    public IvoryDatabase(IvoryDatabase ivoryDBObject) {
        // copying parameter's object variables to this object's variables.
        this.COLUMNS = ivoryDBObject.COLUMNS;
        this.no_of_rows = ivoryDBObject.no_of_rows;
        this.no_of_columns = ivoryDBObject.no_of_columns;
    } // constructor IvoryDatabase(IvoryDatabase Object)


    /** 
     * opening an existing IvoryDatabase from file at {@code file_path}.
     * 
     * @param file_path
     *        the file path in the system where an existing Ivory Database
     *        file (.ivry) is stored.
     * 
     * @throws IllegalFileTypeException
     *         When the file type is not an Ivory Database.
     * 
     * @throws FileNotFoundException
     *         When the file at {@code file_path} does not exist.
     */
    public IvoryDatabase(String file_path) 
        throws IllegalFileTypeException, FileNotFoundException {
        // validating file_path.
        if(file_path == null){
            throw new IllegalArgumentException("File path cannot be null.");
        }
        // checking if the file type is correct.
        else if (!file_path.endsWith(file_extension)){
            throw new IllegalFileTypeException();
        }

        // assigning File object of file_path to FILE_LOCATION.
        FILE_LOCATION = new File(file_path);
        if(!FILE_LOCATION.exists()){
            throw new FileNotFoundException();
        }

        // deserializing the file.
        try {
            FileInputStream fileIn = new FileInputStream(FILE_LOCATION);
            ObjectInputStream streamIn = new ObjectInputStream(fileIn);

            // deserializing and casting.
            IvoryDatabase deserializedDB = (IvoryDatabase) streamIn.readObject();
            this.COLUMNS = deserializedDB.COLUMNS;

            // assigning the size of the ID column to no_of_rows.
            this.no_of_rows = COLUMNS.get(0).getSize();
            // assigning the size of the COLUMNS array list to no_of_columns.
            this.no_of_columns = COLUMNS.size();
            

            // closing streams.
            streamIn.close();
            fileIn.close();

            // deleting deserializedDB object
            deserializedDB = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    } // constructor IvoryDatabase(String)


    /** 
     * opening an existing Ivory Database from {@code ivory_file}.
     * 
     * @param ivory_file
     *        the file where an existing Ivory Database file (.ivry) is stored.
     * 
     * @throws IllegalFileTypeException
     *         When the file type is not an Ivory Database.
     * 
     * @throws FileNotFoundException
     *         When the file does not exist.
     */
    public IvoryDatabase(File ivory_file) 
        throws IllegalFileTypeException, FileNotFoundException {
        
        // validating ivory_file.
        if(ivory_file == null){
            throw new IllegalArgumentException("Ivory Database File cannot be null.");
        }
        // checking if the file type is correct.
        else if (!ivory_file.getName().endsWith(file_extension)){
            throw new IllegalFileTypeException();
        }
        
        // assigning ivory_file to FILE_LOCATION.
        FILE_LOCATION = ivory_file;
        if(!FILE_LOCATION.exists()){
            throw new FileNotFoundException();
        }

        // deserializing the file.
        try {
            FileInputStream fileIn = new FileInputStream(FILE_LOCATION);
            ObjectInputStream streamIn = new ObjectInputStream(fileIn);

            // deserializing and casting.
            IvoryDatabase deserializedDB = (IvoryDatabase) streamIn.readObject();
            this.COLUMNS = deserializedDB.COLUMNS;

            // assigning the size of the ID column to no_of_rows.
            this.no_of_rows = COLUMNS.get(0).getSize();
            // assigning the size of the COLUMNS array list to no_of_columns.
            this.no_of_columns = COLUMNS.size();

            // closing streams.
            streamIn.close();
            fileIn.close();

            // deleting deserializedDB object
            deserializedDB = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    } // constructor IvoryDatabase(File)


    /**
     * method to set the Ivory Database save location
     * 
     * @throws IllegalArgumentException
     *         when input_file is null.
     * 
     * @throws DirectoryNotFoundException
     *         when parent directory of input_file does not exist.
     * 
     * @throws IllegalFileTypeException
     *         when file extension is incorrect, i.e. file extension is not ".ivry".
     */
    public void setFileLocation(String file_path) 
        throws DirectoryNotFoundException, IllegalFileTypeException{
        // validating file_path
        if(file_path == null){
            throw new IllegalArgumentException();
        }

        // create a File object of the file path.
        File file = new File(file_path);

        // checking if parent directory exists.
        if (!file.getParentFile().exists()){
            throw new DirectoryNotFoundException(file.getParent());
        }
        // checking that the file is the correct file type (.ivry).
        else if (!file.getName().endsWith(file_extension)){
            throw new IllegalFileTypeException();
        }
        // checking if a file with the same file path already exists.
        else if (file.exists()) {
            // if true, rectify the filename collision.
            FILE_LOCATION = rectifyFileNameCollision(file);
        }
        // if there are no issues, straight away set the FILE_LOCATION.
        else {
            // set FILE_LOCATION.
            FILE_LOCATION = file;
        }
    } // setFileLocation(String)

    /**
     * Method to set the file location to a default file location.
     * 
     * The default file is placed inside the default directory "Local Ivory Databases"
     * 
     * If the "Local Ivory Databases" directory does not exist, it is created.
     */
    private void setDefaultFileLocation(){
        // the default name for the save file.
        String defaultFilename = "unnamed_ivory_database.ivry";

        /** CREATING Local Ivory Databases DIRECTORY, IF IT DOESN'T ALREADY EXIST **/
        
        // getting the Path string of the current working directory.
        String workingDirectory = System.getProperty("user.dir");

        // creating File object for "Local Ivory Databases" to check if it already exists.
        String defaultDirectoryPath = workingDirectory + file_separator + "Local Ivory Databases";
        File defaultDirectory = new File(defaultDirectoryPath);

        // if the default directory "Local Ivory Databases" does not already exist: create the directory.
        if(!defaultDirectory.exists()){
            if(defaultDirectory.mkdir()) // creating the "Local Ivory Databases" directory.
                System.out.println("\n\"Local Ivory Databases\" directory successfully created in working directory: " + workingDirectory + "\n");
            else 
                System.out.println("\n\"Local Ivory Databases\" directory creation failed.\n"); 
        }

        /** CREATING THE DEFAULT FILE **/

        // creating a default .ivry file in the "Local Ivory Databases" directory.
        File defaultFile = new File(defaultDirectoryPath + file_separator + defaultFilename);
        
        // check if a file with that name already exists in the same directory.
        if (defaultFile.exists()){
            // if true, rectify the file name.
            FILE_LOCATION = rectifyFileNameCollision(defaultFile);
        }
        else{
            // assigning the default file to FILE_LOCATION.
            FILE_LOCATION = defaultFile;
        }

    } // setDefaultFileLocation()

    /**
     * Method to rectify a filename collision when creating a new Ivory Database file. 
     * 
     * If a file with the same filename passed to the constructor already exists 
     * in that directory, the filename needs to be rectified to avoid a file collision.
     * 
     * This is done because a file collision would lead to the existing file being 
     * overwritten, which can lead to unintended consequences and is bad practice.
     * 
     * This method generates an alternate filename by appending a number to the given
     * filename. A File object is created with the rectified file path and returned.
     * 
     * @param input_file
     *        the path to the location where the 
     * 
     * @return The File object with the rectified file name.
     */
    private File rectifyFileNameCollision(File input_file){
        // getting the filename without the file extension.
        String database_name = input_file.getName().substring(0, input_file.getName().indexOf(file_extension));
        // creating partial file path with the directory and filename without file extension.
        String partial_path = input_file.getParent() + file_separator + database_name;

        // rectifying the filename.
        int number = 1; // database number concatenated to the end of the filename.
        String full_path; // stores the complete path string.
        do{
            // creating the filename by appending partial_path, number and file_extension.
            full_path = partial_path + "(" + number + ")" + file_extension;
            number++; // incrementing by 1
        }while(new File(full_path).exists()); // if the file exists, run loop again.

        // return the file path with the rectified filename.
        return new File(full_path);
    } // rectifyFileNameCollision()


    /*** 
        *** Object Attribute and related methods ***
    ***/

    
    /**
     * @return The name of the Ivory Database.
     */
    public String getName(){
        // if FILE_LOCATION has not already been set, set the default file location.
        if (FILE_LOCATION == null){
            setDefaultFileLocation();
        }
        return FILE_LOCATION.getName();
    } // getName()
    

    /**
     * @return The file path of the file where the Ivory Database is saved.
     */
    public String getPath(){
        // if FILE_LOCATION has not already been set, set the default file location.
        if (FILE_LOCATION == null){
            setDefaultFileLocation();
        }
        return FILE_LOCATION.getPath();
    } // getPath()


    /**
     * @return The path to the parent directory of Ivory Database file.
     */
    public String getParentPath(){
        // if FILE_LOCATION has not already been set, set the default file location.
        if (FILE_LOCATION == null){
            setDefaultFileLocation();
        }
        return FILE_LOCATION.getParent();
    } // getParentPath()


    /**
     * @return The names of all the Columns in the Ivory Database.
     */
    public String[] getColumnNames(){
        // getting the number of columns in this database.
        int size = COLUMNS.size();

        // creating output String[].
        String output[] = new String[size];
        
        // running a loop through columns ArrayList.
        for(int index = 0 ; index < size ; index++){
            output[index] = COLUMNS.get(index).getName();
        }
        return output;
    } // getColumnNames()


    /**
     * Method to rename the Ivory Database.
     * 
     * First, the method checks if the new filename contains any File Separators.
     * Presence of File Separators in the new filename will cause a change of the 
     * file directory, which would effectively move the file to a different directory.
     * 
     * To prevent this, ensure the new filename does not contain File Separators using
     * {@code new_filename.indexOf(file_separator)}, which returns -1 if no File 
     * Separators are found in the new filename.
     * 
     * Then a new File object is created with the new filename and the parent path of 
     * the current {@code FILE_LOCATION}.
     * 
     * Check if the new File object already exists and points to a file. This is done
     * to prevent accidentally overwriting any existing files.
     * 
     * Finally, File.renameTo(File) is invoked to rename the file.
     * 
     * @param new_filename
     *        the new filename that the current .ivry file has to be renamed to.
     * 
     * @return {@code true}, if file rename operation was successful. Otherwise, returns {@code false}.
     */
    public boolean renameDatabaseTo(String new_filename){
        // if FILE_LOCATION has not already been set, set the default file location.
        if (FILE_LOCATION == null){
            setDefaultFileLocation();
        }

        // checking if new_filename does not contain the file extension ".ivry".
        if (!new_filename.endsWith(file_extension)){
            // if ".ivry" does not exist in the new_filename, check if any other file extension exists.
            if (new_filename.indexOf(".") == -1) {
                // if no other file extension exists, append ".ivry" to the new_filename 
                // and proceed with renaming process.
                new_filename = new_filename + file_extension;
            }
            // if new_filename contains another file extension than ".ivry", quit renaming process.
            else {
                return false;
            }
        }

        // validating param, checking if new_filename is null or contains any file_separators.
        if (new_filename != null && new_filename.indexOf(file_separator) == -1){
            // creating a File object with the new filename.
            File new_file = new File(FILE_LOCATION.getParent() + file_separator + new_filename);

            // if a file with the new filename already exists, abort file renaming operation.
            if(!new_file.exists()){
                return FILE_LOCATION.renameTo(new_file); // rename
            }
        }
        // if any error occurred, returns false.
        return false;
    } // renameDatabaseTo()


    /**
     * Method to add a Column to the Ivory Database.
     * 
     * @param column_name
     *        The name of the Column to be added to the Database.
     * 
     * @return True if the Column addition was successful, otherwise, false.
     */
    public boolean ADD_COLUMN(String column_name){
        try{
            // adding new Column object with the same number of rows as the current database.
            COLUMNS.add(new Column(column_name.toUpperCase(), no_of_rows));

            // increment no_of_columns.
            no_of_columns++;
        }
        catch(Exception e){
            // return false if column addition failed
            return false;
        }
        // return true for successful column addition.
        return true;
    } // ADD_COLUMN()


    /**
     * Method to delete a Column from the Ivory Database.
     * 
     * @param column_name
     *        The name of the Column to be deleted.
     * 
     * @return True if the Column deletion was successful, otherwise, false.
     */
    public boolean DELETE_COLUMN(String column_name){
        try{
            // getting the column number of column_name.
            int col_num = getColumnNumberOf(column_name.toUpperCase());

            @SuppressWarnings("unused") // suppress warning that 'column' is unused.
            // removing the column from columns.
            Column column = COLUMNS.remove(col_num);
            // set the column to null to effectively delete it.
            column = null;
        }
        catch (Exception e){
            // if column does not exist, an exception is thrown. Deletion failed.
            return false;
        }
        // reduce no_of_columns by one and return true for successful deletion.
        no_of_columns--;
        return true;
    } // DELETE_COLUMN


    /**
     * method to save changes made to the Ivory Database object and store 
     * it in FILE_LOCATION.
     * 
     * @see java.io.Serializable
     */
    public void SAVE(){
        // if the FILE_LOCATION has not already been set, set the default.
        if (FILE_LOCATION == null){
            setDefaultFileLocation();
        }

        boolean save_success; // true when the Ivory Database object is saved to file successfully.
        try {
            // creating FileOutputStream and ObjectOutputStream objects.
            FileOutputStream fileOut = new FileOutputStream(FILE_LOCATION);
            ObjectOutputStream streamOut = new ObjectOutputStream(fileOut);
            
            // writing IvoryDatabase object to FILE_LOCATION.
            streamOut.writeObject(this);

            // closing streams
            streamOut.close();
            fileOut.close();

            save_success = true;
        } catch (IOException e) {
            save_success = false;
            e.printStackTrace();
        }

        if(save_success)
        // printing operation success message.
            System.out.println("IvoryDB Message : Operation Success : " + FILE_LOCATION.getName() + " has been saved successfully.");
        else
        // printing operation failure message.
            System.out.println("IvoryDB Message : Operation Failed : Changes made to " + FILE_LOCATION.getName() + " were not saved.");
    
    } // SAVE()

    /**
     * The close() method will run a loop through the attributes[] array and
     * assign every Attribute object as 'null'. This will be mark the objects 
     * for garbage collection.
     * 
     * Then System.gc() is invoked to call the JVM Garbage Collector which
     * will clean up the objects and free up system resources.
     */
    @Override
    @SuppressWarnings("unused") // suppressing the warning that 'attribute' is unused.
    public void close() {
        // saving all the changes made to the Database to FILE_LOCATION.
        this.SAVE(); 

        // setting all the columns to null.
        for(Column column : COLUMNS) {
            column = null; 
        }
        System.gc(); // invoking java garbage collector
    } // close()


    /*** 
        *** Regular Operation Methods ***
    ***/


    /**
     * Method to add a new row to the Database.
     * 
     * @param newEntry
     *        Object array containing the Column values for the new entry.
     */
    public boolean ADD(Object[] newEntry){
        // creating temporary Column reference.
        Column temp;
        // storing the id of the new Entry in a variable.
        String new_id = ((String) newEntry[0]).toUpperCase();
        // setting the ID value to Uppercase.
        newEntry[0] = new_id;

        // All values in ID column must be unique.
        // run a loop to check if a ID already exists in the ID column with the same value as the 'id' parameter.
        temp = COLUMNS.get(0); // referring to the ID column.

        // check if ID column is empty.
        if(!temp.isEmpty()){
            // run loop through ID column.
            for(int index = 0 ; index < no_of_rows ; index++){
                // check if the new ID equals an existing ID.
                if(new_id.equals(temp.get(index))){
                    // if the new_id is equal to an existing id, return false and do not add the new Entry.
                    return false;
                }
            }
        }


        /** getting the index where the new entry needs to be inserted 
            to maintain alphabetical order in ID column. */
        int insert_index = findInsertIndex(new_id);

        // if insert_index is -1 then the column is empty or the new entry needs to be inserted as the last row.
        if(insert_index == -1){
            // loop going through every Column to add the attributes of this entry.
            for(int col_num = 0 ; col_num < no_of_columns ; col_num++){
                temp = COLUMNS.get(col_num);
                temp.add(newEntry[col_num]);
            }
        }
        else{
            // loop going through every column to add the attributes of this entry.
            for(int col_num = 0 ; col_num < no_of_columns ; col_num++){
                // inserting the new Entry's attribute to the corresponding Attribute column.
                temp = COLUMNS.get(col_num);
                temp.insert(insert_index, newEntry[col_num]);
            }
        }
        // incrementing rows to represent the new number of rows.
        no_of_rows++;
        return true;
    } // ADD()


    /**
     * Method to delete a row from the database.
     * 
     * @param id
     *        ID of the entry that needs to deleted.
     * 
     * @return True if the entry was successfully deleted. Otherwise false.
     */
    public boolean DELETE(String id){
        try{    
        // getting the row number of id.
            int row_num = getRowNumberOf(id);

            for(Column col : COLUMNS){
                // deleting values of the entry from each row
                col.delete(row_num);
            }
        }
        // if id does not exist, an exception will be thrown.
        catch(RowNotFoundException e){
            e.printStackTrace();
            return false;
        }
        // reduce no_of_rows by 1 and return true if deletion was successful.
        no_of_rows--;
        return true;
    } // DELETE()


    /**
     * Method to get a value from the database.
     * 
     * @param id
     *        ID of the row entry whose value is requested.
     * 
     * @param column_name
     *        The name of the column value to returned
     * 
     * @return The object of {@code id} in {@code column_name}.
     */
    public Object GET(String id, String column_name){
        try{
            int col_num = getColumnNumberOf(column_name);
            int row_num = getRowNumberOf(id);

            return COLUMNS.get(col_num).get(row_num);
        }
        // if either id or column_name are invalid, an exception is thrown.
        catch (ColumnNotFoundException | RowNotFoundException e){
            e.printStackTrace();
            return null;
        }
    } // GET()


    /**
     * Method to set the value of cell in the Database.
     * 
     * @param id
     *        The ID of the Database row.
     * 
     * @param column_name
     *        The name of the Column in the Database.
     * 
     * @param value
     *        The value that the cell is to be set to.
     *  
     * @return True, if the cell value was set successfully. Otherwise, returns false.
     */
    public boolean SET(String id, String column_name, Object value){
        int col_num, row_num;
        try{
            col_num = getColumnNumberOf(column_name);
            row_num = getRowNumberOf(id);
        }
        // if either id or column_name are invalid, an exception is thrown.
        catch (ColumnNotFoundException | RowNotFoundException e){
            e.printStackTrace();
            return false;
        }
        // creating a Column reference to col_num.
        Column column = COLUMNS.get(col_num);
        // setting the value of the cell.
        column.set(row_num, value);

        // if value was successfully set, return true.
        return true;
    } // SET()


    /**
     * Method to get all the values of a Column in the Database.
     * 
     * @param column_name
     *        Name of the {@code Column}.
     * 
     * @return {@code Object[]} with the values contained in the requested Column.
     */
    public Object[] GET_COLUMN(String column_name){
        try{
            // getting the column number of the column_name.
            int col_num = getColumnNumberOf(column_name);

            // converting Column to an Object Array and returning it.
            return COLUMNS.get(col_num).toArray();
        }
        catch (ColumnNotFoundException e){
            e.printStackTrace();
            return null;
        }
    } // GET_COLUMN()


    /**
     * @return The Ivory Database as a comma separated table.
     */
    public String getCSV(){
        String output = "";
        Column currentCol;
        for(int row = 0 ; row < no_of_rows ; row++){
            for(int col = 0 ; col < no_of_columns ; col++){
                currentCol = COLUMNS.get(col);
                output = output + String.valueOf(currentCol.get(row)) + ", ";
            }
            output = output + "\b\n";
        }
        return output;
    } // GET_CONTENTS()



    /*** 
        *** Helper Methods ***
    ***/


    /**
     * @param id
     *        the id of the entry.
     * 
     * @return 0 if ID column is empty.
     *         -1 if entry needs to be added at the end of Column.
     *         Otherwise, the respective index value where the entry fits alphabetically.
     */
    private int findInsertIndex(String id){
        // making id uppercase.
        id = id.toUpperCase();
        // getting the ID attribute column.
        Column id_column = COLUMNS.get(0);

        // if ID column is empty, then return -1.
        if (no_of_rows == 0){
            return -1;
        }
        else{
            // running loop through ID column to find alphabetically correct index to insert the entry.
            for(int index = 0 ; index < no_of_rows ; index++){
                if(id.compareTo((String)id_column.get(index)) <= 0){
                    return index;
                }
            }
        }
        
        // if entry needs to be added to the end of the column.
        return -1;
    } // findInsertIndex()


    /**
     * Method to get the column number of a column.
     * 
     * @param column_name
     *        The name of the column.
     * 
     * @return The index number of {@code column_name} in {@code columns}. 
     * 
     * @throws ColumnNotFoundException
     *         if {@code column_name} does not exist.
     */
    private int getColumnNumberOf(String column_name)
        throws ColumnNotFoundException {
        // make the column_name parameter uppercase.
        column_name = column_name.toUpperCase();

        // run loop through columns to search for column with name: column_name.
        for(int index = 0 ; index < no_of_columns ; index++){
            if(column_name.equals(COLUMNS.get(index).getName())){
                return index;
            }
        }

        // throw an Exception if a column with the name column_name is not found.
        throw new ColumnNotFoundException(column_name);
    } // getColumnNumberOf()


    /**
     * Method to get the column number of a column.
     * 
     * @param id
     *        The ID of the row.
     * 
     * @return The row number of {@code id} in the Database.
     * 
     * @throws RowNotFoundException
     *         if {@code id} does not exist in the ID column of the Database.
     */
    private int getRowNumberOf(String id) 
        throws RowNotFoundException {
        // make id uppercase.
        id = id.toUpperCase();
        // making a reference to the ID column.
        Column id_columns = COLUMNS.get(0);

        // run loop through the ID column.
        for(int index = 0 ; index < no_of_rows ; index++){
            if(id.equals((String)id_columns.get(index))){
                return index;
            }
        }
        
        // throw an Exception if a row with the ID id is not found.
        throw new RowNotFoundException(id);
    } // getRowNumberOf()

} // class