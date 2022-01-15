import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadedServer {
    public static void main(String[] args){
        HashMap<SocketAddress, Integer> client_map0 = new HashMap<>();
        HashMap<Integer, Socket> client_map1 = new HashMap<>();
        HashMap<Integer, Scanner> client_in = new HashMap<>();
        HashMap<Integer, PrintWriter> client_out = new HashMap<>();
        HashMap<Integer, ThreadedHandler> thread_map = new HashMap<>();
        HashSet<Integer> busy_map =  new HashSet<>();
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

                if(client_map0.containsKey(add)) continue;
                client_map0.put(add, i);
                client_map1.put(i, incoming);
                System.out.println("客户端" + i + "已连接,IP地址:" + ip_add + " 端口号:" + port_add);
                System.out.println("当前在线设备数: "+client_map0.size());
                InputStream inStream = incoming.getInputStream();
                OutputStream outStream = incoming.getOutputStream();
                var in = new Scanner(inStream, StandardCharsets.UTF_8);
                var out = new PrintWriter(
                        new OutputStreamWriter(outStream, StandardCharsets.UTF_8),
                        true);
                client_in.put(i, in);
                client_out.put(i, out);
                ThreadedHandler r = new ThreadedHandler(incoming, client_map0, client_map1, busy_map, client_in, client_out, thread_map);
                var t = new Thread(r);
                thread_map.put(i, r);
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
    private final HashMap<SocketAddress, Integer> client_map0;  // 地址 to ID
    private final HashMap<Integer, Socket> client_map1;  // ID to 套接字
    private final HashMap<Integer, ThreadedHandler> thread_map;  // ID to 线程
    private final HashSet<Integer> busy_map;  // 忙碌客户
    private final int ClientID;
    private final AtomicBoolean exit;  // 原子量，协调send和receive
    private final HashMap<Integer, Scanner> client_in;  // ID to 输入流
    private final HashMap<Integer, PrintWriter> client_out ;  // ID to 输出流
    private final Scanner in;  // ThreadedHandler所服务的客户的输入流
    private final PrintWriter out;  // ThreadedHandler所服务的客户的输出流
    private final AtomicBoolean CanTalk;  // 原子量，协调请求对话客户和被请求对话客户

    public ThreadedHandler(Socket incomingSocket, HashMap<SocketAddress, Integer> client_map0_, HashMap<Integer, Socket> client_map1_,
                           HashSet<Integer> busy_map_, HashMap<Integer, Scanner> client_in_, HashMap<Integer, PrintWriter> client_out_,
                           HashMap<Integer, ThreadedHandler> thread_map_) {
        exit = new AtomicBoolean(false);
        CanTalk = new AtomicBoolean(false);
        incoming = incomingSocket;
        client_map0 = client_map0_;
        client_map1 = client_map1_;
        busy_map = busy_map_;
        client_in = client_in_;
        client_out = client_out_;
        thread_map = thread_map_;
        ClientID = client_map0.get(incoming.getRemoteSocketAddress());
        in = client_in.get(ClientID);
        out = client_out.get(ClientID);
    }
    public void run() {
        try {
            boolean disconnect = false;
            while (!disconnect) {
                while(busy_map.contains(ClientID)){
                    CanTalk.set(true);
                }
                CanTalk.set(false);
                out.println("0");
                SendList();
                String line;
                while(true){
                    line = in.nextLine();
                    if (line.trim().equals("-1")) { // 结束
                        out.println("BYE");
                        client_map0.remove(incoming.getRemoteSocketAddress());
                        client_map1.remove(ClientID);
                        System.out.println("客户端" + ClientID + "断开连接!");
                        System.out.println("当前在线设备数: " + client_map0.size());
                        disconnect = true;
                        break;
                    } else if (line.trim().equals("-2")) { // 更新
                        out.println("1");
                        SendList();
                        continue;
                    } else if(line.trim().equals("Y") && busy_map.contains(ClientID)){
                        break;
                    }
                    int num = test(line, out, ClientID);
                    if (num > 0) {
                        System.out.println("客户端" + ClientID + "开启了与" + "客户端" + num + "的对话！");
                        client_out.get(num).println("3");
                        client_out.get(num).println(ClientID);
                        client_out.get(ClientID).println("2");
                        client_out.get(ClientID).println(num);
                        in.nextLine();
                        while(!thread_map.get(num).CanTalk.get()){}
                        client_out.get(ClientID).println("可以开始对话！");
                        client_out.get(num).println("可以开始对话！");
                        Runnable receive = new Receive_Send(in, client_out.get(num), ClientID);
                        var t1 = new Thread(receive);
                        Runnable send = new Receive_Send(client_in.get(num), out, num);
                        var t2 = new Thread(send);
                        exit.set(false);
                        t1.start();
                        t2.start();
                        t1.join();
                        t2.join();
                        busy_map.remove(num);
                        busy_map.remove(ClientID);
                        System.out.println("客户端" + ClientID + "结束当前对话！");
                        break;
                    } else if (num == 0) {
                        client_out.get(ClientID).println("2");
                        client_out.get(ClientID).println(num);
                        System.out.println("客户端" + ClientID + "开启了与您的对话！");
                        in.nextLine();
                        System.out.println("可以开始对话！");
                        client_out.get(ClientID).println("可以开始对话！");
                        Runnable receive = new Server_Receive(in, ClientID);
                        var t1 = new Thread(receive);
                        Runnable send = new Server_Send(out);
                        var t2 = new Thread(send);
                        exit.set(false);
                        t1.start();
                        t2.start();
                        t1.join();
                        t2.join();
                        busy_map.remove(num);
                        busy_map.remove(ClientID);
                        System.out.println("客户端" + ClientID + "结束当前对话！");
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            client_map0.remove(incoming.getRemoteSocketAddress());
            e.printStackTrace();
        }
    }

    private void SendList() {
        out.println("服务器：请在在线设备中选择一个进行通话(输入-1以结束连接,输入-2以刷新):");
        out.println("[0] 服务器");
        for (var c : client_map0.keySet()) {
            if (client_map0.get(c) == ClientID) continue;
            out.println("[" + client_map0.get(c) + "] " + c);
        }
        out.println("");
    }

    private synchronized int test(String line, PrintWriter out, int ClientId) {
        int num;
        int res = -1;
        try {
            num = Integer.parseInt(line);
            if (busy_map.contains(num)) {
                out.println("4");
                out.println("对方正忙，请稍后！");
            } else if ((client_map1.containsKey(num) || num == 0) && num != ClientId) {
                busy_map.add(num);
                busy_map.add(ClientID);
                res = num;
            } else {
                out.println("4");
                out.println("查无此ID，请重新输入！");
            }
        } catch (NumberFormatException e) {
            out.println("4");
            out.println("输入格式错误，请输入对应ID！");
        }
        return res;
    }

    private class Server_Receive implements Runnable{
        Scanner in;
        int ClientID;

        public Server_Receive(Scanner in_, int ClientID_){
            in = in_;
            ClientID = ClientID_;
        }

        public void run(){
            while(!exit.get()){
                String line = in.nextLine();
                if (line.trim().equalsIgnoreCase("BYE")){
                    if(exit.get()){
                        System.out.println("已结束当前对话！");
                    }else{
                        exit.set(true);
                        System.out.println("客户端"+ClientID+"结束当前对话！(输入Y以继续)...");
                    }
                    break;
                }
                System.out.println("客户端"+ClientID+": "+line);
            }
        }
    }

    private class Server_Send implements Runnable{
        private final PrintWriter out;

        public Server_Send(PrintWriter out_){
            out = out_;
        }

        public void run(){
            while(!exit.get()){
                var sc = new Scanner(System.in);
                String str;
                str = sc.nextLine();
                if((str.trim().equalsIgnoreCase("Y")|| str.trim().equalsIgnoreCase("Yes")) && exit.get()){
                    out.println("BYE");
                    break;
                }
                if(str.trim().equalsIgnoreCase("BYE")){
                    out.println("BYE");
                    exit.set(true);
                    break;
                }
                out.println("服务器: "+str);
            }
        }
    }

    private class Receive_Send implements Runnable {
        private final Scanner in;
        private final PrintWriter out;
        private final int ClientId;

        public Receive_Send(Scanner in_, PrintWriter out_, int ClientId_) {
            in = in_;
            out = out_;
            ClientId = ClientId_;
        }

        public void run() {
            while (!exit.get()) {
                String line = in.nextLine();
                if (line.trim().equalsIgnoreCase("BYE") ) {
                    out.println("BYE");
                    exit.set(true);
                    break;
                }
                out.println("客户端" + ClientId + ": " + line);
            }
        }
    }
}
