package mitdown;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private String pageURL = "http://ocw.mit.edu";

    public static void main(String[] args) throws IOException {
        Main m = new Main();
        List<Course> courses = m.getCourses();
        new Thread(new Downer(courses)).start();
    }

    private Course getCourse(Element e) throws IOException {
        Course c = new Course();
        Elements es = e.getElementsByTag("a");
        String id = es.get(0).html();
        String url = pageURL + es.get(0).attr("href");
        String name = es.get(1).html();
        c.setId(id);
        c.setName(name);
        c.setUrl(url);
        Document doc = Jsoup.connect(url).get();
        Iterator<Element> fIter = doc.getElementsByClass("specialfeatures").first().getElementsByTag("li").iterator();
        List<Feature> features = new ArrayList<>();
        while (fIter.hasNext()) {
            Element element = fIter.next();
            Element details = element.getElementsByTag("a").first();
            Feature f = new Feature();
            f.setName(details.html());
            f.setUrl(pageURL + details.attr("href"));
            features.add(f);
        }
        c.setFeatures(features);
        if (c.hasVideo()) {
            String videoURL = c.getVideoUrl();
            Document videoDoc = Jsoup.connect(c.getVideoUrl()).get();
            if (videoURL.contains("videos") || videoURL.contains("video")) {
                Iterator<Element> mIter = videoDoc.getElementsByClass("medialisting").iterator();
                List<Video> videos = new ArrayList<>();
                while (mIter.hasNext()) {
                    Element element = mIter.next();
                    Element link = element.getElementsByTag("a").first();
                    Video video = new Video();
                    String downPage = pageURL + link.attr("href");
                    String title = link.attr("title");
                    String downURL = getDownUrl(downPage, false);
                    video.setName(title);
                    video.setUrl(downURL);
                    videos.add(video);
                }
                c.setVideos(videos);
                c.setBatch(false);
            } else {
                String downURL = getDownUrl(videoURL, true);
                assert downURL != null;
                c.setDownurl(downURL.substring(0, downURL.lastIndexOf("/")));
                c.setBatch(true);
            }

        }
        return c;
    }

    private String getDownUrl(String downpage, boolean isbatcth) {

        Document videoDoc;
        try {
            videoDoc = Jsoup.connect(downpage).get();
            if (isbatcth) {
                String s = videoDoc.html();
                Pattern p = Pattern.compile("http://www.archive.org/[/.-_\\w]*.mp4|http://archive.org/[/.-_\\w]*.mp4");
                Matcher m = p.matcher(s);
                if (m.find()) {
                    return m.group(0);
                }
            } else {
                Element e = videoDoc.getElementsContainingOwnText("Internet Archive").first();
                return e.attr("href");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Course> getCourses() throws IOException {
        List<Course> courses = new ArrayList<>();
        File input = new File("urls.html");
        Document doc = Jsoup.parse(input, "UTF-8");
        Element courseList = doc.getElementById("courseList");
        for (Element e : courseList.getElementsByTag("tr")) {
            if (isCourse(e)) {
                Course c = getCourse(e);
                courses.add(c);
            }
        }
        return courses;
    }

    private static class Downer implements Runnable {
        List<Course> courses;
        private String downHome = "download/";

        public Downer(List<Course> courses) {
            this.courses = courses;
        }

        @Override
        public void run() {
            try {
                download();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void download() throws IOException {
            Iterator<Course> cit = courses.iterator();
            ExecutorService exs=Executors.newFixedThreadPool(100);
            int c = 1;
            while (cit.hasNext()) {
                Course course = cit.next();
                File details = new File(downHome + course.getName() + c + "/" + course.getName() + ".html");
                if (course.isBatch()) {
                    System.out.println(course.getDownurl());
                    Document doc = Jsoup.connect(course.getDownurl()).get();
                    Elements els = doc.getElementsByTag("a");
                    int i = 1;
                    for (Element e : els) {
                        if ("..".equals(e.html()))
                            continue;
                        String downURL = course.getDownurl() + '/' + e.attr("href");
                        URL source = new URL(downURL);
                        File destination;
                        if (downURL.contains(".mp4") || downURL.contains(".srt")) {
                            if (downURL.contains(".mp4")) {
                                destination = new File(
                                        downHome + course.getName() + c + "/videos/lecture: " + i + ".mp4");
                                if (destination.exists()) {
                                    continue;
                                }
                                try {
                                    FileUtils.copyURLToFile(source, destination);
                                    System.out.println(
                                            "download " + destination.getName() + c + " from " + source.getFile());
                                } catch (Exception e2) {
                                    System.out.println("fail download " + destination.getName() + c + " from "
                                            + source.getFile());
                                }

                            } else {
                                destination = new File(
                                        downHome + course.getName() + c + "/" + "subtitles/lecture: " + i + ".srt");
                                if (!destination.exists()) {
                                    try {
                                        FileUtils.copyURLToFile(source, destination);
                                        System.out.println(
                                                "download " + destination.getName() + " from " + source.getFile());
                                    } catch (Exception e2) {
                                        System.out.println("fail download " + destination.getName() + c + " from "
                                                + source.getFile());
                                    }
                                }
                                i++;
                            }
                        }
                    }
                } else {
                    List<Video> videos = course.getVideos();
                    if (videos == null)
                        continue;
                    for (Video video : videos) {
                        try {
                            String vURL = video.getUrl();
                            System.out.println(vURL);
                            String srtURL = vURL.substring(0, vURL.lastIndexOf(".")) + ".srt";
                            URL vSource = new URL(vURL);
                            File vDestination = new File(
                                    downHome + course.getName() + c + "/" + "videos/" + video.getName() + ".mp4");
                            URL srtSource = new URL(srtURL);
                            File srtDestination = new File(
                                    downHome + course.getName() + c + "/" + "subtitles/" + video.getName() + ".srt");
                            if (!vDestination.exists()) {
                                exs.submit(new OneDown(vSource, vDestination));

                            }
                            if (!srtDestination.exists()) {
                                exs.submit(new OneDown(srtSource, srtDestination));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                if(!details.exists())
                    FileUtils.write(details, course.toHtml());
                c++;
            }

        }

    }

    private static class OneDown implements Runnable{
        private URL source;
        private File destination;
        public OneDown(URL source, File destination) {
            this.source=source;
            this.destination=destination;
        }
        @Override
        public void run() {
            try {
                FileUtils.copyURLToFile(source, destination);
                System.out.println("download " + destination.getName() + " from " + source.getFile());
            } catch (IOException e) {
                System.out.println("fail download " + destination.getName() + " from " + source.getFile());
                e.printStackTrace();
            }
        }

    }

    private boolean isCourse(Element e) {
        return e.getElementsByTag("td").size() == 3 && e.getElementsByTag("a").size() == 3;
    }

}
