package yakoo5.noveldownloader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * http://m.bookxml.com 香满路言情小说下载器
 *
 * @author yakoo5
 * @date 2016/4/24.
 * @since 1.0
 */
public class BookXmlNovelDownloader {

    public static void main(String[] args) throws IOException {
        String novelCharacterListPageUrl = "http://m.bookxml.com/read/32574.html";
        String downloadPath = "c:/豪门贵妻_冷心帝少宠妻无度";
        // 起始章节
        int start = 10;
        // 结束章节
        int end = 21;
        // 每下载完一个章节当前任务线程休眠500ms，避免服务器屏蔽
        final int period = 500;
        // 启动下载
        download(novelCharacterListPageUrl, downloadPath, start, end, period);
    }

    /**
     * 下载小说章节
     *
     * @param novelCharacterListPageUrl 小说章节列表页URL地址
     * @param downloadPath              下载路径
     * @param start                     起始章节
     * @param end                       结束章节
     * @param period                    每批任务下载间隔时间(ms)
     */
    protected static void download(String novelCharacterListPageUrl, final String downloadPath, int start, int end, final int period) {
        Document doc = null;
        try {
            doc = doGet(novelCharacterListPageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Elements elements = doc.select(".list-group a");

        if (null == elements || elements.isEmpty()) {
            System.out.println("no character found");
        }

        // 如果目录路径不存在，则创建
        File filePath = new File(downloadPath);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }

        Element[] elems = elements.toArray(new Element[elements.size()]);

        if (start < 1) {
            start = 1;
        }

        if (end > elems.length) {
            end = elems.length;
        }

        // 小说下载线程池
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        // 倒计数闭锁：用于任务结束后通知关闭线程池
        final CountDownLatch countDownLatch = new CountDownLatch(end - start + 1);

        String serverUrl = "http://m.bookxml.com";

        for (int i = start - 1; i < end; i++) {
            String link = elems[i].attr("href");
            final String title = elems[i].attr("title");
            final String url = serverUrl + link;
            final String filepath = downloadPath + File.separator + title + ".txt";
            // 提交下载任务
            executor.execute(new Runnable() {
                @Override public void run() {
                    System.out.println("downloading \"" + title + "\" from \"" + url + "\" to \"" + filepath + "\"");
                    try {
                        saveNovelCharacter(title, url, filepath);
                    } catch (IOException e) {
                        System.err.println("failed to download \"" + title + "\" from \"" + url + "\" to \"" + filepath + "\"");
                    }
                    try {
                        // System.out.println("sleep for " + period + " ms");
                        Thread.sleep(period);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    countDownLatch.countDown();
                }
            });
        }

        // 关闭线程池
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取网页内容
     *
     * @param url
     * @return
     * @throws IOException
     */
    protected static Document doGet(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0")
                .timeout(5000)
                .get();
    }

    /**
     * 保存小说章节
     *
     * @param title    章节标题
     * @param url      章节URL
     * @param filepath 保存路径
     * @throws IOException
     */
    protected static void saveNovelCharacter(String title, String url, String filepath) throws IOException {
        // 获取网页内容
        Document doc = doGet(url);
        // 从网页中提取小说章节内容
        String content = preProcess(doc.getElementById("content").html());
        // 保存到文件
        IOUtils.write(content, new FileOutputStream(filepath));
    }

    /**
     * HTML内容预处理
     * <p>
     * <li>HTML换行处理</li>
     * <li>HTML空格处理</li>
     * </p>
     *
     * @param html
     * @return
     */
    protected static String preProcess(String html) {
        return StringUtils.isNotBlank(html) ?
                html.replaceAll("<br\\s*/>", StringUtils.EMPTY).replaceAll("<br\\s*>", StringUtils.EMPTY).replaceAll("&nbsp;", " ") :
                html;
    }

}
