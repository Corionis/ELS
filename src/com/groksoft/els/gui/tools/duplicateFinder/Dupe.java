package com.groksoft.els.gui.tools.duplicateFinder;

import com.groksoft.els.repository.Item;

public class Dupe
{
    Item item;
    boolean gone = false;
    boolean isTop = false;

    public Dupe(Item item)
    {
        this.item = item;
    }

    public Dupe(Item item, boolean isTop)
    {
        this.item = item;
        this.isTop = isTop;
    }

}
