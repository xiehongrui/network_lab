package Server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.*;

import javax.imageio.ImageIO;

public class Server {
    public static void main(String[] args) {
        ThreadedReceive r = new ThreadedReceive();
        var t = new Thread(r);
        t.start();
        char c;
        while(true){
            Scanner sc = new Scanner(System.in);
            String str = "";
            str = sc.nextLine();
            if(Objects.equals(str, "q")){
                r.exit = true;
                break;
            }
        }
        System.out.println("监视结束！");
    }
}
class ThreadedReceive implements Runnable{

    public volatile boolean exit = false; // 中止标志

    final static int PORT = 8800;
    final static int HEADER_SIZE = 14;
    final static int IMAGE_START = 128;
    final static int PACKET_MAX_SIZE = 65507; // UDP报文去除首部后的最大长度
    final static int DATAGRAM_MAX_SIZE = PACKET_MAX_SIZE - HEADER_SIZE;

    public void run(){
        int currentImage = -9999;
        int fragmentsReceived = 0;
        byte[] imageData = new byte[PACKET_MAX_SIZE];
        boolean canReceive = false;
        try {
            // 打开窗口
            Display display = new Display();

            // 初始化套接字
            DatagramSocket socket = new DatagramSocket(PORT);
            long old_time = System.currentTimeMillis();
            double Len = 0.0;
            long delay = 0;
            // 等待接收客户端发送的数据
            System.out.println("服务器端已经启动，等待客户端发送数据...");
            System.out.println("输入q以结束接收");
            while (!exit) {
                byte[] buffer = new byte[PACKET_MAX_SIZE];// 用于接收客户端发送的数据

                // 等待接收
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                Len += (double) packet.getLength();
                // 从包中提取数据
                byte[] fragment = packet.getData();
                // 从数据中提取信息
                byte flag = fragment[0];
                short imageNumber = fragment[1];
                int fragments = fragment[2] & 0xff;
                short currentFragment = (short)(fragment[3] & 0xff);
                int size = (fragment[4] & 0xff) << 8 | (fragment[5] & 0xff);
                byte[] timestampByteArray = new byte[] {
                        (byte) (fragment[6] & 0xff),
                        (byte) (fragment[7] & 0xff),
                        (byte) (fragment[8] & 0xff),
                        (byte) (fragment[9] & 0xff),
                        (byte) (fragment[10] & 0xff),
                        (byte) (fragment[11] & 0xff),
                        (byte) (fragment[12] & 0xff),
                        (byte) (fragment[13] & 0xff),
                };

                // 日期信息
                Date timestamp = new Date(ByteBuffer.wrap(timestampByteArray).getLong());

                long diff = (System.currentTimeMillis() - old_time);


                // 是否收到新图片
                if(((flag & IMAGE_START) == IMAGE_START) && (imageNumber != currentImage)){
                    // 设置当前图像编号
                    currentImage = imageNumber;
                    fragmentsReceived = 0;

                    // 片段数*最大包大小 = 图像最大大小
                    imageData = new byte[fragments * DATAGRAM_MAX_SIZE];
                    canReceive = true;
                }

                if(canReceive && imageNumber == currentImage) {
//                    System.out.println(fragment.length + "  " + imageData.length);
                    System.arraycopy(fragment, HEADER_SIZE, imageData, currentFragment * DATAGRAM_MAX_SIZE, size);

                    fragmentsReceived++;
                }

                // 已接受所有图像
                if(fragmentsReceived == fragments){
                    ByteArrayInputStream bis= new ByteArrayInputStream(imageData);
                    BufferedImage image = ImageIO.read(bis);

                    old_time = System.currentTimeMillis();

                    long rec_time = timestamp.getTime();  // 发送时的时间
                    delay = (System.currentTimeMillis() - rec_time);  // 时延
                    double rate = Len/(diff*1000);  // 速率
                    Len = 0.0;
                    // 更新
                    display.updateImage(image, timestamp, delay, rate);
//                    System.out.println(imageNumber);
                    // 停止接收
                    canReceive = false;
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }
}