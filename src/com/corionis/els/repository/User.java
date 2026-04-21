package com.corionis.els.repository;

import com.corionis.els.Context;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class User
{
    // @formatter:off
    public static final int ADMIN = 2;
    public static final int ADVANCED = 1;
    public static final int BASIC = 0;

    private String name = "";
    private int type = 0;
    private ArrayList<Grants> resources = new ArrayList<>();

    transient Context context = null;
    transient public int lastGrantIndex = 0;
    // @formatter:on

    private User()
    {
    }

    public User(Context context)
    {
        this.context = context;
    }

    public User clone()
    {
        User clone = new User();
        clone.context = this.context;
        clone.name = this.name;
        clone.type = this.type;
        clone.resources = (ArrayList<Grants>) this.resources.clone();
        return clone;
    }

    public Grant findGrant(String forSystem, String library)
    {
        Grant grant = null;
        if (resources != null)
        {
            for (Grants grl : resources)
            {
                if (grl.getKey().equals(forSystem))
                {
                    grant = grl.findGrant(library);
                }
            }
        }
        return grant;
    }

    public Grants findGrants(String forSystem)
    {
        Grants grants = null;
        if (resources != null)
        {
            for (Grants grl : resources)
            {
                if (grl.getKey().equals(forSystem))
                {
                    grants = grl;
                }
            }
        }
        return grants;
    }

    public int findGrantsIndex(String forSystem)
    {
        int index = -1;
        for (int i = 0; i < resources.size(); ++i)
        {
            Grants grl = resources.get(i);
            if (grl.getKey().equals(forSystem))
            {
                index = i;
                break;
            }
        }
        return index;
    }

    public String getName()
    {
        return name;
    }

    public ArrayList<Grants> getResources()
    {
        return resources;
    }

    public int getType()
    {
        return type;
    }

    public boolean isAdmin()
    {
        return type == ADMIN;
    }

    public boolean isAdvanced()
    {
        return type == ADVANCED;
    }

    public boolean isBasic()
    {
        return type == BASIC;
    }

    public boolean mayRead(String forSystem, String libraryName)
    {
        if (!context.preferences.isUsersEnabled() || name.isEmpty() || forSystem == null || forSystem.isEmpty())
            return true;
        boolean may = false;
        Grant grant = findGrant(forSystem, libraryName);
        if (grant != null && grant.read)
            may = true;
        return may;
    }

    public boolean mayWrite(String forSystem, String libraryName)
    {
        if (!context.preferences.isUsersEnabled() || name.isEmpty() || forSystem == null || forSystem.isEmpty())
            return true;
        boolean may = false;
        Grant grant = findGrant(forSystem, libraryName);
        if (grant != null && grant.write)
            may = true;
        return may;
    }

    public User parseUser(String json)
    {
        User user = null;
        class objInstanceCreator implements InstanceCreator
        {
            @Override
            public Object createInstance(Type type)
            {
                return new User(context);
            }
        };

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(User.class, new objInstanceCreator());
        user = builder.create().fromJson(json, User.class);
        user.setContext(context);
        return user;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public void setLibraryGrants(String forSystem, ArrayList<Grant> grants)
    {
        Grants grantsLibrary = findGrants(forSystem);
        if (grantsLibrary == null)
        {
            grantsLibrary = new Grants(forSystem, grants);
            resources.add(grantsLibrary);
        }
        else
        {
            resources.remove(grantsLibrary);
            grantsLibrary.setGrants(grants);
            resources.add(grantsLibrary);
        }
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
