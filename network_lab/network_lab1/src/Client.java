import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

public class Client {
    public static void main(String[] arg) {
        System.out.println("正在连接服务端...");
        try(Socket socket = new Socket("127.0.0.1",8189)) {
            System.out.println("已连接服务端！");
            TCPClient client = new TCPClient(socket);
            client.HandlerClient();
            System.out.println("客户端已关闭！");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
class TCPClient{
    private Socket socket;

    public TCPClient(Socket socket_){
        socket = socket_;
    }
    public void HandlerClient(){
        BufferedReader br = null;
        try(InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            var in = new Scanner(inStream, StandardCharsets.UTF_8);
            var out = new PrintWriter(
                    new OutputStreamWriter(outStream, StandardCharsets.UTF_8),
                    true)){
//            while(true){
//                System.out.print("向服务器发送消息: ");
//
//                Scanner sc = new Scanner(System.in);
//                String str = "";
//                str = sc.nextLine();
//                out.println(str);
//
//                String line = in.nextLine();
//                System.out.println("服务器: " + line);
//
//                if(line.trim().equalsIgnoreCase("BYE")){
//                    System.out.println("已与服务器断开连接！ ");
//                    break;
//                }
//            }
            while(true){
                boolean flag = false;
                System.out.println("向服务器发送消息: ");
                Scanner sc = new Scanner(System.in);
                String str = "";
                do {
                    str = sc.nextLine();
                    out.println(str);
                } while (!str.trim().equals("") && !str.trim().equalsIgnoreCase("bye"));
                System.out.println("消息已发送，等待服务器回应...");
                String line;
                line = in.nextLine();
                while(!line.trim().equals("")){
                    System.out.println("服务器: " + line);
                    if(line.trim().equalsIgnoreCase("BYE")){
                        flag = true;
                        System.out.println("已与服务器断开连接！ ");
                        break;
                    }
                    line = in.nextLine();
                }
                if(flag) break;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
