package com.groksoft.els.tools;

import com.google.gson.Gson;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Tools
{
    public AbstractTool getTool(Configuration config, Context ctxt, String configName, String internalName) throws Exception
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

}
