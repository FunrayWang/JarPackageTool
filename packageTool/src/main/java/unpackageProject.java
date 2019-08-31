import ch.ethz.ssh2.Connection;

import java.util.ArrayList;

/**
 * This is description.
 *
 * @author wangfangrui
 * @date 2019/5/23 16:44
 */
public class unpackageProject {

    private static String hostName = "172.16.0.190";
    private static String username = "root";
    private static String password = "19921027wang";
    private static String linuxPath = "/home/wangfangrui/dnsTemp";
    private static String jarName = "dnsDomain.jar";

    public static void main(String[] args) {
        int port = 22;
        try {
            // Ftp.getSftpUtil(hostName, port, username, password);
            // Ftp.upload(linuxPath, localPath);
            // Ftp.release();
            Connection conn = getAllJars.getConnect(hostName, username, password, port);
            String line = getAllJars.fileExist(linuxPath, conn);
            String[] res = line.split(" ");
            ArrayList<String> arrayList = new ArrayList<String>();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("cd " + linuxPath + "&&");

            for (int i = 0; i < res.length; i++) {
                if (res[i].indexOf(".jar") != -1) {
                    String subStr = res[i].substring(0, res[i].indexOf(".jar") + 4);
                    stringBuffer.append("jar -xvf " + subStr);
                    stringBuffer.append("&&");
                    stringBuffer.append("rm -f " + subStr);
                    stringBuffer.append("&&");
                }
            }
            stringBuffer.append("jar -cvfM " + jarName + " .");
            executeCommand.execute(conn, stringBuffer.toString());
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
