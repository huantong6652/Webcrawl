import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class Httpclient {
	private static HttpURLConnection connection = null;

	public static String httpRequest(String url){
		String content = "";
		try{
			URL u = new URL(url);
			connection = (HttpURLConnection)u.openConnection();
			connection.setRequestMethod("GET");
			InputStream in = connection.getInputStream();
			InputStreamReader isr = new InputStreamReader(in,"utf-8");
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			while((line = reader.readLine()) != null){
				content += line;
			}
		}catch(MalformedURLException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(connection != null){
				connection.disconnect();
			}
		}
		return content;
	}

	// handle response code
	public static int ResponseCode(String url){
		int code = 0;
		try{
			URL u = new URL(url);
			connection = (HttpURLConnection)u.openConnection();
			// set false，otherwise redirect to Location address
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			code = connection.getResponseCode();
		}catch(MalformedURLException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(connection != null){
				connection.disconnect();
			}
		}
//        System.out.println(code);
		return code;
	}

	// retrevie redirect address
	public static String reLocation(String url) throws IOException {
		URL u = new URL(url);
		connection = (HttpURLConnection)u.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("accept", "*/*");
		connection.setRequestProperty("connection", "Keep-Alive");
		// set false，otherwise redirect to Location address
		connection.setInstanceFollowRedirects(false);
		// get Location address
		String location = connection.getHeaderField("Location");
		return location;
	}

	// retreive html a tag
	private static List<String> findMoreURL(String html){
		String str;
		List<String> urlList =new ArrayList<>();

		Pattern pattern_a = compile("(?s)<a.*?href=\"(.*?)\".*?>.*?</a>");
		Matcher matcher_a = pattern_a.matcher(html);
		while (matcher_a.find()) {
			str = matcher_a.group(1);
			if(str.startsWith("http")){
				urlList.add(str);

			}
		}
		return urlList;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		int maxNum = Integer.parseInt(args[1]);
		String Url = args[0];

		String address;
		int count = 0;
		int code;

		Set<String> st = new HashSet<>();
		List<String> ops = new ArrayList<>();
		ops.add(Url);

		while(count < maxNum){
			if(ops == null || ops.size() == 0) {
				System.out.println("end");
				break;
			}
			for(String op:ops){
				if(st.contains(op) ) continue;// check duplication
				code = ResponseCode(op);
				st.add(op);
				if(code == 500){
					System.out.println("Error from " + op + "  Response Code 500");
					for(int i = 1;i <= 3;i++){
						TimeUnit.MILLISECONDS.sleep(1500);
						System.out.println("Retry "+i+": ");
						code = ResponseCode(op);
						if (code == 500)  System.out.println("Error from " + op + "  Response Code 500");
						else break;
					}
				}
				if(code == 301){
					address = op;
					op = reLocation(op);
					System.out.println("Redirecting from "+address+" to "+op);
					code = ResponseCode(op);
					st.add(op);
				}
				if(code == 200){
					count++;
					System.out.println("hop"+count+": "+op);
					ops = findMoreURL(httpRequest(op));// update ops
					break;
				}
				else if(code == 404){
					System.out.println("Error from " + op + "  Response Code 404");
				}
			}
		}
	}
}

