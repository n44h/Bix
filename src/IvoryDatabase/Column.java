package IvoryDatabase;

import java.util.ArrayList;

class Column implements java.io.Serializable{
    private String column_name; // name of the column.
    /**
     * Since we do not know the OBJECT TYPE the Column will 
     * store until it is initialized, we can declare 'cells' 
     * as a Object ArrayList.
     * 
     * Every object class in Java implements the 
     * 'java.lang.Object'. Therefore, by declaring 'cells' 
     * as a 'java.lang.Object', it can store objects of 
     * any class that implements 'java.lang.Object'.
     * 
     * Hence, this gives us the freedom to declare the data 
     * type of the 'cells' ArrayList at a later time during 
     * initializing of a Column object. 
     */
    private final ArrayList<Object> cells;


    /**
     * @param col_name
     *        The name of the Column.
     */
    public Column(String col_name){
        this.column_name = col_name.toUpperCase();
        this.cells = new ArrayList<>(); // initializing ArrayList()
    } // constructor
    
    
    /**
     * @param col_name
     *        The name of the {@code Column}.
     * 
     * @param size
     *        The size to set {@code cells} to.
     */
    public Column(String col_name, int size){
        this.column_name = col_name.toUpperCase();
        this.cells = new ArrayList<>(size); // initializing ArrayList()
    } // constructor
    

    /**
     * Method to get the name of the Column.
     * 
     * @return Name of the Column object.
     */
    public String getName(){
        return column_name;
    } // getName()


    /**
     * Method to get the size of the cells ArrayList.
     * 
     * @return The size of cells.
     */
    public int getSize(){
        return cells.size();
    } // getSize()


    /**
     * Method to add values to {@code cells}.
     * 
     * @param value
     *        The value that is added to {@code cells}.
     */
    public void add(Object value){
        cells.add(value);
    } // add()


    /**
     * Method for inserting values to {@code cells}.
     * 
     * @param index
     *        The index where the value is to be inserted.
     * 
     * @param value
     *        The value that is added to {@code cells}.
     */
    public void insert(int index, Object value){
        cells.add(index, value);
    } // insert()


    /**
     * Method for changing values in {@code cells}.
     * 
     * @param index
     *        The index in {@code cells} where {@code value} is added.
     * 
     * @param value
     *        The value that is added to {@code cells}.
     */
    public void set(int index, Object value){
        cells.set(index, value);
    } // set()


    /**
     * Method to return the requested value from {@code cells}.
     * 
     * @param index
     *        The index of the value in {@code cells}.
     * 
     * @return The value at {@code index} of {@code cells}.         
     */
    public Object get(int index){
        return cells.get(index);
    } // get()


    /**
     * Method to delete a value from {@code cells}.
     * 
     * @param index
     *        The index of the value to be deleted in {@code cells}.
     * 
     * @return The true if object the object at {@code index} in 
     *         {@code cells} is successfully deleted.         
     */
    public boolean delete(int index){
        try{
            cells.remove(index); 
        }
        // if index is out of bounds, an exception is thrown
        catch (Exception e){
            return false;
        }
        return true;
    } // delete()

    /**
     * Method to return {@code cells} as an array.
     * 
     * @return The ArrayList {@code cells} as an Object array.
     */
    public Object[] toArray(){
        return cells.toArray();
    } // getArray()

    public boolean isEmpty(){
        return cells.isEmpty();
    }
} // Attribute class