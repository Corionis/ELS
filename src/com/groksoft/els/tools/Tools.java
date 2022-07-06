package com.groksoft.els.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;
import com.groksoft.els.tools.renamer.RenamerTool;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class Tools
{
    /**
     * Get tool from existing toolList from getAllTools()
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
     * @param guiContext The guiContext, null is allowed
     * @param config The Configuration
     * @param ctxt The Context
     * @param internalName Internal name of desired tool
     * @param configName Config name of desired tool
     * @return The AbstractTool or null if not found
     * @throws Exception File system and parse exceptions
     */
    public AbstractTool loadTool(GuiContext guiContext, Configuration config, Context ctxt, String internalName, String configName) throws Exception
    {
        AbstractTool tool = null;

        if (internalName.equals("JunkRemover"))
        {
            // begin JunkRemover
            JunkRemoverTool tmpTool = new JunkRemoverTool(null, config, ctxt);
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
                            AbstractTool jrt = junkRemoverParser.parseTool(guiContext, config, ctxt, json);
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
        else if (internalName.equals("Renamer"))
        {
            // begin Renamer
            RenamerTool tmpTool = new RenamerTool(null, config, ctxt);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                RenamerParser RenamerParser = new RenamerParser();
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            AbstractTool jrt = RenamerParser.parseTool(guiContext, config, ctxt, json);
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
        else if (0 == 1)
        {
            // TODO Add other tool implementations here
        }

        return tool;
    }

    /**
     * Load all tools of a particular internalName from disk
     *
     * @param guiContext The guiContext, null is allowed
     * @param config The Configuration
     * @param ctxt The Context
     * @param internalName Internal name of desired tool, or null/empty for all tools
     * @return ArrayList of tools
     * @throws Exception File system and parse exceptions
     */
    public ArrayList<AbstractTool> loadAllTools(GuiContext guiContext, Configuration config, Context ctxt, String internalName) throws Exception
    {
        ArrayList<AbstractTool> toolList = new ArrayList<AbstractTool>();

        if (internalName != null && internalName.length() == 0)
            internalName = null;

        File toolDir = null;
        ToolParserI toolParser = null;

        // begin JunkRemover
        if (internalName == null || internalName.equals(JunkRemoverTool.INTERNAL_NAME))
        {
            toolParser = new JunkRemoverParser();
            JunkRemoverTool tmpJrt = new JunkRemoverTool(null, config, ctxt);
            toolDir = new File(tmpJrt.getDirectoryPath());
            toolList = scanTools(guiContext, config, ctxt, toolList, toolParser, toolDir);
        }
        // end JunkRemover

        // begin Renamer
        if (internalName == null || internalName.equals(RenamerTool.INTERNAL_NAME))
        {
            toolParser = new RenamerParser();
            RenamerTool tmpRenamer = new RenamerTool(null, config, ctxt);
            toolDir = new File(tmpRenamer.getDirectoryPath());
            toolList = scanTools(guiContext, config, ctxt, toolList, toolParser, toolDir);
        }
        // end Renamer

        // TODO add other tool parsers here

        // sort the list
        Collections.sort(toolList);

        return toolList;
    }

    /**
     * Scan the disk for a specific tool's configurations
     *
     * @param guiContext The guiContext, null is allowed
     * @param config The Configuration
     * @param ctxt The Context
     * @param toolList Existing toolList
     * @param parser The ToolParserI implementation for this tool
     * @param toolDir The full path to this tool's configurations
     * @return ArrayList&lt;AbstractTool&gt; toolList
     * @throws Exception File system exceptions
     */
    private ArrayList<AbstractTool> scanTools(GuiContext guiContext, Configuration config, Context ctxt, ArrayList<AbstractTool> toolList, ToolParserI parser, File toolDir) throws Exception
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
                        AbstractTool tool = parser.parseTool(guiContext, config, ctxt, json);
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
        AbstractTool parseTool(GuiContext guiContext, Configuration config, Context ctxt, String json);
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
         * @param guiContext The guiContext, null is allowed
         * @param config The Configuration
         * @param ctxt The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(GuiContext guiContext, Configuration config, Context ctxt, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new JunkRemoverTool(guiContext, config, ctxt);
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
         * @param guiContext The guiContext, null is allowed
         * @param config The Configuration
         * @param ctxt The Context
         * @param json String of JSON to parse
         * @return AbstractTool instance
         */
        @Override
        public AbstractTool parseTool(GuiContext guiContext, Configuration config, Context ctxt, String json)
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new RenamerTool(guiContext, config, ctxt);
                }
            };

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(RenamerTool.class, new objInstanceCreator());
            RenamerTool tool = builder.create().fromJson(json, RenamerTool.class);
            return tool;
        }
    }

}
