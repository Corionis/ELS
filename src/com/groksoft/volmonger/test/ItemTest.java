package com.groksoft.volmonger.test;

import com.groksoft.volmonger.Item;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 */
class ItemTest {

    Item item = new Item();

    @Test
    void isDirectory() {
        item.setDirectory(true);
        assertEquals(true, item.isDirectory());
        assertTrue(item.isDirectory());
    }

    @Test
    void setDirectory() {
    }

    @Test
    void getItemPath() {
    }

    @Test
    void setItemPath() {
    }

    @Test
    void getFullPath() {
    }

    @Test
    void setFullPath() {
    }

    @Test
    void getLibrary() {
    }

    @Test
    void setLibrary() {
    }

    @Test
    void getSize() {
    }

    @Test
    void setSize() {
    }

    @Test
    void isSymLink() {
    }

    @Test
    void setSymLink() {
    }

}