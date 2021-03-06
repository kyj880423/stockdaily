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
import java.io.FileInputStream;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

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

		// setPrettyPrinting.registerTypeAdapter(java.lang.String.class, new FormatStringTypeAdapter());

		Gson gson = new Gson();
		HashMap fromJson = gson.fromJson(content, HashMap.class);
		List items = (List) fromJson.get("OutBlock_1");
		String dataTime = (String) fromJson.get("CURRENT_DATETIME");
		dataTime = convertDateFormat(dataTime);
		boolean dataExists = dataExists(dataTime);
		for (Object item : items) {
			System.out.println(item);
			Map m = (Map) item;
			String 코드 = (String) m.get("ISU_SRT_CD");
			String 종가 = (String) m.get("TDD_CLSPRC");
			String 시작가 = (String) m.get("TDD_OPNPRC");
			String 거래량 = (String) m.get("ACC_TRDVOL");
			System.out.println(String.format("시작가 : %s, 종가 :  %s, 거래량 : %s", 시작가, 종가, 거래량));

			if (!dataExists)
				insertData(dataTime, 코드, gson.toJson(m));
		}
		// insertData(dataTime, "x", content);

		// File outXml = new File("out.xml");
		// Files.writeString(outXml.toPath(), dataTime, null)
		// StringWriter w = new StringWriter();
		// try (FileWriter fileWriter = new FileWriter(outXml)) {
		// new XmlMapper().writeValue(fileWriter, fromJson);
		// };

		// GsonXmlBuilder builder = new GsonXmlBuilder();
		// GsonXml create = builder.create();

		System.out.println("데이터 시간 : " + dataTime);
	}

	/**
	 * 서버에서 받은 날짜 포멧을 다른 포멧으로 변경 <br/>
	 *  2021.12.24 PM 02:10:55 -> 2021.12.24 14:10:55 <br/>
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 24. 
	 * @param d
	 * @return
	 * @throws ParseException
	 */
	private static String convertDateFormat(String d) throws ParseException {
		String date = d.substring(0, 10);
		// System.out.println(date);
		String form = d.substring(11, 13);
		// System.out.println(form);
		String tm = d.substring(14);
		// System.out.println(tm);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
		Date parse = simpleDateFormat.parse(tm);
		Calendar timeInstance = Calendar.getInstance();
		timeInstance.setTime(parse);
		if ("PM".equals(form))
			timeInstance.add(Calendar.HOUR_OF_DAY, 12);
		System.out.println(timeInstance.getTime());

		String ret = date + " " + new SimpleDateFormat("HH:mm:ss").format(timeInstance.getTime());
		System.out.println(ret);

		return ret;
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
		// TimeUnit.MINUTES.convert(instance.getTime().getTime());
		long abs = Math.abs(instance.getTime().getTime() - new Date().getTime());
		long convert = TimeUnit.MINUTES.convert(abs, TimeUnit.MILLISECONDS);
		System.out.println("diff minute: " + convert);
		return convert > 20;
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
				String r = "";

				try (InputStream inputStream = conn.getInputStream()) {
					r = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining());
				}

				GsonBuilder setPrettyPrinting = new GsonBuilder().setPrettyPrinting();
				Gson create = setPrettyPrinting.create();
				JsonParser parse = new JsonParser();
				JsonElement je = parse.parse(r);
				String json = create.toJson(je);
				writeTempFile(json);

				content = json;
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
		Files.deleteIfExists(tmp.toPath());
		Files.writeString(tmp.toPath(), json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
	}

	/**
	 * @작성자 : KYJ (callakrsos@naver.com)
	 * @작성일 : 2021. 12. 24. 
	 * @param date
	 * @param code
	 * @param json
	 * @return
	 * @throws Exception
	 */
	static int insertData(String date, String code, String json) throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File("sys.properties")));

		String dbUrl = properties.getProperty("db.url");
		String dbUserName = properties.getProperty("db.username");
		String dbUserPwd = properties.getProperty("db.userpwd");

		try (Connection con = DriverManager.getConnection(dbUrl, dbUserName, dbUserPwd)) {
			String sql = "insert into json_table ( id, code, stock_json_data, fstRegDt )  values(?,?,?,?)";
			PreparedStatement statement2 = con.prepareStatement(sql);
			statement2.setString(1, date);
			statement2.setString(2, code);
			statement2.setString(3, json);
			// statement2.setObject(4, new java.sql.Timestamp(System.currentTimeMillis()));
			statement2.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
			return statement2.executeUpdate();
		}
	}

	static boolean dataExists(String date) throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File("sys.properties")));

		String dbUrl = properties.getProperty("db.url");
		String dbUserName = properties.getProperty("db.username");
		String dbUserPwd = properties.getProperty("db.userpwd");

		try (Connection con = DriverManager.getConnection(dbUrl, dbUserName, dbUserPwd)) {
			PreparedStatement statement = con.prepareStatement("select 1 from json_table where 1=1 and id = ? limit 1 ");
			statement.setFetchSize(1);
			statement.setString(1, date);
			ResultSet rs = statement.executeQuery();
			return rs.next();
		}
	}
}
