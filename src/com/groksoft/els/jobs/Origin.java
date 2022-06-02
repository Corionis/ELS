package com.groksoft.els.jobs;

import com.groksoft.els.gui.browser.NavTreeUserObject;

public class Origin
{
    private String name;
    private int type;


    public Origin(NavTreeUserObject tuo)
    {
        this.name = tuo.getPath();
        this.type = tuo.type;
    }

    public Origin(String name, int type)
    {
        this.name = name;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public int getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setType(int type)
    {
        this.type = type;
    }

}
