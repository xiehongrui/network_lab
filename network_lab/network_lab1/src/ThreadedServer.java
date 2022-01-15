import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;


public class ThreadedServer {
    public static void main(String[] args){
        HashMap<SocketAddress, Integer> client_map = new HashMap<>();
        System.out.println("正在启动服务器...");
        try(var s = new ServerSocket(8189)){
            int i = 1;

            System.out.println("服务器启动完毕！");

            while(true){
                System.out.println("等待客户端连接...");
                Socket incoming = s.accept();

                var add= incoming.getRemoteSocketAddress();
                var ip_add = incoming.getInetAddress();
                var port_add = incoming.getPort();

                if(client_map.containsKey(add)) continue;
                client_map.put(add, i);

                System.out.println("客户端" + i + "已连接,IP地址:" + ip_add + " 端口号:" + port_add);
                System.out.println("当前在线设备数: "+client_map.size());

                Runnable r = new ThreadedHandler(incoming, client_map);
                var t = new Thread(r);
                t.start();

                i++;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}

class ThreadedHandler implements Runnable {
    private final Socket incoming;
    private final HashMap<SocketAddress, Integer> client_map0;

    public ThreadedHandler(Socket incomingSocket, HashMap<SocketAddress, Integer> client_map0_) {
        incoming = incomingSocket;
        client_map0 = client_map0_;
    }

    public void run() {
        try (InputStream inStream = incoming.getInputStream();
             OutputStream outStream = incoming.getOutputStream();
             var in = new Scanner(inStream, StandardCharsets.UTF_8);
             var out = new PrintWriter(
                     new OutputStreamWriter(outStream, StandardCharsets.UTF_8),
                     true)) {
            int clientID = client_map0.get(incoming.getRemoteSocketAddress());

//            while (true) {
//                String line = in.nextLine();
//                System.out.println("客户端" + clientID + ": " + line);
//
//                if (line.trim().equalsIgnoreCase("HELLO")) {
//                    out.println("你好，输入BYE以退出");
//                    continue;
//                }
//
//                if (line.trim().equalsIgnoreCase("BYE")) {
//                    out.println("BYE");
//                    client_map0.remove(incoming.getRemoteSocketAddress());
//                    System.out.println("客户端" + clientID + "断开连接!");
//                    System.out.println("当前在线设备数: " + client_map0.size());
//                    break;
//                }
//
//                out.println("Echo: " + line);
//            }

            while (true) {
                String line;
                line = in.nextLine();
                boolean flag = false;
                while(!line.trim().equals("")){
                    System.out.println("客户端" + clientID + ": " + line);
                    if (line.trim().equalsIgnoreCase("BYE")) {
                        flag = true;
                        out.println("BYE");
                        client_map0.remove(incoming.getRemoteSocketAddress());
                        System.out.println("客户端" + clientID + "断开连接!");
                        System.out.println("当前在线设备数: " + client_map0.size());
                        break;
                    }
                    line = in.nextLine();
                }
                if(flag) break;
                System.out.println("向客户端"+clientID+"发送消息:");
                Scanner sc = new Scanner(System.in);
                String str;
                do {
                    str = sc.nextLine();
                    out.println(str);
                } while (!str.trim().equals(""));
                System.out.println("消息已发送，等待客户端回应...");
            }
        } catch (IOException e) {
            client_map0.remove(incoming.getRemoteSocketAddress());
            e.printStackTrace();
        }
    }
}
