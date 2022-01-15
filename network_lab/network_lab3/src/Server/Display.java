package Server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Display {
    JFrame frame;
    JLabel labelImage;
    JLabel labelTime;
    JLabel labelDelay;
    JLabel labelRate;
    int width = 960;
    int height = 540;

    public Display() throws IOException
    {
        labelImage = new JLabel();
        labelTime = new JLabel();
        labelDelay =  new JLabel();
        labelRate = new JLabel();
        frame = new JFrame("Camera Monitor");
        frame.setSize(width,height);


        // Label style
        labelTime.setFont(labelTime.getFont().deriveFont(Font.BOLD, 12));
        labelTime.setForeground(Color.WHITE);
        labelTime.setHorizontalAlignment(JLabel.CENTER);
        labelTime.setVerticalAlignment(JLabel.BOTTOM);


        // Add label to image
        labelImage.setLayout(new BorderLayout());
        labelImage.add(labelTime);

        frame.add(labelImage);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void updateImage(BufferedImage image, Date timestamp, long delay, double rate) {
        String str1  = rate+"";
        ImageIcon icon = new ImageIcon(image);
        try{
            BigDecimal b = new BigDecimal(rate);
            str1 =String.valueOf(b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        }catch (Exception e){
        }
        String s1 = "延迟为: "+ delay+"ms";
        String s2 = "接收速率为: " + str1 + "MB/s";
        String s3 = timestamp.toString();
        labelTime.setText( "<html>"+s1+"<br />"+s2+"<br />"+s3+"</html>");
        labelImage.setIcon(icon);
        frame.pack();
    }
}