import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Socket socket;
    private final AtomicBoolean exit;
    private final AtomicBoolean flag;

    public TCPClient(Socket socket_){
        exit = new AtomicBoolean(false);
        flag = new AtomicBoolean(true);
        socket = socket_;
    }

    private void DisplayList(Scanner in){
        String line;
        line = in.nextLine();
        while(!line.trim().equals("")){
            System.out.println(line);
            line = in.nextLine();
        }
    }

    public void HandlerClient(){
        try(InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            var in = new Scanner(inStream, StandardCharsets.UTF_8);
            var out = new PrintWriter(
                    new OutputStreamWriter(outStream, StandardCharsets.UTF_8),
                    true)){
            ActiveConnect monitor;
            Thread t1 = null;
            while(true){
                String line;
                line = in.nextLine();
                if(line.equals("0")){  // 初始化在线列表
                    DisplayList(in);
                    flag.set(true);
                    monitor = new ActiveConnect(out);
                    t1 = new Thread(monitor);
                    t1.start();
                }else if(line.equals("1")){ // 更新在线列表
                    DisplayList(in);
                }else if(line.equalsIgnoreCase("2")){ // 请求对话成功
                    line = in.nextLine();
                    flag.set(false);
                    if(line.trim().equalsIgnoreCase("0")){
                        System.out.println("服务端已准备好与您对话(请输入Y以开始)");
                    }else{
                        System.out.println("客户端"+line+"已准备好与您对话(请输入Y以开始)");
                    }
                    assert t1 != null;
                    t1.join();
                    ConnectTalk();
                }else if(line.equalsIgnoreCase("3")){ // 其他客户端发来消息
                    line = in.nextLine();
                    flag.set(false);
                    System.out.println("客户端"+line+"准备与您对话(请输入Y以继续)");
                    assert t1 != null;
                    t1.join();
                    ConnectTalk();
                }else if (line.equalsIgnoreCase("4")){ // 显示其他信息
                    line = in.nextLine();
                    System.out.println(line);
                }else if(line.trim().equalsIgnoreCase("BYE")){ // 断开连接
                    flag.set(false);
                    System.out.println("已与服务器断开连接！(请输入Y以结束)");
                    assert t1 != null;
                    t1.join();
                    break;
                }
            }
        }
        catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    private void ConnectTalk() throws IOException, InterruptedException {
        exit.set(false);
        Runnable receive = new Receive(socket.getInputStream());
        var t1 = new Thread(receive);
        Runnable send = new Send(socket.getOutputStream());
        var t2 = new Thread(send);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    private class ActiveConnect implements Runnable{
        private final PrintWriter out;

        public  ActiveConnect(PrintWriter out_){
            out = out_;
        }

        public void run(){
            while(true){
                Scanner sc = new Scanner(System.in);
                String str;
                str = sc.nextLine();
                out.println(str);
                if(!flag.get() && (str.trim().equalsIgnoreCase("Y") || str.trim().equalsIgnoreCase("Yes"))){
                    break;
                }
            }
        }
    }

    private class Receive implements Runnable{
        private final InputStream inStream;

        public Receive(InputStream inStream_){
            inStream = inStream_;
        }

        public void run(){
            var in = new Scanner(inStream, StandardCharsets.UTF_8);
            while(!exit.get()){
                String line = in.nextLine();
                if (line.trim().equalsIgnoreCase("BYE")){
                    if(exit.get()){
                        System.out.println("已结束当前对话！");
                    }else{
                        exit.set(true);
                        System.out.println("对方结束当前对话！(输入Y以继续)...");
                    }
                    break;
                }
                System.out.println(line);
            }
        }
    }

    private class Send implements Runnable{
        private final OutputStream outStream;
        public Send(OutputStream outStream_){
            outStream = outStream_;
        }

        public void run(){
            var out = new PrintWriter(
                    new OutputStreamWriter(outStream, StandardCharsets.UTF_8),
                    true);
            while(!exit.get()){
                var sc = new Scanner(System.in);
                String str;
                str = sc.nextLine();
                if((str.trim().equalsIgnoreCase("Y")|| str.trim().equalsIgnoreCase("Yes")) && exit.get()){
                    out.println("BYE");
                    break;
                }
                if(str.trim().equalsIgnoreCase("BYE")){
                    System.out.println("中止对话请求中，请稍后...");
                    out.println("BYE");
                    exit.set(true);
                    break;
                }
                out.println(str);
            }
        }
    }
}
