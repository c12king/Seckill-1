package com.seckill.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 简易的工具类,执行一些java 命令
 *
 * @Author idler [idler41@163.com]
 * @Date 16/8/15 下午3:02.
 */
public class BashUtils {

    public static final String PID;

    public static final String SRC_PATH;

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        PID = name.split("@")[0];
        SRC_PATH = System.getProperty("user.dir");
    }

    public static void executeJstack(String fileName) {
        InputStream is = null;
        try {
            String threadName = Thread.currentThread().getName();

            Runtime runtime = Runtime.getRuntime();

            Process p = runtime.exec("jstack " + PID);

            is = p.getInputStream();
            int data;
            StringBuffer strBuffer = new StringBuffer();

            Date now = new Date();
            strBuffer.append("=======================" + threadName + "唤醒,dump start " + sdf.format(now) + " =======================");
            strBuffer.append(System.getProperty("line.separator", "\n")); //换行

            while ((data = is.read()) != -1) {
                strBuffer.append((char) data);
            }
            strBuffer.append("=======================  " + threadName + "唤醒,dump end  " + sdf.format(now) + "  =======================");
            strBuffer.append(System.getProperty("line.separator", "\n")); //换行
//            System.out.println("命令:\n" + CMD_JSTACK);
//            System.out.println("结果:\n" + p.exitValue());
//            System.out.println("log:\n" + strBuffer.toString());

//            int ret = p.exitValue(); // 全路径
//            System.exit(ret); // 直接返回shell执行的结果

            String fullName = SRC_PATH + File.separator + "logs" + File.separator + fileName;

            System.out.println("线程" + threadName + "唤醒" + ",执行命令：" + "jstack " + PID + " >> " + fullName);

            FileWriter fw = new FileWriter(fullName, true);
            fw.write(strBuffer.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void executeJmap() {
        StringBuffer strBuffer = new StringBuffer();
        Runtime runtime = Runtime.getRuntime();
        InputStream is = null;
        try {
            Process p = runtime.exec("jmap  -heap " + PID);
            is = p.getInputStream();

            int data;
            while ((data = is.read()) != -1) {
                strBuffer.append((char) data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(strBuffer.toString());
    }

    public static void main(String[] args) throws IOException {
//        FileWriter fw = new FileWriter(SRC_PATH + File.separator + "jstack"
//                + File.separator  + "123.txt", true);
//        fw.write("123");
//        fw.close();
    }
}
