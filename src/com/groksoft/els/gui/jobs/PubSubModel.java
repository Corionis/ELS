package com.groksoft.els.gui.jobs;

import com.groksoft.els.Configuration;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repositories;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PubSubModel extends DefaultTableModel
{
    Configuration cfg;
    JobsUI owner;
    Task task = null;
    Repositories repositories;

    private PubSubModel()
    {
        // hide default constructor
    }

    public PubSubModel(JobsUI owner, Configuration cfg, Repositories repositories, Task task)
    {
        super();
        this.owner = owner;
        this.cfg = cfg;
        this.repositories = repositories;
        this.task = task;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return String.class;
            case 1:
                return JButton.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    private String getDesc(boolean isPublisher, String key)
    {
        String desc = "";
        if (key.trim().length() == 0)
        {
            if (task.isDual())
            {
                if (isPublisher)
                    desc = cfg.gs("JobsUI.select.publisher");
                else
                    desc = cfg.gs("JobsUI.select.subscriber");
            }
            else
                desc = cfg.gs("JobsUI.select.publisher.or.subscriber");
        }
        else if (key.equals(Task.ANY_SERVER))
        {
            if (isPublisher)
                    desc = cfg.gs("JobsUI.any.publisher");
            else
                    desc = cfg.gs("JobsUI.any.subscriber");
        }
        else
        {
            Repositories.Meta meta = repositories.find(key);
            if (meta != null)
                desc = meta.description;
            else
                desc = cfg.gs("NavTreeNode.library") + " " + key + cfg.gs("Z.not.found");
        }
        return desc;
    }

    @Override
    public int getRowCount()
    {
        if (task != null && task.isDual())
            return 2;
        return 1;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (task != null)
        {
            if (column == 0)
            {
                if (task.isDual())
                {
                    if (row == 0)
                        return getDesc(true, task.getPublisherKey());
                    return getDesc(false, task.getSubscriberKey());
                }
                else
                {
                    String key = "";
                    boolean isPublisher = true;
                    if (task.getPublisherKey().length() > 0)
                        key = task.getPublisherKey();
                    else if (task.getSubscriberKey().length() > 0)
                    {
                        isPublisher = false;
                        key = task.getSubscriberKey();
                    }
                    return getDesc(isPublisher, key);
                }
            }

            if (column == 1)
            {
                JButton button = new JButton();
                button.setRolloverEnabled(true);
                button.setText("...");
                Dimension dim = new Dimension(32, 10);
                button.setPreferredSize(dim);
                button.setMinimumSize(dim);
                button.setMaximumSize(dim);
                String toolTip = "";
                if (!task.isDual())
                    toolTip = cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
                else
                {
                    if (row == 0)
                        toolTip = cfg.gs("JobsUI.select.publisher.tooltip");
                    else
                        toolTip = cfg.gs("JobsUI.select.subscriber.tooltip");
                }
                button.setToolTipText(toolTip);
                ActionEvent ae = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, Integer.toString(row));
                button.addActionListener(e -> owner.actionPubSubClicked(ae));
                return button;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

}
