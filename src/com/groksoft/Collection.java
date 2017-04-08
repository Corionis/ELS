package com.groksoft;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The type Collection.
 */
public class Collection extends HashMap<String, Item> // extends WHAT ????????????? Pros and Cons for various types of Collections (Map, List, etc); or should that just be a data member?????
{
    // Data members to describe a VolMonger collection
    // A set of Item objects as some kind of Java Collection
    // Accessor and Iterator methods for the set of Item objects

    // A load method to read a collection.json file
    // A validate method to check the syntax and existence of the elements in a collection.json file
    // A scan method to scan and generate the set of Item objects
    // A sort method, by context
    // A duplicates method to check for duplicate contexts in the Collection - possibly enforced by the selected Java collection requiring a unique key


    /**
     * Just messing with UML
     */
    private ArrayList<Item> itemArrayLists = new ArrayList<>();
    private Item itemArray[] = new Item[10];
    private Item curItem;

    /**
     * Get item array item [ ].
     *
     * @return the item [ ]
     */
    public Item[] getItemArray() {
        return itemArray;
    }

    /**
     * Sets item array.
     *
     * @param itemArray the item array
     */
    public void setItemArray(Item[] itemArray) {
        this.itemArray = itemArray;
    }

    /**
     * Gets cur item.
     *
     * @return the cur item
     */
    public Item getCurItem() {
        return curItem;
    }

    /**
     * Sets cur item.
     *
     * @param curItem the cur item
     */
    public void setCurItem(Item curItem) {
        this.curItem = curItem;
    }


}
