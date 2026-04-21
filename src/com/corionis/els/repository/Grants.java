package com.corionis.els.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

public class Grants
{
    private String key = ""; // library key
    private ArrayList<Grant> grants = new ArrayList<>();

    public Grants(String key)
    {
        this.key = key;
    }

    public Grants(String key, ArrayList<Grant> grants)
    {
        this.key = key;
        this.grants = grants;
    }

    public void add(Grant grant)
    {
        grants.add(grant);
    }

    public Object clone()
    {
        Grants clone = new Grants(key);
        for (Grant grant : grants)
            clone.grants.add(grant.clone());
        return clone;
    }

    public Grant findGrant(String library)
    {
        Grant grant = null;
        for (Grant gr : grants)
        {
            if (gr.library.equalsIgnoreCase(library))
            {
                grant = gr;
            }
        }
        return grant;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public Grant get(int index)
    {
        if (grants != null && index >= 0 && index < grants.size())
            return grants.get(index);
        return null;
    }

    public ArrayList<Grant> getGrants()
    {
        return grants;
    }

    public Iterator iterator()
    {
        return grants.iterator();
    }

    public void remove(Grant grant)
    {
        grants.remove(grant);
    }

    public void setGrants(ArrayList<Grant> grants)
    {
        this.grants = grants;
    }

    public int size()
    {
        return grants.size();
    }

    public void sort()
    {
        grants.sort(new Comparator<Grant>()
        {
            @Override
            public int compare(Grant grant1, Grant grant2)
            {
                return grant1.library.compareTo(grant2.library);
            }
        });
    }

}
