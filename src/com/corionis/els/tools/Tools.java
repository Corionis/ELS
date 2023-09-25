package com.corionis.els.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.corionis.els.Context;
import com.corionis.els.tools.operations.OperationsTool;
import com.corionis.els.tools.junkremover.JunkRemoverTool;
import com.corionis.els.tools.renamer.RenamerTool;
import com.corionis.els.tools.sleep.SleepTool;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")

public class Tools
{

    /**
     * Get tool from existing toolList from loadAllTools()
     *
     * @param toolList Existing toolList
     * @param internalName Internal name of desired tool
     * @param configName Config name of desired tool
     * @return The AbstractTool or null if not found
     */
    public AbstractTool getTool(ArrayList<AbstractTool> toolList, String internalName, String configName)
    {
        for (int i = 0; i < toolList.size(); ++i)
        {
            AbstractTool tool = toolList.get(i);
            if (tool.getInternalName().equals(internalName) && tool.getConfigName().equals(configName))
                return tool;
        }
        return null;
    }

    /**
     * Load a specific tool from disk
     *
     * @param context The Context
     * @param internalName Internal name of desired tool
     * @param configName Config name of desired tool
     * @return The AbstractTool or null if not found
     * @throws Exception File system and parse exceptions
     */
    public AbstractTool loadTool(Context context, String internalName, String configName) throws Exception
    {
        AbstractTool tool = null;

        if (internalName.equals(OperationsTool.INTERNAL_NAME))
        {
            // begin Operations
            OperationsTool tmpTool = new OperationsTool(context);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                OperationParser operationParser = new OperationParser();
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            AbstractTool but = operationParser.parseTool(context, json);
                            if (but != null)
                            {
                                if (but.getConfigName().equalsIgnoreCase(configName))
                                {
                                    tool = but;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // end Operations
        }
        else if (internalName.equals(JunkRemoverTool.INTERNAL_NAME))
        {
            // begin JunkRemover
            JunkRemoverTool tmpTool = new JunkRemoverTool(context);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                JunkRemoverParser junkRemoverParser = new JunkRemoverParser();
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            AbstractTool jrt = junkRemoverParser.parseTool(context, json);
                            if (jrt != null)
                            {
                                if (jrt.getConfigName().equalsIgnoreCase(configName))
                                {
                                    tool = jrt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // end JunkRemover
        }
        else if (internalName.equals(RenamerTool.INTERNAL_NAME))
        {
            // begin Renamer
            RenamerTool tmpTool = new RenamerTool(context);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                RenamerParser renamerParser = new RenamerParser();
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            AbstractTool jrt = renamerParser.parseTool(context, json);
                            if (jrt != null)
                            {
                                if (jrt.getConfigName().equalsIgnoreCase(configName))
                                {
                                    tool = jrt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // end Renamer
        }
        else if (internalName.equals(SleepTool.INTERNAL_NAME))
        {
            // begin SleepTool
            SleepTool tmpTool = new SleepTool(context);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                SleepParser sleepParser = new SleepParser();
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            AbstractTool slp = sleepParser.parseTool(context, json);
                            if (slp != null)
                            {
                                if (slp.getConfigName().equalsIgnoreCase(configName))
                                {
                                    tool = slp;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // end SleepTool
        }
        else if (0 == 1)
        {
            // TODO EXTEND+ Add other tool implementations here
        }

        return tool;
    }

    /**
     * Load all tools of a particular internalName from disk
     * <br/>
     * Creates an ArrayList of AbstractTool that is populated and returned
     *
     * @param context The Context, null is allowed
     * @param internalName Internal name of desired tool, or null/empty for all tools
     * @return ArrayList of tools
     * @throws Exception File system and parse exceptions
     */
    public ArrayList<AbstractTool> loadAllTools(Context context, String internalName) throws Exception
    {
        ArrayList<AbstractTool> toolList = new ArrayList<AbstractTool>();
        return loadAllTools(context, internalName, toolList);
    }

    /**
     * Load all tools of a particular internalName from disk
     * <p><br>
     * Note the Duplicate Finder and Empty Directory Finder tools are run manually and
     * are not appropriate for Jobs. Therefore they are not loaded here.
     *
     * @param context The Context, null is allowed
     * @param internalName Internal name of desired tool, or null/empty for all tools
     * @param  toolList ArrayList of AbstractTool to add new items to
     * @return ArrayList of tools
     * @throws Exception File system and parse exceptions
     */
    public ArrayList<AbstractTool> loadAllTools(Context context, String internalName, ArrayList<AbstractTool> toolList) throws Exception
    {
        if (internalName != null && internalName.length() == 0)
            internalName = null;

        File toolDir = null;
        ToolParserI toolParser = null;

        // being OperationsUI
        if (internalName == null || internalName.equals(OperationsTool.INTERNAL_NAME))
        {
            toolParser = new OperationParser();
            OperationsTool tmpOperation = new OperationsTool(context);
            toolDir = new File(tmpOperation.getDirectoryPath());
            toolList = scanTools(context, toolList, toolParser, toolDir);
        }
        // end OperationsUI

        // begin JunkRemover
        if (internalName == null || internalName.equals(JunkRemoverTool.INTERNAL_NAME))
        {
            toolParser = new JunkRemoverParser();
            JunkRemoverTool tmpJrt = new JunkRemoverTool(context);
            toolDir = new File(tmpJrt.getDirectoryPath());
            toolList = scanTools(context, toolList, toolParser, toolDir);
        }
        // end JunkRemover

        // begin Renamer
        if (internalName == null || internalName.equals(RenamerTool.INTERNAL_NAME))
        {
            toolParser = new RenamerParser();
            RenamerTool tmpRenamer = new RenamerTool(context);
            toolDir = new File(tmpRenamer.getDirectoryPath());
            toolList = scanTools(context, toolList, toolParser, toolDir);
        }
        // end Renamer

        // begin SleepUI Tool
        if (internalName == null || internalName.equals(SleepTool.INTERNAL_NAME))
        {
            toolParser = new SleepParser();
            SleepTool tmpSleep = new SleepTool(context);
            toolDir = new File(tmpSleep.getDirectoryPath());
            toolList = scanTools(context, toolList, toolParser, toolDir);
        }
        // end SleepUI Tool

        // TODO EXTEND+ Add other tool parsers here

        // sort the list
        Collections.sort(toolList);

        return toolList;
    }

    /**
     * Create an AbstractTool object based on the internal name
     *
     * @param internalName Internal name of tool
     * @param context The Context
     * @return AbstractTool object
     */
    public AbstractTool makeTempTool(String internalName, Context context)
    {
        AbstractTool tmpTool = null;
        if (internalName.equals(OperationsTool.INTERNAL_NAME))
        {
            tmpTool = new OperationsTool(context);
        }
        else if (internalName.equals(JunkRemoverTool.INTERNAL_NAME))
        {
            tmpTool = new JunkRemoverTool(context);
        }
        else if (internalName.equals(RenamerTool.INTERNAL_NAME))
        {
            tmpTool = new RenamerTool(context);
        }
        else if (internalName.equals(SleepTool.INTERNAL_NAME))
        {
            tmpTool = new SleepTool(context);
        }
        return tmpTool;
    }

    /**
     * Scan the disk for a specific tool's configurations
     *
     * @param context The Context, null is allowed
     * @param toolList   Existing toolList
     * @param parser     The ToolParserI implementation for this tool
     * @param toolDir    The full path to this tool's configurations
     * @return ArrayList&lt;AbstractTool&gt; toolList
     * @throws Exception File system exceptions
     */
    private ArrayList<AbstractTool> scanTools(Context context, ArrayList<AbstractTool> toolList, ToolParserI parser, File toolDir) throws Exception
    {
        if (toolDir.exists() && toolDir.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                    if (json != null)
                    {
                        AbstractTool tool = parser.parseTool(context, json);
                        if (tool != null)
                            toolList.add(tool);
                    }
                }
            }
        }
        return toolList;
    }

    //=================================================================================================================

    /**
     * Interface for Google GSON tool parsers
     */
    private interface ToolParserI
    {
        AbstractTool parseTool(Context context, String json);
    }

    //=================================================================================================================

    /**
     * ToolParserI implementation for the OperationsTool
     */
    private class OperationParser implements ToolParserI
    {
        /**
         * Parse a OperationsTool
         *
         * @param context The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(Context context, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new OperationsTool(context);
                }
            };

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(OperationsTool.class, new objInstanceCreator());
            OperationsTool tool = builder.create().fromJson(json, OperationsTool.class);
            return tool;
        }
    }

    //=================================================================================================================

    /**
     * ToolParserI implementation for the JunkRemoverTool
     */
    private class JunkRemoverParser implements ToolParserI
    {
        /**
         * Parse a JunkRemoverTool
         *
         * @param context The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(Context context, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new JunkRemoverTool(context);
                }
            };

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(JunkRemoverTool.class, new objInstanceCreator());
            JunkRemoverTool tool = builder.create().fromJson(json, JunkRemoverTool.class);
            return tool;
        }
    }

    //=================================================================================================================

    /**
     * ToolParserI implementation for the RenamerTool
     */
    private class RenamerParser implements ToolParserI
    {
        /**
         * Parse a RenamerTool
         *
         * @param context The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(Context context, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new RenamerTool(context);
                }
            };

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(RenamerTool.class, new objInstanceCreator());
            RenamerTool tool = builder.create().fromJson(json, RenamerTool.class);
            return tool;
        }
    }

    //=================================================================================================================

    /**
     * ToolParserI implementation for the SleepTool
     */
    private class SleepParser implements ToolParserI
    {
        /**
         * Parse a SleepTool
         *
         * @param context The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(Context context, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new SleepTool(context);
                }
            };

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(SleepTool.class, new objInstanceCreator());
            SleepTool tool = builder.create().fromJson(json, SleepTool.class);
            return tool;
        }
    }

}
