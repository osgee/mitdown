package mitdown;

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

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

	private String pageurl = "http://ocw.mit.edu";

	public static void main(String[] args) throws IOException {
		Main m = new Main();
		List<Course> courses = m.getCourses();
		new Thread(new Downer(courses)).start();
	}

	private Course getCourse(Element e) throws IOException {
		Course c = new Course();
		Elements es = e.getElementsByTag("a");
		String id = es.get(0).html();
		String url = pageurl + es.get(0).attr("href");
		String name = es.get(1).html();
		c.setId(id);
		c.setName(name);
		c.setUrl(url);
		Document doc = Jsoup.connect(url).get();
		Iterator<Element> fiter = doc.getElementsByClass("specialfeatures").first().getElementsByTag("li").iterator();
		List<Feature> features = new ArrayList<Feature>();
		while (fiter.hasNext()) {
			Element element = (Element) fiter.next();
			Element details = element.getElementsByTag("a").first();
			Feature f = new Feature();
			f.setName(details.html());
			f.setUrl(pageurl + details.attr("href"));
			features.add(f);
		}
		c.setFeatures(features);
		if (c.hasVideo()) {
			String videourl = c.getVideoUrl();
			Document videodoc = Jsoup.connect(c.getVideoUrl()).get();
			if (videourl.contains("videos") || videourl.contains("video")) {
				Iterator<Element> mediasit = videodoc.getElementsByClass("medialisting").iterator();
				List<Video> videos = new ArrayList<Video>();
				while (mediasit.hasNext()) {
					Element element = mediasit.next();
					Element link = element.getElementsByTag("a").first();
					Video video = new Video();
					String downpage = pageurl + link.attr("href");
					String title = link.attr("title");
					String downurl = getDownUrl(downpage, false);
					video.setName(title);
					video.setUrl(downurl);
					videos.add(video);
				}
				c.setVideos(videos);
				c.setBatch(false);
			} else {
				String downurl = getDownUrl(videourl, true);
				c.setDownurl(downurl.substring(0, downurl.lastIndexOf("/")));
				c.setBatch(true);
			}

		}
		return c;
	}

	private String getDownUrl(String downpage, boolean isbatcth) {

		Document videodoc;
		try {
			videodoc = Jsoup.connect(downpage).get();
			if (isbatcth) {
				String s = videodoc.html();
				Pattern p = Pattern.compile("http://www.archive.org/[/.-_\\w]*.mp4|http://archive.org/[/.-_\\w]*.mp4");
				Matcher m = p.matcher(s);
				if (m.find()) {
					return m.group(0);
				}
			} else {
				Element e = videodoc.getElementsContainingOwnText("Internet Archive").first();
				return e.attr("href");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<Course> getCourses() throws IOException {
		List<Course> courses = new ArrayList<Course>();
		File input = new File("urls.html");
		Document doc = Jsoup.parse(input, "UTF-8");
		Element courseList = doc.getElementById("courseList");
		Iterator<Element> eles = courseList.getElementsByTag("tr").iterator();
		while (eles.hasNext()) {
			Element e = eles.next();
			if (isCourse(e)) {
				Course c = getCourse(e);
				courses.add(c);
			}
		}
		return courses;
	}

	private static class Downer implements Runnable {
		List<Course> courses;
		private String downhome = "mitopen/";

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
				Course course = (Course) cit.next();
				File details = new File(downhome + course.getName() + c + "/" + course.getName() + ".html");
				if (course.isBatch()) {
//					System.out.println(course.getDownurl());
//					Document doc = Jsoup.connect(course.getDownurl()).get();
//					Elements els = doc.getElementsByTag("a");
//					int i = 1;
//					Iterator<Element> it = els.iterator();
//					while (it.hasNext()) {
//						Element e = it.next();
//						if (e.html() == "..")
//							continue;
//						String downurl = course.getDownurl() + '/' + e.attr("href");
//						URL source = new URL(downurl);
//						File destination;
//						if (downurl.contains(".mp4") || downurl.contains(".srt")) {
//							if (downurl.contains(".mp4")) {
//								destination = new File(
//										downhome + course.getName() + c + "/videos/lecture: " + i + ".mp4");
//								if (!destination.exists()) {
//									try {
//										FileUtils.copyURLToFile(source, destination);
//										System.out.println(
//												"download " + destination.getName() + c + " from " + source.getFile());
//									} catch (Exception e2) {
//										System.out.println("fail download " + destination.getName() + c + " from "
//												+ source.getFile());
//									}
//								}
//
//							} else {
//								destination = new File(
//										downhome + course.getName() + c + "/" + "subtitles/lecture: " + i + ".srt");
//								if (!destination.exists()) {
//									try {
//										FileUtils.copyURLToFile(source, destination);
//										System.out.println(
//												"download " + destination.getName() + " from " + source.getFile());
//									} catch (Exception e2) {
//										System.out.println("fail download " + destination.getName() + c + " from "
//												+ source.getFile());
//									}
//								}
//								i++;
//							}
//						}
//					}
				} else {
					List<Video> videos = course.getVideos();
					if (videos == null)
						continue;
					Iterator<Video> vit = videos.iterator();
					while (vit.hasNext()) {
						Video video = (Video) vit.next();
						try {
							String vurl = video.getUrl();
							System.out.println(vurl);
							String srturl = vurl.substring(0, vurl.lastIndexOf(".")) + ".srt";
							URL vsource = new URL(vurl);
							File vdestination = new File(
									downhome + course.getName() + c + "/" + "videos/" + video.getName() + ".mp4");
							URL srtsource = new URL(srturl);
							File srtdestination = new File(
									downhome + course.getName() + c + "/" + "subtitles/" + video.getName() + ".srt");
							if (!vdestination.exists()) {
								exs.submit(new OneDown(vsource, vdestination));
								
							}
							if (!srtdestination.exists()) {
								exs.submit(new OneDown(srtsource, srtdestination));
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
		if (e.getElementsByTag("td").size() == 3 && e.getElementsByTag("a").size() == 3)
			return true;
		else
			return false;
	}

}
