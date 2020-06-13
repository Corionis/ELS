package com.groksoft.volmunger.comm.gui;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.swing.*;
import java.awt.*;

public class TerminalGui implements WindowListener, ActionListener
{
    private transient Logger logger = LogManager.getLogger("applog");

    Configuration cfg = null;
    DataInputStream in = null;
    DataOutputStream out = null;
    private Repository myRepo;
    private Repository theirRepo;

    JFrame frame;
    JTextArea textArea;
    JTextField commandField;

    public TerminalGui(Configuration cfg, DataInputStream in, DataOutputStream out) {
        this.cfg = cfg;
        this.in = in;
        this.out = out;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = e.getActionCommand();

        switch (name)
        {
            case "command" :
                JOptionPane.showMessageDialog(frame, "Command: " + commandField.getText());
                roundTrip(commandField.getText());
                commandField.setText("");
                commandField.grabFocus();
                commandField.requestFocus();
                break;
            case "exit" :
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                break;
            case "send" :
                JOptionPane.showMessageDialog(frame, "Send: " + commandField.getText());
                roundTrip(commandField.getText());
                commandField.setText("");
                commandField.grabFocus();
                commandField.requestFocus();
                break;
            case "reset" :
                JOptionPane.showMessageDialog(frame, "Reset");
                commandField.setText("");
                commandField.grabFocus();
                commandField.requestFocus();
                break;
        }
    }

    private int build()
    {
        frame = new JFrame("VolMunger " + cfg.getVOLMUNGER_VERSION() + " connected to " + theirRepo.getLibraryData().libraries.description);
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return 1;
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(this);

        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("File");
        JMenu m2 = new JMenu("Help");
        mb.add(m1);
        mb.add(m2);
        JMenuItem exitButton = new JMenuItem("Exit");
        exitButton.addActionListener(this);
        exitButton.setActionCommand("exit");
        m1.add(exitButton);

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

        // Text Area at the Center
        textArea = new JTextArea();

        //Adding Components to the frame.
        frame.getContentPane().add(BorderLayout.NORTH, mb);
        frame.getContentPane().add(BorderLayout.CENTER, textArea);
        frame.getContentPane().add(BorderLayout.SOUTH, panel);

        return 0;
    }

    public String receive()
    {
        String response = Utils.read(in, theirRepo.getLibraryData().libraries.key);
        textArea.append(response + "\r\n");
        return response;
    }

    public String roundTrip(String command)
    {
        send(command);
        String response = receive();
        return response;
    }

    public int run(Repository myRepo, Repository theirRepo) {
        int returnValue = 0;

        this.myRepo = myRepo;
        this.theirRepo = theirRepo;

        returnValue = build();
        if (returnValue == 0) {
            frame.setVisible(true);
            textArea.append(": ");
            commandField.grabFocus();
            commandField.requestFocus();
        }

        return returnValue;
    }

    public int send(String command)
    {
        textArea.append(command + "\r\n");
        Utils.write(out, theirRepo.getLibraryData().libraries.key, command);
        return 0;
    }

    @Override
    public void windowOpened(WindowEvent e) {
        commandField.grabFocus();
        commandField.requestFocus();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        JOptionPane.showMessageDialog(frame, "Window Closing");
//        frame.setVisible(false);
//        frame.dispose();
//        System.exit(0);
}

    @Override
    public void windowClosed(WindowEvent e) {
        JOptionPane.showMessageDialog(frame, "Window Closed");
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        commandField.grabFocus();
        commandField.requestFocus();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        commandField.grabFocus();
        commandField.requestFocus();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
