/********************************
 *	프로젝트 : stockdaily
 *	패키지   : com.kyj.stockdaily
 *	작성일   : 2021. 12. 23.
 *	작성자   : KYJ (callakrsos@naver.com)
 *******************************/
package com.kyj.stockdaily;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * @author KYJ (callakrsos@naver.com)
 *
 */
public class RealdTimeStock {
	static File tmp = new File("stock.tmp");

	/**
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 23.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		System.out.println(currentDate());
		String content = "";
		if (!tmp.exists()) {
			content = fetchData(tmp);
		} else {
			// 파일을 다시 다운받아야하는지 확인
			boolean downLoad = isDownLoad();
			if (downLoad)
				content = fetchData(tmp);
			else
				content = Files.readString(tmp.toPath());
		}

		Gson gson = new Gson();
		HashMap fromJson = gson.fromJson(content, HashMap.class);
		List items = (List) fromJson.get("OutBlock_1");
		String dataTime = (String) fromJson.get("CURRENT_DATETIME");

		for (Object item : items) {
			System.out.println(item);
		}
		System.out.println("데이터 시간 : " + dataTime);
	}

	/**
	 * 현재 시간 리턴 <br/>
	 * 
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 23.
	 * @return
	 */
	static String currentDate() {
		return new SimpleDateFormat("yyyyMMdd").format(new Date());
	}

	/**
	 * 파일 날짜가 현재 시간보다 20차이가 넘는다면 다시 받는다.
	 * 
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 23.
	 * @return
	 */
	static boolean isDownLoad() {
		long lastModified = tmp.lastModified();

		Date fileDate = new Date(lastModified);
		// Date currentDate = new Date();

		Calendar instance = Calendar.getInstance();
		instance.setTime(fileDate);
		instance.add(Calendar.MINUTE, 20);
		return instance.after(new Date());
	}

	/**
	 * 서버에서 데이터를 받는다.
	 * 
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 23.
	 * @param tmp
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private static String fetchData(File tmp) throws MalformedURLException, IOException, UnsupportedEncodingException {
		String content;
		String url = "http://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd";
		URL u = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();

		conn.setDefaultUseCaches(true);
		conn.setUseCaches(true);

		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0");
		conn.setRequestProperty("Accept-Encoding", "UTF-8");
		// conn.setRequestProperty("Connection", "keep-alive");

		// conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Accept-Charset", "UTF-8");
		conn.setRequestProperty("Accept-Encoding", "UTF-8");
		conn.setRequestProperty("Accept-Language", "KR");

		conn.setDoOutput(true);
		String currentDate = currentDate();
		conn.getOutputStream().write(String
				.format("bld=dbms/MDC/STAT/standard/MDCSTAT01501&mktId=ALL&trdDd=%s", currentDate, "&share=1&money=1&csvxls_isNo=false")
				.getBytes("utf-8"));

		// conn.connect();

		int responseCode = conn.getResponseCode();
		System.out.println(responseCode);
		System.out.println(conn.getResponseMessage());
		if (responseCode == 200) {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

				try (InputStream inputStream = conn.getInputStream()) {
					String r = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining());
					Gson create = new GsonBuilder().setPrettyPrinting().create();
					JsonElement parse = new JsonParser().parse(r);
					String json = create.toJson(parse);
					writeTempFile(json);
					content = json;
				}
			}
		} else
			content = "";
		return content;
	}

	/**
	 * 임시 파일에 정보를 저장.
	 * 
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 23.
	 * @param tmp
	 * @param json
	 * @throws IOException
	 */
	private static void writeTempFile(String json) throws IOException {
		Files.writeString(tmp.toPath(), json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
	}

}
