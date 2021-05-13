package com.groksoft.els.stty.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.stty.ClientStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class TerminalGui implements WindowListener, ActionListener
{
    Configuration cfg = null;
    JTextField commandField;
    JFrame frame;
    DataInputStream in = null;
    DataOutputStream out = null;
    JScrollPane scroll;
    ClientStty terminal;
    JTextArea textArea;
    private transient Logger logger = LogManager.getLogger("applog");
    private Repository myRepo;
    private Repository theirRepo;

    public TerminalGui(ClientStty clientStty, Configuration cfg, DataInputStream in, DataOutputStream out)
    {
        this.terminal = clientStty;
        this.cfg = cfg;
        this.in = in;
        this.out = out;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String action = e.getActionCommand();
        String response = "";
        JScrollBar sb;

        logger.info("Processing input: " + action + " = " + commandField.getText());

        try
        {
            switch (action)
            {
                case "clear":
                    textArea.setText("");
                    frame.revalidate();
                    sb = scroll.getVerticalScrollBar();
                    sb.setValue(sb.getMaximum());
                    commandField.setText("");
                    commandField.grabFocus();
                    commandField.requestFocus();
                    break;
                case "command":
                    response = roundTrip(commandField.getText());
                    frame.revalidate();
                    sb = scroll.getVerticalScrollBar();
                    sb.setValue(sb.getMaximum());
                    commandField.setText("");
                    commandField.grabFocus();
                    commandField.requestFocus();
                    break;
                case "exit":
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    break;
                case "reset":
                    commandField.setText("");
                    commandField.grabFocus();
                    commandField.requestFocus();
                    break;
                case "send":
                    response = roundTrip(commandField.getText());
                    frame.revalidate();
                    sb = scroll.getVerticalScrollBar();
                    sb.setValue(sb.getMaximum());
                    commandField.setText("");
                    commandField.grabFocus();
                    commandField.requestFocus();
                    break;
            }
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
            response = ex.getMessage();
        }
        if ((response != null) && response.equalsIgnoreCase("End-Execution"))
        {
            logger.info("End-Execution command received from server");
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    private int build()
    {
        frame = new JFrame("ELS " + cfg.getProgramVersionN() + " connected to " + theirRepo.getLibraryData().libraries.description);
//        try
//        {
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//        } catch (Exception e)
//        {
//            logger.error(e.getMessage());
//            return 1;
//        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(this);

        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("File");
        mb.add(m1);
        JMenuItem clearButton = new JMenuItem("Clear");
        clearButton.addActionListener(this);
        clearButton.setActionCommand("clear");
        m1.add(clearButton);
        JMenuItem exitButton = new JMenuItem("Exit");
        exitButton.addActionListener(this);
        exitButton.setActionCommand("exit");
        m1.add(exitButton);
        //JMenu m2 = new JMenu("Help");
        //mb.add(m2);

        JPanel panel = new JPanel();
        JLabel label = new JLabel("Command: ");

        commandField = new JTextField(40);
        commandField.setAutoscrolls(true);
        commandField.setActionCommand("command");
        commandField.addActionListener(this);

        JPanel buttons = new JPanel();
        JButton send = new JButton("Send");
        send.setActionCommand("send");
        send.addActionListener(this);
        JButton reset = new JButton("Reset");
        reset.setActionCommand("reset");
        reset.addActionListener(this);
        buttons.add(send);
        buttons.add(reset);

        panel.add(BorderLayout.WEST, label);
        panel.add(BorderLayout.CENTER, commandField);
        panel.add(BorderLayout.LINE_END, buttons);

        textArea = new JTextArea();
        textArea.setAutoscrolls(true);
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.WHITE);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        scroll = new JScrollPane(textArea);

        frame.getContentPane().add(BorderLayout.NORTH, mb);
        frame.getContentPane().add(BorderLayout.CENTER, scroll);
        frame.getContentPane().add(BorderLayout.SOUTH, panel);

        return 0;
    }

    public String receive() throws Exception
    {
        String response = Utils.read(in, theirRepo.getLibraryData().libraries.key);
        textArea.append(response);
        return response;
    }

    public String roundTrip(String command) throws Exception
    {
        send(command);
        String response = receive();
        return response;
    }

    public int run(Repository myRepo, Repository theirRepo) throws Exception
    {
        int returnValue = 0;

        this.myRepo = myRepo;
        this.theirRepo = theirRepo;

        returnValue = build();
        if (returnValue == 0)
        {
            frame.setVisible(true);
            receive(); // get and display initial response from server, the prompt
            commandField.grabFocus();
            commandField.requestFocus();
        }

        return returnValue;
    }

    public int send(String command) throws Exception
    {
        textArea.append(command + "\r\n");
        Utils.write(out, theirRepo.getLibraryData().libraries.key, command);
        return 0;
    }

    public void stop()
    {
        frame.setVisible(false);
    }

    @Override
    public void windowActivated(WindowEvent e)
    {
        commandField.grabFocus();
        commandField.requestFocus();
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
        //JOptionPane.showMessageDialog(frame, "Window Closed");
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        stop();
    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {

    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
        commandField.grabFocus();
        commandField.requestFocus();
    }

    @Override
    public void windowIconified(WindowEvent e)
    {

    }

    @Override
    public void windowOpened(WindowEvent e)
    {
        commandField.grabFocus();
        commandField.requestFocus();
    }
}
