package com.corionis.els.repository;

public class Grant
{
    public String library = "";
    public boolean read = false;
    public boolean write = false;

    public Grant(String library)
    {
        this.library = library;
    }

    public Grant clone()
    {
        Grant clone = new Grant(this.library);
        clone.read = this.read;
        clone.write = this.write;
        return clone;
    }
}
