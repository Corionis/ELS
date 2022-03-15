package com.groksoft.els.gui.util;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

// From http://www.java2s.com/Code/Java/Swing-Components/VerticalLabelUI.htm
public class VerticalLabel extends BasicLabelUI
{
    private static final Rectangle paintIconR = new Rectangle();
    private static final Rectangle paintTextR = new Rectangle();
    private static Insets paintViewInsets = new Insets(0, 0, 0, 0);
    private static final Rectangle paintViewR = new Rectangle();

    static
    {
        labelUI = new VerticalLabel(false);
    }

    protected boolean clockwise;

    public VerticalLabel(boolean clockwise)
    {
        super();
        this.clockwise = clockwise;
    }

    public Dimension getPreferredSize(JComponent c)
    {
        Dimension dim = super.getPreferredSize(c);
        return new Dimension(dim.height, dim.width);
    }

    public void paint(Graphics g, JComponent c)
    {
        JLabel label = (JLabel) c;
        String text = label.getText();
        Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

        if ((icon == null) && (text == null))
        {
            return;
        }

        FontMetrics fm = g.getFontMetrics();
        paintViewInsets = c.getInsets(paintViewInsets);

        paintViewR.x = paintViewInsets.left;
        paintViewR.y = paintViewInsets.top;

        // invert height & width
        paintViewR.height = c.getWidth() - (paintViewInsets.left + paintViewInsets.right);
        paintViewR.width = c.getHeight() - (paintViewInsets.top + paintViewInsets.bottom);

        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

        String clippedText = layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform tr = g2.getTransform();
        if (clockwise)
        {
            g2.rotate(Math.PI / 2);
            g2.translate(0, -c.getWidth());
        }
        else
        {
            g2.rotate(-Math.PI / 2);
            g2.translate(-c.getHeight(), 0);
        }

        if (icon != null)
        {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
        }

        if (text != null)
        {
            int textX = paintTextR.x;
            int textY = paintTextR.y + fm.getAscent();

            if (label.isEnabled())
                paintEnabledText(label, g, clippedText, textX, textY);
            else
                paintDisabledText(label, g, clippedText, textX, textY);
        }
        g2.setTransform(tr);
    }

}
