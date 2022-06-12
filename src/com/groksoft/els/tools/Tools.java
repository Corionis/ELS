package com.groksoft.els.tools;

import com.google.gson.Gson;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tools
{
    public AbstractTool getTool(Configuration config, Context ctxt, String internalName, String configName) throws Exception
    {
        AbstractTool tool = null;
        if (internalName.equals("JunkRemover"))
        {
            JunkRemoverTool tmpTool = new JunkRemoverTool(config, ctxt);
            File toolDir = new File(tmpTool.getDirectoryPath());
            if (toolDir.exists() && toolDir.isDirectory())
            {
                File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        Gson gson = new Gson();
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            JunkRemoverTool jrt = gson.fromJson(json, JunkRemoverTool.class);
                            if (jrt != null)
                            {
                                if (jrt.getConfigName().equalsIgnoreCase(configName))
                                {
                                    jrt.setDisplayName(config.gs("JunkRemover.displayName"));
                                    jrt.setContext(config, ctxt);
                                    tool = jrt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return tool;
    }

    public DefaultListModel<AbstractTool> getAllTools(Configuration config, Context ctxt) throws Exception
    {
        DefaultListModel<AbstractTool> toolListModel = new DefaultListModel<AbstractTool>();

        // JunkRemover tool
        ToolParserI junkRemoverParser = new ToolParserI()
        {
            @Override
            public DefaultListModel<AbstractTool>  addTool(DefaultListModel<AbstractTool> toolListModel, String json)
            {
                Gson gson = new Gson();
                JunkRemoverTool tool = gson.fromJson(json, JunkRemoverTool.class);
                if (tool != null)
                {
                    tool.setDisplayName(config.gs("JunkRemover.displayName"));
                    tool.setContext(config, ctxt);
                    toolListModel.addElement(tool);
                }
                return toolListModel;
            }
        };
        //
        JunkRemoverTool tmpJrt = new JunkRemoverTool(config, ctxt);
        File toolDir = new File(tmpJrt.getDirectoryPath());
        toolListModel = scanTools(config, ctxt, toolListModel, junkRemoverParser, toolDir);


        // TODO add other tools here


        // sort the list
        List<AbstractTool> toolsList = Collections.list(toolListModel.elements());
        Collections.sort(toolsList);
        toolListModel.removeAllElements();;
        for (AbstractTool tool : toolsList)
            toolListModel.addElement(tool);

        return toolListModel;
    }

    private DefaultListModel<AbstractTool> scanTools(Configuration config, Context ctxt, DefaultListModel<AbstractTool> toolListModel, ToolParserI parser, File toolDir) throws Exception
    {
        if (toolDir.exists() && toolDir.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    Gson gson = new Gson();
                    String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                    if (json != null)
                    {
                        toolListModel = parser.addTool(toolListModel, json);
                    }
                }
            }
        }
        return toolListModel;
    }

    private interface ToolParserI
    {
        DefaultListModel<AbstractTool> addTool(DefaultListModel<AbstractTool> toolListModel, String json);
    }

}
