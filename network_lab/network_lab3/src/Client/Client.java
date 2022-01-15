
package Client;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Client {
    // 参数
    final static int PORTA = 8800;  // 目的端口号
    final static String HOST = "127.0.0.1";  // 目的IP地址
    final static int HEADER_SIZE = 14;  // UDP数据部分的额外首部大小
    final static int IMAGE_START = 128;  // 开始标志即0x80，在byte[]中占一个byte
    final static int PACKET_MAX_SIZE = 65507;  // UDP报文去除首部后的最大长度
    final static int DATAGRAM_MAX_SIZE = PACKET_MAX_SIZE - HEADER_SIZE;  // 一个UDP报文内图像数据的最大长度
    final static double SCALE = 1;  // 大小压缩率
    private static DatagramSocket socket;
    private static InetAddress inet;

    public static void main(String[] args)  {
        byte imageNumber = 0;

        // 初始化摄像头
        Webcam cam = Webcam.getDefault();

        cam.setViewSize(WebcamResolution.VGA.getSize());
//        Dimension[] nonStandardResolutions = new Dimension[] {
//                WebcamResolution.PAL.getSize(),
//                WebcamResolution.HD720.getSize(),
//                new Dimension(1000, 500),
//                new Dimension(800, 400),
//        };
//        cam.setCustomViewSizes(nonStandardResolutions);
//        cam.setViewSize(WebcamResolution.HD720.getSize());

        cam.open();
        System.out.println("开始发送！");
        //初始化套接字
        try{
            socket = new DatagramSocket();
            inet = InetAddress.getByName(HOST);
        }catch (Exception e){
            e.printStackTrace();
        }

        while(true) {
            try {
                BufferedImage image;

                // 摄像机图像捕获与压缩
//                BufferedImage image = cam.getImage();
                BufferedImage image0 = cam.getImage();
                int width = image0.getWidth();
                int height = image0.getHeight();
                width = (int) (width * SCALE);
                height = (int) (height * SCALE);
                image= new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                image.getGraphics().drawImage(image0, 0, 0, width, height, null);

                // 使用jpg格式将图像转换为字节流
                ByteArrayOutputStream imageByteArrayOS = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", imageByteArrayOS);

                // 转字节数组
                byte[] imageByteArray = imageByteArrayOS.toByteArray();

                // 计算需要发送的包数
                int fragments = (int) Math.ceil(imageByteArray.length / (float)DATAGRAM_MAX_SIZE);
                System.out.print("图像总大小: " + imageByteArray.length + "B 分片数: " + fragments + " 每片大小为:");

                //时间信息转化成8字节
                long timestamp = System.currentTimeMillis();
                byte[] timestampByteArray = ByteBuffer.allocate(8).putLong(timestamp).array();
                // 发送每一分片
                for(int i = 0; i < fragments; i++){

                    // 新图片开始标志
                    int flags = i == 0 ? IMAGE_START : 0;

                    // 计算包的大小
                    int size = (i + 1) < fragments ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE;

                    // 为该分片创建适当大小的字节数组
                    byte[] fragment = new byte[HEADER_SIZE + size];
                    System.out.print(fragment.length+"B ");


                    // 首部数据处理
                    fragment[0] = (byte)flags;						// 开始标志
                    fragment[1] = imageNumber;			  	// 同一图像片段计数
                    fragment[2] = (byte)fragments;					// 片数
                    fragment[3] = (byte)i;							// 当前片段
                    fragment[4] = (byte)(size >> 8);				// 需要2个字节来表示当前大小
                    fragment[5] = (byte)size;
                    fragment[6] = timestampByteArray[0];		    // 需要8个字节来表示时间戳
                    fragment[7] = timestampByteArray[1];
                    fragment[8] = timestampByteArray[2];
                    fragment[9] = timestampByteArray[3];
                    fragment[10] = timestampByteArray[4];
                    fragment[11] = timestampByteArray[5];
                    fragment[12] = timestampByteArray[6];
                    fragment[13] = timestampByteArray[7];

                    // 复制图像的图像部分以及首部
                    System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, fragment, HEADER_SIZE, size);

                    // 创建要发送的包
                    DatagramPacket packet = new DatagramPacket(fragment, fragment.length, inet, PORTA);
                    // 发送数据
                    socket.send(packet);
                }

                // 计数
                imageNumber++;
                System.out.println();
//                Thread.sleep(500);
            } catch (IndexOutOfBoundsException | ArrayStoreException | NullPointerException
                    | IllegalArgumentException | IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
