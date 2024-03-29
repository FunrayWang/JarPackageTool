/**
 * This is description.
 *
 * @author wangfangrui
 * @date 2019/5/21 15:43
 */

import ch.ethz.ssh2.SCPClient;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class Ftp {

    //打印log日志
    // private static final Log logger = LogFactory.getLog(Ftp.class);

    private static Date last_push_date = null;

    private Session sshSession;

    private static ChannelSftp channel;

    private static ThreadLocal<Ftp> sftpLocal = new ThreadLocal<Ftp>();

    private Ftp(String host, int port, String username, String password) throws Exception {
        JSch jsch = new JSch();
        jsch.getSession(username, host, port);
        //根据用户名，密码，端口号获取session
        sshSession = jsch.getSession(username, host, port);
        sshSession.setPassword(password);
        //修改服务器/etc/ssh/sshd_config 中 GSSAPIAuthentication的值yes为no，解决用户不能远程登录
        sshSession.setConfig("userauth.gssapi-with-mic", "no");

        //为session对象设置properties,第一次访问服务器时不用输入yes
        sshSession.setConfig("StrictHostKeyChecking", "no");
        sshSession.connect();
        //获取sftp通道
        channel = (ChannelSftp) sshSession.openChannel("sftp");
        channel.connect();
        System.out.println("连接ftp成功!" + sshSession);
    }

    /**
     * 是否已连接
     *
     * @return
     */
    private boolean isConnected() {
        return null != channel && channel.isConnected();
    }

    /**
     * 获取本地线程存储的sftp客户端
     *
     * @return
     * @throws Exception
     */
    public static Ftp getSftpUtil(String host, int port, String username, String password) throws Exception {
        //获取本地线程
        Ftp sftpUtil = sftpLocal.get();
        if (null == sftpUtil || !sftpUtil.isConnected()) {
            //将新连接防止本地线程，实现并发处理
            sftpLocal.set(new Ftp(host, port, username, password));
        }
        return sftpLocal.get();
    }

    /**
     * 释放本地线程存储的sftp客户端
     */
    public static void release() {
        if (null != sftpLocal.get()) {
            sftpLocal.get().closeChannel();
            System.out.println("关闭连接" + sftpLocal.get().sshSession);
            sftpLocal.set(null);

        }
    }

    /**
     * 关闭通道
     *
     * @throws Exception
     */
    public void closeChannel() {
        if (null != channel) {
            try {
                channel.disconnect();
            } catch (Exception e) {
                System.out.println("关闭SFTP通道发生异常:" + e);
            }
        }
        if (null != sshSession) {
            try {
                sshSession.disconnect();
            } catch (Exception e) {
                System.out.println("SFTP关闭 session异常:" + e);
            }
        }
    }

    /**
     * @param directory  上传ftp的目录
     * @param uploadFile 本地文件目录
     */
    public static void upload(String directory, String uploadFile) throws Exception {
        try {        //执行列表展示ls 命令
            channel.ls(directory);        //执行盘符切换cd 命令
            channel.cd(directory);
            List<File> files = getFiles(uploadFile, new ArrayList<File>());
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                InputStream input = new BufferedInputStream(new FileInputStream(file));
                channel.put(input, file.getName());
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(file.getName() + "关闭文件时.....异常!" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("【子目录创建中】：" + e);
            //创建子目录
            channel.mkdir(directory);
            upload(directory,uploadFile);
        }

    }

    //获取文件
    public static List<File> getFiles(String realpath, List<File> files) {
        File realFile = new File(realpath);
        if (realFile.isDirectory()) {
            File[] subfiles = realFile.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    if (null == last_push_date) {
                        return true;
                    } else {
                        long modifyDate = file.lastModified();
                        return modifyDate > last_push_date.getTime();
                    }
                }
            });
            for (File file : subfiles) {
                if (file.isDirectory()) {
                    getFiles(file.getAbsolutePath(), files);
                } else {
                    files.add(file);
                }
                if (null == last_push_date) {
                    last_push_date = new Date(file.lastModified());
                } else {
                    long modifyDate = file.lastModified();
                    if (modifyDate > last_push_date.getTime()) {
                        last_push_date = new Date(modifyDate);
                    }
                }
            }
        }
        return files;
    }

    public static void main(String args[]){
        try {
            getSftpUtil("192.168.57.129",22,"root","19921027wang");
            upload("/home/wangfangrui/deploy","D:/work/工作文档/tempForTest");
            release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
