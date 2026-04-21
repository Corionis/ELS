package com.corionis.els.repository;

/**
 * The type Library data.
 * <p>
 * An "anonymous" outer class for GSON to read the JSON and for potential future expansion
 */
public class LibraryData
{
    /**
     * top-level structures
     */
    public Libraries libraries;
    public User user; // this is why LibraryData was created from the beginning ... new ideas


    /**
     * Get specific library
     * <p>
     * Do these Libraries have a particular Library?
     * <p>
     * Does not throw any exceptions.
     *
     * @param libraryName the library name
     * @return the Library, or null if not found
     */
    public Library getLibrary(String libraryName)
    {
        Library retLib = null;
        for (Library lib : libraries.bibliography)
        {
            if (lib.name.equalsIgnoreCase(libraryName))
            {
                retLib = lib;
            }
        }
        return retLib;
    }


}
