package com.corionis.els.gui.hints;

import com.corionis.els.Context;
import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintStatus;
import com.corionis.els.repository.RepoMeta;
import com.corionis.els.repository.Repositories;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;

public class HintsTableModel extends DefaultTableModel
{
    private Context context;
    public ArrayList<Hint> hints = null;
    private ImageIcon iconGreen;
    private ImageIcon iconRed;
    private ImageIcon iconYellow;
    private Repositories repositories;

    public HintsTableModel(Context context, Repositories repositories, ArrayList<Hint> hints)
    {
        super();
        this.context = context;
        this.hints = hints;
        this.repositories = repositories;
        iconGreen = getIcon("hint-green.png");
        iconGreen.setDescription(context.cfg.gs("HintsUI.status.done"));
        iconRed = getIcon("hint-red.png");
        iconRed.setDescription(context.cfg.gs("HintsUI.status.for"));
        iconYellow = getIcon("hint-yellow.png");
        iconYellow.setDescription(context.cfg.gs("HintsUI.status.unknown"));

    }

    private ImageIcon getIcon(String name)
    {
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource(name);
            Image icon = ImageIO.read(url);
            ImageIcon image = new ImageIcon(icon);
            return image;
        }
        catch (Exception e)
        {
        }
        return null;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch(column)
        {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return HintDate.class;
            case 4:
                return String.class;
            case 5:
                return String.class;
            case 6:
                return String.class;
            case 7:
                return String.class;
            case 8:
                return String.class;
        }
        return ImageIcon.class;
    }

    @Override
    public int getColumnCount()
    {
        if (context.hintKeys != null)
            return context.hintKeys.size() + 9;
        return 9;
    }

    @Override
    public String getColumnName(int column)
    {
        String name = "";
        switch(column)
        {
            case 0:
                name = "";
                break;
            case 1:
                name = context.cfg.gs("HintsUI.header.system");
                break;
            case 2:
                name = context.cfg.gs("HintsUI.header.author");
                break;
            case 3:
                name = context.cfg.gs("HintsUI.header.utc");
                break;
            case 4:
                name = context.cfg.gs("HintsUI.header.action");
                break;
            case 5:
                name = context.cfg.gs("HintsUI.header.from.library");
                break;
            case 6:
                name = context.cfg.gs("HintsUI.header.from.item");
                break;
            case 7:
                name = context.cfg.gs("HintsUI.header.to.library");
                break;
            case 8:
                name = context.cfg.gs("HintsUI.header.to.item");
                break;
            default:
                HintKey hk = context.hintKeys.get().get(column - 9);
                String key = hk.uuid;
                RepoMeta meta = repositories.find(key);
                if (meta != null)
                    name = meta.description;
                else if (hk != null)
                    name = hk.system; // use system if repo not on this instance
                break;
        }
        return name;
    }

    @Override
    public int getRowCount()
    {
        return (hints == null) ? 0 : hints.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Object object = null;
        if (row < hints.size())
        {
            Hint hint = hints.get(row);
            switch (column)
            {
                case 0:
                    object = hint.selected;
                    break;
                case 1:
                    String system = hint.system;
                    HintKey hk = context.hintKeys.findSystem(system);
                    if (hk != null)
                    {
                        RepoMeta meta = repositories.find(hk.uuid);
                        if (meta != null)
                            object = meta.description;
                        else
                            object = hk.system; // use system if repo not on this instance
                    }
                    break;
                case 2:
                    object = hint.author;
                    break;
                case 3:
                    object = new HintDate(context, hint.utc);
                    break;
                case 4:
                    object = (hint.action.trim().toLowerCase().equals("mv") ?
                        context.cfg.gs("HintsUI.action.move") :
                        context.cfg.gs("HintsUI.action.delete"));
                    break;
                case 5:
                    object = hint.fromLibrary;
                    break;
                case 6:
                    object = hint.fromItemPath;
                    break;
                case 7:
                    object = hint.toLibrary;
                    break;
                case 8:
                    object = hint.toItemPath;
                    break;
                default:
                    HintStatus hs = null;
                    if ((column - 9) < context.hintKeys.get().size())
                        hs = hint.findStatus(context.hintKeys.get().get(column - 9).system);
                    String stat = (hs != null) ? hs.status.trim().toLowerCase() : "for";
                    if (stat.equals("done"))
                        object = iconGreen;
                    else if (stat.equals("fault"))
                        object = iconYellow;
                    else
                        object = iconRed;
                    break;
            }
        }
        return object;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        // checkbox and from/to fields
        if (col == 0) // || (col >= 5 && col <= 8))
            return true;
        return false;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (object instanceof Boolean)
        {
            Hint hint = hints.get(row);
            hint.selected = ((Boolean) object).booleanValue();
            int count = context.navigator.dialogHints.setButtons();
            context.navigator.dialogHints.labelStatus.setText(count + context.cfg.gs("HintsUI.hints.selected"));
        }
    }

}
